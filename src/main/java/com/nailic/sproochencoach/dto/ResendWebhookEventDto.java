package com.nailic.sproochencoach.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ResendWebhookEventDto {
    private String type;
    private String created_at;
    private Data data;

    @Getter
    @Setter
    @NoArgsConstructor
    public static class Data {
        private String email_id;
    }
}
