package com.nailic.sproochencoach.config;

import com.nailic.sproochencoach.constants.AppConstants;
import com.nailic.sproochencoach.model.AppRole;
import com.nailic.sproochencoach.model.AppUser;
import com.nailic.sproochencoach.repository.AppUserRepo;
import com.nailic.sproochencoach.repository.RoleRepo;
import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class AdminInitializer implements CommandLineRunner {

    private final AppUserRepo userRepo;
    private final RoleRepo roleRepo;

    @Value(AppConstants.PropertyPlaceholders.APP_ADMIN_EMAIL)
    private String adminEmail;

    @Override
    @Transactional
    public void run(String... args) {

        if (adminEmail == null || adminEmail.isBlank()) {
            return;
        }

        AppUser user = userRepo.findByEmail(adminEmail)
                .orElse(null);

        if (user == null) {
            return;
        }

        AppRole adminRole = roleRepo.findByName(AppConstants.Roles.ADMIN);
        if (adminRole == null) {
            throw new IllegalStateException("ADMIN role does not exist");
        }
        user.getRoles().add(adminRole);
        userRepo.save(user);
    }
}
