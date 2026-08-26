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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private static final Logger log = LoggerFactory.getLogger(AppUserService.class);

    private final ModelMapper mapper;
    private final AppUserRepo appUserRepo;
    private final RoleRepo roleRepo;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final EmailAndOtpService emailAndOtpService;
    private final AuthenticationManager authenticationManager;
    private final UserLoginDayService userLoginDayService;

    public List<ResponseUserDto> findAll() {
        List<ResponseUserDto> users = appUserRepo.findAll().stream()
                .map(s -> mapper.map(s, ResponseUserDto.class))
                .collect(Collectors.toList());

        log.debug("Retrieved {} users", users.size());

        return users;
    }

    public ResponseUserDto addUser(RequestUserDto request) {
        log.debug("Registering user with email {}", maskEmail(request.getEmail()));

        if (appUserRepo.existsByUsername(request.getUsername())) {
            log.warn("Registration rejected because username already exists");
            throw new UserAlreadyExistsException("Username already exists");
        }

        if (appUserRepo.existsByEmail(request.getEmail())) {
            log.warn("Registration rejected because email already exists: {}", maskEmail(request.getEmail()));
            throw new UserAlreadyExistsException("Email already exists");
        }

        AppUser user = mapper.map(request, AppUser.class);
        user.setRoles(Set.of(roleRepo.findByName("ADMIN")));
        user.setPassword(passwordEncoder.encode(request.getPassword()));

        AppUser savedUser = appUserRepo.save(user);

        log.debug("User registered successfully with id {}", savedUser.getId());

        return mapper.map(savedUser, ResponseUserDto.class);
    }

    public RequestUserDto updateUser(RequestUserDto appUserDto) {
        log.debug("Updating user with email {}", maskEmail(appUserDto.getEmail()));

        AppUser userOp = appUserRepo.findByUsernameAndEmail(appUserDto.getUsername(),
                appUserDto.getEmail());
        if (userOp != null) {
            userOp.setUsername(appUserDto.getUsername());
            userOp.setEmail(appUserDto.getEmail());
            userOp.setPassword(passwordEncoder.encode(appUserDto.getPassword()));
            Set<AppRole> roles = new HashSet<>(roleRepo.findByNameIn(appUserDto.getRoles()));
            userOp.setRoles(roles);
            AppUser userSaved = appUserRepo.save(userOp);
            log.debug("User updated successfully with id {}", userSaved.getId());
            return mapper.map(userSaved, RequestUserDto.class);
        } else {
            log.warn("User update skipped because user was not found for email {}", maskEmail(appUserDto.getEmail()));
            return null;
        }
    }

    public ResponseUserDto findById(int id) {
        log.debug("Finding user by id {}", id);
        ResponseUserDto responseUserDto = mapper.map(appUserRepo.findById(id), ResponseUserDto.class);
        log.debug("User lookup by id {} completed", id);
        return responseUserDto;
    }

    public ResponseUserDto login(RequestUserDto appUserDto) {
        log.debug("Login attempt for email {}", maskEmail(appUserDto.getEmail()));

        Authentication authentication = authenticationManager
                .authenticate(new UsernamePasswordAuthenticationToken(appUserDto.getEmail(), appUserDto.getPassword()));
        AppUser user = (AppUser) authentication.getPrincipal();
        userLoginDayService.recordLogin(user);

        ResponseUserDto userDto = mapper.map(user,
                ResponseUserDto.class);
        userDto.setJwt(jwtService.generateToken(user));

        log.debug("Login successful for user id {}", user.getId());

        return userDto;
    }

    private String maskEmail(String email) {
        if (email == null || email.isBlank()) {
            return "<blank>";
        }

        int atIndex = email.indexOf('@');
        if (atIndex <= 1) {
            return "***";
        }

        return email.charAt(0) + "***" + email.substring(atIndex);
    }
}
