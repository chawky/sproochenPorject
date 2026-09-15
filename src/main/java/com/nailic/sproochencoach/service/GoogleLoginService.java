package com.nailic.sproochencoach.service;

import com.nailic.sproochencoach.constants.AppConstants;
import com.nailic.sproochencoach.dto.ResponseUserDto;
import com.nailic.sproochencoach.exceptions.AccountLinkingRequiredException;
import com.nailic.sproochencoach.exceptions.BadRequestException;
import com.nailic.sproochencoach.exceptions.InvalidGoogleTokenException;
import com.nailic.sproochencoach.model.AppRole;
import com.nailic.sproochencoach.model.AppUser;
import com.nailic.sproochencoach.repository.AppUserRepo;
import com.nailic.sproochencoach.repository.RoleRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Locale;
import java.util.Optional;
import java.util.Set;

@Service
@Transactional
@RequiredArgsConstructor
public class GoogleLoginService {
    private static final String ACCOUNT_LINKING_REQUIRED_MESSAGE =
            "Account already exists. Log in first to link Google.";

    private final GoogleIdentityService googleIdentityService;
    private final AppUserRepo appUserRepo;
    private final RoleRepo roleRepo;
    private final JwtService jwtService;
    private final AppUserService appUserService;
    private final UserLoginDayService userLoginDayService;
    private final LoginRateLimitService loginRateLimitService;

    public AuthenticatedUser login(String idToken, String clientIp) {
        loginRateLimitService.checkAllowed("", clientIp);

        VerifiedGoogleAccount googleAccount;
        try {
            googleAccount = googleIdentityService.verify(idToken);
        } catch (InvalidGoogleTokenException exception) {
            loginRateLimitService.recordFailure("", clientIp);
            throw exception;
        }

        loginRateLimitService.checkAllowed(googleAccount.email(), clientIp);

        if (!googleAccount.emailVerified()) {
            rejectGoogleLogin(googleAccount.email(), clientIp, new BadRequestException("Google email is not verified"));
        }

        Optional<AppUser> googleUser = appUserRepo.findByGoogleSubject(googleAccount.subject());
        if (googleUser.isPresent()) {
            return authenticateGoogleUser(googleUser.get(), googleAccount.email(), clientIp);
        }

        if (appUserRepo.findByEmail(googleAccount.email()).isPresent()) {
            rejectGoogleLogin(
                    googleAccount.email(),
                    clientIp,
                    new AccountLinkingRequiredException(ACCOUNT_LINKING_REQUIRED_MESSAGE)
            );
        }

        AppUser user = createGoogleUser(googleAccount);
        return authenticateGoogleUser(user, googleAccount.email(), clientIp);
    }

    public ResponseUserDto linkCurrentUser(AppUser currentUser, String idToken) {
        VerifiedGoogleAccount googleAccount = googleIdentityService.verify(idToken);

        if (!googleAccount.emailVerified()) {
            throw new BadRequestException("Google email is not verified");
        }

        AppUser user = appUserRepo.findById(currentUser.getId())
                .orElseThrow(() -> new AccessDeniedException("Current user not found"));

        if (user.getEmail() == null || !user.getEmail().equalsIgnoreCase(googleAccount.email())) {
            throw new BadRequestException("Google email must match your account email");
        }

        Optional<AppUser> userWithGoogleSubject = appUserRepo.findByGoogleSubject(googleAccount.subject());
        if (userWithGoogleSubject.isPresent() && !userWithGoogleSubject.get().getId().equals(user.getId())) {
            throw new AccountLinkingRequiredException("Google account is already linked to another account");
        }

        if (StringUtils.hasText(user.getGoogleSubject())
                && !user.getGoogleSubject().equals(googleAccount.subject())) {
            throw new AccountLinkingRequiredException("Account is already linked to a different Google account");
        }

        user.setGoogleSubject(googleAccount.subject());
        return appUserService.toResponseUserDto(appUserRepo.save(user));
    }

    private AuthenticatedUser authenticateGoogleUser(AppUser user, String email, String clientIp) {
        if (user.isAdminDisabled()) {
            rejectGoogleLogin(email, clientIp, new AccessDeniedException("Account is disabled"));
        }

        loginRateLimitService.recordSuccess(email);
        userLoginDayService.recordLogin(user);

        return new AuthenticatedUser(
                appUserService.toResponseUserDto(user),
                jwtService.generateToken(user)
        );
    }

    private AppUser createGoogleUser(VerifiedGoogleAccount googleAccount) {
        AppRole defaultRole = roleRepo.findByName(AppConstants.Roles.USER);
        if (defaultRole == null) {
            throw new IllegalStateException("Default registration role USER is not configured");
        }

        AppUser user = new AppUser();
        user.setGoogleSubject(googleAccount.subject());
        user.setEmail(googleAccount.email());
        user.setEnabled(true);
        user.setRoles(Set.of(defaultRole));
        user.setFirstName(googleAccount.firstName());
        user.setLastName(googleAccount.lastName());
        user.setUsername(uniqueUsernameFromEmail(googleAccount.email()));
        user.setPassword(null);

        return appUserRepo.save(user);
    }

    private String uniqueUsernameFromEmail(String email) {
        String localPart = email.substring(0, email.indexOf('@'));
        String baseUsername = localPart
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9._-]", "");
        if (!StringUtils.hasText(baseUsername)) {
            baseUsername = "google-user";
        }
        if (baseUsername.length() > 80) {
            baseUsername = baseUsername.substring(0, 80);
        }

        String username = baseUsername;
        int suffix = 1;
        while (appUserRepo.existsByUsername(username)) {
            username = baseUsername + suffix;
            suffix++;
        }
        return username;
    }

    private void rejectGoogleLogin(String email, String clientIp, RuntimeException exception) {
        loginRateLimitService.recordFailure(email, clientIp);
        throw exception;
    }
}
