package com.nailic.sproochencoach.model;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum AIRoleEnum {
    SYSTEM("system"),
    USER("user"),
    ASSISTANT("assistant");
    private String value;
    AIRoleEnum(String value) {
        this.value = value;
    }
    @JsonValue
    String getValue() {
        return value;
    }
}
