package com.tracker.server.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.InetAddress;
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
    private final String fallbackBaseUrl;
    private final Duration timeout;
    private final HttpClient httpClient;

    public IpGeolocationService(
            ObjectMapper objectMapper,
            @Value("${app.ip-geolocation.enabled:true}") boolean enabled,
            @Value("${app.ip-geolocation.base-url:https://ipwho.is}") String baseUrl,
            @Value("${app.ip-geolocation.fallback-base-url:https://ipapi.co}") String fallbackBaseUrl,
            @Value("${app.ip-geolocation.timeout-millis:3000}") long timeoutMillis
    ) {
        this.objectMapper = objectMapper;
        this.enabled = enabled;
        this.baseUrl = cleanBaseUrl(baseUrl);
        this.fallbackBaseUrl = cleanBaseUrl(fallbackBaseUrl);
        this.timeout = Duration.ofMillis(Math.max(500, timeoutMillis));
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(this.timeout)
                .build();
    }

    public Optional<IpLocation> find(String publicIpAddress) {
        if (!enabled || publicIpAddress == null || publicIpAddress.isBlank()) {
            return Optional.empty();
        }

        if (baseUrl != null) {
            try {
                return Optional.of(findWithPrimaryProvider(publicIpAddress));
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                LOGGER.error("Primary IP geolocation lookup was interrupted", exception);
                return Optional.empty();
            } catch (IOException | RuntimeException exception) {
                LOGGER.error("Primary IP geolocation lookup failed", exception);
            }
        }

        if (fallbackBaseUrl == null) {
            return Optional.empty();
        }

        try {
            return Optional.of(findWithFallbackProvider(publicIpAddress));
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            LOGGER.error("Fallback IP geolocation lookup was interrupted", exception);
            return Optional.empty();
        } catch (IOException | RuntimeException exception) {
            LOGGER.error("Fallback IP geolocation lookup failed", exception);
            return Optional.empty();
        }
    }

    private IpLocation findWithPrimaryProvider(String publicIpAddress)
            throws IOException, InterruptedException {
        JsonNode body = fetch(
                URI.create(baseUrl + "/" + publicIpAddress + "?fields=success,ip,country,region")
        );
        if (!body.path("success").asBoolean(false)) {
            throw new IOException("Primary IP geolocation provider rejected the address");
        }
        verifyAddress(publicIpAddress, body.path("ip").asText(null), "Primary");
        return locationFrom(
                body.path("country").asText(null),
                body.path("region").asText(null),
                "Primary IP geolocation provider returned no location"
        );
    }

    private IpLocation findWithFallbackProvider(String publicIpAddress)
            throws IOException, InterruptedException {
        JsonNode body = fetch(
                URI.create(fallbackBaseUrl + "/" + publicIpAddress + "/json/")
        );
        if (body.path("error").asBoolean(false)) {
            throw new IOException("Fallback IP geolocation provider rejected the address");
        }
        verifyAddress(publicIpAddress, body.path("ip").asText(null), "Fallback");
        return locationFrom(
                body.path("country_name").asText(null),
                body.path("region").asText(null),
                "Fallback IP geolocation provider returned no location"
        );
    }

    private JsonNode fetch(URI uri) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(timeout)
                .header("Accept", "application/json")
                .header("User-Agent", "Windows-Tracker-Server/1.0")
                .GET()
                .build();
        HttpResponse<String> response = httpClient.send(
                request,
                HttpResponse.BodyHandlers.ofString()
        );
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("IP geolocation returned HTTP " + response.statusCode());
        }
        return objectMapper.readTree(response.body());
    }

    private IpLocation locationFrom(
            String countryValue,
            String stateValue,
            String missingLocationMessage
    ) throws IOException {
        String country = clean(countryValue);
        String state = clean(stateValue);
        if (country == null && state == null) {
            throw new IOException(missingLocationMessage);
        }
        return new IpLocation(country, state);
    }

    private void verifyAddress(String requested, String returned, String provider)
            throws IOException {
        String cleaned = clean(returned);
        if (cleaned == null
                || !InetAddress.getByName(requested).equals(InetAddress.getByName(cleaned))) {
            throw new IOException(provider + " IP geolocation provider returned a different address");
        }
    }

    private String cleanBaseUrl(String value) {
        String cleaned = clean(value);
        return cleaned == null ? null : cleaned.replaceAll("/+$", "");
    }

    private String clean(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    public record IpLocation(String country, String state) {
    }
}
