package com.nailic.sproochencoach.repository;

import com.nailic.sproochencoach.model.OutboundApiCallLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OutboundApiCallLogRepo extends JpaRepository<OutboundApiCallLog, Long> {
}
