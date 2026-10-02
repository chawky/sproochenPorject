package com.nailic.sproochencoach.repository;

import com.nailic.sproochencoach.model.AiFeatureQuotaReservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface AiFeatureQuotaReservationRepo extends JpaRepository<AiFeatureQuotaReservation, String> {
    long countByUserIdAndFeatureAndWindowStartAndCreatedAtAfter(
            Integer userId,
            String feature,
            LocalDateTime windowStart,
            LocalDateTime expiryCutoff
    );

    @Modifying
    @Query("delete from AiFeatureQuotaReservation reservation where reservation.userId = :userId and reservation.feature = :feature and reservation.windowStart = :windowStart and reservation.createdAt <= :expiryCutoff")
    int deleteExpired(
            @Param("userId") Integer userId,
            @Param("feature") String feature,
            @Param("windowStart") LocalDateTime windowStart,
            @Param("expiryCutoff") LocalDateTime expiryCutoff
    );

    @Modifying
    @Query("delete from AiFeatureQuotaReservation reservation where reservation.id = :id and reservation.userId = :userId and reservation.feature = :feature")
    int deleteOwned(@Param("id") String id, @Param("userId") Integer userId, @Param("feature") String feature);

    @Modifying
    @Query("delete from AiFeatureQuotaReservation reservation where reservation.id = :id and reservation.userId = :userId and reservation.feature = :feature and reservation.createdAt > :expiryCutoff")
    int deleteOwnedActive(
            @Param("id") String id,
            @Param("userId") Integer userId,
            @Param("feature") String feature,
            @Param("expiryCutoff") LocalDateTime expiryCutoff
    );
}
