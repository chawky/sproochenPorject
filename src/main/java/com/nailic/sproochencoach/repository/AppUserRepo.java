package com.nailic.sproochencoach.repository;

import com.nailic.sproochencoach.model.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AppUserRepo extends JpaRepository<AppUser, Integer> {

  AppUser findByUsernameAndEmail(String username, String email);

 Optional<AppUser> findByUsername(String username);

    AppUser findByEmail(String email);
}
