package com.nailic.sproochencoach.service;

import com.nailic.sproochencoach.dto.AdminSupportEmailSyncResultDto;
import com.nailic.sproochencoach.dto.ResendReceivedEmailDto;
import com.nailic.sproochencoach.dto.ResendReceivedEmailListDto;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class SupportEmailSyncService {
    private static final Logger log = LoggerFactory.getLogger(SupportEmailSyncService.class);
    private static final int PAGE_LIMIT = 100;

    private final ResendReceivedEmailClient resendReceivedEmailClient;
    private final SupportEmailIngestionService supportEmailIngestionService;

    public AdminSupportEmailSyncResultDto sync() {
        int imported = 0;
        int existing = 0;
        int ignored = 0;
        int attachmentsAdded = 0;
        int failed = 0;
        String after = null;
        Set<String> seenPageCursors = new HashSet<>();

        while (true) {
            ResendReceivedEmailListDto page = resendReceivedEmailClient.listReceivedEmails(PAGE_LIMIT, after);
            List<ResendReceivedEmailDto> emails = page.getData() == null ? List.of() : page.getData();

            for (ResendReceivedEmailDto listedEmail : emails) {
                if (!StringUtils.hasText(listedEmail.getId())) {
                    ignored++;
                    continue;
                }

                try {
                    SupportEmailIngestionResult result =
                            supportEmailIngestionService.ingestFromResend(listedEmail.getId());
                    imported += result.importCreated() ? 1 : 0;
                    existing += result.alreadyExisting() ? 1 : 0;
                    ignored += result.ignoredEmail() ? 1 : 0;
                    attachmentsAdded += result.attachmentsAdded();
                } catch (RuntimeException exception) {
                    failed++;
                    log.warn("Failed to sync Resend received email. emailId={}, reason={}", listedEmail.getId(), exception.getMessage());
                }
            }

            if (!page.isHas_more() || emails.isEmpty()) {
                break;
            }

            String nextAfter = emails.get(emails.size() - 1).getId();
            if (!StringUtils.hasText(nextAfter) || !seenPageCursors.add(nextAfter)) {
                log.warn("Stopping Resend received email sync because pagination cursor did not advance. cursor={}", nextAfter);
                break;
            }
            after = nextAfter;
        }

        return new AdminSupportEmailSyncResultDto(imported, existing, ignored, attachmentsAdded, failed);
    }
}
