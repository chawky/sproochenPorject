package com.nailic.sproochencoach.service;

import com.nailic.sproochencoach.dto.ResponseUserDto;

public record AuthenticatedUser(ResponseUserDto user, String jwt) {
}
