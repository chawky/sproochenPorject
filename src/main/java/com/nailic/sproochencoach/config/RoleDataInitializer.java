package com.nailic.sproochencoach.config;

import com.nailic.sproochencoach.model.AppRole;
import com.nailic.sproochencoach.repository.RoleRepo;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class RoleDataInitializer implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(RoleDataInitializer.class);
    private static final List<String> REQUIRED_ROLES = List.of("USER", "ADMIN");

    private final RoleRepo roleRepo;

    @Override
    public void run(ApplicationArguments args) {
        REQUIRED_ROLES.forEach(this::createRoleIfMissing);
    }

    private void createRoleIfMissing(String roleName) {
        if (roleRepo.existsByName(roleName)) {
            return;
        }

        AppRole role = new AppRole();
        role.setName(roleName);
        roleRepo.save(role);
        log.info("Created missing application role. roleName={}", roleName);
    }
}
