package com.nailic.sproochencoach.repository;

import com.nailic.sproochencoach.model.PromptTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PromptTemplateRepo extends JpaRepository<PromptTemplate, Long> {
    Optional<PromptTemplate> findByPromptKey(String promptKey);

    boolean existsByPromptKey(String promptKey);

    void deleteByPromptKey(String promptKey);
}
