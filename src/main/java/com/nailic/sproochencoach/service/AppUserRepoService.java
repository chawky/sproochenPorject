package com.nailic.sproochencoach.service;

import com.nailic.sproochencoach.dto.AppUserDto;
import com.nailic.sproochencoach.model.AppRole;
import com.nailic.sproochencoach.model.AppUser;
import com.nailic.sproochencoach.repository.AppUserRepo;
import com.nailic.sproochencoach.repository.RoleRepo;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class AppUserRepoService {
  private final ModelMapper mapper;
  private final AppUserRepo appUserRepo;
  private final RoleRepo roleService;

  public List<AppUserDto> findAll() {
    return appUserRepo.findAll().stream()
        .map(s -> mapper.map(s, AppUserDto.class))
        .collect(Collectors.toList());
  }

  public AppUserDto addUser(AppUserDto appUserDto) {
    AppUser user =
        appUserRepo.findByUsernameAndEmail(appUserDto.getUsername(), appUserDto.getEmail());
    if (user != null) {}

    AppUser userSaved = appUserRepo.save(mapper.map(appUserDto, AppUser.class));
    return mapper.map(userSaved, AppUserDto.class);
  }

  public AppUserDto updateUser(AppUserDto appUserDto) {
    Optional<AppUser> userOp = appUserRepo.findById(appUserDto.getId());
    if (userOp.isPresent()) {
      AppUser user = userOp.get();
      user.setUsername(appUserDto.getUsername());
      user.setEmail(appUserDto.getEmail());
      user.setPassword(appUserDto.getPassword());
      Set<AppRole> roles = new HashSet<>(roleService.findAllByName(appUserDto.getRoles()));
      user.setRoles(roles);
      appUserRepo.save(user);
    }
    AppUser userSaved = appUserRepo.save(mapper.map(appUserDto, AppUser.class));
    return mapper.map(userSaved, AppUserDto.class);
  }
}
