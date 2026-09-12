package com.nailic.sproochencoach.service;

import com.nailic.sproochencoach.dto.RequestUserDto;
import com.nailic.sproochencoach.model.AppUser;
import com.nailic.sproochencoach.repository.AppUserRepo;
import com.nailic.sproochencoach.repository.RoleRepo;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
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

    private AppUser verifiedUser() {
        AppUser user = new AppUser();
        user.setId(42);
        user.setEmail("old@example.com");
        user.setEnabled(true);
        return user;
    }

    private AppUserService service(AppUserRepo appUserRepo) {
        return new AppUserService(
                appUserRepo,
                mock(RoleRepo.class),
                mock(PasswordEncoder.class),
                mock(JwtService.class),
                mock(EmailAndOtpService.class),
                mock(AuthenticationManager.class),
                mock(UserLoginDayService.class),
                new SubscriptionAccessService()
        );
    }
}
