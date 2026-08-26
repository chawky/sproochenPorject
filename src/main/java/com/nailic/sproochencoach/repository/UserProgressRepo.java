package com.nailic.sproochencoach.repository;

import com.nailic.sproochencoach.model.UserProgress;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

@Repository
public interface UserProgressRepo extends JpaRepository<UserProgress,Long> {
    List<UserProgress> findAllByUser_IdOrderByIdDesc(Integer userId);
}
