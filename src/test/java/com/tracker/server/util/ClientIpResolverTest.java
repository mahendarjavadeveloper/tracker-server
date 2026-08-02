package com.tracker.server.util;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ClientIpResolverTest {

    @Test
    void resolvesFirstForwardedClientAddress() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("X-Forwarded-For"))
                .thenReturn("203.0.113.24, 10.0.0.8");

        assertThat(ClientIpResolver.resolve(request)).isEqualTo("203.0.113.24");
    }

    @Test
    void resolvesForwardedIpv6Address() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("Forwarded"))
                .thenReturn("for=\"[2001:db8:cafe::17]:4711\";proto=https");

        assertThat(ClientIpResolver.resolve(request)).isEqualTo("2001:db8:cafe::17");
    }

    @Test
    void fallsBackToRemoteAddress() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRemoteAddr()).thenReturn("198.51.100.9");

        assertThat(ClientIpResolver.resolve(request)).isEqualTo("198.51.100.9");
    }
}
