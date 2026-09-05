package com.nailic.sproochencoach.config;

import com.nailic.sproochencoach.constants.AppConstants;
import com.nailic.sproochencoach.service.OutboundApiCallLogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;

@Component
public class OutboundApiCallLoggingInterceptorFactory {
    private final OutboundApiCallLogService outboundApiCallLogService;
    private final long slowThresholdMs;
    private final boolean enabled;

    public OutboundApiCallLoggingInterceptorFactory(
            OutboundApiCallLogService outboundApiCallLogService,
            @Value(AppConstants.PropertyPlaceholders.OUTBOUND_API_SLOW_THRESHOLD_MS) long slowThresholdMs,
            @Value(AppConstants.PropertyPlaceholders.OUTBOUND_API_ENABLED) boolean enabled
    ) {
        this.outboundApiCallLogService = outboundApiCallLogService;
        this.slowThresholdMs = slowThresholdMs;
        this.enabled = enabled;
    }

    public ClientHttpRequestInterceptor create(String provider) {
        return new OutboundApiCallLoggingInterceptor(provider, outboundApiCallLogService, slowThresholdMs, enabled);
    }

    private static class OutboundApiCallLoggingInterceptor implements ClientHttpRequestInterceptor {
        private static final Logger log = LoggerFactory.getLogger(OutboundApiCallLoggingInterceptor.class);

        private final String provider;
        private final OutboundApiCallLogService outboundApiCallLogService;
        private final long slowThresholdMs;
        private final boolean enabled;

        private OutboundApiCallLoggingInterceptor(
                String provider,
                OutboundApiCallLogService outboundApiCallLogService,
                long slowThresholdMs,
                boolean enabled
        ) {
            this.provider = provider;
            this.outboundApiCallLogService = outboundApiCallLogService;
            this.slowThresholdMs = slowThresholdMs;
            this.enabled = enabled;
        }

        @Override
        public ClientHttpResponse intercept(
                HttpRequest request,
                byte[] body,
                ClientHttpRequestExecution execution
        ) throws IOException {
            long start = System.nanoTime();

            try {
                ClientHttpResponse response = execution.execute(request, body);
                long durationMs = elapsedMs(start);
                int statusCode = response.getStatusCode().value();

                if (enabled && response.getStatusCode().isError()) {
                    save(request, statusCode, durationMs, AppConstants.OutboundApiOutcomes.FAILED, null);
                } else if (enabled && durationMs >= slowThresholdMs) {
                    save(request, statusCode, durationMs, AppConstants.OutboundApiOutcomes.SLOW, null);
                }

                return response;
            } catch (IOException | RuntimeException exception) {
                long durationMs = elapsedMs(start);

                if (enabled) {
                    save(request, null, durationMs, AppConstants.OutboundApiOutcomes.FAILED, exception);
                }

                throw exception;
            }
        }

        private long elapsedMs(long start) {
            return Duration.ofNanos(System.nanoTime() - start).toMillis();
        }

        private void save(
                HttpRequest request,
                Integer statusCode,
                long durationMs,
                String outcome,
                Exception exception
        ) {
            try {
                outboundApiCallLogService.save(
                        provider,
                        request.getMethod().name(),
                        sanitizedUri(request.getURI()),
                        statusCode,
                        durationMs,
                        outcome,
                        exception
                );
            } catch (RuntimeException saveException) {
                log.error(
                        "Failed to save outbound API call log for provider={}, outcome={}, durationMs={}",
                        provider,
                        outcome,
                        durationMs,
                        saveException
                );
            }
        }

        private String sanitizedUri(URI uri) {
            String path = uri.getRawPath();
            if (path == null || path.isBlank()) {
                path = "/";
            }

            if (uri.getRawQuery() != null && !uri.getRawQuery().isBlank()) {
                return path + "?<redacted>";
            }

            return path;
        }
    }
}
