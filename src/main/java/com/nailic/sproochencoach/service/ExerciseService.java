package com.nailic.sproochencoach.service;

import com.nailic.sproochencoach.dto.ExerciseRequestDto;
import com.nailic.sproochencoach.dto.GeneratedExerciseDto;
import com.nailic.sproochencoach.exceptions.OpenRouterError;
import com.nailic.sproochencoach.model.AIRoleEnum;
import com.nailic.sproochencoach.model.AiBody;
import com.nailic.sproochencoach.model.MessageBody;
import com.nailic.sproochencoach.model.OpenRouterResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

@Service
public class ExerciseService {

    @Value("${ai.openrouter.free.model}")
    private String openrouterFreeModel;

    @Value("${ai.openrouter.paid.model}")
    private String openrouterPaidModel;

    @Value("${ai.completion.uri}")
    private String completionURI;

    @Value("${ai.system.content}")
    private String systemContent;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public ExerciseService(
            @Qualifier("openRouterRestClient") RestClient restClient,
            ObjectMapper objectMapper
    ) {
        this.restClient = restClient;
        this.objectMapper = objectMapper;
    }

    public GeneratedExerciseDto generateExercise(ExerciseRequestDto exerciseRequestDto) {

        AiBody aiBody = new AiBody();
        aiBody.setModel(openrouterFreeModel);

        MessageBody systemMessageBody = new MessageBody();
        systemMessageBody.setRole(AIRoleEnum.SYSTEM);
        systemMessageBody.setContent(systemContent);

        MessageBody userMessageBody = new MessageBody();
        userMessageBody.setRole(AIRoleEnum.USER);
        userMessageBody.setContent("""
                Generate exactly ONE %s exercise
                for level %s
                about the topic %s.
                
                Make the exercise noticeably different each time.
                Vary the vocabulary, sentence structure, verbs, time expressions,
                and situation used in the exercise.
                Avoid always using the most obvious examples for this topic.
                
                Return ONLY valid JSON.
                
                Use exactly this structure:
                {
                  "question": "...",
                  "type": "%s",
                  "options": [],
                  "expectedAnswer": "...",
                  "hint": "..."
                }
                
                Rules:
                - Do not include markdown.
                - Do not include headings.
                - Do not generate multiple exercises.
                - Keep the exercise appropriate for the requested level.
                """
                .formatted(
                        exerciseRequestDto.getType(),
                        exerciseRequestDto.getLevel(),
                        exerciseRequestDto.getTopic(),
                        exerciseRequestDto.getType()
                ));

        aiBody.setMessages(
                List.of(systemMessageBody, userMessageBody)
        );

        OpenRouterResponse openRouterResponse = restClient.post()
                .uri(completionURI)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(aiBody)
                .retrieve()
                .onStatus(
                        HttpStatusCode::isError,
                        (request, response) -> {

                            OpenRouterResponse errorResponse =
                                    objectMapper.readValue(
                                            response.getBody(),
                                            OpenRouterResponse.class
                                    );

                            if (errorResponse.getError() == null) {
                                throw new OpenRouterError(
                                        response.getStatusCode().value(),
                                        "OpenRouter returned an unknown error"
                                );
                            }

                            throw new OpenRouterError(
                                    errorResponse.getError().getCode(),
                                    errorResponse.getError().getMessage()
                            );
                        }
                )
                .body(OpenRouterResponse.class);

        if (openRouterResponse == null
                || openRouterResponse.getChoices() == null
                || openRouterResponse.getChoices().isEmpty()
                || openRouterResponse.getChoices().get(0).getMessage() == null
                || openRouterResponse.getChoices().get(0).getMessage().getContent() == null) {

            throw new OpenRouterError(
                    HttpStatus.BAD_GATEWAY.value(),
                    "OpenRouter returned an invalid response"
            );
        }

        String content = openRouterResponse
                .getChoices()
                .get(0)
                .getMessage()
                .getContent();

        try {
            return objectMapper.readValue(
                    content,
                    GeneratedExerciseDto.class
            );
        } catch (JacksonException exception) {
            throw new OpenRouterError(
                    HttpStatus.BAD_GATEWAY.value(),
                    "OpenRouter returned invalid exercise JSON"
            );
        }
    }
}
