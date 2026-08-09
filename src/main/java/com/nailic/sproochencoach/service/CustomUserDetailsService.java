package com.nailic.sproochencoach.service;

import com.nailic.sproochencoach.model.AppUser;
import com.nailic.sproochencoach.repository.AppUserRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CustomUserDetailsService implements UserDetailsService {

  private final AppUserRepo appUserRepo;

  @Override
  public AppUser loadUserByUsername(String email) {
    return appUserRepo.findByEmail(email)
        .orElseThrow(() ->
            new UsernameNotFoundException(
                "User not found with email: " + email
            )
        );
  }
}
