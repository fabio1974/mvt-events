package com.mvt.mvt_events.service;

import com.mvt.mvt_events.jpa.Delivery;
import com.mvt.mvt_events.jpa.Evaluation;
import com.mvt.mvt_events.jpa.User;
import com.mvt.mvt_events.repository.DeliveryRepository;
import com.mvt.mvt_events.repository.EvaluationRepository;
import com.mvt.mvt_events.repository.UserRepository;
import com.mvt.mvt_events.specification.EvaluationSpecification;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Service para Evaluation
 * Gerenciamento de avaliações de deliveries
 */
@Service
@Transactional
public class EvaluationService {

    @Autowired
    private EvaluationRepository evaluationRepository;

    @Autowired
    private DeliveryRepository deliveryRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CourierProfileService courierProfileService;

    /**
     * Cria avaliação
     * VALIDA: Delivery existe, está completada, não tem avaliação, usuário tem
     * permissão
     */
    public Evaluation create(Evaluation evaluation, Long deliveryId, UUID evaluatorId) {
        // Validar delivery
        Delivery delivery = deliveryRepository.findById(deliveryId)
                .orElseThrow(() -> new RuntimeException("Delivery não encontrada"));

        if (delivery.getStatus() != Delivery.DeliveryStatus.COMPLETED) {
            throw new RuntimeException("Delivery deve estar completada para avaliação");
        }

        // Verificar se já existe avaliação
        if (evaluationRepository.existsByDeliveryId(deliveryId)) {
            throw new RuntimeException("Delivery já possui avaliação");
        }

        // Validar avaliador
        User evaluator = userRepository.findById(evaluatorId)
                .orElseThrow(() -> new RuntimeException("Avaliador não encontrado"));

        // Validar permissão e tipo de avaliação
        if (evaluation.getEvaluationType() == Evaluation.EvaluationType.CLIENT_TO_COURIER) {
            if (!delivery.getClient().getId().equals(evaluatorId)) {
                throw new RuntimeException("Apenas o cliente pode avaliar o courier");
            }
        } else if (evaluation.getEvaluationType() == Evaluation.EvaluationType.COURIER_TO_CLIENT) {
            if (delivery.getCourier() == null || !delivery.getCourier().getId().equals(evaluatorId)) {
                throw new RuntimeException("Apenas o courier pode avaliar o cliente");
            }
        }

        // Validar rating
        if (evaluation.getRating() < 1 || evaluation.getRating() > 5) {
            throw new RuntimeException("Rating deve estar entre 1 e 5");
        }

        evaluation.setDelivery(delivery);
        evaluation.setEvaluator(evaluator);

        Evaluation saved = evaluationRepository.save(evaluation);

        // Atualizar rating do courier (se avaliação cliente→courier)
        if (evaluation.getEvaluationType() == Evaluation.EvaluationType.CLIENT_TO_COURIER &&
                delivery.getCourier() != null) {
            updateCourierRating(delivery.getCourier().getId());
        }

        return saved;
    }

    /**
     * Busca avaliação por delivery ID
     */
    public Evaluation findByDeliveryId(Long deliveryId) {
        return evaluationRepository.findByDeliveryId(deliveryId)
                .orElseThrow(() -> new RuntimeException("Avaliação não encontrada"));
    }

    /**
     * Lista avaliações com filtros
     */
    public Page<Evaluation> findAll(UUID admId, UUID courierId,
            Evaluation.EvaluationType evaluationType,
            Integer minRating, Pageable pageable) {
        Specification<Evaluation> spec = EvaluationSpecification.hasAdmId(admId)
                .and(EvaluationSpecification.hasCourierId(courierId))
                .and(EvaluationSpecification.hasEvaluationType(evaluationType))
                .and(EvaluationSpecification.hasRatingGreaterThan(minRating));

        return evaluationRepository.findAll(spec, pageable);
    }

    /**
     * Busca avaliações recebidas por um courier
     */
    public List<Evaluation> findReceivedByCourier(UUID courierId) {
        return evaluationRepository.findReceivedByCourierId(courierId);
    }

    /**
     * Calcula rating médio do courier
     */
    public Double getAverageRatingForCourier(UUID courierId) {
        Double average = evaluationRepository.calculateAverageRatingForCourier(courierId);
        return average != null ? average : 0.0;
    }

    /**
     * Atualiza rating do courier baseado em todas as avaliações
     */
    private void updateCourierRating(UUID courierId) {
        List<Evaluation> evaluations = evaluationRepository.findReceivedByCourierId(courierId);

        if (!evaluations.isEmpty()) {
            double sum = evaluations.stream()
                    .mapToInt(Evaluation::getRating)
                    .sum();
            double average = sum / evaluations.size();

            courierProfileService.updateRating(
                    courierId,
                    java.math.BigDecimal.valueOf(average),
                    evaluations.size());
        }
    }

    /**
     * Busca avaliações ruins (rating <= 2) para análise
     */
    public List<Evaluation> findPoorRatings(UUID admId) {
        Specification<Evaluation> spec = EvaluationSpecification.hasAdmId(admId)
                .and(EvaluationSpecification.isPoorRating());

        return evaluationRepository.findAll(spec);
    }

    /**
     * Distribuição de ratings de um courier
     */
    public List<Object[]> getRatingDistribution(UUID courierId) {
        return evaluationRepository.countRatingDistributionForCourier(courierId);
    }
}
