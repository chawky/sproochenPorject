package com.nailic.sproochencoach.controller;

import com.nailic.sproochencoach.dto.AdminSupportEmailDetailDto;
import com.nailic.sproochencoach.dto.AdminSupportEmailListDto;
import com.nailic.sproochencoach.dto.AdminSupportEmailSyncResultDto;
import com.nailic.sproochencoach.dto.ApiResponse;
import com.nailic.sproochencoach.dto.SupportEmailAttachmentDownload;
import com.nailic.sproochencoach.service.AdminSupportEmailService;
import com.nailic.sproochencoach.service.SupportEmailSyncService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/support-emails")
@RequiredArgsConstructor
public class AdminSupportEmailController {
    private final AdminSupportEmailService adminSupportEmailService;
    private final SupportEmailSyncService supportEmailSyncService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<AdminSupportEmailListDto>>> list() {
        return ResponseEntity.ok(new ApiResponse<>(
                true,
                "Support emails retrieved successfully",
                adminSupportEmailService.list()
        ));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AdminSupportEmailDetailDto>> get(@PathVariable Long id) {
        return ResponseEntity.ok(new ApiResponse<>(
                true,
                "Support email retrieved successfully",
                adminSupportEmailService.get(id)
        ));
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<ApiResponse<AdminSupportEmailDetailDto>> markRead(@PathVariable Long id) {
        return ResponseEntity.ok(new ApiResponse<>(
                true,
                "Support email marked as read",
                adminSupportEmailService.markRead(id)
        ));
    }

    @PostMapping("/sync")
    public ResponseEntity<ApiResponse<AdminSupportEmailSyncResultDto>> sync() {
        return ResponseEntity.ok(new ApiResponse<>(
                true,
                "Support emails synchronized successfully",
                supportEmailSyncService.sync()
        ));
    }

    @GetMapping("/{emailId}/attachments/{attachmentId}")
    public ResponseEntity<byte[]> downloadAttachment(
            @PathVariable Long emailId,
            @PathVariable Long attachmentId
    ) {
        SupportEmailAttachmentDownload download =
                adminSupportEmailService.downloadAttachment(emailId, attachmentId);

        return ResponseEntity.ok()
                .contentType(safeMediaType(download.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(safeFilename(download.filename()))
                        .build()
                        .toString())
                .body(download.content());
    }

    private MediaType safeMediaType(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }

        try {
            return MediaType.parseMediaType(contentType);
        } catch (Exception exception) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }

    private String safeFilename(String filename) {
        if (filename == null || filename.isBlank()) {
            return "attachment";
        }

        String cleaned = filename.replace("\\", "/");
        int slashIndex = cleaned.lastIndexOf('/');
        if (slashIndex >= 0) {
            cleaned = cleaned.substring(slashIndex + 1);
        }

        cleaned = cleaned.replaceAll("[\\r\\n\\t\\x00-\\x1F\\x7F]", "_").trim();
        if (cleaned.isBlank() || cleaned.equals(".") || cleaned.equals("..")) {
            return "attachment";
        }

        return cleaned.length() > 150 ? cleaned.substring(cleaned.length() - 150) : cleaned;
    }
}
