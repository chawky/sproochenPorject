package com.nailic.sproochencoach.service;

import com.nailic.sproochencoach.dto.ExerciseRequestDto;
import com.nailic.sproochencoach.dto.SpeakingDto;
import com.nailic.sproochencoach.dto.SpeakingEvaluation;
import com.nailic.sproochencoach.exceptions.AiProviderException;
import jakarta.annotation.PostConstruct;
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
    private String speakingGenerationPrompt;
    private String speakingEvaluationPrompt;
    private String transcriptionPrompt;

    public SpeakingService(
            AiChatClient aiChatClient,
            @Qualifier("groqRestClient") RestClient groqRestClient,
            ObjectMapper objectMapper,
            PromptFileService promptFileService,
            AudioExerciseGenerationService audioExerciseGenerationService,
            UserProgressService userProgressService
    ) {
        this.aiChatClient = aiChatClient;
        this.groqRestClient = groqRestClient;
        this.objectMapper = objectMapper;
        this.promptFileService = promptFileService;
        this.audioExerciseGenerationService = audioExerciseGenerationService;
        this.userProgressService = userProgressService;
    }

    @PostConstruct
    void loadPromptFiles() {
        speakingGenerationPrompt = promptFileService.read(speakingGenerationPromptResource);
        speakingEvaluationPrompt = promptFileService.read(speakingEvaluationPromptResource);
        transcriptionPrompt = promptFileService.read(transcriptionPromptResource);
    }

    public SpeakingDto generateSpeakingPrompt(ExerciseRequestDto exerciseRequestDto) {
        SpeakingDto exercise = audioExerciseGenerationService.generateAudioExercise(
                exerciseRequestDto,
                speakingGenerationPrompt,
                SpeakingDto.class,
                "speaking prompt"
        );
        userProgressService.recordGeneratedExercise("SPEAKING", exerciseRequestDto);
        return exercise;
    }

    public SpeakingEvaluation generateEvaluation(MultipartFile audio) {
        String transcription = transcribeAudio(audio);

        String content = aiChatClient.complete(
                speakingEvaluationPrompt.formatted(transcription),
                "speaking evaluation"
        );

        try {
            SpeakingEvaluation evaluation = objectMapper.readValue(
                    content,
                    SpeakingEvaluation.class
            );
            userProgressService.recordEvaluation("SPEAKING", "speaking evaluation", evaluation.getScore());
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

            return transcription;
        } catch (Exception exception) {
            log.error("Groq transcription request failed. audioName={}, audioSize={}, reason={}", audio.getOriginalFilename(), audio.getSize(), exception.getMessage());

            throw exception;
        }
    }
}
