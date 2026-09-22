package com.nailic.sproochencoach.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class ResendReceivedEmailListDto {
    private boolean has_more;
    private List<ResendReceivedEmailDto> data;
}
