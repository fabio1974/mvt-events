package com.mvt.mvt_events.service;

import com.mvt.mvt_events.jpa.Delivery;
import com.mvt.mvt_events.jpa.DeliveryStop;
import com.mvt.mvt_events.repository.DeliveryRepository;
import com.mvt.mvt_events.repository.DeliveryStopRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages the planned_route column on deliveries using Google Directions.
 *
 * ACCEPTED phase  : route from courier current position → origin (pickup point).
 * IN_TRANSIT phase: route from courier current position → remaining stops (nearest-neighbor order).
 *
 * Deviation check : 100 m tolerance, 60 s cooldown per delivery (in-memory).
 */
@Service
public class PlannedRouteService {

    private static final Logger log = LoggerFactory.getLogger(PlannedRouteService.class);

    private static final double DEVIATION_THRESHOLD_METERS = 100.0;
    private static final long COOLDOWN_SECONDS = 60L;

    /** Last recalculation timestamp for planned_route (IN_TRANSIT) per delivery ID. */
    private final ConcurrentHashMap<Long, Instant> lastRecalculation = new ConcurrentHashMap<>();

    /** Last recalculation timestamp for approach_planned_route (ACCEPTED) per delivery ID. */
    private final ConcurrentHashMap<Long, Instant> lastApproachRecalculation = new ConcurrentHashMap<>();

    @Autowired
    private DeliveryRepository deliveryRepository;

    @Autowired
    private DeliveryStopRepository deliveryStopRepository;

    @Autowired
    private GoogleDirectionsService googleDirectionsService;

    // -------------------------------------------------------------------------
    // Public API called from UserService on each GPS update
    // -------------------------------------------------------------------------

    /**
     * Called for every GPS update while delivery is ACCEPTED.
     * Creates/recalculates approach_planned_route (courier → pickup).
     * Never touches planned_route (the main delivery route).
     */
    @Transactional
    public void handleApproachRouteUpdate(Delivery delivery, double lat, double lng) {
        Long deliveryId = delivery.getId();
        Double originLat = delivery.getFromLatitude();
        Double originLng = delivery.getFromLongitude();

        if (originLat == null || originLng == null) return;

        String existing = deliveryRepository.getApproachPlannedRouteAsGeoJson(deliveryId);

        if (existing == null) {
            recalculateApproach(deliveryId, lat, lng, originLat, originLng);
            return;
        }

        if (shouldRecalculateApproach(deliveryId, lat, lng)) {
            recalculateApproach(deliveryId, lat, lng, originLat, originLng);
        }
    }

