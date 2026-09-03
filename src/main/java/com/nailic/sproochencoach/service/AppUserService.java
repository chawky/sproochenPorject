package com.nailic.sproochencoach.service;

import com.nailic.sproochencoach.dto.RequestUserDto;
import com.nailic.sproochencoach.dto.ResponseUserDto;
import com.nailic.sproochencoach.dto.SubscriptionInfoDto;
import com.nailic.sproochencoach.model.AppRole;
import com.nailic.sproochencoach.model.AppUser;
import com.nailic.sproochencoach.model.SubscriptionPlan;
import com.nailic.sproochencoach.repository.AppUserRepo;
import com.nailic.sproochencoach.repository.RoleRepo;
import com.nailic.sproochencoach.exceptions.UserAlreadyExistsException;
import com.nailic.sproochencoach.exceptions.UserNotFoundException;
import lombok.RequiredArgsConstructor;
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
    private static final String DEFAULT_REGISTRATION_ROLE = "USER";

    private final AppUserRepo appUserRepo;
    private final RoleRepo roleRepo;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final EmailAndOtpService emailAndOtpService;
    private final AuthenticationManager authenticationManager;
    private final UserLoginDayService userLoginDayService;
    private final SubscriptionAccessService subscriptionAccessService;

    public List<ResponseUserDto> findAll() {
        List<ResponseUserDto> users = appUserRepo.findAll().stream()
                .map(this::toResponseUserDto)
                .collect(Collectors.toList());

        return users;
    }

    public ResponseUserDto addUser(RequestUserDto request) {
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
        AppRole defaultRole = roleRepo.findByName(DEFAULT_REGISTRATION_ROLE);
        if (defaultRole == null) {
            throw new IllegalStateException("Default registration role USER is not configured");
        }
        user.setRoles(Set.of(defaultRole));
        user.setPassword(passwordEncoder.encode(request.getPassword()));

        AppUser savedUser = appUserRepo.save(user);

        return toResponseUserDto(savedUser);
    }

    public ResponseUserDto updateUser(Integer id, RequestUserDto request) {
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

        return toResponseUserDto(savedUser);
    }

    public ResponseUserDto findById(int id) {
        AppUser user = appUserRepo.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User not found"));
        ResponseUserDto responseUserDto = toResponseUserDto(user);
        return responseUserDto;
    }

    public ResponseUserDto login(RequestUserDto appUserDto) {
        Authentication authentication = authenticationManager
                .authenticate(new UsernamePasswordAuthenticationToken(appUserDto.getEmail(), appUserDto.getPassword()));
        AppUser user = (AppUser) authentication.getPrincipal();
        userLoginDayService.recordLogin(user);

        ResponseUserDto userDto = toResponseUserDto(user);
        userDto.setJwt(jwtService.generateToken(user));

        return userDto;
    }

    public ResponseUserDto toResponseUserDto(AppUser user) {
        ResponseUserDto response = new ResponseUserDto();
        response.setId(user.getId());
        response.setUsername(user.getUsername());
        response.setEmail(user.getEmail());
        response.setFirstName(user.getFirstName());
        response.setLastName(user.getLastName());
        response.setStreet(user.getStreet());
        response.setStreetNumber(user.getStreetNumber());
        response.setPostalCode(user.getPostalCode());
        response.setCity(user.getCity());
        response.setAddressInfo(user.getAddressInfo());
        response.setEmailVerified(user.isEnabled());
        response.setAdminDisabled(user.isAdminDisabled());
        response.setRoles(toRoleNames(user));
        response.setSubscription(toSubscriptionInfoDto(user.getSubscriptionPlan()));
        return response;
    }

    private Set<String> toRoleNames(AppUser user) {
        return user.getRoles().stream()
                .filter(role -> role != null && role.getName() != null)
                .map(AppRole::getName)
                .collect(Collectors.toSet());
    }

    private SubscriptionInfoDto toSubscriptionInfoDto(SubscriptionPlan subscriptionPlan) {
        if (subscriptionPlan == null) {
            return new SubscriptionInfoDto(false, null, null, null);
        }

        String subscriptionStatus = subscriptionPlan.getSubscriptionStatus();

        return new SubscriptionInfoDto(
                subscriptionAccessService.hasSubscriptionAccess(subscriptionStatus),
                subscriptionStatus,
                subscriptionPlan.getStartedAt(),
                subscriptionPlan.getCurrentPeriodEnd()
        );
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
