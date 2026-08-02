package com.nailic.sproochencoach.service;

import com.nailic.sproochencoach.model.AppRole;
import com.nailic.sproochencoach.repository.RoleRepo;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@Transactional
@RequiredArgsConstructor
public class RoleService {
  public final RoleRepo roleRepo;

  public AppRole getRoleByName(String roleName) {
    return roleRepo.findByName(roleName);
  }
}
