package com.nailic.sproochencoach.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminUserStatusUpdateRequest {
    @NotNull
    private Boolean adminDisabled;

    @Size(max = 500)
    private String reason;
}
