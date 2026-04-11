package com.mvt.mvt_events.controller;

import com.mvt.mvt_events.dto.TrackingResponse;
import com.mvt.mvt_events.jpa.Delivery;
import com.mvt.mvt_events.jpa.DeliveryStop;
import com.mvt.mvt_events.jpa.User;
import com.mvt.mvt_events.jpa.Vehicle;
import com.mvt.mvt_events.repository.DeliveryRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Endpoint PÚBLICO de rastreamento de entregas.
 * Acessado pelo destinatário via link compartilhado por WhatsApp.
 * NÃO requer autenticação — acesso via UUID token.
 */
@RestController
@RequestMapping("/api/tracking")
@CrossOrigin(origins = "*")
@Tag(name = "Tracking", description = "Rastreamento público de entregas (sem autenticação)")
public class TrackingController {

    private final DeliveryRepository deliveryRepository;

    public TrackingController(DeliveryRepository deliveryRepository) {
        this.deliveryRepository = deliveryRepository;
    }

    @GetMapping("/{token}")
    @Operation(summary = "Rastrear entrega por token", description = "Endpoint público - sem autenticação. Retorna dados limitados da entrega.")
    public ResponseEntity<TrackingResponse> track(@PathVariable UUID token) {
        Delivery delivery = deliveryRepository.findByTrackingToken(token)
                .orElse(null);

        if (delivery == null) {
            return ResponseEntity.notFound().build();
        }

        // Token expirado?
        if (delivery.getTrackingTokenExpiresAt() != null
                && delivery.getTrackingTokenExpiresAt().isBefore(OffsetDateTime.now())) {
            return ResponseEntity.notFound().build();
        }

        // Montar resposta com dados limitados
        TrackingResponse.TrackingResponseBuilder builder = TrackingResponse.builder()
                .deliveryId(delivery.getId())
                .status(delivery.getStatus().name())
                .fromAddress(delivery.getFromAddress())
                .toAddress(delivery.getToAddress())
                .fromLatitude(delivery.getFromLatitude())
                .fromLongitude(delivery.getFromLongitude())
                .toLatitude(delivery.getToLatitude())
                .toLongitude(delivery.getToLongitude())
                .recipientName(delivery.getRecipientName())
                .acceptedAt(delivery.getAcceptedAt())
                .inTransitAt(delivery.getInTransitAt())
                .completedAt(delivery.getCompletedAt())
                .cancelledAt(delivery.getCancelledAt());

        // Courier (dados limitados)
        User courier = delivery.getCourier();
        if (courier != null) {
            String firstName = courier.getName();
            if (firstName != null && firstName.contains(" ")) {
                firstName = firstName.substring(0, firstName.indexOf(" "));
            }
            builder.courierFirstName(firstName)
                    .courierLatitude(courier.getGpsLatitude())
                    .courierLongitude(courier.getGpsLongitude());
        }

        // Veículo (dados limitados — placa parcial)
        Vehicle vehicle = delivery.getVehicle();
        if (vehicle != null) {
            builder.vehicleType(vehicle.getType() != null ? vehicle.getType().name() : null);
            String desc = "";
            if (vehicle.getBrand() != null) desc += vehicle.getBrand();
            if (vehicle.getModel() != null) desc += " " + vehicle.getModel();
            if (vehicle.getPlate() != null) {
                String plate = vehicle.getPlate();
                desc += " - ***" + plate.substring(Math.max(0, plate.length() - 3));
            }
            builder.vehicleDescription(desc.trim());
        }

        // Paradas
        List<DeliveryStop> stops = delivery.getStops();
        if (stops != null && !stops.isEmpty()) {
            List<TrackingResponse.TrackingStopDTO> stopDTOs = stops.stream()
                    .sorted(Comparator.comparingInt(DeliveryStop::getStopOrder))
                    .map(s -> TrackingResponse.TrackingStopDTO.builder()
                            .stopOrder(s.getStopOrder())
                            .address(s.getAddress())
                            .latitude(s.getLatitude())
                            .longitude(s.getLongitude())
                            .recipientName(s.getRecipientName())
                            .status(s.getStatus() != null ? s.getStatus().name() : null)
                            .completedAt(s.getCompletedAt())
                            .build())
                    .collect(Collectors.toList());
            builder.stops(stopDTOs);
        }

        // Rota planejada (LineString → array de [lat, lng])
        LineString planned = delivery.getPlannedRoute();
        if (planned != null) {
            List<double[]> coords = new ArrayList<>();
            for (Coordinate c : planned.getCoordinates()) {
                coords.add(new double[]{c.y, c.x}); // y=lat, x=lng
            }
            builder.plannedRoute(coords);
        }

        return ResponseEntity.ok(builder.build());
    }
}
