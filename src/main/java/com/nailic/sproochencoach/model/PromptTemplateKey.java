package com.nailic.sproochencoach.model;

import com.nailic.sproochencoach.exceptions.BadRequestException;
import lombok.Getter;

import java.util.Arrays;

@Getter
public enum PromptTemplateKey {
    EXERCISE_GENERATION("exercise-generation", "Text exercise generation"),
    VOCABULARY_GENERATION("vocabulary-generation", "Vocabulary generation"),
    SPEAKING_GENERATION("speaking-generation", "Speaking prompt generation"),
    SPEAKING_EVALUATION("speaking-evaluation", "Speaking evaluation"),
    LISTENING_GENERATION("listening-generation", "Listening exercise generation"),
    IMAGE_GENERATION("image-generation", "Image description generation"),
    IMAGE_DESCRIPTION_EVALUATION("image-description-evaluation", "Image description evaluation"),
    GROQ_TRANSCRIPTION("groq-transcription", "Groq transcription");

    private final String key;
    private final String title;

    PromptTemplateKey(String key, String title) {
        this.key = key;
        this.title = title;
    }

    public static PromptTemplateKey fromKey(String key) {
        return Arrays.stream(values())
                .filter(promptKey -> promptKey.key.equals(key))
                .findFirst()
                .orElseThrow(() -> new BadRequestException("Unknown prompt key"));
    }
}
