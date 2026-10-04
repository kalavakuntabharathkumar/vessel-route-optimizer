package com.vessel.optimizer.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import java.util.Map;

@Component
public class OsrmClient implements DistanceProvider {
    private static final Logger log = LoggerFactory.getLogger(OsrmClient.class);
    private final RestClient restClient;
    private final String baseUrl;

    public OsrmClient(@Value("${osrm.base-url:https://router.project-osrm.org}") String baseUrl) {
        this.baseUrl = baseUrl;
        this.restClient = RestClient.create(baseUrl);
    }

    @Override
    public double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        String url = String.format("/route/v1/driving/%f,%f;%f,%f?overview=false", lon1, lat1, lon2, lat2);
        try {
            Map<String, Object> response = restClient.get()
                .uri(url)
                .retrieve()
                .body(Map.class);

            if (response != null && response.get("code").equals("Ok")) {
                @SuppressWarnings("unchecked")
                var routes = (java.util.List<Map<String, Object>>) response.get("routes");
                if (!routes.isEmpty()) {
                    return ((Number) routes.get(0).get("distance")).doubleValue() / 1000.0;
                }
            }
        } catch (RestClientException e) {
            log.warn("OSRM request failed, falling back to haversine: {}", e.getMessage());
        }
        return new HaversineProvider().calculateDistance(lat1, lon1, lat2, lon2);
    }

    @Override
    public String getProviderName() {
        return "OSRM";
    }
}