package com.nailic.sproochencoach.repository;

import com.nailic.sproochencoach.model.SupportEmailAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SupportEmailAttachmentRepo extends JpaRepository<SupportEmailAttachment, Long> {
    List<SupportEmailAttachment> findBySupportEmailIdOrderByIdAsc(Long supportEmailId);

    Optional<SupportEmailAttachment> findByIdAndSupportEmailId(Long id, Long supportEmailId);

    boolean existsBySupportEmailIdAndResendAttachmentId(Long supportEmailId, String resendAttachmentId);
}
