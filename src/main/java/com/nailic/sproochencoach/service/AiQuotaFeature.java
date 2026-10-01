package com.nailic.sproochencoach.service;

import com.nailic.sproochencoach.constants.AppConstants;

public enum AiQuotaFeature {
    SPEAKING(AppConstants.ExerciseAttemptTypes.SPEAKING, "Speaking"),
    LISTENING(AppConstants.ExerciseAttemptTypes.LISTENING, "Listening"),
    IMAGE_DESCRIPTION(AppConstants.ExerciseAttemptTypes.IMAGE_DESCRIPTION, "Image Description"),
    VOCABULARY(AppConstants.ExerciseAttemptTypes.VOCABULARY, "Vocabulary"),
    TOPIC_EXERCISE(AppConstants.ExerciseAttemptTypes.TEXT_EXERCISE, "Topic Exercise");

    private final String exerciseType;
    private final String displayName;

    AiQuotaFeature(String exerciseType, String displayName) {
        this.exerciseType = exerciseType;
        this.displayName = displayName;
    }

    public String exerciseType() {
        return exerciseType;
    }

    public String displayName() {
        return displayName;
    }
}
