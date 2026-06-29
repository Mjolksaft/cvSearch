package com.cvsearch.job;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Converts a text location (e.g. "Växjö, Kronobergs län") into coordinates
 * using the OpenStreetMap Nominatim API.
 * <p>
 * Rate-limited to 1 request/second as required by Nominatim usage policy.
 */
@Service
public class GeocodingService {

    private static final Logger log = LoggerFactory.getLogger(GeocodingService.class);
    private static final long MIN_INTERVAL_MS = 1100; // slightly more than 1s to be safe

    private final RestClient restClient;
    private final Map<String, double[]> cache = new ConcurrentHashMap<>();
    private volatile long lastRequestTime = 0;

    public GeocodingService() {
        this.restClient = RestClient.builder()
                .baseUrl("https://nominatim.openstreetmap.org")
                .defaultHeader("User-Agent", "CVSearch/1.0 (job search tool)")
                .defaultHeader("Accept", "application/json")
                .build();
    }

    /**
     * Geocode a location string into [latitude, longitude].
     * Returns null if the location cannot be resolved.
     * Rate-limited to 1 request/second.
     */
    public double[] geocode(String location) {
        if (location == null || location.isBlank()) {
            return null;
        }

        // Clean location: strip parenthetical suffixes like "(Hybrid)", "(Remote)", "(On-site)"
        String cleaned = location.replaceAll("\\s*\\([^)]*\\)", "").trim();

        if (cleaned.isBlank()) {
            return null;
        }

        // Check cache first
        String key = cleaned.toLowerCase();
        double[] cached = cache.get(key);
        if (cached != null) {
            return cached;
        }

        // Rate limit: wait if needed
        rateLimit();

        try {
            NominatimResponse[] response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/search")
                            .queryParam("q", cleaned)
                            .queryParam("format", "json")
                            .queryParam("limit", 1)
                            .build())
                    .retrieve()
                    .body(NominatimResponse[].class);

            if (response != null && response.length > 0 && response[0].lat() != null) {
                double lat = Double.parseDouble(response[0].lat());
                double lon = Double.parseDouble(response[0].lon());
                double[] result = { lat, lon };
                cache.put(key, result);
                log.debug("Geocoded '{}' → [{}, {}]", location, lat, lon);
                return result;
            }

            log.warn("Could not geocode location: {}", location);
            cache.put(key, null); // cache negative result too
            return null;

        } catch (Exception e) {
            log.error("Geocoding failed for '{}': {}", location, e.getMessage());
            return null;
        }
    }

    private synchronized void rateLimit() {
        long now = System.currentTimeMillis();
        long elapsed = now - lastRequestTime;
        if (elapsed < MIN_INTERVAL_MS) {
            try {
                Thread.sleep(MIN_INTERVAL_MS - elapsed);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        lastRequestTime = System.currentTimeMillis();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record NominatimResponse(String lat, String lon) {}
}
