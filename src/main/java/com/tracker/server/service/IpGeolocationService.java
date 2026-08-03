package com.tracker.server.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Optional;

@Service
public class IpGeolocationService {
    private static final Logger LOGGER = LoggerFactory.getLogger(IpGeolocationService.class);

    private final ObjectMapper objectMapper;
    private final boolean enabled;
    private final String baseUrl;
    private final Duration timeout;
    private final HttpClient httpClient;

    public IpGeolocationService(
            ObjectMapper objectMapper,
            @Value("${app.ip-geolocation.enabled:true}") boolean enabled,
            @Value("${app.ip-geolocation.base-url:https://ipwho.is}") String baseUrl,
            @Value("${app.ip-geolocation.timeout-millis:3000}") long timeoutMillis
    ) {
        this.objectMapper = objectMapper;
        this.enabled = enabled;
        this.baseUrl = baseUrl.replaceAll("/+$", "");
        this.timeout = Duration.ofMillis(Math.max(500, timeoutMillis));
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(this.timeout)
                .build();
    }

    public Optional<IpLocation> find(String publicIpAddress) {
        if (!enabled || publicIpAddress == null || publicIpAddress.isBlank()) {
            return Optional.empty();
        }

        try {
            URI uri = URI.create(
                    baseUrl + "/" + publicIpAddress + "?fields=success,country,region"
            );
            HttpRequest request = HttpRequest.newBuilder(uri)
                    .timeout(timeout)
                    .header("Accept", "application/json")
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
            );
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IOException("IP geolocation returned HTTP " + response.statusCode());
            }

            JsonNode body = objectMapper.readTree(response.body());
            if (!body.path("success").asBoolean(false)) {
                throw new IOException("IP geolocation rejected the address");
            }

            String country = clean(body.path("country").asText(null));
            String state = clean(body.path("region").asText(null));
            if (country == null && state == null) {
                return Optional.empty();
            }
            return Optional.of(new IpLocation(country, state));
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            LOGGER.error("IP geolocation lookup was interrupted", exception);
            return Optional.empty();
        } catch (IOException | RuntimeException exception) {
            LOGGER.error("IP geolocation lookup failed", exception);
            return Optional.empty();
        }
    }

    private String clean(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    public record IpLocation(String country, String state) {
    }
}
