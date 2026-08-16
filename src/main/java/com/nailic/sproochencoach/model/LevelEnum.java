package com.nailic.sproochencoach.model;

import lombok.Getter;

@Getter
public enum LevelEnum {

    A1("Beginner", "Basic words, expressions and simple sentences"),
    A2("Elementary", "Target level for Sproochentest speaking"),
    B1("Intermediate", "Target level for Sproochentest listening");

    private final String label;
    private final String description;

    LevelEnum(String label, String description) {
        this.label = label;
        this.description = description;
    }
}
