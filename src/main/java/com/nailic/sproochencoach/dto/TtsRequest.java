package com.nailic.sproochencoach.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TtsRequest {
    private String text;
    private String model_id;
}