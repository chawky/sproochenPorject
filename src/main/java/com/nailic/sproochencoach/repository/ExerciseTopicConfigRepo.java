package com.nailic.sproochencoach.repository;

import com.nailic.sproochencoach.model.ExerciseTopicConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ExerciseTopicConfigRepo extends JpaRepository<ExerciseTopicConfig, Long> {
    Optional<ExerciseTopicConfig> findByCode(String code);

    boolean existsByCode(String code);

    boolean existsByLevelCode(String levelCode);

    List<ExerciseTopicConfig> findAllByOrderByLevelCodeAscCodeAsc();
}
