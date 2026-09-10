package com.nailic.sproochencoach.config;

import com.nailic.sproochencoach.constants.AppConstants;
import com.nailic.sproochencoach.service.OutboundApiCallLogService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.mock.http.client.MockClientHttpRequest;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.longThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class OutboundApiCallLoggingInterceptorFactoryTest {
    @Test
    void recordsSlowCallAfterResponseBodyIsConsumed() throws IOException {
        OutboundApiCallLogService logService = mock(OutboundApiCallLogService.class);
        OutboundApiCallLoggingInterceptorFactory factory =
                new OutboundApiCallLoggingInterceptorFactory(logService, 10, true);
        ClientHttpRequestInterceptor interceptor = factory.create(AppConstants.Providers.OPEN_ROUTER);
        MockClientHttpRequest request = new MockClientHttpRequest(
                org.springframework.http.HttpMethod.POST,
                URI.create("https://openrouter.ai/api/v1/chat/completions?apiKey=secret")
        );

        ClientHttpResponse response = interceptor.intercept(
                request,
                new byte[0],
                (httpRequest, body) -> new SlowBodyClientHttpResponse()
        );

        response.getBody().readAllBytes();
        response.close();

        verify(logService).save(
                eq(AppConstants.Providers.OPEN_ROUTER),
                eq("POST"),
                eq("/api/v1/chat/completions?<redacted>"),
                eq(200),
                longThat(durationMs -> durationMs >= 10),
                eq(AppConstants.OutboundApiOutcomes.SLOW),
                isNull()
        );
    }

    private static class SlowBodyClientHttpResponse implements ClientHttpResponse {
        private final HttpHeaders headers = new HttpHeaders();

        @Override
        public HttpStatusCode getStatusCode() {
            return HttpStatus.OK;
        }

        @Override
        public String getStatusText() {
            return "OK";
        }

        @Override
        public HttpHeaders getHeaders() {
            return headers;
        }

        @Override
        public InputStream getBody() {
            return new SlowInputStream("{}".getBytes());
        }

        @Override
        public void close() {
        }
    }

    private static class SlowInputStream extends ByteArrayInputStream {
        private SlowInputStream(byte[] buffer) {
            super(buffer);
        }

        @Override
        public synchronized int read(byte[] buffer, int offset, int length) {
            sleepPastSlowThreshold();
            return super.read(buffer, offset, length);
        }

        private void sleepPastSlowThreshold() {
            try {
                Thread.sleep(15);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
