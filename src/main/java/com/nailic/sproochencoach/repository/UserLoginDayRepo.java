package com.nailic.sproochencoach.repository;

import com.nailic.sproochencoach.model.UserLoginDay;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface UserLoginDayRepo extends JpaRepository<UserLoginDay, Long> {
    boolean existsByUser_IdAndLoginDate(Integer userId, LocalDate loginDate);

    List<UserLoginDay> findAllByUser_IdOrderByLoginDateDesc(Integer userId);
}
