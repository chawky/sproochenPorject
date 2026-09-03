package com.nailic.sproochencoach.repository;

import com.nailic.sproochencoach.model.ExerciseTypeConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ExerciseTypeConfigRepo extends JpaRepository<ExerciseTypeConfig, Long> {
    Optional<ExerciseTypeConfig> findByCode(String code);

    boolean existsByCode(String code);

    List<ExerciseTypeConfig> findAllByOrderByCodeAsc();
}
