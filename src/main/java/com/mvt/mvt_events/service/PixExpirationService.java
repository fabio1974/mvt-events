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
 * Serviço para tratamento de expiração de pagamentos PIX do CUSTOMER.
 * 
 * Opção B: Quando o QR Code PIX expira sem pagamento,
 * desassocia o courier e retorna a delivery para PENDING,
 * depois notifica motoboys disponíveis (mesmo fluxo de delivery nova).
 * 
 * Somente processa pagamentos de pagadores com role CUSTOMER.
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
     * Somente pagamentos de pagadores CUSTOMER são processados.
     * 
     * Para cada pagamento expirado:
     * 1. Marca o pagamento como EXPIRED
     * 2. Para cada delivery em WAITING_PAYMENT associada:
     *    - Desassocia o courier
     *    - Retorna o status para PENDING
     *    - Limpa dados do aceite (acceptedAt, vehicle)
     *    - Notifica motoboys disponíveis (push)
     * 
     * Roda a cada 30 segundos.
     */
    @Scheduled(fixedRate = 30000)
    @Transactional
    public void checkExpiredPixPayments() {
        LocalDateTime now = LocalDateTime.now();

        // Buscar pagamentos PIX PENDING cujo expiresAt já passou
        List<Payment> expiredPayments = paymentRepository.findExpiredPendingPixPayments(now);

        if (expiredPayments.isEmpty()) {
            return;
        }

        log.info("⏰ Encontrados {} pagamentos PIX expirados — verificando se são CUSTOMER", 
                expiredPayments.size());

        for (Payment payment : expiredPayments) {
            try {
                // Filtrar: somente CUSTOMER
                if (payment.getPayer() == null || payment.getPayer().getRole() != User.Role.CUSTOMER) {
                    log.debug("⏭️ Payment #{} não é de CUSTOMER (payer role: {}) — ignorando no cron", 
                            payment.getId(),
                            payment.getPayer() != null ? payment.getPayer().getRole() : "null");
                    continue;
                }

                processExpiredPayment(payment);
            } catch (Exception e) {
                log.error("❌ Erro ao processar expiração do Payment #{}: {}", 
                        payment.getId(), e.getMessage(), e);
            }
        }
    }

    /**
     * Processa um pagamento PIX expirado de CUSTOMER.
     * Marca como EXPIRED, reverte deliveries para PENDING e notifica motoboys.
     */
    @Transactional
    public void processExpiredPayment(Payment payment) {
        log.info("⏰ Expirando Payment #{} (CUSTOMER PIX, expiresAt: {}, agora: {})", 
                payment.getId(), payment.getExpiresAt(), LocalDateTime.now());

        // 1. Marcar pagamento como EXPIRED
        payment.setStatus(PaymentStatus.EXPIRED);
        paymentRepository.save(payment);

        // 2. Reverter deliveries em WAITING_PAYMENT para PENDING e notificar motoboys
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

                    // 3. Notificar motoboys disponíveis (mesmo fluxo de delivery nova)
                    try {
                        deliveryNotificationService.notifyAvailableDrivers(delivery);
                        log.info("   ├─ 📢 Push enviado para motoboys disponíveis (delivery #{})", delivery.getId());
                    } catch (Exception e) {
                        log.error("   ├─ ❌ Falha ao enviar push para motoboys (delivery #{}): {}", 
                                delivery.getId(), e.getMessage());
                    }

                    log.info("   └─ ✅ Delivery #{} revertida para PENDING — motoboys notificados", 
                            delivery.getId());
                }
            }
        }

        log.info("✅ Payment #{} CUSTOMER PIX expirado — deliveries revertidas e motoboys notificados", 
                payment.getId());
    }
}
