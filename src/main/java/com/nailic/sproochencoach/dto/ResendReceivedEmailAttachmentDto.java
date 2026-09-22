package com.nailic.sproochencoach.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ResendReceivedEmailAttachmentDto {
    private String id;
    private String filename;
    private String content_type;
    private String content_disposition;
    private String content_id;
    private Long size;
    private String download_url;
    private String expires_at;
}
