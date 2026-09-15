package com.nailic.sproochencoach.service;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

class ClientIpResolverTest {
    @Test
    void usesForwardedForWhenPresent() {
        ClientIpResolver resolver = new ClientIpResolver();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("198.51.100.20");
        request.addHeader("X-Forwarded-For", "203.0.113.10");

        assertThat(resolver.resolve(request)).isEqualTo("203.0.113.10");
    }

    @Test
    void usesFirstForwardedForAddressWhenChainIsPresent() {
        ClientIpResolver resolver = new ClientIpResolver();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("172.18.0.2");
        request.addHeader("X-Forwarded-For", "1.2.3.4, 203.0.113.10");

        assertThat(resolver.resolve(request)).isEqualTo("1.2.3.4");
    }

    @Test
    void fallsBackToRemoteAddressWhenForwardedForIsMissing() {
        ClientIpResolver resolver = new ClientIpResolver();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("198.51.100.20");

        assertThat(resolver.resolve(request)).isEqualTo("198.51.100.20");
    }
}
