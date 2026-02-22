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
         * Busca todos os pagamentos que incluem uma entrega (N:M via payment_deliveries)
         */
        @Query("SELECT p FROM Payment p JOIN p.deliveries d WHERE d.id = :deliveryId ORDER BY p.createdAt DESC")
        List<Payment> findByDeliveryId(@Param("deliveryId") UUID deliveryId);

        /**
         * Busca todos os pagamentos de um pagador
         */
        @Query("SELECT p FROM Payment p WHERE p.payer.id = :payerId ORDER BY p.createdAt DESC")
        List<Payment> findByPayerId(@Param("payerId") UUID payerId);

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
         * Verifica se existe pagamento para uma entrega (N:M via payment_deliveries)
         */
        @Query("SELECT COUNT(p) > 0 FROM Payment p JOIN p.deliveries d WHERE d.id = :deliveryId")
        boolean existsByDeliveryId(@Param("deliveryId") UUID deliveryId);

        /**
         * Verifica se existe pagamento para uma entrega por ID Long (N:M via payment_deliveries)
         */
        @Query("SELECT COUNT(p) > 0 FROM Payment p JOIN p.deliveries d WHERE d.id = :deliveryId")
        boolean existsByDeliveryIdLong(@Param("deliveryId") Long deliveryId);

        /**
         * Busca todos os pagamentos que incluem uma entrega por ID Long (N:M via payment_deliveries)
         */
        @Query("SELECT p FROM Payment p JOIN p.deliveries d WHERE d.id = :deliveryId ORDER BY p.createdAt DESC")
        List<Payment> findByDeliveryIdLong(@Param("deliveryId") Long deliveryId);

        /**
         * Busca pagamentos que incluem uma entrega com status específico (N:M via payment_deliveries)
         */
        @Query("SELECT p FROM Payment p JOIN p.deliveries d WHERE d = :delivery AND p.status = :status")
        List<Payment> findByDeliveryAndStatus(@Param("delivery") com.mvt.mvt_events.jpa.Delivery delivery, @Param("status") PaymentStatus status);

        /**
         * Busca pagamentos PENDING ou COMPLETED que incluem qualquer uma das deliveries especificadas.
         * Usado para evitar criar pagamentos duplicados para as mesmas entregas.
         * 
         * @param deliveryIds Lista de IDs das deliveries a verificar
         * @return Lista de pagamentos ativos que incluem essas deliveries
         */
        @Query("SELECT DISTINCT p FROM Payment p JOIN p.deliveries d " +
                "WHERE d.id IN :deliveryIds " +
                "AND p.status IN ('PENDING', 'COMPLETED') " +
                "ORDER BY p.createdAt DESC")
        List<Payment> findPendingOrCompletedPaymentsForDeliveries(@Param("deliveryIds") List<Long> deliveryIds);

        /**
         * Verifica se existe um pagamento PENDING para uma delivery específica.
         * Usado para prevenir duplicação de tentativas de pagamento.
         * 
         * @param deliveryId ID da delivery
         * @return true se existe pagamento PENDING, false caso contrário
         */
        @Query("SELECT COUNT(p) > 0 FROM Payment p JOIN p.deliveries d " +
                "WHERE d.id = :deliveryId AND p.status = 'PENDING'")
        boolean existsPendingPaymentForDelivery(@Param("deliveryId") Long deliveryId);

        /**
         * Verifica se existe um pagamento PENDING ou PAID para uma delivery específica.
         * Usado para impedir que a mesma corrida seja paga duas vezes.
         * 
         * @param deliveryId ID da delivery
         * @return true se existe pagamento PENDING ou PAID
         */
        @Query("SELECT COUNT(p) > 0 FROM Payment p JOIN p.deliveries d " +
                "WHERE d.id = :deliveryId AND p.status IN ('PENDING', 'PAID')")
        boolean existsPendingOrPaidPaymentForDelivery(@Param("deliveryId") Long deliveryId);

        /**
         * Busca pagamentos PIX PENDING cujo QR Code expirou (expiresAt < now).
         * Usado pelo cron de expiração PIX (PixExpirationService).
         */
        @Query("SELECT p FROM Payment p " +
                "WHERE p.status = 'PENDING' " +
                "AND p.paymentMethod = 'PIX' " +
                "AND p.expiresAt IS NOT NULL " +
                "AND p.expiresAt < :now")
        List<Payment> findExpiredPendingPixPayments(@Param("now") LocalDateTime now);
}
