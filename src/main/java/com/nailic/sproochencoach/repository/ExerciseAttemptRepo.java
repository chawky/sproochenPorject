package com.nailic.sproochencoach.repository;

import com.nailic.sproochencoach.model.ExerciseAttempt;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.time.LocalDateTime;

@Repository
public interface ExerciseAttemptRepo extends JpaRepository<ExerciseAttempt, Long> {
    List<ExerciseAttempt> findAllByUser_IdOrderByIdDesc(Integer userId);

    Page<ExerciseAttempt> findByUser_Id(Integer userId, Pageable pageable);

    long countByUser_IdAndExerciseTypeAndGeneratedAtGreaterThanEqualAndGeneratedAtLessThan(
            Integer userId,
            String exerciseType,
            LocalDateTime fromInclusive,
            LocalDateTime toExclusive
    );

    void deleteByUser_Id(Integer userId);

    @Modifying
    @Query("""
            update ExerciseAttempt attempt
            set attempt.status = com.nailic.sproochencoach.model.ExerciseAttemptStatus.EVALUATING
            where attempt.id = :attemptId
              and attempt.user.id = :userId
              and attempt.exerciseType = :exerciseType
              and attempt.status = com.nailic.sproochencoach.model.ExerciseAttemptStatus.GENERATED
              and attempt.evaluatedAt is null
            """)
    int claimEvaluation(@Param("attemptId") Long attemptId, @Param("userId") Integer userId, @Param("exerciseType") String exerciseType);

    @Modifying
    @Query("""
            update ExerciseAttempt attempt
            set attempt.status = com.nailic.sproochencoach.model.ExerciseAttemptStatus.GENERATED
            where attempt.id = :attemptId
              and attempt.user.id = :userId
              and attempt.exerciseType = :exerciseType
              and attempt.status = com.nailic.sproochencoach.model.ExerciseAttemptStatus.EVALUATING
              and attempt.evaluatedAt is null
            """)
    int releaseEvaluation(@Param("attemptId") Long attemptId, @Param("userId") Integer userId, @Param("exerciseType") String exerciseType);
}
