package com.mvt.mvt_events.schedule;

import com.mvt.mvt_events.jpa.PagarmeTransfer;
import com.mvt.mvt_events.jpa.User;
import com.mvt.mvt_events.repository.PagarmeTransferRepository;
import com.mvt.mvt_events.service.CourierTransferService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Processa transfers PENDING via PixOutProvider.
 *
 * ⚠️ DESABILITADO por padrão — fluxo atual é SEMI-AUTOMÁTICO:
 * admin dispara manualmente via "Dívidas com Couriers".
 * Para ativar modo automático, setar `pix.out.scheduler.enabled=true`.
 */
@Component
@Slf4j
@RequiredArgsConstructor
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(
        name = "pix.out.scheduler.enabled", havingValue = "true")
public class PixOutTransferScheduler {

    private final PagarmeTransferRepository transferRepository;
    private final CourierTransferService transferService;

    @Scheduled(cron = "0 */5 * * * *")
    public void processPendingTransfers() {
        List<PagarmeTransfer> pending = transferRepository.findByStatus(PagarmeTransfer.Status.PENDING);
        if (pending.isEmpty()) return;

        log.info("💸 [PixOutScheduler] processando {} transfers PENDING", pending.size());
        int sent = 0, skipped = 0, failed = 0;
        for (PagarmeTransfer t : pending) {
            User courier = t.getRecipient();
            if (courier == null || courier.getPixKey() == null || courier.getPixKey().isBlank()) {
                skipped++;
                continue;
            }
            try {
                transferService.executeTransfer(t, courier);
                if (t.getStatus() == PagarmeTransfer.Status.SUCCEEDED) sent++;
                else if (t.getStatus() == PagarmeTransfer.Status.FAILED) failed++;
            } catch (Exception e) {
                log.error("❌ Falha processando transfer #{}: {}", t.getId(), e.getMessage(), e);
                failed++;
            }
        }
        log.info("💸 [PixOutScheduler] resultado — sent: {}, skipped (sem pixKey): {}, failed: {}",
                sent, skipped, failed);
    }
}
