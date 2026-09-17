package com.nailic.sproochencoach.service;

import com.nailic.sproochencoach.dto.RequestUserDto;
import com.nailic.sproochencoach.dto.ChangePasswordRequest;
import com.nailic.sproochencoach.dto.SetPasswordRequest;
import com.nailic.sproochencoach.model.AppUser;
import com.nailic.sproochencoach.model.SubscriptionPlan;
import com.nailic.sproochencoach.repository.AppUserRepo;
import com.nailic.sproochencoach.repository.RoleRepo;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AppUserServiceTest {
    @Test
    void updateUserMarksEmailUnverifiedWhenEmailChanges() {
        AppUserRepo appUserRepo = mock(AppUserRepo.class);
        AppUser user = verifiedUser();
        RequestUserDto request = new RequestUserDto();
        request.setEmail("new@example.com");

        when(appUserRepo.findById(42)).thenReturn(Optional.of(user));
        when(appUserRepo.save(any(AppUser.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AppUserService service = service(appUserRepo);

        service.updateUser(42, request);

        assertThat(user.getEmail()).isEqualTo("new@example.com");
        assertThat(user.isEnabled()).isFalse();
    }

    @Test
    void updateUserKeepsEmailVerifiedWhenEmailIsUnchanged() {
        AppUserRepo appUserRepo = mock(AppUserRepo.class);
        AppUser user = verifiedUser();
        RequestUserDto request = new RequestUserDto();
        request.setFirstName("Sam");

        when(appUserRepo.findById(42)).thenReturn(Optional.of(user));
        when(appUserRepo.save(any(AppUser.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AppUserService service = service(appUserRepo);

        service.updateUser(42, request);

        assertThat(user.getEmail()).isEqualTo("old@example.com");
        assertThat(user.isEnabled()).isTrue();
    }

    @Test
    void updateUserDoesNotChangePasswordFromProfileRequest() {
        AppUserRepo appUserRepo = mock(AppUserRepo.class);
        AppUser user = verifiedUser();
        user.setPassword("old-password");
        RequestUserDto request = new RequestUserDto();
        request.setPassword("new-password");

        when(appUserRepo.findById(42)).thenReturn(Optional.of(user));
        when(appUserRepo.save(any(AppUser.class))).thenAnswer(invocation -> invocation.getArgument(0));
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        when(passwordEncoder.encode("new-password")).thenReturn("encoded-password");

        AppUserService service = service(
                appUserRepo,
                mock(AuthenticationManager.class),
                mock(LoginRateLimitService.class),
                passwordEncoder
        );

        service.updateUser(42, request);

        assertThat(user.getPassword()).isEqualTo("old-password");
        assertThat(user.getTokenVersion()).isZero();
        verify(passwordEncoder, never()).encode("new-password");
    }

    @Test
    void setPasswordCreatesFirstPasswordAndReturnsFreshJwt() {
        AppUserRepo appUserRepo = mock(AppUserRepo.class);
        AppUser user = verifiedUser();
        user.setPassword(null);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        JwtService jwtService = mock(JwtService.class);

        when(appUserRepo.findById(42)).thenReturn(Optional.of(user));
        when(appUserRepo.save(user)).thenReturn(user);
        when(passwordEncoder.encode("new-password")).thenReturn("encoded-password");
        when(jwtService.generateToken(user)).thenReturn("fresh-jwt");

        AppUserService service = service(
                appUserRepo,
                mock(AuthenticationManager.class),
                mock(LoginRateLimitService.class),
                passwordEncoder,
                jwtService
        );

        AuthenticatedUser authenticatedUser = service.setPassword(
                42,
                new SetPasswordRequest("new-password", "new-password")
        );

        assertThat(user.getPassword()).isEqualTo("encoded-password");
        assertThat(user.getTokenVersion()).isEqualTo(1);
        assertThat(authenticatedUser.jwt()).isEqualTo("fresh-jwt");
        assertThat(authenticatedUser.user().isHasPassword()).isTrue();
    }

    @Test
    void changePasswordRequiresCurrentPassword() {
        AppUserRepo appUserRepo = mock(AppUserRepo.class);
        AppUser user = verifiedUser();
        user.setPassword("encoded-old-password");
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);

        when(appUserRepo.findById(42)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong-password", "encoded-old-password")).thenReturn(false);

        AppUserService service = service(
                appUserRepo,
                mock(AuthenticationManager.class),
                mock(LoginRateLimitService.class),
                passwordEncoder
        );

        assertThatThrownBy(() -> service.changePassword(
                42,
                new ChangePasswordRequest("wrong-password", "new-password", "new-password")
        )).isInstanceOf(BadCredentialsException.class);

        verify(appUserRepo, never()).save(any(AppUser.class));
    }

    @Test
    void changePasswordUpdatesPasswordAndReturnsFreshJwt() {
        AppUserRepo appUserRepo = mock(AppUserRepo.class);
        AppUser user = verifiedUser();
        user.setPassword("encoded-old-password");
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        JwtService jwtService = mock(JwtService.class);

        when(appUserRepo.findById(42)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("old-password", "encoded-old-password")).thenReturn(true);
        when(passwordEncoder.encode("new-password")).thenReturn("encoded-new-password");
        when(appUserRepo.save(user)).thenReturn(user);
        when(jwtService.generateToken(user)).thenReturn("fresh-jwt");

        AppUserService service = service(
                appUserRepo,
                mock(AuthenticationManager.class),
                mock(LoginRateLimitService.class),
                passwordEncoder,
                jwtService
        );

        AuthenticatedUser authenticatedUser = service.changePassword(
                42,
                new ChangePasswordRequest("old-password", "new-password", "new-password")
        );

        assertThat(user.getPassword()).isEqualTo("encoded-new-password");
        assertThat(user.getTokenVersion()).isEqualTo(1);
        assertThat(authenticatedUser.jwt()).isEqualTo("fresh-jwt");
    }

    @Test
    void loginRecordsFailedAuthenticationForRateLimiting() {
        AuthenticationManager authenticationManager = mock(AuthenticationManager.class);
        LoginRateLimitService loginRateLimitService = mock(LoginRateLimitService.class);
        AppUserRepo appUserRepo = mock(AppUserRepo.class);
        RequestUserDto request = new RequestUserDto();
        request.setEmail("learner@example.com");
        request.setPassword("wrong-password");

        when(appUserRepo.findByEmail("learner@example.com")).thenReturn(Optional.empty());
        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        AppUserService service = service(
                appUserRepo,
                authenticationManager,
                loginRateLimitService
        );

        assertThatThrownBy(() -> service.login(request, "203.0.113.10"))
                .isInstanceOf(BadCredentialsException.class);

        verify(loginRateLimitService).checkAllowed("learner@example.com", "203.0.113.10");
        verify(loginRateLimitService).recordFailure("learner@example.com", "203.0.113.10");
    }

    @Test
    void loginRejectsGoogleOnlyAccountWithGenericBadCredentials() {
        AuthenticationManager authenticationManager = mock(AuthenticationManager.class);
        LoginRateLimitService loginRateLimitService = mock(LoginRateLimitService.class);
        AppUserRepo appUserRepo = mock(AppUserRepo.class);
        AppUser googleOnlyUser = verifiedUser();
        googleOnlyUser.setPassword(null);
        RequestUserDto request = new RequestUserDto();
        request.setEmail("old@example.com");
        request.setPassword("password");

        when(appUserRepo.findByEmail("old@example.com")).thenReturn(Optional.of(googleOnlyUser));

        AppUserService service = service(
                appUserRepo,
                authenticationManager,
                loginRateLimitService
        );

        assertThatThrownBy(() -> service.login(request, "203.0.113.10"))
                .isInstanceOf(BadCredentialsException.class);

        verify(loginRateLimitService).recordFailure("old@example.com", "203.0.113.10");
        verify(authenticationManager, never()).authenticate(any());
    }

    @Test
    void toResponseUserDtoMarksGoogleLinked() {
        AppUser user = verifiedUser();
        user.setGoogleSubject("google-subject");

        AppUserService service = service(mock(AppUserRepo.class));

        assertThat(service.toResponseUserDto(user).isGoogleLinked()).isTrue();
    }

    @Test
    void toResponseUserDtoExposesHasPassword() {
        AppUser user = verifiedUser();
        user.setPassword("encoded-password");

        AppUserService service = service(mock(AppUserRepo.class));

        assertThat(service.toResponseUserDto(user).isHasPassword()).isTrue();
    }

    @Test
    void toResponseUserDtoExposesSubscriptionCancelAtPeriodEnd() {
        AppUser user = verifiedUser();
        SubscriptionPlan subscriptionPlan = new SubscriptionPlan();
        subscriptionPlan.setSubscriptionStatus("active");
        subscriptionPlan.setCancelAtPeriodEnd(true);
        user.setSubscriptionPlan(subscriptionPlan);

        AppUserService service = service(mock(AppUserRepo.class));

        assertThat(service.toResponseUserDto(user).getSubscription().isSubscribed()).isTrue();
        assertThat(service.toResponseUserDto(user).getSubscription().isCancelAtPeriodEnd()).isTrue();
    }

    private AppUser verifiedUser() {
        AppUser user = new AppUser();
        user.setId(42);
        user.setEmail("old@example.com");
        user.setEnabled(true);
        return user;
    }

    private AppUserService service(AppUserRepo appUserRepo) {
        return service(
                appUserRepo,
                mock(AuthenticationManager.class),
                mock(LoginRateLimitService.class)
        );
    }

    private AppUserService service(
            AppUserRepo appUserRepo,
            AuthenticationManager authenticationManager,
            LoginRateLimitService loginRateLimitService
    ) {
        return service(
                appUserRepo,
                authenticationManager,
                loginRateLimitService,
                mock(PasswordEncoder.class),
                mock(JwtService.class)
        );
    }

    private AppUserService service(
            AppUserRepo appUserRepo,
            AuthenticationManager authenticationManager,
            LoginRateLimitService loginRateLimitService,
            PasswordEncoder passwordEncoder
    ) {
        return service(
                appUserRepo,
                authenticationManager,
                loginRateLimitService,
                passwordEncoder,
                mock(JwtService.class)
        );
    }

    private AppUserService service(
            AppUserRepo appUserRepo,
            AuthenticationManager authenticationManager,
            LoginRateLimitService loginRateLimitService,
            PasswordEncoder passwordEncoder,
            JwtService jwtService
    ) {
        return new AppUserService(
                appUserRepo,
                mock(RoleRepo.class),
                passwordEncoder,
                jwtService,
                mock(EmailAndOtpService.class),
                authenticationManager,
                mock(UserLoginDayService.class),
                new SubscriptionAccessService(),
                loginRateLimitService
        );
    }
}

