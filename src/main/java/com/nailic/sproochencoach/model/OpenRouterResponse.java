package com.nailic.sproochencoach.model;

import lombok.Data;

import java.util.List;
@Data
public class OpenRouterResponse {
    private List<Choice> choices;
    private OpenRouterErrorBody error;
}
