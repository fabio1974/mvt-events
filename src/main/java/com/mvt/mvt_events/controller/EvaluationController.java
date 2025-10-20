package com.mvt.mvt_events.controller;

import com.mvt.mvt_events.dto.*;
import com.mvt.mvt_events.jpa.Evaluation;
import com.mvt.mvt_events.service.EvaluationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Controller REST para Evaluation
 */
@RestController
@RequestMapping("/api/zapi10/evaluations")
@CrossOrigin(origins = "*")
@Tag(name = "Zapi10 - Evaluations", description = "Gerenciamento de avaliações")
@SecurityRequirement(name = "bearerAuth")
public class EvaluationController {

    @Autowired
    private EvaluationService evaluationService;

    @PostMapping
    @Operation(summary = "Criar avaliação", description = "Cliente avalia courier ou courier avalia cliente após delivery completada")
    public ResponseEntity<EvaluationResponse> create(
            @RequestBody @Valid EvaluationCreateRequest request,
            Authentication authentication) {

        UUID evaluatorId = UUID.fromString(authentication.getName());

        Evaluation evaluation = new Evaluation();
        evaluation.setRating(request.getRating());
        evaluation.setEvaluationType(Evaluation.EvaluationType.valueOf(request.getEvaluationType()));
        evaluation.setComments(request.getComments());

        Evaluation created = evaluationService.create(evaluation, request.getDeliveryId(), evaluatorId);

        return ResponseEntity.status(HttpStatus.CREATED).body(mapToResponse(created));
    }

    @GetMapping("/delivery/{deliveryId}")
    @Operation(summary = "Buscar avaliação de uma delivery")
    public ResponseEntity<EvaluationResponse> getByDeliveryId(@PathVariable Long deliveryId) {
        Evaluation evaluation = evaluationService.findByDeliveryId(deliveryId);
        return ResponseEntity.ok(mapToResponse(evaluation));
    }

    @GetMapping("/courier/{courierId}/received")
    @Operation(summary = "Buscar avaliações recebidas por um courier")
    public ResponseEntity<?> getCourierReceivedEvaluations(@PathVariable String courierId) {
        UUID courierUuid = UUID.fromString(courierId);
        var evaluations = evaluationService.findReceivedByCourier(courierUuid);
        return ResponseEntity.ok(evaluations.stream().map(this::mapToResponse).toList());
    }

    @GetMapping("/courier/{courierId}/rating")
    @Operation(summary = "Buscar rating médio de um courier")
    public ResponseEntity<?> getCourierAverageRating(@PathVariable String courierId) {
        UUID courierUuid = UUID.fromString(courierId);
        Double rating = evaluationService.getAverageRatingForCourier(courierUuid);
        return ResponseEntity.ok().body(java.util.Map.of("averageRating", rating));
    }

    @GetMapping("/poor")
    @Operation(summary = "Buscar avaliações ruins para análise", description = "Rating <= 2. Requer role ADM.")
    public ResponseEntity<?> getPoorRatings(Authentication authentication) {
        UUID admId = UUID.fromString(authentication.getName());
        var evaluations = evaluationService.findPoorRatings(admId);
        return ResponseEntity.ok(evaluations.stream().map(this::mapToResponse).toList());
    }

    private EvaluationResponse mapToResponse(Evaluation evaluation) {
        return EvaluationResponse.builder()
                .id(evaluation.getId())
                .createdAt(evaluation.getCreatedAt())
                .deliveryId(evaluation.getDelivery().getId())
                .deliveryFromAddress(evaluation.getDelivery().getFromAddress())
                .deliveryToAddress(evaluation.getDelivery().getToAddress())
                .evaluatorId(evaluation.getEvaluator().getId().toString())
                .evaluatorName(evaluation.getEvaluator().getName())
                .rating(evaluation.getRating())
                .evaluationType(evaluation.getEvaluationType().name())
                .comments(evaluation.getComments())
                .courierId(evaluation.getDelivery().getCourier() != null
                        ? evaluation.getDelivery().getCourier().getId().toString()
                        : null)
                .courierName(
                        evaluation.getDelivery().getCourier() != null ? evaluation.getDelivery().getCourier().getName()
                                : null)
                .build();
    }
}
