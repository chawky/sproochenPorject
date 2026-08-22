package com.nailic.sproochencoach.service;

import com.nailic.sproochencoach.model.AppUser;
import com.nailic.sproochencoach.repository.AppUserRepo;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CustomUserDetailsService implements UserDetailsService {

  private static final Logger log = LoggerFactory.getLogger(CustomUserDetailsService.class);

  private final AppUserRepo appUserRepo;

  @Override
  public AppUser loadUserByUsername(String email) {
    log.debug("Loading user for authentication");

    return appUserRepo.findByEmail(email)
        .orElseThrow(() -> {
          log.warn("Authentication user lookup failed");
          return new UsernameNotFoundException(
              "User not found with email: " + email
          );
        });
  }
}
