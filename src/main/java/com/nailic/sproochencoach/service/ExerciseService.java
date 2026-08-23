package com.nailic.sproochencoach.service;

import com.nailic.sproochencoach.dto.ExerciseRequestDto;
import com.nailic.sproochencoach.dto.GeneratedExerciseDto;
import com.nailic.sproochencoach.exceptions.AiProviderException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Service
public class ExerciseService {
    private static final Logger log = LoggerFactory.getLogger(ExerciseService.class);

    private final AiChatClient aiChatClient;
    private final ObjectMapper objectMapper;

    public ExerciseService(
            AiChatClient aiChatClient,
            ObjectMapper objectMapper
    ) {
        this.aiChatClient = aiChatClient;
        this.objectMapper = objectMapper;
    }

    public GeneratedExerciseDto generateExercise(ExerciseRequestDto exerciseRequestDto) {
        log.debug(
                "Generating text exercise. level={}, topic={}, type={}",
                exerciseRequestDto.getLevel(),
                exerciseRequestDto.getTopic(),
                exerciseRequestDto.getType()
        );

        String prompt = """
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
                );

        String content = aiChatClient.complete(prompt, "text exercise");

        try {
            GeneratedExerciseDto exercise = objectMapper.readValue(
                    content,
                    GeneratedExerciseDto.class
            );
            log.debug("Text exercise generated successfully. type={}", exercise.getType());
            return exercise;
        } catch (JacksonException exception) {
            log.error(
                    "Failed to parse AI provider text exercise JSON. jacksonMessage={}, aiReturned={}",
                    exception.getMessage(),
                    content,
                    exception
            );

            throw new AiProviderException(
                    HttpStatus.BAD_GATEWAY.value(),
                    "AI provider returned invalid exercise JSON"
            );
        }
    }
}
