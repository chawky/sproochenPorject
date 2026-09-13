package com.nailic.sproochencoach.config;

import com.nailic.sproochencoach.constants.AppConstants;
import com.nailic.sproochencoach.model.AppRole;
import com.nailic.sproochencoach.model.AppUser;
import com.nailic.sproochencoach.repository.AppUserRepo;
import com.nailic.sproochencoach.repository.RoleRepo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminInitializerTest {
    @Mock
    private AppUserRepo userRepo;

    @Mock
    private RoleRepo roleRepo;

    @Test
    void grantsAdminRoleToConfiguredUsers() {
        AdminInitializer initializer = new AdminInitializer(userRepo, roleRepo);
        ReflectionTestUtils.setField(initializer, "adminEmails", List.of(
                " first@example.com ",
                "second@example.com",
                "",
                "first@example.com",
                "missing@example.com"
        ));

        AppRole adminRole = new AppRole();
        adminRole.setName(AppConstants.Roles.ADMIN);
        AppUser firstUser = new AppUser();
        AppUser secondUser = new AppUser();

        when(roleRepo.findByName(AppConstants.Roles.ADMIN)).thenReturn(adminRole);
        when(userRepo.findByEmail("first@example.com")).thenReturn(Optional.of(firstUser));
        when(userRepo.findByEmail("second@example.com")).thenReturn(Optional.of(secondUser));
        when(userRepo.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        initializer.run();

        assertThat(firstUser.getRoles()).containsExactly(adminRole);
        assertThat(secondUser.getRoles()).containsExactly(adminRole);
        verify(userRepo).save(firstUser);
        verify(userRepo).save(secondUser);
        verify(userRepo).findByEmail("first@example.com");
    }
}
