package com.nailic.sproochencoach.service;

import com.nailic.sproochencoach.constants.AppConstants;
import com.nailic.sproochencoach.dto.AudioExerciseDto;
import com.nailic.sproochencoach.dto.ExerciseRequestDto;
import com.nailic.sproochencoach.dto.TtsRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class AudioExerciseGenerationService {
    private static final Logger log = LoggerFactory.getLogger(AudioExerciseGenerationService.class);

    @Value(AppConstants.PropertyPlaceholders.AI_ELEVENLABS_VOICE_ID)
    private String voiceId;
    private final AiChatClient aiChatClient;
    private final RestClient ttsRestClient;
    private final AiJsonParser aiJsonParser;
    private final AiUsageService aiUsageService;
    private final ExerciseConfigService exerciseConfigService;
    private final AiQuotaService aiQuotaService;

    public AudioExerciseGenerationService(
            AiChatClient aiChatClient,
            @Qualifier(AppConstants.RestClientBeans.TTS) RestClient ttsRestClient,
            AiJsonParser aiJsonParser,
            AiUsageService aiUsageService,
            ExerciseConfigService exerciseConfigService,
            AiQuotaService aiQuotaService
    ) {
        this.aiChatClient = aiChatClient;
        this.ttsRestClient = ttsRestClient;
        this.aiJsonParser = aiJsonParser;
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
        T result = aiJsonParser.parseObject(content, responseType, exerciseName);

        TtsRequest ttsRequest = new TtsRequest(
                result.getQuestion(),
                AppConstants.Models.ELEVEN_MULTILINGUAL_V2
        );

        byte[] audio;
        try {
            audio = ttsRestClient.post()
                    .uri(AppConstants.ApiPaths.ELEVENLABS_TEXT_TO_SPEECH + voiceId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.valueOf(AppConstants.Http.AUDIO_MPEG))
                    .body(ttsRequest)
                    .retrieve()
                    .body(byte[].class);
        } catch (RuntimeException exception) {
            log.error("ElevenLabs TTS request failed while generating {}. reason={}", exerciseName, exception.getMessage());
            throw exception;
        }

        result.setAudio(audio);
        aiUsageService.recordCharacterUsage(
                AppConstants.Providers.ELEVENLABS,
                AppConstants.Models.ELEVEN_MULTILINGUAL_V2,
                exerciseName + " audio",
                ttsRequest.getText()
        );
        return result;
    }
}
