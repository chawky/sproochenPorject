package com.nailic.sproochencoach.service;

import com.nailic.sproochencoach.dto.RequestUserDto;
import com.nailic.sproochencoach.dto.ResponseUserDto;
import com.nailic.sproochencoach.model.AppRole;
import com.nailic.sproochencoach.model.AppUser;
import com.nailic.sproochencoach.repository.AppUserRepo;
import com.nailic.sproochencoach.repository.RoleRepo;
import com.nailic.sproochencoach.exceptions.UserAlreadyExistsException;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class AppUserService {
    private final ModelMapper mapper;
    private final AppUserRepo appUserRepo;
    private final RoleRepo roleRepo;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final EmailAndOtpService emailAndOtpService;
    private final AuthenticationManager authenticationManager;

    public List<ResponseUserDto> findAll() {
        return appUserRepo.findAll().stream()
                .map(s -> mapper.map(s, ResponseUserDto.class))
                .collect(Collectors.toList());
    }

    public ResponseUserDto addUser(RequestUserDto request) {
        if (appUserRepo.existsByUsername(request.getUsername())) {
            throw new UserAlreadyExistsException("Username already exists");
        }

        if (appUserRepo.existsByEmail(request.getEmail())) {
            throw new UserAlreadyExistsException("Email already exists");
        }

        AppUser user = mapper.map(request, AppUser.class);
        user.setRoles(Set.of(roleRepo.findByName("ADMIN")));
        user.setPassword(passwordEncoder.encode(request.getPassword()));

        AppUser savedUser = appUserRepo.save(user);

        return mapper.map(savedUser, ResponseUserDto.class);
    }

    public RequestUserDto updateUser(RequestUserDto appUserDto) {
        AppUser userOp = appUserRepo.findByUsernameAndEmail(appUserDto.getUsername(),
                appUserDto.getEmail());
        if (userOp != null) {
            userOp.setUsername(appUserDto.getUsername());
            userOp.setEmail(appUserDto.getEmail());
            userOp.setPassword(passwordEncoder.encode(appUserDto.getPassword()));
            Set<AppRole> roles = new HashSet<>(roleRepo.findByNameIn(appUserDto.getRoles()));
            userOp.setRoles(roles);
            AppUser userSaved = appUserRepo.save(userOp);
            return mapper.map(userSaved, RequestUserDto.class);
        } else {
            return null;
        }
    }

    public ResponseUserDto findById(int id) {
        return mapper.map(appUserRepo.findById(id), ResponseUserDto.class);
    }

    public ResponseUserDto login(RequestUserDto appUserDto) {
        Authentication authentication = authenticationManager
                .authenticate(new UsernamePasswordAuthenticationToken(appUserDto.getEmail(), appUserDto.getPassword()));
        AppUser user = (AppUser) authentication.getPrincipal();
        ResponseUserDto userDto = mapper.map(user,
                ResponseUserDto.class);
        userDto.setJwt(jwtService.generateToken(user));
        return userDto;
    }
}
