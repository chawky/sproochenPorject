package com.nailic.sproochencoach.service;

import com.nailic.sproochencoach.dto.AdminSupportEmailDetailDto;
import com.nailic.sproochencoach.dto.AdminSupportEmailAttachmentDto;
import com.nailic.sproochencoach.dto.AdminSupportEmailListDto;
import com.nailic.sproochencoach.dto.SupportEmailAttachmentDownload;
import com.nailic.sproochencoach.model.SupportEmail;
import com.nailic.sproochencoach.model.SupportEmailAttachment;
import com.nailic.sproochencoach.repository.SupportEmailAttachmentRepo;
import com.nailic.sproochencoach.repository.SupportEmailRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminSupportEmailService {
    private final SupportEmailRepo supportEmailRepo;
    private final SupportEmailAttachmentRepo supportEmailAttachmentRepo;
    private final ResendReceivedEmailClient resendReceivedEmailClient;

    @Transactional(readOnly = true)
    public List<AdminSupportEmailListDto> list() {
        return supportEmailRepo.findAllByOrderByReceivedAtDesc()
                .stream()
                .map(this::toListDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public AdminSupportEmailDetailDto get(Long id) {
        return toDetailDto(findById(id));
    }

    @Transactional
    public AdminSupportEmailDetailDto markRead(Long id) {
        SupportEmail supportEmail = findById(id);
        supportEmail.setRead(true);
        return toDetailDto(supportEmail);
    }

    @Transactional(readOnly = true)
    public SupportEmailAttachmentDownload downloadAttachment(Long emailId, Long attachmentId) {
        SupportEmail supportEmail = findById(emailId);
        SupportEmailAttachment attachment = supportEmailAttachmentRepo.findByIdAndSupportEmailId(attachmentId, emailId)
                .orElseThrow(() -> new com.nailic.sproochencoach.exceptions.UserNotFoundException("Support email attachment not found"));

        return resendReceivedEmailClient.downloadReceivedAttachment(
                supportEmail.getResendEmailId(),
                attachment.getResendAttachmentId(),
                attachment.getFilename(),
                attachment.getContentType()
        );
    }

    private SupportEmail findById(Long id) {
        return supportEmailRepo.findById(id)
                .orElseThrow(() -> new com.nailic.sproochencoach.exceptions.UserNotFoundException("Support email not found"));
    }

    private AdminSupportEmailListDto toListDto(SupportEmail supportEmail) {
        return new AdminSupportEmailListDto(
                supportEmail.getId(),
                supportEmail.getFromEmail(),
                supportEmail.getSubject(),
                supportEmail.getReceivedAt(),
                supportEmail.isRead()
        );
    }

    private AdminSupportEmailDetailDto toDetailDto(SupportEmail supportEmail) {
        return new AdminSupportEmailDetailDto(
                supportEmail.getId(),
                supportEmail.getFromEmail(),
                supportEmail.getToEmail(),
                supportEmail.getSubject(),
                supportEmail.getTextBody(),
                supportEmail.getHtmlBody(),
                supportEmail.getReceivedAt(),
                supportEmail.isRead(),
                attachments(supportEmail.getId())
        );
    }

    private List<AdminSupportEmailAttachmentDto> attachments(Long supportEmailId) {
        return supportEmailAttachmentRepo.findBySupportEmailIdOrderByIdAsc(supportEmailId)
                .stream()
                .map(this::toAttachmentDto)
                .toList();
    }

    private AdminSupportEmailAttachmentDto toAttachmentDto(SupportEmailAttachment attachment) {
        return new AdminSupportEmailAttachmentDto(
                attachment.getId(),
                attachment.getFilename(),
                attachment.getContentType(),
                attachment.getContentDisposition(),
                attachment.getContentId(),
                attachment.getSizeBytes()
        );
    }
}
