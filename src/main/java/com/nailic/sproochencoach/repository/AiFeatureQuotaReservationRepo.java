package com.nailic.sproochencoach.repository;

import com.nailic.sproochencoach.model.AiFeatureQuotaReservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface AiFeatureQuotaReservationRepo extends JpaRepository<AiFeatureQuotaReservation, String> {
    long countByUserIdAndFeatureAndWindowStart(Integer userId, String feature, LocalDateTime windowStart);

    @Modifying
    @Query("delete from AiFeatureQuotaReservation reservation where reservation.id = :id and reservation.userId = :userId and reservation.feature = :feature")
    int deleteOwned(@Param("id") String id, @Param("userId") Integer userId, @Param("feature") String feature);
}