    /**
     * Called for every GPS update while delivery is IN_TRANSIT.
     * Creates planned_route on first call; recalculates on deviation.
     */
    @Transactional
    public void handleDeliveryRouteUpdate(Delivery delivery, double lat, double lng) {
        Long deliveryId = delivery.getId();

        List<DeliveryStop> remainingStops = getRemainingStops(delivery);
        if (remainingStops.isEmpty()) return;

        // Order stops by nearest-neighbor from current position
        List<DeliveryStop> orderedStops = nearestNeighborOrder(lat, lng, remainingStops);

        String existing = deliveryRepository.getPlannedRouteAsGeoJson(deliveryId);

        if (existing == null) {
            recalculateDelivery(deliveryId, lat, lng, orderedStops);
            return;
        }

        if (shouldRecalculate(deliveryId, lat, lng)) {
            recalculateDelivery(deliveryId, lat, lng, orderedStops);
        }
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    private void recalculateApproach(Long deliveryId, double courierLat, double courierLng,
                                     double originLat, double originLng) {
        log.info("🗺️ Recalculating approach_planned_route for delivery #{}", deliveryId);
        // Claim cooldown slot BEFORE the API call to prevent concurrent threads from
        // both passing shouldRecalculateApproach() and triggering a double Google API request.
        lastApproachRecalculation.put(deliveryId, Instant.now());
        List<double[]> coords = googleDirectionsService.getRoute(
                courierLat, courierLng, originLat, originLng, List.of());
        if (coords.size() >= 2) {
            persistApproachPlannedRoute(deliveryId, coords);
        } else {
            // Release cooldown so next GPS update can retry
            lastApproachRecalculation.remove(deliveryId);
        }
    }

    private void recalculateDelivery(Long deliveryId, double courierLat, double courierLng,
                                     List<DeliveryStop> orderedStops) {
        log.info("🗺️ Recalculating in-transit planned_route for delivery #{} ({} stops)",
                deliveryId, orderedStops.size());

        // Claim cooldown slot BEFORE the API call (same rationale as recalculateApproach)
        lastRecalculation.put(deliveryId, Instant.now());

        DeliveryStop lastStop = orderedStops.get(orderedStops.size() - 1);
        double destLat = lastStop.getLatitude();
        double destLng = lastStop.getLongitude();

        List<double[]> waypoints = new ArrayList<>();
        for (int i = 0; i < orderedStops.size() - 1; i++) {
            DeliveryStop s = orderedStops.get(i);
            waypoints.add(new double[]{s.getLatitude(), s.getLongitude()});
        }

        List<double[]> coords = googleDirectionsService.getRoute(
                courierLat, courierLng, destLat, destLng, waypoints);
        if (coords.size() >= 2) {
            persistPlannedRoute(deliveryId, coords);
            // Update planned completionOrder for PENDING stops based on the new visit sequence
            updatePlannedCompletionOrders(deliveryId, orderedStops);
        } else {
            // Release cooldown so next GPS update can retry
            lastRecalculation.remove(deliveryId);
        }
    }

    /**
     * Assigns planned completionOrder values to PENDING stops based on the nearest-neighbor order.
     * Starts from lastCompletedOrder + 1 so values are always relative to the real completion history.
     * Only updates stops that are still PENDING — completed/skipped values are immutable.
     */
    private void updatePlannedCompletionOrders(Long deliveryId, List<DeliveryStop> orderedStops) {
        int base = deliveryStopRepository.maxCompletionOrder(deliveryId) + 1;
        for (int i = 0; i < orderedStops.size(); i++) {
            deliveryStopRepository.updatePlannedOrder(orderedStops.get(i).getId(), base + i);
        }
    }

    /**
     * Returns true if the courier has deviated from planned_route (IN_TRANSIT) beyond the threshold AND cooldown has elapsed.
     */
    private boolean shouldRecalculate(Long deliveryId, double lat, double lng) {
        Instant last = lastRecalculation.get(deliveryId);
        if (last != null && Instant.now().minusSeconds(COOLDOWN_SECONDS).isBefore(last)) {
            return false;
        }
        Double distanceMeters = deliveryRepository.getDistanceFromPlannedRouteMeters(deliveryId, lat, lng);
        if (distanceMeters == null) return true;
        boolean deviated = distanceMeters > DEVIATION_THRESHOLD_METERS;
        if (deviated) {
            log.info("📍 Courier deviated {}m from planned route on delivery #{} — recalculating",
                    Math.round(distanceMeters), deliveryId);
        }
        return deviated;
    }

    /**
     * Returns true if the courier has deviated from approach_planned_route (ACCEPTED) beyond the threshold AND cooldown has elapsed.
     */
    private boolean shouldRecalculateApproach(Long deliveryId, double lat, double lng) {
        Instant last = lastApproachRecalculation.get(deliveryId);
        if (last != null && Instant.now().minusSeconds(COOLDOWN_SECONDS).isBefore(last)) {
            return false;
        }
        Double distanceMeters = deliveryRepository.getDistanceFromApproachPlannedRouteMeters(deliveryId, lat, lng);
        if (distanceMeters == null) return true;
        boolean deviated = distanceMeters > DEVIATION_THRESHOLD_METERS;
        if (deviated) {
            log.info("📍 Courier deviated {}m from approach planned route on delivery #{} — recalculating",
                    Math.round(distanceMeters), deliveryId);
        }
        return deviated;
    }

    private void persistPlannedRoute(Long deliveryId, List<double[]> coords) {
        deliveryRepository.updatePlannedRoute(deliveryId, buildWkt(coords));
    }

    private void persistApproachPlannedRoute(Long deliveryId, List<double[]> coords) {
        deliveryRepository.updateApproachPlannedRoute(deliveryId, buildWkt(coords));
    }

    private static String buildWkt(List<double[]> coords) {
        StringBuilder wkt = new StringBuilder("LINESTRING(");
        for (int i = 0; i < coords.size(); i++) {
            if (i > 0) wkt.append(",");
            wkt.append(coords.get(i)[1]).append(" ").append(coords.get(i)[0]); // WKT: lng lat
        }
        wkt.append(")");
        return wkt.toString();
    }

    /** Returns stops whose status is not COMPLETED and not SKIPPED. */
    private List<DeliveryStop> getRemainingStops(Delivery delivery) {
        if (delivery.getStops() == null) return List.of();
        return delivery.getStops().stream()
                .filter(s -> s.getLatitude() != null && s.getLongitude() != null)
                .filter(s -> s.getStatus() == DeliveryStop.StopStatus.PENDING)
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * Nearest-neighbor greedy ordering.
     * Starting from the courier position, always picks the closest remaining stop.
     */
    private List<DeliveryStop> nearestNeighborOrder(double fromLat, double fromLng,
                                                     List<DeliveryStop> stops) {
        List<DeliveryStop> remaining = new ArrayList<>(stops);
        List<DeliveryStop> ordered = new ArrayList<>();
        double curLat = fromLat;
        double curLng = fromLng;

        while (!remaining.isEmpty()) {
            DeliveryStop nearest = null;
            double minDist = Double.MAX_VALUE;
            for (DeliveryStop s : remaining) {
                double d = haversineKm(curLat, curLng, s.getLatitude(), s.getLongitude());
                if (d < minDist) {
                    minDist = d;
                    nearest = s;
                }
            }
            ordered.add(nearest);
            remaining.remove(nearest);
            curLat = nearest.getLatitude();
            curLng = nearest.getLongitude();
        }
        return ordered;
    }

    private static double haversineKm(double lat1, double lon1, double lat2, double lon2) {
        final double R = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                        * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    /** Cleans up cooldown state when a delivery completes/cancels. */
    public void clearCooldown(Long deliveryId) {
        lastRecalculation.remove(deliveryId);
        lastApproachRecalculation.remove(deliveryId);
    }
}
