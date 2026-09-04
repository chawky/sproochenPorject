package com.nailic.sproochencoach.service;

import com.nailic.sproochencoach.exceptions.AiProviderException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AiChatClient {
    private static final Logger log = LoggerFactory.getLogger(AiChatClient.class);

    @Value("${ai.chat.temperature}")
    private double temperature;

    @Value("${ai.openrouter.completion-uri}")
    private String openRouterCompletionUri;

    @Value("${ai.kimi.messages-uri}")
    private String kimiMessagesUri;

    @Value("${ai.kimi.max-tokens}")
    private int kimiMaxTokens;

    @Value("${ai.kimi.thinking-enabled}")
    private boolean kimiThinkingEnabled;

    @Value("${ai.kimi.thinking-budget-tokens}")
    private int kimiThinkingBudgetTokens;

    @Value("${ai.system.content}")
    private String systemContent;

    private final RestClient openRouterRestClient;
    private final RestClient anthropicRestClient;
    private final ObjectMapper objectMapper;
    private final AiUsageService aiUsageService;
    private final AiModelRouter aiModelRouter;

    public AiChatClient(
            @Qualifier("openRouterRestClient") RestClient openRouterRestClient,
            @Qualifier("anthropicRestClient") RestClient anthropicRestClient,
            ObjectMapper objectMapper,
            AiUsageService aiUsageService,
            AiModelRouter aiModelRouter
    ) {
        this.openRouterRestClient = openRouterRestClient;
        this.anthropicRestClient = anthropicRestClient;
        this.objectMapper = objectMapper;
        this.aiUsageService = aiUsageService;
        this.aiModelRouter = aiModelRouter;
    }

    public String complete(String userPrompt, String requestName) {
        AiModelRoute route = aiModelRouter.currentUserRoute();

        return switch (route.provider().toLowerCase(Locale.ROOT)) {
            case "openrouter" -> completeWithOpenRouter(userPrompt, requestName, route.model());
            case "kimi" -> completeWithKimi(userPrompt, requestName, route.model());
            default -> throw new AiProviderException(
                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    "Unsupported AI provider: " + route.provider()
            );
        };
    }

    private String completeWithOpenRouter(String userPrompt, String requestName, String model) {
        log.info("AI request. provider={}, model={}, request={}", "openrouter", model, requestName);

        Map<String, Object> requestBody = Map.of(
                "model", model,
                "temperature", temperature,
                "messages", List.of(
                        Map.of(
                                "role", "system",
                                "content", systemContent
                        ),
                        Map.of(
                                "role", "user",
                                "content", userPrompt
                        )
                )
        );

        String responseBody = openRouterRestClient.post()
                .uri(openRouterCompletionUri)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(requestBody)
                .retrieve()
                .onStatus(
                        HttpStatusCode::isError,
                        (request, response) -> handleError("OpenRouter", response.getStatusCode(), response.getBody().readAllBytes(), requestName)
                )
                .body(String.class);

        Map<?, ?> responseMap = readResponseMap(responseBody, "OpenRouter", requestName);
        String content = extractOpenRouterText(responseMap, requestName);
        TokenUsage tokenUsage = extractOpenRouterTokenUsage(responseMap);
        recordTokenUsage("openrouter", model, requestName, tokenUsage);

        return content;
    }

    private String completeWithKimi(String userPrompt, String requestName, String model) {
        log.info("AI request. provider={}, model={}, request={}", "kimi", model, requestName);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", model);
        requestBody.put("max_tokens", kimiMaxTokens);
        requestBody.put("temperature", temperature);
        requestBody.put("system", systemContent);
        requestBody.put("messages", List.of(
                Map.of(
                        "role", "user",
                        "content", userPrompt
                )
        ));

        if (kimiThinkingEnabled) {
            requestBody.put(
                    "thinking",
                    Map.of(
                            "type", "enabled",
                            "budget_tokens", kimiThinkingBudgetTokens
                    )
            );
        } else {
            requestBody.put("thinking", Map.of("type", "disabled"));
        }

        String responseBody = anthropicRestClient.post()
                .uri(kimiMessagesUri)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(requestBody)
                .retrieve()
                .onStatus(
                        HttpStatusCode::isError,
                        (request, response) -> handleError("Kimi", response.getStatusCode(), response.getBody().readAllBytes(), requestName)
                )
                .body(String.class);

        Map<?, ?> responseMap = readResponseMap(responseBody, "Kimi", requestName);
        String content = extractAnthropicText(responseMap, requestName);
        TokenUsage tokenUsage = extractAnthropicTokenUsage(responseMap);
        recordTokenUsage("kimi", model, requestName, tokenUsage);

        return content;
    }

    private void handleError(String providerName, HttpStatusCode statusCode, byte[] responseBody, String requestName) {
        String providerMessage = providerName + " returned an unknown error";
        String providerErrorType = null;

        try {
            Map<?, ?> responseMap = objectMapper.readValue(
                    new String(responseBody, StandardCharsets.UTF_8),
                    Map.class
            );

            Object errorObject = responseMap.get("error");
            if (errorObject instanceof Map<?, ?> errorMap) {
                Object messageObject = errorMap.get("message");
                Object typeObject = errorMap.get("type");
                Object codeObject = errorMap.get("code");

                if (messageObject instanceof String message && StringUtils.hasText(message)) {
                    providerMessage = message;
                }

                if (typeObject instanceof String type && StringUtils.hasText(type)) {
                    providerErrorType = type;
                } else if (codeObject != null) {
                    providerErrorType = codeObject.toString();
                }
            }
        } catch (JacksonException exception) {
            providerErrorType = "unreadable";
        }

        log.error("{} returned error while processing {}. status={}, type={}, message={}", providerName, requestName, statusCode.value(), providerErrorType, providerMessage);

        throw new AiProviderException(statusCode.value(), providerMessage);
    }

    private String extractOpenRouterText(Map<?, ?> responseMap, String requestName) {
        Object choicesObject = responseMap.get("choices");
        if (!(choicesObject instanceof List<?> choices) || choices.isEmpty()) {
            throw invalidResponse("OpenRouter", requestName, "choices are missing");
        }

        Object firstChoiceObject = choices.get(0);
        if (!(firstChoiceObject instanceof Map<?, ?> firstChoice)) {
            throw invalidResponse("OpenRouter", requestName, "first choice is invalid");
        }

        Object messageObject = firstChoice.get("message");
        if (!(messageObject instanceof Map<?, ?> message)) {
            throw invalidResponse("OpenRouter", requestName, "message is missing");
        }

        Object contentObject = message.get("content");
        if (!(contentObject instanceof String content) || !StringUtils.hasText(content)) {
            throw invalidResponse("OpenRouter", requestName, "message content is missing");
        }

        return content;
    }

    private String extractAnthropicText(Map<?, ?> responseMap, String requestName) {
        Object contentObject = responseMap.get("content");
        if (!(contentObject instanceof List<?> contentBlocks) || contentBlocks.isEmpty()) {
            throw invalidResponse("Kimi", requestName, "content blocks are missing");
        }

        StringBuilder textBuilder = new StringBuilder();
        for (Object contentBlockObject : contentBlocks) {
            if (contentBlockObject instanceof Map<?, ?> contentBlock) {
                Object textObject = contentBlock.get("text");
                if (textObject instanceof String text && StringUtils.hasText(text)) {
                    textBuilder.append(text);
                }
            }
        }

        if (!StringUtils.hasText(textBuilder.toString())) {
            log.error("Kimi returned no text for {}. stopReason={}, contentBlockTypes={}", requestName, responseMap.get("stop_reason"), contentBlockTypes(contentBlocks));
            throw invalidResponse("Kimi", requestName, "text content is missing");
        }

        return textBuilder.toString();
    }

    private TokenUsage extractOpenRouterTokenUsage(Map<?, ?> responseMap) {
        Object usageObject = responseMap.get("usage");
        if (!(usageObject instanceof Map<?, ?> usageMap)) {
            return TokenUsage.empty();
        }

        Integer inputTokens = integerValue(usageMap.get("prompt_tokens"));
        Integer outputTokens = integerValue(usageMap.get("completion_tokens"));
        Integer totalTokens = integerValue(usageMap.get("total_tokens"));

        return new TokenUsage(inputTokens, outputTokens, totalTokens(inputTokens, outputTokens, totalTokens));
    }

    private TokenUsage extractAnthropicTokenUsage(Map<?, ?> responseMap) {
        Object usageObject = responseMap.get("usage");
        if (!(usageObject instanceof Map<?, ?> usageMap)) {
            return TokenUsage.empty();
        }

        Integer inputTokens = integerValue(usageMap.get("input_tokens"));
        Integer outputTokens = integerValue(usageMap.get("output_tokens"));

        return new TokenUsage(inputTokens, outputTokens, totalTokens(inputTokens, outputTokens, null));
    }

    private void recordTokenUsage(String provider, String model, String requestName, TokenUsage tokenUsage) {
        aiUsageService.recordChatUsage(
                provider,
                model,
                requestName,
                tokenUsage.inputTokens(),
                tokenUsage.outputTokens(),
                tokenUsage.totalTokens()
        );
    }

    private Integer integerValue(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }

        return null;
    }

    private Integer totalTokens(Integer inputTokens, Integer outputTokens, Integer providerTotalTokens) {
        if (providerTotalTokens != null) {
            return providerTotalTokens;
        }

        if (inputTokens == null && outputTokens == null) {
            return null;
        }

        int input = inputTokens == null ? 0 : inputTokens;
        int output = outputTokens == null ? 0 : outputTokens;
        return input + output;
    }

    private String contentBlockTypes(List<?> contentBlocks) {
        return contentBlocks.stream()
                .map(contentBlockObject -> {
                    if (contentBlockObject instanceof Map<?, ?> contentBlock) {
                        Object typeObject = contentBlock.get("type");
                        return typeObject == null ? "unknown" : typeObject.toString();
                    }
                    return "invalid";
                })
                .collect(Collectors.joining(","));
    }

    private Map<?, ?> readResponseMap(String responseBody, String providerName, String requestName) {
        if (!StringUtils.hasText(responseBody)) {
            throw invalidResponse(providerName, requestName, "response body is empty");
        }

        try {
            return objectMapper.readValue(responseBody, Map.class);
        } catch (JacksonException exception) {
            throw invalidResponse(providerName, requestName, "response JSON could not be parsed");
        }
    }

    private AiProviderException invalidResponse(String providerName, String requestName, String reason) {
        log.error("{} returned invalid {} response structure. reason={}", providerName, requestName, reason);
        return new AiProviderException(
                HttpStatus.BAD_GATEWAY.value(),
                providerName + " returned an invalid response: " + reason
        );
    }

    private record TokenUsage(Integer inputTokens, Integer outputTokens, Integer totalTokens) {
        private static TokenUsage empty() {
            return new TokenUsage(null, null, null);
        }
    }
}
