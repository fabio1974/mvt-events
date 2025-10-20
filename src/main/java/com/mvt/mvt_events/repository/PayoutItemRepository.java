package com.mvt.mvt_events.repository;

import com.mvt.mvt_events.jpa.PayoutItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository para PayoutItem
 * Tabela intermediária entre Payment e UnifiedPayout
 */
@Repository
public interface PayoutItemRepository
                extends JpaRepository<PayoutItem, Long>, JpaSpecificationExecutor<PayoutItem> {

        /**
         * Busca items de um payout específico
         */
        List<PayoutItem> findByPayoutIdOrderByCreatedAtAsc(Long payoutId);

        /**
         * Busca items de um payment específico
         */
        List<PayoutItem> findByPaymentId(Long paymentId);

        /**
         * Verifica se um payment já está em algum payout
         */
        boolean existsByPaymentId(Long paymentId);

        /**
         * Busca item específico por payout e payment
         */
        Optional<PayoutItem> findByPayoutIdAndPaymentId(Long payoutId, Long paymentId);

        /**
         * Busca items por tipo de valor
         */
        @Query("SELECT pi FROM PayoutItem pi WHERE pi.valueType = :valueType")
        List<PayoutItem> findByValueType(@Param("valueType") String valueType);

        /**
         * Soma valores de items de um payout
         */
        @Query("SELECT SUM(pi.itemValue) FROM PayoutItem pi WHERE pi.payout.id = :payoutId")
        Double sumItemValuesByPayoutId(@Param("payoutId") Long payoutId);

        /**
         * Conta items de um payout
         */
        @Query("SELECT COUNT(pi) FROM PayoutItem pi WHERE pi.payout.id = :payoutId")
        Long countByPayoutId(@Param("payoutId") Long payoutId);

        /**
         * Busca payments não incluídos em nenhum payout
         * (para processamento de novos repasses)
         */
        @Query("SELECT p.id FROM Payment p " +
                        "WHERE NOT EXISTS (SELECT pi FROM PayoutItem pi WHERE pi.payment.id = p.id) " +
                        "AND p.status = 'COMPLETED' " +
                        "AND p.processedAt IS NOT NULL")
        List<Long> findPaymentIdsNotInAnyPayout();
}
