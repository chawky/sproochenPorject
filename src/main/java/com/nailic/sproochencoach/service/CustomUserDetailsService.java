package com.nailic.sproochencoach.service;

import com.nailic.sproochencoach.repository.AppUserRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
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
  public UserDetails loadUserByUsername(String username) {
    return appUserRepo.findByUsername(username)
        .orElseThrow(() ->
            new UsernameNotFoundException(
                "User not found with username: " + username
            )
        );
  }
}
