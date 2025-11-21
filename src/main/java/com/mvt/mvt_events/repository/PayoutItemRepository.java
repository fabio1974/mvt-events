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
 * Cada item representa um repasse individual para um beneficiário
 */
@Repository
public interface PayoutItemRepository
                extends JpaRepository<PayoutItem, Long>, JpaSpecificationExecutor<PayoutItem> {

        /**
         * Busca items de um payment específico
         */
        List<PayoutItem> findByPaymentIdOrderByCreatedAtAsc(Long paymentId);

        /**
         * Verifica se um payment já tem items de repasse
         */
        boolean existsByPaymentId(Long paymentId);

        /**
         * Busca items por tipo de valor
         */
        @Query("SELECT pi FROM PayoutItem pi WHERE pi.valueType = :valueType")
        List<PayoutItem> findByValueType(@Param("valueType") String valueType);

        /**
         * Busca payments não incluídos em nenhum item de repasse
         * (para processamento de novos repasses)
         */
        @Query("SELECT p.id FROM Payment p " +
                        "WHERE NOT EXISTS (SELECT pi FROM PayoutItem pi WHERE pi.payment.id = p.id) " +
                        "AND p.status = 'COMPLETED'")
        List<Long> findPaymentIdsNotInAnyPayout();

        /**
         * Busca items de repasse por beneficiário
         */
        List<PayoutItem> findByBeneficiaryIdOrderByCreatedAtDesc(java.util.UUID beneficiaryId);

        /**
         * Busca items de repasse por beneficiário e status
         */
        List<PayoutItem> findByBeneficiaryIdAndStatusOrderByCreatedAtDesc(
                        java.util.UUID beneficiaryId,
                        PayoutItem.PayoutStatus status);

        /**
         * Busca items de repasse por status
         */
        List<PayoutItem> findByStatusOrderByCreatedAtAsc(PayoutItem.PayoutStatus status);

        /**
         * Busca items pendentes de pagamento para um beneficiário
         */
        @Query("SELECT pi FROM PayoutItem pi WHERE pi.beneficiary.id = :beneficiaryId " +
                        "AND pi.status = 'PENDING' ORDER BY pi.createdAt ASC")
        List<PayoutItem> findPendingByBeneficiaryId(@Param("beneficiaryId") java.util.UUID beneficiaryId);

        /**
         * Soma total de repasses pagos para um beneficiário
         */
        @Query("SELECT SUM(pi.itemValue) FROM PayoutItem pi WHERE pi.beneficiary.id = :beneficiaryId " +
                        "AND pi.status = 'PAID'")
        Double sumPaidAmountByBeneficiaryId(@Param("beneficiaryId") java.util.UUID beneficiaryId);

        /**
         * Soma total de repasses pendentes para um beneficiário
         */
        @Query("SELECT SUM(pi.itemValue) FROM PayoutItem pi WHERE pi.beneficiary.id = :beneficiaryId " +
                        "AND pi.status = 'PENDING'")
        Double sumPendingAmountByBeneficiaryId(@Param("beneficiaryId") java.util.UUID beneficiaryId);

        /**
         * Busca items de repasse por tipo de valor e status
         */
        List<PayoutItem> findByValueTypeAndStatus(
                        PayoutItem.ValueType valueType,
                        PayoutItem.PayoutStatus status);
}
