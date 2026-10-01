package com.nailic.sproochencoach.repository;

import com.nailic.sproochencoach.model.AppUser;
import com.nailic.sproochencoach.model.SubscriptionPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AppUserRepo extends JpaRepository<AppUser, Integer> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select user from AppUser user where user.id = :id")
    Optional<AppUser> findByIdForUpdate(@Param("id") Integer id);

    AppUser findByUsernameAndEmail(String username, String email);

    Optional<AppUser> findByUsername(String username);

    Optional<AppUser> findByEmail(String email);

    Optional<AppUser> findByGoogleSubject(String googleSubject);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    @Query("""
            select subscriptionPlan
            from AppUser appUser
            join appUser.subscriptionPlan subscriptionPlan
            where appUser.id = :id
            """)
    Optional<SubscriptionPlan> findSubscriptionPlanByUserId(@Param("id") Integer id);

    List<AppUser> findAllByEmail(String email);
}
