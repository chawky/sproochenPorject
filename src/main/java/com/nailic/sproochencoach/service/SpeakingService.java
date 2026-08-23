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
    private String speakingGenerationPrompt;
    private String speakingEvaluationPrompt;
    private String transcriptionPrompt;

    public SpeakingService(
            AiChatClient aiChatClient,
            @Qualifier("groqRestClient") RestClient groqRestClient,
            ObjectMapper objectMapper,
            PromptFileService promptFileService,
            AudioExerciseGenerationService audioExerciseGenerationService
    ) {
        this.aiChatClient = aiChatClient;
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

        String content = aiChatClient.complete(
                speakingEvaluationPrompt.formatted(transcription),
                "speaking evaluation"
        );

        try {
            SpeakingEvaluation evaluation = objectMapper.readValue(
                    content,
                    SpeakingEvaluation.class
            );
            log.debug("Speaking evaluation parsed successfully. score={}, corrections={}", evaluation.getScore(), evaluation.getCorrections() == null ? 0 : evaluation.getCorrections().size());
            return evaluation;
        } catch (JacksonException exception) {
            log.error(
                    "Failed to parse AI provider speaking evaluation JSON. jacksonMessage={}, aiReturned={}",
                    exception.getMessage(),
                    content,
                    exception
            );

            throw new AiProviderException(
                    HttpStatus.BAD_GATEWAY.value(),
                    "AI provider returned invalid evaluation JSON"
            );
        }
    }

    public String transcribeAudio(MultipartFile audio) {
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
