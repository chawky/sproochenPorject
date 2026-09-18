package com.nailic.sproochencoach.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Getter
@Setter
public class SupportRequestDto {
    @Schema(maxLength = 254)
    private String email;

    @Schema(maxLength = 150)
    private String subject;

    @Schema(maxLength = 5000)
    private String message;

    private List<MultipartFile> attachments;
}
