package com.nailic.sproochencoach.service;

import com.nailic.sproochencoach.model.AppUser;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class LoggedInUser {

    public AppUser get() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AuthenticationCredentialsNotFoundException("No authenticated user found");
        }

        Object principal = authentication.getPrincipal();
        if (!(principal instanceof AppUser user)) {
            throw new AuthenticationCredentialsNotFoundException("Authenticated principal is not an application user");
        }

        return user;
    }

    public Integer getId() {
        return get().getId();
    }

    public String getEmail() {
        return get().getEmail();
    }
}
