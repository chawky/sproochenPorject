package com.nailic.sproochencoach.service;

import com.nailic.sproochencoach.dto.ExerciseRequestDto;
import com.nailic.sproochencoach.dto.UsefulSentencesDto;
import com.nailic.sproochencoach.dto.VocabularyDto;
import com.nailic.sproochencoach.exceptions.AiProviderException;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Service
public class VocabularyService {
    private static final Logger log = LoggerFactory.getLogger(VocabularyService.class);
    private static final int VOCABULARY_ITEM_COUNT = 5;

    private final AiChatClient aiChatClient;
    private final ObjectMapper objectMapper;
    private final PromptFileService promptFileService;
    private final UserProgressService userProgressService;
    private String vocabPrompt;
    @Value("${ai.prompts.vocabulary-generation}")
    private Resource resource;
    public VocabularyService(
            AiChatClient aiChatClient,
            ObjectMapper objectMapper,
            PromptFileService promptFileService,
            UserProgressService userProgressService

    ) {
        this.aiChatClient = aiChatClient;
        this.objectMapper = objectMapper;
        this.promptFileService = promptFileService;
        this.userProgressService = userProgressService;
    }

    @PostConstruct
    void loadPromptFiles() {
        vocabPrompt = promptFileService.read(resource);
        log.debug("Vocabulary generation prompt loaded. characters={}", vocabPrompt.length());
    }

    public VocabularyDto generateVocabExercise(ExerciseRequestDto exerciseRequestDto) {
        log.debug(
                "Generating vocabulary exercise. level={}, topic={}",
                exerciseRequestDto.getLevel(),
                exerciseRequestDto.getTopic()
        );

        String topicLabel = exerciseRequestDto.getTopic() == null
                ? null
                : exerciseRequestDto.getTopic().getLabel();

        String prompt = vocabPrompt.formatted(
                VOCABULARY_ITEM_COUNT,
                exerciseRequestDto.getLevel(),
                topicLabel
        );

        String content = aiChatClient.complete(prompt, "vocabulary exercise");

        try {
            VocabularyDto exercise = objectMapper.readValue(
                    content,
                    VocabularyDto.class
            );
            validateVocabularyExercise(exercise);
            userProgressService.recordGeneratedExercise("VOCABULARY", exerciseRequestDto);
            log.debug("Vocabulary exercise generated successfully. items={}", exercise.getUsefulSentences().size());
            return exercise;
        } catch (JacksonException exception) {
            log.error(
                    "Failed to parse AI provider vocabulary exercise JSON. jacksonMessage={}, aiReturned={}",
                    exception.getMessage(),
                    content,
                    exception
            );

            throw new AiProviderException(
                    HttpStatus.BAD_GATEWAY.value(),
                    "AI provider returned invalid vocabulary exercise JSON"
            );
        }
    }

    private void validateVocabularyExercise(VocabularyDto exercise) {
        if (exercise == null || exercise.getUsefulSentences() == null) {
            throw invalidVocabularyExercise("usefulSentences is missing");
        }

        if (exercise.getUsefulSentences().size() < VOCABULARY_ITEM_COUNT) {
            throw invalidVocabularyExercise("usefulSentences has fewer than " + VOCABULARY_ITEM_COUNT + " items");
        }

        for (UsefulSentencesDto usefulSentence : exercise.getUsefulSentences()) {
            if (usefulSentence == null) {
                throw invalidVocabularyExercise("usefulSentences contains a null item");
            }

            if (!StringUtils.hasText(usefulSentence.getVocabularyWord())) {
                throw invalidVocabularyExercise("vocabularyWord is missing");
            }

            if (!StringUtils.hasText(usefulSentence.getWordTranslation())) {
                throw invalidVocabularyExercise("wordTranslation is missing");
            }

            if (!StringUtils.hasText(usefulSentence.getSentence())) {
                throw invalidVocabularyExercise("sentence is missing");
            }

            if (!StringUtils.hasText(usefulSentence.getSentenceTranslation())) {
                throw invalidVocabularyExercise("sentenceTranslation is missing");
            }
        }
    }

    private AiProviderException invalidVocabularyExercise(String reason) {
        log.error("AI provider returned invalid vocabulary exercise JSON. reason={}", reason);
        return new AiProviderException(
                HttpStatus.BAD_GATEWAY.value(),
                "AI provider returned invalid vocabulary exercise JSON"
        );
    }
}
