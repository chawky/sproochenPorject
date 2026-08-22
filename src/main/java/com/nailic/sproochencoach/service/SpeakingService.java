package com.nailic.sproochencoach.service;

import com.nailic.sproochencoach.dto.ExerciseRequestDto;
import com.nailic.sproochencoach.dto.SpeakingDto;
import com.nailic.sproochencoach.dto.SpeakingEvaluation;
import com.nailic.sproochencoach.exceptions.OpenRouterError;
import com.nailic.sproochencoach.model.AIRoleEnum;
import com.nailic.sproochencoach.model.AiBody;
import com.nailic.sproochencoach.model.MessageBody;
import com.nailic.sproochencoach.model.OpenRouterResponse;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

@Service
public class SpeakingService {
    private static final Logger log = LoggerFactory.getLogger(SpeakingService.class);

    @Value("${ai.openrouter.free.model}")
    private String openrouterFreeModel;

    @Value("${ai.completion.uri}")
    private String completionURI;

    @Value("${ai.system.content}")
    private String systemContent;

    @Value("${ai.prompts.speaking-generation}")
    private Resource speakingGenerationPromptResource;

    @Value("${ai.prompts.speaking-evaluation}")
    private Resource speakingEvaluationPromptResource;

    @Value("${ai.prompts.transcription}")
    private Resource transcriptionPromptResource;

    private final RestClient openRouterRestClient;
    private final RestClient groqRestClient;
    private final ObjectMapper objectMapper;
    private final PromptFileService promptFileService;
    private final AudioExerciseGenerationService audioExerciseGenerationService;
    private String speakingGenerationPrompt;
    private String speakingEvaluationPrompt;
    private String transcriptionPrompt;

    public SpeakingService(
            @Qualifier("openRouterRestClient") RestClient openRouterRestClient,
            @Qualifier("groqRestClient") RestClient groqRestClient,
            ObjectMapper objectMapper,
            PromptFileService promptFileService,
            AudioExerciseGenerationService audioExerciseGenerationService
    ) {
        this.openRouterRestClient = openRouterRestClient;
        this.groqRestClient = groqRestClient;
        this.objectMapper = objectMapper;
        this.promptFileService = promptFileService;
        this.audioExerciseGenerationService = audioExerciseGenerationService;
    }

    @PostConstruct
    void loadPromptFiles() {
        speakingGenerationPrompt = promptFileService.read(speakingGenerationPromptResource);
        speakingEvaluationPrompt = promptFileService.read(speakingEvaluationPromptResource);
        transcriptionPrompt = promptFileService.read(transcriptionPromptResource);
        log.debug(
                "Speaking prompts loaded. generationCharacters={}, evaluationCharacters={}, transcriptionCharacters={}",
                speakingGenerationPrompt.length(),
                speakingEvaluationPrompt.length(),
                transcriptionPrompt.length()
        );
    }

    public SpeakingDto generateSpeakingPrompt(ExerciseRequestDto exerciseRequestDto) {
        log.debug(
                "Generating speaking prompt. level={}, topic={}, type={}",
                exerciseRequestDto.getLevel(),
                exerciseRequestDto.getTopic(),
                exerciseRequestDto.getType()
        );

        return audioExerciseGenerationService.generateAudioExercise(
                exerciseRequestDto,
                speakingGenerationPrompt,
                SpeakingDto.class,
                "speaking prompt"
        );
    }

    public SpeakingEvaluation generateEvaluation(MultipartFile audio) {
        log.debug("Generating speaking evaluation. audioName={}, audioSize={}", audio.getOriginalFilename(), audio.getSize());

        String transcription = transcribeAudio(audio);

        AiBody aiBody = new AiBody();
        aiBody.setModel(openrouterFreeModel);

        MessageBody systemMessageBody = new MessageBody();
        systemMessageBody.setRole(AIRoleEnum.SYSTEM);
        systemMessageBody.setContent(systemContent);

        MessageBody userMessageBody = new MessageBody();
        userMessageBody.setRole(AIRoleEnum.USER);
        userMessageBody.setContent(
                speakingEvaluationPrompt.formatted(transcription)
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
                                log.error("OpenRouter returned unknown error while evaluating speaking answer. status={}", response.getStatusCode().value());
                                throw new OpenRouterError(
                                        response.getStatusCode().value(),
                                        "OpenRouter returned an unknown error"
                                );
                            }

                            log.error(
                                    "OpenRouter returned error while evaluating speaking answer. code={}, message={}",
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

            log.error("OpenRouter returned invalid speaking evaluation response structure. reason={}", invalidReason);

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

        try {
            log.debug("OpenRouter speaking evaluation response: {}", content);
            SpeakingEvaluation evaluation = objectMapper.readValue(
                    content,
                    SpeakingEvaluation.class
            );
            log.debug("Speaking evaluation parsed successfully. score={}, corrections={}", evaluation.getScore(), evaluation.getCorrections() == null ? 0 : evaluation.getCorrections().size());
            return evaluation;
        } catch (JacksonException exception) {
            log.error(
                    "Failed to parse OpenRouter speaking evaluation JSON. jacksonMessage={}, aiReturned={}",
                    exception.getMessage(),
                    content,
                    exception
            );

            throw new OpenRouterError(
                    HttpStatus.BAD_GATEWAY.value(),
                    "OpenRouter returned invalid evaluation JSON"
            );
        }
    }

    private String transcribeAudio(MultipartFile audio) {
        log.debug("Sending audio to Groq transcription. audioName={}, audioSize={}", audio.getOriginalFilename(), audio.getSize());

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();

        body.add("file", audio.getResource());
        body.add("model", "whisper-large-v3");
        body.add("response_format", "text");
        body.add("language", "lb");
        body.add("prompt", transcriptionPrompt);

        try {
            String transcription = groqRestClient.post()
                    .uri("/audio/transcriptions")
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(body)
                    .retrieve()
                    .body(String.class);

            log.debug("Groq transcription response: {}", transcription);

            return transcription;
        } catch (Exception exception) {
            log.error("Groq transcription request failed", exception);

            throw exception;
        }
    }
}
