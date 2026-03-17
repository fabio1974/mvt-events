package com.mvt.mvt_events.schedule;

import com.mvt.mvt_events.jpa.Payment;
import com.mvt.mvt_events.repository.PaymentRepository;
import com.mvt.mvt_events.service.PushNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Scheduler de lembrete de pagamento consolidado para CLIENTs.
 *
 * Roda às 16:05 — sempre 5 minutos após o scheduler de geração das 16:00,
 * garantindo que o PIX já foi gerado e nunca cai na janela de expiração.
 *
 * Envia 1 push por dia por cliente: "Você tem cobranças pendentes — pague via PIX."
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class ConsolidatedPaymentReminderScheduler {

    private final PaymentRepository paymentRepository;
    private final PushNotificationService pushNotificationService;

    /**
     * Envia lembrete de pagamento para todos os CLIENTs com PIX PENDING.
     * Roda às 16:05 todo dia.
     */
    @Scheduled(cron = "0 5 16 * * *")
    public void sendPaymentReminders() {
        log.info("╔════════════════════════════════════════════════════════════════╗");
        log.info("║ 🔔 CRONJOB: Lembretes de Pagamento Consolidado (16:05)          ║");
        log.info("╚════════════════════════════════════════════════════════════════╝");

        List<Payment> pendingPixPayments = paymentRepository.findPendingPixPaymentsByClients();

        if (pendingPixPayments.isEmpty()) {
            log.info("📭 Nenhum PIX PENDING de CLIENT encontrado — sem lembretes a enviar");
            return;
        }

        log.info("📋 {} clientes com PIX PENDING — enviando lembretes", pendingPixPayments.size());

        int sent = 0;
        int failed = 0;

        for (Payment payment : pendingPixPayments) {
            try {
                if (payment.getPayer() == null) {
                    log.warn("⏭️ Payment #{} sem payer — ignorando", payment.getId());
                    continue;
                }

                BigDecimal amount = payment.getAmount();
                String title = "💳 Pagamento pendente";
                String body = String.format(
                    "Você tem R$ %.2f em fretes pendentes. Pague via PIX ainda hoje e evite bloqueios.",
                    amount
                );

                Map<String, Object> data = new HashMap<>();
                data.put("type", "consolidated_payment_reminder");
                data.put("paymentId", payment.getId());
                data.put("amount", amount.toString());
                data.put("pixQrCode", payment.getPixQrCode());
                data.put("pixQrCodeUrl", payment.getPixQrCodeUrl());

                boolean notified = pushNotificationService.sendNotificationToUser(
                    payment.getPayer().getId(),
                    title,
                    body,
                    data
                );

                if (notified) {
                    log.info("✅ Lembrete enviado — cliente {} | payment #{} | R$ {}",
                        payment.getPayer().getUsername(), payment.getId(), amount);
                    sent++;
                } else {
                    log.warn("⚠️ Cliente {} sem token push ativo — payment #{}",
                        payment.getPayer().getUsername(), payment.getId());
                    failed++;
                }

            } catch (Exception e) {
                log.error("❌ Erro ao enviar lembrete para payment #{}: {}", payment.getId(), e.getMessage());
                failed++;
            }
        }

        log.info("═══════════════════════════════════════════════════════════════");
        log.info("📊 Lembretes: {} enviados, {} falharam", sent, failed);
        log.info("═══════════════════════════════════════════════════════════════");
    }
}
