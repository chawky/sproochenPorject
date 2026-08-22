package com.nailic.sproochencoach.service;

import com.nailic.sproochencoach.dto.AudioExerciseDto;
import com.nailic.sproochencoach.dto.ExerciseRequestDto;
import com.nailic.sproochencoach.dto.TtsRequest;
import com.nailic.sproochencoach.exceptions.OpenRouterError;
import com.nailic.sproochencoach.model.AIRoleEnum;
import com.nailic.sproochencoach.model.AiBody;
import com.nailic.sproochencoach.model.MessageBody;
import com.nailic.sproochencoach.model.OpenRouterResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
public class AudioExerciseGenerationService {
    private static final Logger log = LoggerFactory.getLogger(AudioExerciseGenerationService.class);

    @Value("${ai.openrouter.free.model}")
    private String openrouterFreeModel;

    @Value("${ai.completion.uri}")
    private String completionURI;

    @Value("${ai.system.content}")
    private String systemContent;

    @Value("${ai.elevenlabs.voice-id}")
    private String voiceId;

    private final RestClient openRouterRestClient;
    private final RestClient ttsRestClient;
    private final ObjectMapper objectMapper;

    public AudioExerciseGenerationService(
            @Qualifier("openRouterRestClient") RestClient openRouterRestClient,
            @Qualifier("ttsRestClient") RestClient ttsRestClient,
            ObjectMapper objectMapper
    ) {
        this.openRouterRestClient = openRouterRestClient;
        this.ttsRestClient = ttsRestClient;
        this.objectMapper = objectMapper;
    }

    public <T extends AudioExerciseDto> T generateAudioExercise(
            ExerciseRequestDto exerciseRequestDto,
            String promptTemplate,
            Class<T> responseType,
            String exerciseName
    ) {
        log.debug(
                "Generating {}. level={}, topic={}, type={}",
                exerciseName,
                exerciseRequestDto.getLevel(),
                exerciseRequestDto.getTopic(),
                exerciseRequestDto.getType()
        );

        AiBody aiBody = new AiBody();
        aiBody.setModel(openrouterFreeModel);

        MessageBody systemMessageBody = new MessageBody();
        systemMessageBody.setRole(AIRoleEnum.SYSTEM);
        systemMessageBody.setContent(systemContent);

        MessageBody userMessageBody = new MessageBody();
        userMessageBody.setRole(AIRoleEnum.USER);
        userMessageBody.setContent(
                promptTemplate.formatted(
                        exerciseRequestDto.getLevel(),
                        exerciseRequestDto.getTopic(),
                        exerciseRequestDto.getType()
                )
        );

        aiBody.setMessages(
                List.of(systemMessageBody, userMessageBody)
        );

        OpenRouterResponse openRouterResponse = openRouterRestClient.post()
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
                                log.error("OpenRouter returned unknown error while generating {}. status={}", exerciseName, response.getStatusCode().value());
                                throw new OpenRouterError(
                                        response.getStatusCode().value(),
                                        "OpenRouter returned an unknown error"
                                );
                            }

                            log.error(
                                    "OpenRouter returned error while generating {}. code={}, message={}",
                                    exerciseName,
                                    errorResponse.getError().getCode(),
                                    errorResponse.getError().getMessage()
                            );

                            throw new OpenRouterError(
                                    errorResponse.getError().getCode(),
                                    errorResponse.getError().getMessage()
                            );
                        }
                )
                .body(OpenRouterResponse.class);

        String invalidReason = OpenRouterResponseValidator.invalidReason(openRouterResponse);
        if (invalidReason != null) {

            log.error("OpenRouter returned invalid {} response structure. reason={}", exerciseName, invalidReason);

            throw new OpenRouterError(
                    HttpStatus.BAD_GATEWAY.value(),
                    "OpenRouter returned an invalid response: " + invalidReason
            );
        }

        String content = openRouterResponse
                .getChoices()
                .get(0)
                .getMessage()
                .getContent();

        log.debug("OpenRouter {} response: {}", exerciseName, content);

        try {
            T result = objectMapper.readValue(
                    content,
                    responseType
            );

            log.debug("{} JSON parsed successfully. questionLength={}", exerciseName, result.getQuestion() == null ? 0 : result.getQuestion().length());

            TtsRequest ttsRequest = new TtsRequest(
                    result.getQuestion(),
                    "eleven_multilingual_v2"
            );

            byte[] audio;
            try {
                audio = ttsRestClient.post()
                        .uri("/text-to-speech/" + voiceId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.valueOf("audio/mpeg"))
                        .body(ttsRequest)
                        .retrieve()
                        .body(byte[].class);
            } catch (RuntimeException exception) {
                log.error("ElevenLabs TTS request failed while generating {}", exerciseName, exception);
                throw exception;
            }

            result.setAudio(audio);
            log.debug("{} audio generated successfully. audioBytes={}", exerciseName, audio == null ? 0 : audio.length);
            return result;
        } catch (JacksonException exception) {
            log.error(
                    "Failed to parse OpenRouter {} JSON. jacksonMessage={}, aiReturned={}",
                    exerciseName,
                    exception.getMessage(),
                    content,
                    exception
            );

            throw new OpenRouterError(
                    HttpStatus.BAD_GATEWAY.value(),
                    "OpenRouter returned invalid " + exerciseName + " JSON"
            );
        }
    }
}
