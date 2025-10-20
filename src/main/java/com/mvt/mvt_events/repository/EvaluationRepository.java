package com.mvt.mvt_events.repository;

import com.mvt.mvt_events.jpa.Evaluation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository para Evaluation
 * Avaliações 1:1 com Delivery (cliente→courier ou courier→cliente)
 */
@Repository
public interface EvaluationRepository
        extends JpaRepository<Evaluation, Long>, JpaSpecificationExecutor<Evaluation> {

    /**
     * Busca avaliação de uma delivery específica
     */
    Optional<Evaluation> findByDeliveryId(Long deliveryId);

    /**
     * Verifica se existe avaliação para uma delivery
     */
    boolean existsByDeliveryId(Long deliveryId);

    /**
     * Busca avaliações por avaliador
     */
    List<Evaluation> findByEvaluatorId(UUID evaluatorId);

    /**
     * Busca avaliações recebidas por um courier
     */
    @Query("SELECT e FROM Evaluation e " +
            "WHERE e.delivery.courier.id = :courierId " +
            "AND e.evaluationType = 'CLIENT_TO_COURIER' " +
            "ORDER BY e.createdAt DESC")
    List<Evaluation> findReceivedByCourierId(@Param("courierId") UUID courierId);

    /**
     * Calcula rating médio de um courier
     */
    @Query("SELECT AVG(e.rating) FROM Evaluation e " +
            "WHERE e.delivery.courier.id = :courierId " +
            "AND e.evaluationType = 'CLIENT_TO_COURIER'")
    Double calculateAverageRatingForCourier(@Param("courierId") UUID courierId);

    /**
     * Busca avaliações por tipo
     */
    @Query("SELECT e FROM Evaluation e WHERE e.evaluationType = :type")
    List<Evaluation> findByEvaluationType(@Param("type") String type);

    /**
     * Conta avaliações por rating para um courier
     */
    @Query("SELECT e.rating, COUNT(e) FROM Evaluation e " +
            "WHERE e.delivery.courier.id = :courierId " +
            "AND e.evaluationType = 'CLIENT_TO_COURIER' " +
            "GROUP BY e.rating " +
            "ORDER BY e.rating DESC")
    List<Object[]> countRatingDistributionForCourier(@Param("courierId") UUID courierId);
}
