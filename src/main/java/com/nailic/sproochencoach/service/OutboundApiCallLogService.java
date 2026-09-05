package com.nailic.sproochencoach.service;

import com.nailic.sproochencoach.model.OutboundApiCallLog;
import com.nailic.sproochencoach.repository.OutboundApiCallLogRepo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OutboundApiCallLogService {
    private static final int MAX_ERROR_MESSAGE_LENGTH = 1000;

    private final OutboundApiCallLogRepo outboundApiCallLogRepo;

    public OutboundApiCallLogService(OutboundApiCallLogRepo outboundApiCallLogRepo) {
        this.outboundApiCallLogRepo = outboundApiCallLogRepo;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void save(
            String provider,
            String method,
            String uri,
            Integer statusCode,
            long durationMs,
            String outcome,
            Exception exception
    ) {
        OutboundApiCallLog apiCallLog = new OutboundApiCallLog();
        apiCallLog.setProvider(provider);
        apiCallLog.setMethod(method);
        apiCallLog.setUri(uri);
        apiCallLog.setStatusCode(statusCode);
        apiCallLog.setDurationMs(durationMs);
        apiCallLog.setOutcome(outcome);

        if (exception != null) {
            apiCallLog.setErrorType(exception.getClass().getSimpleName());
            apiCallLog.setErrorMessage(truncate(exception.getMessage(), MAX_ERROR_MESSAGE_LENGTH));
        }

        outboundApiCallLogRepo.save(apiCallLog);
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
