package com.nailic.sproochencoach.service;

import com.nailic.sproochencoach.dto.RequestUserDto;
import com.nailic.sproochencoach.dto.ResponseUserDto;
import com.nailic.sproochencoach.model.AppUser;
import com.nailic.sproochencoach.repository.AppUserRepo;
import com.nailic.sproochencoach.repository.RoleRepo;
import com.nailic.sproochencoach.exceptions.UserAlreadyExistsException;
import com.nailic.sproochencoach.exceptions.UserNotFoundException;
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

        AppUser user = new AppUser();
        mapUserFields(request, user);
        user.setRoles(Set.of(roleRepo.findByName("ADMIN")));
        user.setPassword(passwordEncoder.encode(request.getPassword()));

        AppUser savedUser = appUserRepo.save(user);

        log.debug("User registered successfully with id {}", savedUser.getId());

        return mapper.map(savedUser, ResponseUserDto.class);
    }

    public ResponseUserDto updateUser(Integer id, RequestUserDto request) {
        log.debug("Updating user with id {}", id);

        AppUser user = appUserRepo.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        if (isUsernameTakenByAnotherUser(request.getUsername(), user)) {
            log.warn("User update rejected because username already exists");
            throw new UserAlreadyExistsException("Username already exists");
        }

        if (isEmailTakenByAnotherUser(request.getEmail(), user)) {
            log.warn("User update rejected because email already exists: {}", maskEmail(request.getEmail()));
            throw new UserAlreadyExistsException("Email already exists");
        }

        updateUserFields(request, user);

        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }

        AppUser savedUser = appUserRepo.save(user);

        log.debug("User updated successfully with id {}", savedUser.getId());

        return mapper.map(savedUser, ResponseUserDto.class);
    }

    public ResponseUserDto findById(int id) {
        log.debug("Finding user by id {}", id);
        AppUser user = appUserRepo.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User not found"));
        ResponseUserDto responseUserDto = mapper.map(user, ResponseUserDto.class);
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

    private void mapUserFields(RequestUserDto request, AppUser user) {
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setStreet(request.getStreet());
        user.setStreetNumber(request.getStreetNumber());
        user.setPostalCode(request.getPostalCode());
        user.setCity(request.getCity());
        user.setAddressInfo(request.getAddressInfo());
    }

    private void updateUserFields(RequestUserDto request, AppUser user) {
        if (request.getUsername() != null) {
            user.setUsername(request.getUsername());
        }

        if (request.getEmail() != null) {
            user.setEmail(request.getEmail());
        }

        if (request.getFirstName() != null) {
            user.setFirstName(request.getFirstName());
        }

        if (request.getLastName() != null) {
            user.setLastName(request.getLastName());
        }

        if (request.getStreet() != null) {
            user.setStreet(request.getStreet());
        }

        if (request.getStreetNumber() != null) {
            user.setStreetNumber(request.getStreetNumber());
        }

        if (request.getPostalCode() != null) {
            user.setPostalCode(request.getPostalCode());
        }

        if (request.getCity() != null) {
            user.setCity(request.getCity());
        }

        if (request.getAddressInfo() != null) {
            user.setAddressInfo(request.getAddressInfo());
        }
    }

    private boolean isUsernameTakenByAnotherUser(String username, AppUser existingUser) {
        return username != null
                && !username.equals(existingUser.getUsername())
                && appUserRepo.existsByUsername(username);
    }

    private boolean isEmailTakenByAnotherUser(String email, AppUser existingUser) {
        return email != null
                && !email.equals(existingUser.getEmail())
                && appUserRepo.existsByEmail(email);
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
