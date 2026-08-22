package com.nailic.sproochencoach.service;

import com.nailic.sproochencoach.model.OpenRouterResponse;

public final class OpenRouterResponseValidator {

    private OpenRouterResponseValidator() {
    }

    public static String invalidReason(OpenRouterResponse response) {
        if (response == null) {
            return "response body was null";
        }

        if (response.getChoices() == null) {
            return "choices was null";
        }

        if (response.getChoices().isEmpty()) {
            return "choices was empty";
        }

        if (response.getChoices().get(0) == null) {
            return "choices[0] was null";
        }

        if (response.getChoices().get(0).getMessage() == null) {
            return "choices[0].message was null";
        }

        if (response.getChoices().get(0).getMessage().getContent() == null) {
            return "choices[0].message.content was null";
        }

        if (response.getChoices().get(0).getMessage().getContent().isBlank()) {
            return "choices[0].message.content was blank";
        }

        return null;
    }
}
