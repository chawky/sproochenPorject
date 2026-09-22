package com.nailic.sproochencoach.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class ResendReceivedEmailDto {
    private String id;
    private List<String> to;
    private String from;
    private String subject;
    private String html;
    private String text;
    private String created_at;
    private List<ResendReceivedEmailAttachmentDto> attachments;
}
