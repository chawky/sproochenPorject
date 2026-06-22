package com.nailic.sproochencoach.controller;

import com.nailic.sproochencoach.dto.AppUserDto;
import com.nailic.sproochencoach.service.AppUserRepoService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@CrossOrigin
@RequiredArgsConstructor
public class AppUserController {
  private final AppUserRepoService appUserRepoService;
  @GetMapping("getUsers")
  public ResponseEntity<List<AppUserDto>> findAll() {
    return ResponseEntity.ok(appUserRepoService.findAll());
  }
}
