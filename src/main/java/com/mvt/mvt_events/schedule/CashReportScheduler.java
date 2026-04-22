package com.mvt.mvt_events.schedule;

import com.mvt.mvt_events.dto.CashReportDto;
import com.mvt.mvt_events.jpa.ClientSubscription;
import com.mvt.mvt_events.jpa.User;
import com.mvt.mvt_events.repository.ClientSubscriptionRepository;
import com.mvt.mvt_events.service.CashReportService;
import com.mvt.mvt_events.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

/**
 * Envia o relatório diário de caixa para todo CLIENT com TABLE_SERVICE ativo.
 * Roda às 00:50 (TZ Fortaleza) — agrega pedidos do dia anterior.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class CashReportScheduler {

    private static final ZoneId TZ = ZoneId.of("America/Fortaleza");
    private static final String SERVICE_CODE = "TABLE_SERVICE";

    private final ClientSubscriptionRepository subscriptionRepository;
    private final CashReportService cashReportService;
    private final EmailService emailService;

    @Scheduled(cron = "0 50 0 * * *", zone = "America/Fortaleza")
    public void sendDailyCashReports() {
        LocalDate yesterday = LocalDate.now(TZ).minusDays(1);
        log.info("📊 [CashReport] Iniciando envio do relatório de {}", yesterday);

        List<ClientSubscription> subs = subscriptionRepository.findByActiveTrue().stream()
                .filter(s -> s.getService() != null && SERVICE_CODE.equals(s.getService().getCode()))
                .toList();

        if (subs.isEmpty()) {
            log.info("📊 [CashReport] Nenhum client com {} ativo", SERVICE_CODE);
            return;
        }

        int sent = 0, empty = 0, errors = 0;
        for (ClientSubscription sub : subs) {
            User client = sub.getClient();
            if (client == null || client.getUsername() == null) continue;
            try {
                CashReportDto report = cashReportService.generateFor(client, yesterday);
                if (report == null) {
                    empty++;
                    log.debug("📊 [CashReport] Sem pedidos para {} em {}", client.getUsername(), yesterday);
                    continue;
                }
                emailService.sendCashReport(client, report);
                sent++;
            } catch (Exception e) {
                errors++;
                log.error("❌ [CashReport] Erro ao processar {}: {}", client.getUsername(), e.getMessage(), e);
            }
        }
        log.info("📊 [CashReport] Concluído — enviados: {}, sem pedidos: {}, erros: {}", sent, empty, errors);
    }
}
