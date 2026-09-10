package com.nailic.sproochencoach.config;

import com.nailic.sproochencoach.constants.AppConstants;
import com.nailic.sproochencoach.service.OutboundApiCallLogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;

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
                if (!enabled) {
                    return response;
                }
                return new TimedClientHttpResponse(request, response, start);
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

        private class TimedClientHttpResponse implements ClientHttpResponse {
            private final HttpRequest request;
            private final ClientHttpResponse response;
            private final long start;
            private final int statusCode;
            private final boolean errorStatus;
            private final AtomicBoolean saved = new AtomicBoolean(false);
            private InputStream body;

            private TimedClientHttpResponse(
                    HttpRequest request,
                    ClientHttpResponse response,
                    long start
            ) throws IOException {
                this.request = request;
                this.response = response;
                this.start = start;

                HttpStatusCode responseStatusCode = response.getStatusCode();
                this.statusCode = responseStatusCode.value();
                this.errorStatus = responseStatusCode.isError();
            }

            @Override
            public HttpStatusCode getStatusCode() throws IOException {
                return response.getStatusCode();
            }

            @Override
            public String getStatusText() throws IOException {
                return response.getStatusText();
            }

            @Override
            public org.springframework.http.HttpHeaders getHeaders() {
                return response.getHeaders();
            }

            @Override
            public InputStream getBody() throws IOException {
                if (body == null) {
                    body = new TimedInputStream(response.getBody());
                }
                return body;
            }

            @Override
            public void close() {
                try {
                    response.close();
                } finally {
                    saveCompleted(null);
                }
            }

            private void saveCompleted(Exception exception) {
                if (!saved.compareAndSet(false, true)) {
                    return;
                }

                long durationMs = elapsedMs(start);
                if (exception != null) {
                    save(request, statusCode, durationMs, AppConstants.OutboundApiOutcomes.FAILED, exception);
                } else if (errorStatus) {
                    save(request, statusCode, durationMs, AppConstants.OutboundApiOutcomes.FAILED, null);
                } else if (durationMs >= slowThresholdMs) {
                    save(request, statusCode, durationMs, AppConstants.OutboundApiOutcomes.SLOW, null);
                }
            }

            private class TimedInputStream extends FilterInputStream {
                private TimedInputStream(InputStream inputStream) {
                    super(inputStream);
                }

                @Override
                public int read() throws IOException {
                    try {
                        int value = super.read();
                        if (value == -1) {
                            saveCompleted(null);
                        }
                        return value;
                    } catch (IOException exception) {
                        saveCompleted(exception);
                        throw exception;
                    }
                }

                @Override
                public int read(byte[] buffer, int offset, int length) throws IOException {
                    try {
                        int bytesRead = super.read(buffer, offset, length);
                        if (bytesRead == -1) {
                            saveCompleted(null);
                        }
                        return bytesRead;
                    } catch (IOException exception) {
                        saveCompleted(exception);
                        throw exception;
                    }
                }

                @Override
                public void close() throws IOException {
                    try {
                        super.close();
                        saveCompleted(null);
                    } catch (IOException exception) {
                        saveCompleted(exception);
                        throw exception;
                    }
                }
            }
        }
    }
}
