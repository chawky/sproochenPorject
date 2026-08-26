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

    @Value("${ai.elevenlabs.voice-id}")
    private String voiceId;
    private final AiChatClient aiChatClient;
    private final RestClient ttsRestClient;
    private final ObjectMapper objectMapper;

    public AudioExerciseGenerationService(
            AiChatClient aiChatClient,
            @Qualifier("ttsRestClient") RestClient ttsRestClient,
            ObjectMapper objectMapper
    ) {
        this.aiChatClient = aiChatClient;
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

        String prompt = promptTemplate.formatted(
                exerciseRequestDto.getLevel(),
                exerciseRequestDto.getTopic(),
                exerciseRequestDto.getType()
        );

        String content = aiChatClient.complete(prompt, exerciseName);

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
                    "Failed to parse AI provider {} JSON. jacksonMessage={}, aiReturned={}",
                    exerciseName,
                    exception.getMessage(),
                    content,
                    exception
            );

            throw new AiProviderException(
                    HttpStatus.BAD_GATEWAY.value(),
                    "AI provider returned invalid " + exerciseName + " JSON"
            );
        }
    }
}
