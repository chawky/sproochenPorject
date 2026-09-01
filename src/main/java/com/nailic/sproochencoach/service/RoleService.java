package com.nailic.sproochencoach.service;

import com.nailic.sproochencoach.model.AppRole;
import com.nailic.sproochencoach.repository.RoleRepo;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
@Transactional
@RequiredArgsConstructor
public class RoleService {
  private static final Logger log = LoggerFactory.getLogger(RoleService.class);

  public final RoleRepo roleRepo;

  public AppRole getRoleByName(String roleName) {
    AppRole role = roleRepo.findByName(roleName);
    if (role == null) {
      log.warn("Role lookup failed. roleName={}", roleName);
    }
    return role;
  }
}
