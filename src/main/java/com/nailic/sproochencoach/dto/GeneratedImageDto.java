package com.nailic.sproochencoach.dto;

import lombok.Data;

@Data
public class GeneratedImageDto {
    private Long attemptId;
    private byte[] image;
    private String imageDescription;
}
