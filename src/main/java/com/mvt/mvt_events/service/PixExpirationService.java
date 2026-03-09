package com.mvt.mvt_events.service;

import com.mvt.mvt_events.jpa.Delivery;
import com.mvt.mvt_events.jpa.Payment;
import com.mvt.mvt_events.jpa.PaymentStatus;
import com.mvt.mvt_events.jpa.User;
import com.mvt.mvt_events.repository.PaymentRepository;
import com.mvt.mvt_events.repository.DeliveryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Serviço para tratamento de expiração de pagamentos PIX.
 *
 * CUSTOMER: Quando o QR Code PIX expira sem pagamento,
 * desassocia o courier e retorna a delivery para PENDING,
 * depois notifica motoboys disponíveis (mesmo fluxo de delivery nova).
 *
 * CLIENT: Quando o QR Code PIX consolidado expira sem pagamento,
 * apenas marca o pagamento como EXPIRED. O scheduler consolidado
 * (ConsolidatedPaymentScheduler) irá gerar um novo PIX na próxima execução.
 *
 * Executa a cada 30 segundos para detectar expiração rapidamente.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class PixExpirationService {

    private final PaymentRepository paymentRepository;
    private final DeliveryRepository deliveryRepository;
    private final DeliveryNotificationService deliveryNotificationService;

    /**
     * Verifica pagamentos PIX PENDING com expiresAt vencido.
     *
     * - CUSTOMER: reverte deliveries para PENDING e notifica motoboys.
     * - CLIENT: apenas marca o pagamento como EXPIRED (novo PIX será gerado
     *   pelo ConsolidatedPaymentScheduler na próxima execução).
     *
     * Roda a cada 30 segundos.
     */
    @Scheduled(fixedRate = 30000)
    @Transactional
    public void checkExpiredPixPayments() {
        LocalDateTime now = LocalDateTime.now();

        List<Payment> expiredPayments = paymentRepository.findExpiredPendingPixPayments(now);

        if (expiredPayments.isEmpty()) {
            return;
        }

        log.info("⏰ Encontrados {} pagamentos PIX expirados", expiredPayments.size());

        for (Payment payment : expiredPayments) {
            try {
                if (payment.getPayer() == null) {
                    log.warn("⏭️ Payment #{} sem payer — ignorando", payment.getId());
                    continue;
                }

                User.Role role = payment.getPayer().getRole();

                if (role == User.Role.CUSTOMER) {
                    processExpiredCustomerPayment(payment);
                } else if (role == User.Role.CLIENT) {
                    processExpiredClientPayment(payment);
                } else {
                    log.debug("⏭️ Payment #{} ignorado (payer role: {})", payment.getId(), role);
                }
            } catch (Exception e) {
                log.error("❌ Erro ao processar expiração do Payment #{}: {}",
                        payment.getId(), e.getMessage(), e);
            }
        }

        // Cancelar deliveries PENDING sem aceite há mais de 30 minutos
        expireStalePendingDeliveries(now);
    }

    /**
     * Cancela deliveries que estão em PENDING sem aceite de motoboy há mais de 30 minutos.
     * Status final: CANCELLED com motivo "Expirada: sem aceite em 30 minutos".
     */
    private void expireStalePendingDeliveries(LocalDateTime now) {
        LocalDateTime cutoff = now.minusMinutes(30);
        List<com.mvt.mvt_events.jpa.Delivery> stale =
                deliveryRepository.findStalePendingDeliveries(cutoff);

        if (stale.isEmpty()) return;

        log.info("⏰ [PENDING EXPIRY] {} deliveries PENDING há mais de 30 min — cancelando", stale.size());

        for (com.mvt.mvt_events.jpa.Delivery delivery : stale) {
            try {
                delivery.setStatus(com.mvt.mvt_events.jpa.Delivery.DeliveryStatus.CANCELLED);
                delivery.setCancelledAt(now);
                delivery.setCancellationReason("Expirada: sem aceite em 30 minutos");
                deliveryRepository.save(delivery);
                log.info("   ✅ Delivery #{} cancelada (criada em {})", delivery.getId(), delivery.getCreatedAt());
            } catch (Exception e) {
                log.error("   ❌ Erro ao cancelar delivery #{}: {}", delivery.getId(), e.getMessage());
            }
        }
    }

    /**
     * Processa um pagamento PIX expirado de CUSTOMER.
     * Marca como EXPIRED, reverte deliveries para PENDING e notifica motoboys.
     */
    @Transactional
    public void processExpiredCustomerPayment(Payment payment) {
        log.info("⏰ Expirando Payment #{} (CUSTOMER PIX, expiresAt: {})", 
                payment.getId(), payment.getExpiresAt());

        payment.setStatus(PaymentStatus.EXPIRED);
        paymentRepository.save(payment);

        if (payment.getDeliveries() != null) {
            for (Delivery delivery : payment.getDeliveries()) {
                if (delivery.getStatus() == Delivery.DeliveryStatus.WAITING_PAYMENT) {
                    log.info("   ├─ Delivery #{}: WAITING_PAYMENT → PENDING (desassociando courier {})",
                            delivery.getId(),
                            delivery.getCourier() != null ? delivery.getCourier().getName() : "null");

                    delivery.setStatus(Delivery.DeliveryStatus.PENDING);
                    delivery.setCourier(null);
                    delivery.setAcceptedAt(null);
                    delivery.setVehicle(null);
                    delivery.setPaymentCompleted(false);
                    delivery.setPaymentCaptured(false);
                    deliveryRepository.save(delivery);

                    try {
                        deliveryNotificationService.notifyAvailableDrivers(delivery);
                        log.info("   ├─ 📢 Push enviado para motoboys disponíveis (delivery #{})", delivery.getId());
                    } catch (Exception e) {
                        log.error("   ├─ ❌ Falha ao enviar push para motoboys (delivery #{}): {}",
                                delivery.getId(), e.getMessage());
                    }

                    log.info("   └─ ✅ Delivery #{} revertida para PENDING — motoboys notificados", delivery.getId());
                }
            }
        }

        log.info("✅ Payment #{} CUSTOMER PIX expirado — deliveries revertidas e motoboys notificados",
                payment.getId());
    }

    /**
     * Processa um pagamento PIX consolidado expirado de CLIENT.
     * Apenas marca o pagamento como EXPIRED.
     * O ConsolidatedPaymentScheduler gerará um novo PIX na próxima execução.
     */
    @Transactional
    public void processExpiredClientPayment(Payment payment) {
        log.info("⏰ Expirando Payment #{} (CLIENT PIX consolidado, expiresAt: {})",
                payment.getId(), payment.getExpiresAt());

        payment.setStatus(PaymentStatus.EXPIRED);
        paymentRepository.save(payment);

        log.info("✅ Payment #{} CLIENT PIX expirado — novo PIX será gerado pelo scheduler consolidado",
                payment.getId());
    }
}
