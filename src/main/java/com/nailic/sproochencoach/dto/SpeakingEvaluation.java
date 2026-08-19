package com.nailic.sproochencoach.dto;

import lombok.Data;

import java.util.List;

@Data
public class SpeakingEvaluation {
    private String transcript;
    private int score;
    private String feedback;
    private List<String> corrections;
}
