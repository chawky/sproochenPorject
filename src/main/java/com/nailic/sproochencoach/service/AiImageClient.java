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
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class AiImageClient {
    private static final Logger log = LoggerFactory.getLogger(AiImageClient.class);

    @Value("${ai.chat.provider}")
    private String provider;

    @Value("${ai.openrouter.image-uri}")
    private String openRouterImageUri;

    @Value("${ai.openrouter.image-model}")
    private String openRouterImageModel;

    @Value("${ai.kimi.image-uri}")
    private String kimiImageUri;

    @Value("${ai.kimi.image-model}")
    private String kimiImageModel;

    private final RestClient openRouterRestClient;
    private final RestClient kimiImageRestClient;
    private final ObjectMapper objectMapper;

    public AiImageClient(
            @Qualifier("openRouterRestClient") RestClient openRouterRestClient,
            @Qualifier("kimiImageGenerationRestClient") RestClient kimiImageRestClient,
            ObjectMapper objectMapper
    ) {
        this.openRouterRestClient = openRouterRestClient;
        this.kimiImageRestClient = kimiImageRestClient;
        this.objectMapper = objectMapper;
    }

    public byte[] generateImage(String userPrompt) {
        return switch (provider.toLowerCase(Locale.ROOT)) {
            case "openrouter" -> generateImageWithOpenRouter(userPrompt);
            case "kimi" -> generateImageWithKimi(userPrompt);
            default -> throw new AiProviderException(
                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    "Unsupported AI provider: " + provider
            );
        };
    }

    private byte[] generateImageWithKimi(String userPrompt) {
        String requestName = "image generation";
        log.info("AI request. provider={}, model={}, request={}", "kimi-image", kimiImageModel, requestName);

        Map<String, Object> requestBody = Map.of(
                "model", kimiImageModel,
                "prompt", userPrompt,
                "n", 1
        );

        String responseBody = kimiImageRestClient.post()
                .uri(kimiImageUri)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(requestBody)
                .retrieve()
                .onStatus(
                        HttpStatusCode::isError,
                        (request, response) -> handleError("Kimi image", response.getStatusCode(), response.getBody().readAllBytes(), requestName)
                )
                .body(String.class);

        byte[] image = extractImageBytes(responseBody, "Kimi image", requestName);
        return image;
    }

    private byte[] generateImageWithOpenRouter(String userPrompt) {
        String requestName = "image generation";
        log.info("AI request. provider={}, model={}, request={}", "openrouter-image", openRouterImageModel, requestName);

        Map<String, Object> requestBody = Map.of(
                "model", openRouterImageModel,
                "prompt", userPrompt
        );

        String responseBody = openRouterRestClient.post()
                .uri(openRouterImageUri)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(requestBody)
                .retrieve()
                .onStatus(
                        HttpStatusCode::isError,
                        (request, response) -> handleError("OpenRouter image", response.getStatusCode(), response.getBody().readAllBytes(), requestName)
                )
                .body(String.class);

        byte[] image = extractImageBytes(responseBody, "OpenRouter image", requestName);
        return image;
    }

    private byte[] extractImageBytes(String responseBody, String providerName, String requestName) {
        Map<?, ?> responseMap = readResponseMap(responseBody, providerName, requestName);

        Object dataObject = responseMap.get("data");
        if (!(dataObject instanceof List<?> data) || data.isEmpty()) {
            throw invalidResponse(providerName, requestName, "image data is missing");
        }

        Object firstImageObject = data.get(0);
        if (!(firstImageObject instanceof Map<?, ?> firstImage)) {
            throw invalidResponse(providerName, requestName, "first image is invalid");
        }

        Object b64Object = firstImage.get("b64_json");
        if (!(b64Object instanceof String b64Json) || !StringUtils.hasText(b64Json)) {
            throw invalidResponse(providerName, requestName, "base64 image is missing");
        }

        try {
            return Base64.getDecoder().decode(b64Json);
        } catch (IllegalArgumentException exception) {
            throw invalidResponse(providerName, requestName, "base64 image is invalid");
        }
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
}
