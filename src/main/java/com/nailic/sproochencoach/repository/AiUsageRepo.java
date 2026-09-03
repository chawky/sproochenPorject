package com.nailic.sproochencoach.repository;

import com.nailic.sproochencoach.model.AiUsage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AiUsageRepo extends JpaRepository<AiUsage, Long>, JpaSpecificationExecutor<AiUsage> {
    Page<AiUsage> findByUserId(Integer userId, Pageable pageable);

    List<AiUsage> findAllByUserId(Integer userId);
}
