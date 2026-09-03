package com.nailic.sproochencoach.repository;

import com.nailic.sproochencoach.model.ExerciseLevelConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ExerciseLevelConfigRepo extends JpaRepository<ExerciseLevelConfig, Long> {
    Optional<ExerciseLevelConfig> findByCode(String code);

    boolean existsByCode(String code);

    List<ExerciseLevelConfig> findAllByOrderByCodeAsc();
}
