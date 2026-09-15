package com.nailic.sproochencoach.service;

import com.nailic.sproochencoach.dto.ResponseUserDto;
import com.nailic.sproochencoach.exceptions.AccountLinkingRequiredException;
import com.nailic.sproochencoach.exceptions.BadRequestException;
import com.nailic.sproochencoach.model.AppRole;
import com.nailic.sproochencoach.model.AppUser;
import com.nailic.sproochencoach.repository.AppUserRepo;
import com.nailic.sproochencoach.repository.RoleRepo;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GoogleLoginServiceTest {
    @Test
    void loginAuthenticatesExistingGoogleSubject() {
        GoogleIdentityService googleIdentityService = mock(GoogleIdentityService.class);
        AppUserRepo appUserRepo = mock(AppUserRepo.class);
        JwtService jwtService = mock(JwtService.class);
        AppUserService appUserService = mock(AppUserService.class);
        AppUser user = googleUser();
        ResponseUserDto userDto = new ResponseUserDto();

        when(googleIdentityService.verify("id-token"))
                .thenReturn(new VerifiedGoogleAccount("google-sub", "learner@example.com", true, "Sam", "Learner"));
        when(appUserRepo.findByGoogleSubject("google-sub")).thenReturn(Optional.of(user));
        when(appUserService.toResponseUserDto(user)).thenReturn(userDto);
        when(jwtService.generateToken(user)).thenReturn("app-jwt");

        GoogleLoginService service = service(googleIdentityService, appUserRepo, jwtService, appUserService);

        AuthenticatedUser authenticatedUser = service.login("id-token", "203.0.113.10");

        assertThat(authenticatedUser.user()).isSameAs(userDto);
        assertThat(authenticatedUser.jwt()).isEqualTo("app-jwt");
    }

    @Test
    void loginDoesNotAutoLinkExistingPasswordAccountByEmail() {
        GoogleIdentityService googleIdentityService = mock(GoogleIdentityService.class);
        AppUserRepo appUserRepo = mock(AppUserRepo.class);
        LoginRateLimitService loginRateLimitService = mock(LoginRateLimitService.class);
        AppUser existingUser = new AppUser();

        when(googleIdentityService.verify("id-token"))
                .thenReturn(new VerifiedGoogleAccount("google-sub", "learner@example.com", true, "Sam", "Learner"));
        when(appUserRepo.findByGoogleSubject("google-sub")).thenReturn(Optional.empty());
        when(appUserRepo.findByEmail("learner@example.com")).thenReturn(Optional.of(existingUser));

        GoogleLoginService service = service(
                googleIdentityService,
                appUserRepo,
                mock(JwtService.class),
                mock(AppUserService.class),
                loginRateLimitService
        );

        assertThatThrownBy(() -> service.login("id-token", "203.0.113.10"))
                .isInstanceOf(AccountLinkingRequiredException.class)
                .hasMessage("Account already exists. Log in first to link Google.");

        verify(loginRateLimitService).recordFailure("learner@example.com", "203.0.113.10");
    }

    @Test
    void loginCreatesNewGoogleOnlyUserWithNullPassword() {
        GoogleIdentityService googleIdentityService = mock(GoogleIdentityService.class);
        AppUserRepo appUserRepo = mock(AppUserRepo.class);
        RoleRepo roleRepo = mock(RoleRepo.class);
        JwtService jwtService = mock(JwtService.class);
        AppUserService appUserService = mock(AppUserService.class);
        AppRole userRole = new AppRole();
        userRole.setName("USER");
        ResponseUserDto userDto = new ResponseUserDto();

        when(googleIdentityService.verify("id-token"))
                .thenReturn(new VerifiedGoogleAccount("google-sub", "learner@example.com", true, "Sam", "Learner"));
        when(appUserRepo.findByGoogleSubject("google-sub")).thenReturn(Optional.empty());
        when(appUserRepo.findByEmail("learner@example.com")).thenReturn(Optional.empty());
        when(appUserRepo.existsByUsername("learner")).thenReturn(false);
        when(roleRepo.findByName("USER")).thenReturn(userRole);
        when(appUserRepo.save(any(AppUser.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(appUserService.toResponseUserDto(any(AppUser.class))).thenReturn(userDto);
        when(jwtService.generateToken(any(AppUser.class))).thenReturn("app-jwt");

        GoogleLoginService service = service(googleIdentityService, appUserRepo, roleRepo, jwtService, appUserService);

        AuthenticatedUser authenticatedUser = service.login("id-token", "203.0.113.10");

        assertThat(authenticatedUser.jwt()).isEqualTo("app-jwt");
        verify(appUserRepo).save(org.mockito.ArgumentMatchers.argThat(user ->
                "google-sub".equals(user.getGoogleSubject())
                        && "learner@example.com".equals(user.getEmail())
                        && user.isEnabled()
                        && user.getPassword() == null
                        && "learner".equals(user.getUsername())
        ));
    }

    @Test
    void loginRejectsUnverifiedGoogleEmail() {
        GoogleIdentityService googleIdentityService = mock(GoogleIdentityService.class);
        LoginRateLimitService loginRateLimitService = mock(LoginRateLimitService.class);

        when(googleIdentityService.verify("id-token"))
                .thenReturn(new VerifiedGoogleAccount("google-sub", "learner@example.com", false, "Sam", "Learner"));

        GoogleLoginService service = service(
                googleIdentityService,
                mock(AppUserRepo.class),
                mock(JwtService.class),
                mock(AppUserService.class),
                loginRateLimitService
        );

        assertThatThrownBy(() -> service.login("id-token", "203.0.113.10"))
                .isInstanceOf(BadRequestException.class);

        verify(loginRateLimitService).recordFailure("learner@example.com", "203.0.113.10");
    }

    @Test
    void loginRejectsAdminDisabledGoogleUser() {
        GoogleIdentityService googleIdentityService = mock(GoogleIdentityService.class);
        AppUserRepo appUserRepo = mock(AppUserRepo.class);
        LoginRateLimitService loginRateLimitService = mock(LoginRateLimitService.class);
        AppUser user = googleUser();
        user.setAdminDisabled(true);

        when(googleIdentityService.verify("id-token"))
                .thenReturn(new VerifiedGoogleAccount("google-sub", "learner@example.com", true, "Sam", "Learner"));
        when(appUserRepo.findByGoogleSubject("google-sub")).thenReturn(Optional.of(user));

        GoogleLoginService service = service(
                googleIdentityService,
                appUserRepo,
                mock(JwtService.class),
                mock(AppUserService.class),
                loginRateLimitService
        );

        assertThatThrownBy(() -> service.login("id-token", "203.0.113.10"))
                .isInstanceOf(AccessDeniedException.class);

        verify(loginRateLimitService).recordFailure("learner@example.com", "203.0.113.10");
    }

    @Test
    void linkCurrentUserRequiresMatchingEmailAndStoresGoogleSubject() {
        GoogleIdentityService googleIdentityService = mock(GoogleIdentityService.class);
        AppUserRepo appUserRepo = mock(AppUserRepo.class);
        AppUser currentUser = new AppUser();
        currentUser.setId(42);
        currentUser.setEmail("learner@example.com");
        ResponseUserDto response = new ResponseUserDto();
        AppUserService appUserService = mock(AppUserService.class);

        when(googleIdentityService.verify("id-token"))
                .thenReturn(new VerifiedGoogleAccount("google-sub", "learner@example.com", true, "Sam", "Learner"));
        when(appUserRepo.findById(42)).thenReturn(Optional.of(currentUser));
        when(appUserRepo.findByGoogleSubject("google-sub")).thenReturn(Optional.empty());
        when(appUserRepo.save(currentUser)).thenReturn(currentUser);
        when(appUserService.toResponseUserDto(currentUser)).thenReturn(response);

        GoogleLoginService service = service(
                googleIdentityService,
                appUserRepo,
                mock(JwtService.class),
                appUserService
        );

        ResponseUserDto linkedUser = service.linkCurrentUser(currentUser, "id-token");

        assertThat(linkedUser).isSameAs(response);
        assertThat(currentUser.getGoogleSubject()).isEqualTo("google-sub");
    }

    private AppUser googleUser() {
        AppUser user = new AppUser();
        user.setId(42);
        user.setEmail("learner@example.com");
        user.setGoogleSubject("google-sub");
        user.setEnabled(true);
        return user;
    }

    private GoogleLoginService service(
            GoogleIdentityService googleIdentityService,
            AppUserRepo appUserRepo,
            JwtService jwtService,
            AppUserService appUserService
    ) {
        return service(
                googleIdentityService,
                appUserRepo,
                mock(RoleRepo.class),
                jwtService,
                appUserService,
                mock(LoginRateLimitService.class)
        );
    }

    private GoogleLoginService service(
            GoogleIdentityService googleIdentityService,
            AppUserRepo appUserRepo,
            JwtService jwtService,
            AppUserService appUserService,
            LoginRateLimitService loginRateLimitService
    ) {
        return service(
                googleIdentityService,
                appUserRepo,
                mock(RoleRepo.class),
                jwtService,
                appUserService,
                loginRateLimitService
        );
    }

    private GoogleLoginService service(
            GoogleIdentityService googleIdentityService,
            AppUserRepo appUserRepo,
            RoleRepo roleRepo,
            JwtService jwtService,
            AppUserService appUserService
    ) {
        return service(
                googleIdentityService,
                appUserRepo,
                roleRepo,
                jwtService,
                appUserService,
                mock(LoginRateLimitService.class)
        );
    }

    private GoogleLoginService service(
            GoogleIdentityService googleIdentityService,
            AppUserRepo appUserRepo,
            RoleRepo roleRepo,
            JwtService jwtService,
            AppUserService appUserService,
            LoginRateLimitService loginRateLimitService
    ) {
        return new GoogleLoginService(
                googleIdentityService,
                appUserRepo,
                roleRepo,
                jwtService,
                appUserService,
                mock(UserLoginDayService.class),
                loginRateLimitService
        );
    }
}
