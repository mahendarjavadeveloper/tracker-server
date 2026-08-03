package com.tracker.server.service;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

class ClientIpResolverTest {
    private final ClientIpResolver resolver = new ClientIpResolver();

    @Test
    void resolvesTheFirstPublicForwardedAddress() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Forwarded-For", "8.8.8.8, 10.0.0.10");
        request.setRemoteAddr("10.0.0.20");

        assertThat(resolver.resolve(request)).isEqualTo("8.8.8.8");
    }

    @Test
    void doesNotTreatLocalAddressAsPublic() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Forwarded-For", "192.168.1.25");
        request.setRemoteAddr("127.0.0.1");

        assertThat(resolver.resolve(request)).isNull();
    }
}
