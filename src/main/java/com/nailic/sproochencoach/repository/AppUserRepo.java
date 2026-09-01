package com.nailic.sproochencoach.repository;

import com.nailic.sproochencoach.model.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AppUserRepo extends JpaRepository<AppUser, Integer> {

    AppUser findByUsernameAndEmail(String username, String email);

    Optional<AppUser> findByUsername(String username);

    Optional<AppUser> findByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    @Query("""
            select case when count(appUser) > 0 then true else false end
            from AppUser appUser
            where appUser.id = :id
              and appUser.subscriptionPlan is not null
            """)
    boolean hasSubscriptionPlan(@Param("id") Integer id);

    List<AppUser> findAllByEmail(String email);
}
