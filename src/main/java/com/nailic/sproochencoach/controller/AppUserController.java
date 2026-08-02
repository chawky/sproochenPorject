package com.nailic.sproochencoach.controller;

import com.nailic.sproochencoach.dto.RequestUserDto;
import com.nailic.sproochencoach.dto.ResponseUserDto;
import com.nailic.sproochencoach.dto.SendOtpRequest;
import com.nailic.sproochencoach.model.AppUser;
import com.nailic.sproochencoach.repository.AppUserRepo;
import com.nailic.sproochencoach.service.AppUserService;
import com.nailic.sproochencoach.service.EmailAndOtpService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class AppUserController {
  private final AppUserService appUserRepoService;
  private final AppUserRepo appUserRepo;
  private final EmailAndOtpService emailService;

  @GetMapping("getUsers")
  public ResponseEntity<List<RequestUserDto>> findAll() {
    return ResponseEntity.ok(appUserRepoService.findAll());
  }

  @GetMapping("getUser/{id}")
  public ResponseEntity<ResponseUserDto> findById(@PathVariable int id) {
    ResponseUserDto appUserDto = appUserRepoService.findById(id);
    if (appUserDto == null) {
      return ResponseEntity.notFound().build();
    }
    return ResponseEntity.ok(appUserDto);
  }

  @PostMapping("/addUser")
  public ResponseEntity<RequestUserDto> createUser(@RequestBody RequestUserDto appUserDto) {
    AppUser user =
        appUserRepo.findByUsernameAndEmail(appUserDto.getUsername(), appUserDto.getEmail());
    if (user != null) {
      return ResponseEntity.notFound().build();
    }
    return ResponseEntity.ok(appUserRepoService.addUser(appUserDto));
  }

  @PostMapping("/login")
  public ResponseEntity<ResponseUserDto> login(@RequestBody RequestUserDto appUserDto) {
    return ResponseEntity.ok(appUserRepoService.login(appUserDto));
  }
  @PostMapping("/sendOtp")
  public ResponseEntity<String> sendOtp(
          @RequestBody SendOtpRequest request
  ) {
    emailService.sendEmailAndSaveOtp(request.getEmail());

    return ResponseEntity.ok("If the email exists, a verification code has been sent");
  }
}
