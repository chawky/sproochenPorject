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
public class PageResponseDto<T> {
    private List<T> items = new ArrayList<>();
    private int page;
    private int size;
    private long totalItems;
    private int totalPages;
}
