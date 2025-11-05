package com.mvt.mvt_events.repository;

import com.mvt.mvt_events.jpa.Payment;
import com.mvt.mvt_events.jpa.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long>, JpaSpecificationExecutor<Payment> {

        /**
         * Busca pagamento por transaction ID
         */
        Optional<Payment> findByTransactionId(String transactionId);

        /**
         * Busca pagamento por provider payment ID
         */
        Optional<Payment> findByProviderPaymentId(String providerPaymentId);

        /**
         * Busca todos os pagamentos de uma entrega
         */
        List<Payment> findByDeliveryId(Long deliveryId);

        /**
         * Busca todos os pagamentos de um pagador
         */
        @Query("SELECT p FROM Payment p WHERE p.payer.id = :payerId ORDER BY p.createdAt DESC")
        List<Payment> findByPayerId(@Param("payerId") UUID payerId);

        /**
         * Busca todos os pagamentos de uma organização
         */
        @Query("SELECT p FROM Payment p WHERE p.organization.id = :organizationId ORDER BY p.createdAt DESC")
        List<Payment> findByOrganizationId(@Param("organizationId") Long organizationId);

        /**
         * Busca pagamentos por status
         */
        List<Payment> findByStatus(PaymentStatus status);

        /**
         * Busca pagamentos pendentes
         */
        @Query("SELECT p FROM Payment p WHERE p.status = 'PENDING' ORDER BY p.createdAt")
        List<Payment> findPendingPayments();

        /**
         * Busca pagamentos completados em um período
         */
        @Query("SELECT p FROM Payment p WHERE p.status = 'COMPLETED' " +
                        "AND p.paymentDate BETWEEN :startDate AND :endDate " +
                        "ORDER BY p.paymentDate DESC")
        List<Payment> findCompletedPaymentsBetween(
                        @Param("startDate") LocalDateTime startDate,
                        @Param("endDate") LocalDateTime endDate);

        /**
         * Busca pagamentos por provider
         */
        List<Payment> findByProvider(String provider);

        /**
         * Verifica se existe pagamento para uma entrega
         */
        boolean existsByDeliveryId(Long deliveryId);

        /**
         * Busca pagamentos que não estão em nenhum payout
         */
        @Query("SELECT p FROM Payment p WHERE p.id NOT IN " +
                        "(SELECT pi.payment.id FROM PayoutItem pi WHERE pi.payment IS NOT NULL)")
        List<Payment> findPaymentsNotInAnyPayout();

        /**
         * Conta pagamentos por status de uma organização
         */
        @Query("SELECT COUNT(p) FROM Payment p WHERE p.organization.id = :organizationId AND p.status = :status")
        Long countByOrganizationIdAndStatus(
                        @Param("organizationId") Long organizationId,
                        @Param("status") PaymentStatus status);
}
