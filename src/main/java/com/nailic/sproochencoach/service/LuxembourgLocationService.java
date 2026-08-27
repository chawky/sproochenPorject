package com.nailic.sproochencoach.service;

import com.nailic.sproochencoach.dto.LocationSuggestionDto;
import com.nailic.sproochencoach.exceptions.LocationProviderException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class LuxembourgLocationService {
    private static final Logger log = LoggerFactory.getLogger(LuxembourgLocationService.class);

    private final RestClient geoportailRestClient;

    @Value("${luxembourg.geoportail.fulltext-search-uri}")
    private String fulltextSearchUri;

    @Value("${luxembourg.geoportail.location-layers}")
    private String locationLayers;

    @Value("${luxembourg.geoportail.default-limit}")
    private int defaultLimit;

    @Value("${luxembourg.geoportail.max-limit}")
    private int maxLimit;

    public LuxembourgLocationService(
            @Qualifier("geoportailRestClient") RestClient geoportailRestClient
    ) {
        this.geoportailRestClient = geoportailRestClient;
    }

    public List<LocationSuggestionDto> searchLocations(String query, int limit) {
        if (query == null || query.isBlank()) {
            return List.of();
        }

        String trimmedQuery = query.trim();
        int normalizedLimit = normalizeLimit(limit);

        try {
            Map<?, ?> response = geoportailRestClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path(fulltextSearchUri)
                            .queryParam("query", trimmedQuery)
                            .queryParam("limit", normalizedLimit)
                            .queryParam("fuzziness", 1)
                            .queryParam("layer", locationLayers)
                            .build())
                    .retrieve()
                    .body(Map.class);

            return mapResponse(response);
        } catch (RestClientException exception) {
            log.error(
                    "Geoportail location lookup failed. queryLength={}, limit={}, reason={}",
                    trimmedQuery.length(),
                    normalizedLimit,
                    exception.getMessage()
            );

            throw new LocationProviderException(
                    HttpStatus.BAD_GATEWAY.value(),
                    "Location lookup is currently unavailable"
            );
        }
    }

    private int normalizeLimit(int limit) {
        if (limit < 1) {
            return defaultLimit;
        }

        return Math.min(limit, maxLimit);
    }

    private List<LocationSuggestionDto> mapResponse(Map<?, ?> response) {
        if (response == null || !(response.get("features") instanceof List<?> features)) {
            return List.of();
        }

        return features.stream()
                .filter(Map.class::isInstance)
                .map(Map.class::cast)
                .map(this::mapFeature)
                .filter(Objects::nonNull)
                .toList();
    }

    private LocationSuggestionDto mapFeature(Map<?, ?> feature) {
        if (!(feature.get("properties") instanceof Map<?, ?> properties)) {
            return null;
        }

        String label = stringValue(properties.get("label"));
        if (label == null || label.isBlank()) {
            return null;
        }

        return new LocationSuggestionDto(
                stringValue(feature.get("id")),
                label,
                stringValue(properties.get("layer_name"))
        );
    }

    private String stringValue(Object value) {
        return value == null ? null : value.toString();
    }
}
