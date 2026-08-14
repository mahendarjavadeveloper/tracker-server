package com.tracker.server.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IpGeolocationServiceTest {
    private final List<HttpServer> servers = new ArrayList<>();

    @AfterEach
    void stopServers() {
        servers.forEach(server -> server.stop(0));
    }

    @Test
    void usesPrimaryProviderWhenItReturnsLocation() throws Exception {
        AtomicInteger fallbackRequests = new AtomicInteger();
        String primaryUrl = startServer(200, "{\"success\":true,\"ip\":\"8.8.8.8\",\"country\":\"India\",\"region\":\"Telangana\"}", null);
        String fallbackUrl = startServer(200, "{\"ip\":\"8.8.8.8\",\"country_name\":\"Fallback\",\"region\":\"Fallback\"}", fallbackRequests);
        IpGeolocationService service = service(primaryUrl, fallbackUrl);

        IpGeolocationService.IpLocation location = service.find("8.8.8.8").orElseThrow();

        assertEquals("India", location.country());
        assertEquals("Telangana", location.state());
        assertEquals(0, fallbackRequests.get());
    }

    @Test
    void usesFallbackProviderWhenPrimaryProviderFails() throws Exception {
        String primaryUrl = startServer(503, "{}", null);
        String fallbackUrl = startServer(200, "{\"ip\":\"8.8.4.4\",\"country_name\":\"India\",\"region\":\"Andhra Pradesh\"}", null);
        IpGeolocationService service = service(primaryUrl, fallbackUrl);

        IpGeolocationService.IpLocation location = service.find("8.8.4.4").orElseThrow();

        assertEquals("India", location.country());
        assertEquals("Andhra Pradesh", location.state());
    }

    @Test
    void rejectsLocationForAnotherIpAndUsesMatchingFallback() throws Exception {
        String primaryUrl = startServer(200, "{\"success\":true,\"ip\":\"1.1.1.1\",\"country\":\"Wrong\",\"region\":\"Wrong\"}", null);
        String fallbackUrl = startServer(200, "{\"ip\":\"8.8.8.8\",\"country_name\":\"India\",\"region\":\"Telangana\"}", null);
        IpGeolocationService service = service(primaryUrl, fallbackUrl);

        IpGeolocationService.IpLocation location = service.find("8.8.8.8").orElseThrow();

        assertEquals("India", location.country());
        assertEquals("Telangana", location.state());
    }

    @Test
    void returnsEmptyWhenBothProvidersFail() throws Exception {
        String primaryUrl = startServer(503, "{}", null);
        String fallbackUrl = startServer(429, "{}", null);
        IpGeolocationService service = service(primaryUrl, fallbackUrl);

        assertTrue(service.find("1.1.1.1").isEmpty());
    }

    private IpGeolocationService service(String primaryUrl, String fallbackUrl) {
        return new IpGeolocationService(
                new ObjectMapper(),
                true,
                primaryUrl,
                fallbackUrl,
                1_000
        );
    }

    private String startServer(int status, String body, AtomicInteger requests) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            if (requests != null) {
                requests.incrementAndGet();
            }
            respond(exchange, status, body);
        });
        server.start();
        servers.add(server);
        return "http://127.0.0.1:" + server.getAddress().getPort();
    }

    private void respond(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }
}
