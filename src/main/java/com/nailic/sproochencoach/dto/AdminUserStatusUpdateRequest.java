package com.nailic.sproochencoach.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminUserStatusUpdateRequest {
    @NotNull
    private Boolean adminDisabled;
}
