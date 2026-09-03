package com.nailic.sproochencoach.service;

import com.nailic.sproochencoach.dto.ExerciseRequestDto;
import com.nailic.sproochencoach.dto.SpeakingDto;
import com.nailic.sproochencoach.dto.SpeakingEvaluation;
import com.nailic.sproochencoach.exceptions.AiProviderException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Service
public class SpeakingService {
    private static final Logger log = LoggerFactory.getLogger(SpeakingService.class);
    private static final String STT_PROVIDER = "groq";
    private static final String STT_MODEL = "whisper-large-v3";
    private static final String SPEAKING_GENERATION_PROMPT_KEY = "speaking-generation";
    private static final String SPEAKING_EVALUATION_PROMPT_KEY = "speaking-evaluation";
    private static final String TRANSCRIPTION_PROMPT_KEY = "groq-transcription";

    @Value("${ai.prompts.speaking-generation}")
    private Resource speakingGenerationPromptResource;

    @Value("${ai.prompts.speaking-evaluation}")
    private Resource speakingEvaluationPromptResource;

    @Value("${ai.prompts.transcription}")
    private Resource transcriptionPromptResource;
    private final AiChatClient aiChatClient;
    private final RestClient groqRestClient;
    private final ObjectMapper objectMapper;
    private final PromptFileService promptFileService;
    private final AudioExerciseGenerationService audioExerciseGenerationService;
    private final UserProgressService userProgressService;
    private final AiUsageService aiUsageService;
    private final ExerciseConfigService exerciseConfigService;

    public SpeakingService(
            AiChatClient aiChatClient,
            @Qualifier("groqRestClient") RestClient groqRestClient,
            ObjectMapper objectMapper,
            PromptFileService promptFileService,
            AudioExerciseGenerationService audioExerciseGenerationService,
            UserProgressService userProgressService,
            AiUsageService aiUsageService,
            ExerciseConfigService exerciseConfigService
    ) {
        this.aiChatClient = aiChatClient;
        this.groqRestClient = groqRestClient;
        this.objectMapper = objectMapper;
        this.promptFileService = promptFileService;
        this.audioExerciseGenerationService = audioExerciseGenerationService;
        this.userProgressService = userProgressService;
        this.aiUsageService = aiUsageService;
        this.exerciseConfigService = exerciseConfigService;
    }

    public SpeakingDto generateSpeakingPrompt(ExerciseRequestDto exerciseRequestDto) {
        ExerciseRequestDto request = exerciseConfigService.normalizedRequest(exerciseRequestDto);
        SpeakingDto exercise = audioExerciseGenerationService.generateAudioExercise(
                request,
                promptFileService.readWithAdminGuidance(SPEAKING_GENERATION_PROMPT_KEY, speakingGenerationPromptResource),
                SpeakingDto.class,
                "speaking prompt"
        );
        exercise.setAttemptId(userProgressService.recordGeneratedExercise("SPEAKING", request));
        return exercise;
    }

    public SpeakingEvaluation generateEvaluation(MultipartFile audio) {
        return generateEvaluation(audio, null);
    }

    public SpeakingEvaluation generateEvaluation(MultipartFile audio, Long audioDurationSeconds) {
        return generateEvaluation(audio, audioDurationSeconds, null);
    }

    public SpeakingEvaluation generateEvaluation(MultipartFile audio, Long audioDurationSeconds, Long attemptId) {
        String transcription = transcribeAudio(audio, audioDurationSeconds);

        String content = aiChatClient.complete(
                promptFileService.readWithAdminGuidance(SPEAKING_EVALUATION_PROMPT_KEY, speakingEvaluationPromptResource)
                        .formatted(transcription),
                "speaking evaluation"
        );

        try {
            SpeakingEvaluation evaluation = objectMapper.readValue(
                    content,
                    SpeakingEvaluation.class
            );
            userProgressService.recordEvaluation(
                    "SPEAKING",
                    "speaking evaluation",
                    evaluation.getScore(),
                    attemptId,
                    evaluation.getTranscript()
            );
            return evaluation;
        } catch (JacksonException exception) {
            log.error("Failed to parse AI provider speaking evaluation JSON. contentLength={}, jacksonMessage={}", content == null ? 0 : content.length(), exception.getMessage());

            throw new AiProviderException(
                    HttpStatus.BAD_GATEWAY.value(),
                    "AI provider returned invalid evaluation JSON"
            );
        }
    }

    public String transcribeAudio(MultipartFile audio) {
        return transcribeAudio(audio, null);
    }

    public String transcribeAudio(MultipartFile audio, Long audioDurationSeconds) {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();

        body.add("file", audio.getResource());
        body.add("model", STT_MODEL);
        body.add("response_format", "text");
        body.add("language", "lb");
        body.add("prompt", promptFileService.readWithAdminGuidance(TRANSCRIPTION_PROMPT_KEY, transcriptionPromptResource));

        try {
            String transcription = groqRestClient.post()
                    .uri("/audio/transcriptions")
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(body)
                    .retrieve()
                    .body(String.class);

            recordTranscriptionUsage(audio, audioDurationSeconds);
            return transcription;
        } catch (Exception exception) {
            log.error("Groq transcription request failed. audioName={}, audioSize={}, reason={}", audio.getOriginalFilename(), audio.getSize(), exception.getMessage());

            throw exception;
        }
    }

    private void recordTranscriptionUsage(MultipartFile audio, Long audioDurationSeconds) {
        if (audioDurationSeconds != null && audioDurationSeconds > 0) {
            aiUsageService.recordAudioDurationUsage(STT_PROVIDER, STT_MODEL, "transcription", audioDurationSeconds);
            return;
        }

        aiUsageService.recordAudioUploadUsage(STT_PROVIDER, STT_MODEL, "transcription", audio.getSize());
    }
}
