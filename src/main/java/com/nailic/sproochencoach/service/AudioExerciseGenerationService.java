package com.nailic.sproochencoach.service;

import com.nailic.sproochencoach.dto.AudioExerciseDto;
import com.nailic.sproochencoach.dto.ExerciseRequestDto;
import com.nailic.sproochencoach.dto.TtsRequest;
import com.nailic.sproochencoach.exceptions.AiProviderException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Service
public class AudioExerciseGenerationService {
    private static final Logger log = LoggerFactory.getLogger(AudioExerciseGenerationService.class);
    private static final String TTS_PROVIDER = "elevenlabs";
    private static final String TTS_MODEL = "eleven_multilingual_v2";

    @Value("${ai.elevenlabs.voice-id}")
    private String voiceId;
    private final AiChatClient aiChatClient;
    private final RestClient ttsRestClient;
    private final ObjectMapper objectMapper;
    private final AiUsageService aiUsageService;
    private final ExerciseConfigService exerciseConfigService;
    private final AiQuotaService aiQuotaService;

    public AudioExerciseGenerationService(
            AiChatClient aiChatClient,
            @Qualifier("ttsRestClient") RestClient ttsRestClient,
            ObjectMapper objectMapper,
            AiUsageService aiUsageService,
            ExerciseConfigService exerciseConfigService,
            AiQuotaService aiQuotaService
    ) {
        this.aiChatClient = aiChatClient;
        this.ttsRestClient = ttsRestClient;
        this.objectMapper = objectMapper;
        this.aiUsageService = aiUsageService;
        this.exerciseConfigService = exerciseConfigService;
        this.aiQuotaService = aiQuotaService;
    }

    public <T extends AudioExerciseDto> T generateAudioExercise(
            ExerciseRequestDto exerciseRequestDto,
            String promptTemplate,
            Class<T> responseType,
            String exerciseName
    ) {
        aiQuotaService.checkCurrentUserQuota(AiQuotaCategory.TTS);
        ExerciseRequestDto request = exerciseConfigService.normalizedRequest(exerciseRequestDto);
        String prompt = promptTemplate.formatted(
                request.getLevel(),
                exerciseConfigService.topicLabel(request.getTopic()),
                request.getType()
        );

        String content = aiChatClient.complete(prompt, exerciseName);

        try {
            T result = objectMapper.readValue(
                    content,
                    responseType
            );

            TtsRequest ttsRequest = new TtsRequest(
                    result.getQuestion(),
                    TTS_MODEL
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
                log.error("ElevenLabs TTS request failed while generating {}. reason={}", exerciseName, exception.getMessage());
                throw exception;
            }

            result.setAudio(audio);
            aiUsageService.recordCharacterUsage(TTS_PROVIDER, TTS_MODEL, exerciseName + " audio", ttsRequest.getText());
            return result;
        } catch (JacksonException exception) {
            log.error("Failed to parse AI provider {} JSON. contentLength={}, jacksonMessage={}", exerciseName, content == null ? 0 : content.length(), exception.getMessage());

            throw new AiProviderException(
                    HttpStatus.BAD_GATEWAY.value(),
                    "AI provider returned invalid " + exerciseName + " JSON"
            );
        }
    }
}
