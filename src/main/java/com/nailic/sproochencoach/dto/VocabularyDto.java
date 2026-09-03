package com.nailic.sproochencoach.dto;

import lombok.Data;

import java.util.List;

@Data
public class VocabularyDto {
    private Long attemptId;
    private List<UsefulSentencesDto> usefulSentences;
}
