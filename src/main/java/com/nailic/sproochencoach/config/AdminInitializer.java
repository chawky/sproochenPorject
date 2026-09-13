package com.nailic.sproochencoach.config;

import com.nailic.sproochencoach.constants.AppConstants;
import com.nailic.sproochencoach.model.AppRole;
import com.nailic.sproochencoach.model.AppUser;
import com.nailic.sproochencoach.repository.AppUserRepo;
import com.nailic.sproochencoach.repository.RoleRepo;
import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@Order(1)
@RequiredArgsConstructor
public class AdminInitializer implements CommandLineRunner {

    private final AppUserRepo userRepo;
    private final RoleRepo roleRepo;

    @Value(AppConstants.PropertyPlaceholders.APP_ADMIN_EMAILS)
    private List<String> adminEmails;

    @Override
    @Transactional
    public void run(String... args) {

        if (adminEmails == null) {
            return;
        }

        List<String> emails = adminEmails.stream()
                .map(String::trim)
                .filter(email -> !email.isBlank())
                .distinct()
                .toList();

        if (emails.isEmpty()) {
            return;
        }

        AppRole adminRole = roleRepo.findByName(AppConstants.Roles.ADMIN);
        if (adminRole == null) {
            throw new IllegalStateException("ADMIN role does not exist");
        }

        emails.forEach(email -> userRepo.findByEmail(email).ifPresent(user -> {
            user.getRoles().add(adminRole);
            userRepo.save(user);
        }));
    }
}
