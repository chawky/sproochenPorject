package com.nailic.sproochencoach.service;

import com.nailic.sproochencoach.constants.AppConstants;
import com.nailic.sproochencoach.dto.UsefulSentencesDto;
import com.nailic.sproochencoach.dto.VocabularyDto;
import com.nailic.sproochencoach.dto.VocabularyRequestDto;
import com.nailic.sproochencoach.exceptions.AiProviderException;
import com.nailic.sproochencoach.model.PromptTemplateKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class VocabularyService {
    private static final Logger log = LoggerFactory.getLogger(VocabularyService.class);
    private static final int VOCABULARY_ITEM_COUNT = 5;
    private static final String PROMPT_KEY = PromptTemplateKey.VOCABULARY_GENERATION.getKey();

    private final AiChatClient aiChatClient;
    private final AiJsonParser aiJsonParser;
    private final PromptFileService promptFileService;
    private final UserProgressService userProgressService;
    private final ExerciseConfigService exerciseConfigService;
    @Value(AppConstants.PropertyPlaceholders.AI_PROMPTS_VOCABULARY_GENERATION)
    private Resource resource;
    public VocabularyService(
            AiChatClient aiChatClient,
            AiJsonParser aiJsonParser,
            PromptFileService promptFileService,
            UserProgressService userProgressService,
            ExerciseConfigService exerciseConfigService

    ) {
        this.aiChatClient = aiChatClient;
        this.aiJsonParser = aiJsonParser;
        this.promptFileService = promptFileService;
        this.userProgressService = userProgressService;
        this.exerciseConfigService = exerciseConfigService;
    }

    public VocabularyDto generateVocabExercise(VocabularyRequestDto exerciseRequestDto) {
        VocabularyRequestDto request = exerciseConfigService.normalizedVocabularyRequest(exerciseRequestDto);
        String topicLabel = exerciseConfigService.topicLabel(request.getTopic());

        String promptTemplate = promptFileService.readWithAdminGuidance(PROMPT_KEY, resource);
        String prompt = promptTemplate.formatted(
                VOCABULARY_ITEM_COUNT,
                request.getLevel(),
                topicLabel
        );

        String content = aiChatClient.complete(prompt, "vocabulary exercise");

        VocabularyDto exercise = aiJsonParser.parseObject(content, VocabularyDto.class, "vocabulary exercise");
        validateVocabularyExercise(exercise);
        exercise.setAttemptId(userProgressService.recordGeneratedVocabularyExercise(AppConstants.ExerciseAttemptTypes.VOCABULARY, request));
        return exercise;
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
