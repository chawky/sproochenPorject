package com.nailic.sproochencoach.repository;

import com.nailic.sproochencoach.model.ProcessedStripeEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProcessedStripeEventRepo
        extends JpaRepository<ProcessedStripeEvent, String> {
}