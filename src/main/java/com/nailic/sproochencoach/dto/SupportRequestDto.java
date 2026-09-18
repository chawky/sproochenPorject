package com.nailic.sproochencoach.dto;

import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Getter
@Setter
public class SupportRequestDto {
    private String email;
    private String subject;
    private String message;
    private List<MultipartFile> attachments;
}
