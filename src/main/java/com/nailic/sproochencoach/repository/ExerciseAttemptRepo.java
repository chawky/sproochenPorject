package com.nailic.sproochencoach.repository;

import com.nailic.sproochencoach.model.ExerciseAttempt;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExerciseAttemptRepo extends JpaRepository<ExerciseAttempt, Long> {
    List<ExerciseAttempt> findAllByUser_IdOrderByIdDesc(Integer userId);

    Page<ExerciseAttempt> findByUser_Id(Integer userId, Pageable pageable);
}
