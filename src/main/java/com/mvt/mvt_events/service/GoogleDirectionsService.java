package com.mvt.mvt_events.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;

/**
 * Calls Google Directions API and decodes the encoded polyline into a list of lat/lng pairs.
 */
@Service
public class GoogleDirectionsService {

    private static final Logger log = LoggerFactory.getLogger(GoogleDirectionsService.class);

    @Value("${google.maps.api-key:}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String DIRECTIONS_URL = "https://maps.googleapis.com/maps/api/directions/json";

    /**
     * Get a route from origin to destination, optionally passing through intermediate waypoints.
     *
     * @param originLat      latitude of the origin
     * @param originLng      longitude of the origin
     * @param destLat        latitude of the destination
     * @param destLng        longitude of the destination
     * @param waypoints      list of [lat, lng] pairs for intermediate stops (order matters)
     * @return list of [lat, lng] pairs representing the decoded polyline, or empty list on failure
     */
    public List<double[]> getRoute(double originLat, double originLng,
                                   double destLat, double destLng,
                                   List<double[]> waypoints) {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("⚠️ Google Maps API key not configured — skipping route fetch");
            return List.of();
        }

        try {
            String origin = originLat + "," + originLng;
            String destination = destLat + "," + destLng;

            UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(DIRECTIONS_URL)
                    .queryParam("origin", origin)
                    .queryParam("destination", destination)
                    .queryParam("mode", "driving")
                    .queryParam("language", "pt-BR")
                    .queryParam("key", apiKey);

            if (waypoints != null && !waypoints.isEmpty()) {
                StringBuilder wp = new StringBuilder("optimize:true");
                for (double[] pt : waypoints) {
                    wp.append("|").append(pt[0]).append(",").append(pt[1]);
                }
                builder.queryParam("waypoints", wp.toString());
            }

            String url = builder.toUriString();
            String response = restTemplate.getForObject(url, String.class);
            if (response == null) return List.of();

            JsonNode root = objectMapper.readTree(response);
            String status = root.path("status").asText();
            if (!"OK".equals(status)) {
                log.warn("⚠️ Google Directions returned status={} for route ({},{})→({},{})",
                        status, originLat, originLng, destLat, destLng);
                return List.of();
            }

            JsonNode route = root.path("routes").get(0);
            JsonNode legs = route.path("legs");

            List<double[]> allCoords = new ArrayList<>();
            for (JsonNode leg : legs) {
                for (JsonNode step : leg.path("steps")) {
                    String encoded = step.path("polyline").path("points").asText();
                    if (!encoded.isBlank()) {
                        allCoords.addAll(decodePolyline(encoded));
                    }
                }
            }

            return allCoords;

        } catch (Exception e) {
            log.warn("⚠️ Failed to fetch Google Directions route: {}", e.getMessage());
            return List.of();
        }
    }

    /**
     * Retorna a distância rodável em metros entre origem e destino.
     * Usa routes[0].legs[0].distance.value da Directions API.
     * Retorna -1 se a API não estiver configurada, falhar, ou retornar status != OK.
     */
    public int getDistanceMeters(double originLat, double originLng,
                                 double destLat, double destLng) {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("⚠️ Google Maps API key not configured — cannot compute driving distance");
            return -1;
        }
        try {
            String url = UriComponentsBuilder.fromHttpUrl(DIRECTIONS_URL)
                    .queryParam("origin", originLat + "," + originLng)
                    .queryParam("destination", destLat + "," + destLng)
                    .queryParam("mode", "driving")
                    .queryParam("language", "pt-BR")
                    .queryParam("key", apiKey)
                    .toUriString();
            String response = restTemplate.getForObject(url, String.class);
            if (response == null) return -1;
            JsonNode root = objectMapper.readTree(response);
            String status = root.path("status").asText();
            if (!"OK".equals(status)) {
                log.warn("⚠️ Google Directions distance status={} for ({},{})→({},{})",
                        status, originLat, originLng, destLat, destLng);
                return -1;
            }
            JsonNode leg = root.path("routes").get(0).path("legs").get(0);
            int meters = leg.path("distance").path("value").asInt(-1);
            log.debug("📍 Directions distance ({},{})→({},{}) = {}m",
                    originLat, originLng, destLat, destLng, meters);
            return meters;
        } catch (Exception e) {
            log.warn("⚠️ Failed to fetch Google Directions distance: {}", e.getMessage());
            return -1;
        }
    }

    /**
     * Decodes a Google Maps encoded polyline string into a list of [lat, lng] pairs.
     */
    public static List<double[]> decodePolyline(String encoded) {
        List<double[]> result = new ArrayList<>();
        int index = 0, len = encoded.length();
        int lat = 0, lng = 0;

        while (index < len) {
            int b, shift = 0, resultVal = 0;
            do {
                b = encoded.charAt(index++) - 63;
                resultVal |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlat = ((resultVal & 1) != 0 ? ~(resultVal >> 1) : (resultVal >> 1));
            lat += dlat;

            shift = 0;
            resultVal = 0;
            do {
                b = encoded.charAt(index++) - 63;
                resultVal |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlng = ((resultVal & 1) != 0 ? ~(resultVal >> 1) : (resultVal >> 1));
            lng += dlng;

            result.add(new double[]{lat / 1e5, lng / 1e5});
        }
        return result;
    }
}
