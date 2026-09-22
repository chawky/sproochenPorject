package com.nailic.sproochencoach.service;

import com.nailic.sproochencoach.dto.AdminSupportEmailDetailDto;
import com.nailic.sproochencoach.dto.AdminSupportEmailListDto;
import com.nailic.sproochencoach.model.SupportEmail;
import com.nailic.sproochencoach.repository.SupportEmailRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminSupportEmailService {
    private final SupportEmailRepo supportEmailRepo;

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
                supportEmail.isRead()
        );
    }
}
