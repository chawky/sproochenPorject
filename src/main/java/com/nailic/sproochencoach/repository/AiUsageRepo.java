package com.nailic.sproochencoach.repository;

import com.nailic.sproochencoach.model.AiUsage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface AiUsageRepo extends JpaRepository<AiUsage, Long>, JpaSpecificationExecutor<AiUsage> {
    Page<AiUsage> findByUserId(Integer userId, Pageable pageable);

    List<AiUsage> findAllByUserId(Integer userId);

    void deleteByUserId(Integer userId);

    @Query("""
            select
                usage.provider as provider,
                usage.model as model,
                count(usage.id) as requests,
                coalesce(sum(usage.inputTokens), 0) as inputTokens,
                coalesce(sum(usage.outputTokens), 0) as outputTokens,
                coalesce(sum(usage.totalTokens), 0) as totalTokens,
                coalesce(sum(usage.estimatedCostUsd), 0) as estimatedCostUsd
            from AiUsage usage
            group by usage.provider, usage.model
            order by usage.provider, usage.model
            """)
    List<AiUsageModelSummaryProjection> summarizeByProviderAndModel();

    interface AiUsageModelSummaryProjection {
        String getProvider();

        String getModel();

        Long getRequests();

        Long getInputTokens();

        Long getOutputTokens();

        Long getTotalTokens();

        BigDecimal getEstimatedCostUsd();
    }
}
