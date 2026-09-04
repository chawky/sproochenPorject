package com.nailic.sproochencoach.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class AiQuotaStatusDto {
    private String tier;
    private List<AiQuotaCategoryStatusDto> categories = new ArrayList<>();
}
