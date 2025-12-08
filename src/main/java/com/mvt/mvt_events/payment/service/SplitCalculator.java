package com.mvt.mvt_events.payment.service;

import com.mvt.mvt_events.dto.RecipientSplit;
import com.mvt.mvt_events.dto.RecipientSplit.RecipientType;
import com.mvt.mvt_events.jpa.Delivery;
import com.mvt.mvt_events.jpa.User;
import com.mvt.mvt_events.config.IuguConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Serviço para calcular splits de pagamento entre múltiplos motoboys/gerentes
 * 
 * <p><strong>Lógica:</strong></p>
 * <ol>
 *   <li>Para cada delivery, calcula 87% motoboy + 5% gerente</li>
 *   <li>Agrupa por pessoa (soma se mesma pessoa aparece em várias deliveries)</li>
 *   <li>Plataforma recebe o resto (8% + ajustes de arredondamento)</li>
 * </ol>
 * 
 * <p><strong>Exemplo:</strong></p>
 * <pre>
 * Delivery 1: R$ 50 (Motoboy A, Gerente X)
 *   → Motoboy A: R$ 43,50 (87%)
 *   → Gerente X: R$ 2,50 (5%)
 * 
 * Delivery 2: R$ 30 (Motoboy B, Gerente X)
 *   → Motoboy B: R$ 26,10 (87%)
 *   → Gerente X: R$ 1,50 (5%)
 * 
 * Delivery 3: R$ 20 (Motoboy A, Gerente Y)
 *   → Motoboy A: R$ 17,40 (87%)
 *   → Gerente Y: R$ 1,00 (5%)
 * 
 * Total: R$ 100
 * Splits consolidados:
 *   → Motoboy A: R$ 60,90 (43,50 + 17,40)
 *   → Motoboy B: R$ 26,10
 *   → Gerente X: R$ 4,00 (2,50 + 1,50)
 *   → Gerente Y: R$ 1,00
 *   → Plataforma: R$ 8,00 (resto)
 * </pre>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SplitCalculator {

    private final IuguConfig iuguConfig;

    /**
     * Calcula splits consolidados para múltiplas deliveries
     * 
     * @param deliveries Lista de deliveries a pagar
     * @return Lista de splits para enviar ao Iugu
     * @throws IllegalArgumentException se alguma delivery não tiver motoboy ou gerente
     */
    public List<RecipientSplit> calculateSplits(List<Delivery> deliveries) {
        log.info("═══════════════════════════════════════════════════════════════");
        log.info("📊 CALCULANDO SPLITS CONSOLIDADOS");
        log.info("═══════════════════════════════════════════════════════════════");
        log.info("📦 Deliveries: {}", deliveries.size());
        log.info("💰 Percentuais: Motoboy {}%, Gerente {}%, Plataforma (resto)", 
                iuguConfig.getSplit().getMotoboyPercentage(),
                iuguConfig.getSplit().getManagerPercentage());
        log.info("───────────────────────────────────────────────────────────────");

        // 1. Validar todas as deliveries
        validateDeliveries(deliveries);

        // 2. Calcular valor total em centavos (usando shippingFee)
        int totalCents = deliveries.stream()
                .map(Delivery::getShippingFee)
                .map(this::toRoundedCents)
                .mapToInt(Integer::intValue)
                .sum();

        log.info("💰 Valor total dos fretes: R$ {}", BigDecimal.valueOf(totalCents).divide(BigDecimal.valueOf(100)));
        log.info("───────────────────────────────────────────────────────────────");

        // 3. Calcular quanto cada pessoa deve receber
        Map<String, RecipientSplit> splitsByAccount = new HashMap<>();

        log.info("🔢 CÁLCULO POR DELIVERY:");
        for (Delivery delivery : deliveries) {
            BigDecimal shippingFee = delivery.getShippingFee();
            int deliveryCents = toRoundedCents(shippingFee);

            log.info("📦 Delivery #{} - Frete: R$ {} (Pedido: R$ {} - não entra no split)", 
                    delivery.getId(), shippingFee, delivery.getTotalAmount());

            // 87% para o motoboy
            User courier = delivery.getCourier();
            String courierAccountId = courier.getIuguAccountId();
            int courierAmount = calculatePercentage(deliveryCents, iuguConfig.getSplit().getMotoboyPercentage());

            log.info("   👨‍🚀 Motoboy: {} ({})", courier.getName(), courierAccountId);
            log.info("      R$ {} × {}% = R$ {} ({}¢)", 
                    shippingFee,
                    iuguConfig.getSplit().getMotoboyPercentage(),
                    BigDecimal.valueOf(courierAmount).divide(BigDecimal.valueOf(100)),
                    courierAmount);
            
            addOrUpdateSplit(splitsByAccount, courierAccountId, RecipientType.COURIER, courierAmount);

            // 5% para o gerente
            User manager = delivery.getOrganizer();
            String managerAccountId = manager.getIuguAccountId();
            int managerAmount = calculatePercentage(deliveryCents, iuguConfig.getSplit().getManagerPercentage());
            
            log.info("   👔 Gerente: {} ({})", manager.getName(), managerAccountId);
            log.info("      R$ {} × {}% = R$ {} ({}¢)", 
                    shippingFee,
                    iuguConfig.getSplit().getManagerPercentage(),
                    BigDecimal.valueOf(managerAmount).divide(BigDecimal.valueOf(100)),
                    managerAmount);
            
            addOrUpdateSplit(splitsByAccount, managerAccountId, RecipientType.MANAGER, managerAmount);
            
            log.info("   ───────────────────────────────────────────────────────────");
        }

        // 4. Calcular quanto vai para a plataforma (resto)
        int distributedCents = splitsByAccount.values().stream()
                .mapToInt(RecipientSplit::getAmountCents)
                .sum();

        int platformCents = totalCents - distributedCents;

        if (platformCents < 0) {
            log.error("❌ Erro no cálculo: plataforma ficaria com valor negativo!");
            throw new IllegalStateException("Erro no cálculo de splits");
        }

        // 5. Adicionar split da plataforma (se houver)
        if (platformCents > 0) {
            splitsByAccount.put("PLATFORM", new RecipientSplit(
                    null, // Conta master (null = plataforma)
                    RecipientType.PLATFORM,
                    platformCents
            ));
        }

        // 6. Log do resultado
        List<RecipientSplit> result = new ArrayList<>(splitsByAccount.values());
        logSplitsSummary(result, totalCents);

        return result;
    }

    /**
     * Valida se todas as deliveries têm motoboy e gerente com contas Iugu
     */
    private void validateDeliveries(List<Delivery> deliveries) {
        for (Delivery delivery : deliveries) {
            // Validar shippingFee
            if (delivery.getShippingFee() == null || delivery.getShippingFee().compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException(
                        String.format("Delivery #%d não tem valor de frete (shippingFee) configurado", delivery.getId()));
            }
            
            // Validar courier
            if (delivery.getCourier() == null) {
                throw new IllegalArgumentException(
                        "Delivery #" + delivery.getId() + " não tem motoboy atribuído");
            }
            if (delivery.getCourier().getIuguAccountId() == null) {
                throw new IllegalArgumentException(
                        "Motoboy " + delivery.getCourier().getName() + " não tem conta Iugu configurada");
            }
            
            // Validar organizer
            if (delivery.getOrganizer() == null) {
                throw new IllegalArgumentException(
                        "Delivery #" + delivery.getId() + " não tem gerente atribuído");
            }
            if (delivery.getOrganizer().getIuguAccountId() == null) {
                throw new IllegalArgumentException(
                        "Gerente " + delivery.getOrganizer().getName() + " não tem conta Iugu configurada");
            }
        }
    }

    /**
     * Adiciona ou incrementa o valor de um split
     */
    private void addOrUpdateSplit(
            Map<String, RecipientSplit> splits,
            String accountId,
            RecipientType type,
            int amountCents
    ) {
        splits.merge(
                accountId,
                new RecipientSplit(accountId, type, amountCents),
                (existing, newSplit) -> new RecipientSplit(
                        accountId,
                        type,
                        existing.getAmountCents() + amountCents
                )
        );
    }

    /**
     * Calcula percentual de um valor em centavos
     */
    private int calculatePercentage(int totalCents, BigDecimal percentage) {
        return BigDecimal.valueOf(totalCents)
                .multiply(percentage.divide(BigDecimal.valueOf(100))) // Converte % para decimal
                .setScale(0, RoundingMode.DOWN) // Arredonda para baixo
                .intValue();
    }

    /**
     * Converte BigDecimal para centavos (inteiro)
     */
    private int toRoundedCents(BigDecimal amount) {
        return amount.multiply(BigDecimal.valueOf(100))
                .setScale(0, RoundingMode.HALF_UP)
                .intValue();
    }

    /**
     * Log resumido dos splits calculados
     */
    private void logSplitsSummary(List<RecipientSplit> splits, int totalCents) {
        log.info("═══════════════════════════════════════════════════════════════");
        log.info("✅ SPLITS CONSOLIDADOS (após agrupamento):");
        log.info("═══════════════════════════════════════════════════════════════");
        
        // Agrupar e exibir por pessoa
        Map<RecipientType, List<RecipientSplit>> byType = splits.stream()
                .collect(Collectors.groupingBy(RecipientSplit::getType));
        
        // Motoboys
        List<RecipientSplit> couriers = byType.getOrDefault(RecipientType.COURIER, Collections.emptyList());
        if (!couriers.isEmpty()) {
            log.info("👨‍🚀 MOTOBOYS ({} pessoa(s)):", couriers.size());
            for (RecipientSplit split : couriers) {
                log.info("   {} ({}): R$ {}", 
                        split.getIuguAccountId(),
                        split.getAmountCents() + "¢",
                        formatCents(split.getAmountCents()));
            }
            log.info("   TOTAL MOTOBOYS: R$ {}", 
                    formatCents(couriers.stream().mapToInt(RecipientSplit::getAmountCents).sum()));
            log.info("───────────────────────────────────────────────────────────────");
        }
        
        // Gerentes
        List<RecipientSplit> managers = byType.getOrDefault(RecipientType.MANAGER, Collections.emptyList());
        if (!managers.isEmpty()) {
            log.info("👔 GERENTES ({} pessoa(s)):", managers.size());
            for (RecipientSplit split : managers) {
                log.info("   {} ({}): R$ {}", 
                        split.getIuguAccountId(),
                        split.getAmountCents() + "¢",
                        formatCents(split.getAmountCents()));
            }
            log.info("   TOTAL GERENTES: R$ {}", 
                    formatCents(managers.stream().mapToInt(RecipientSplit::getAmountCents).sum()));
            log.info("───────────────────────────────────────────────────────────────");
        }
        
        // Plataforma
        List<RecipientSplit> platform = byType.getOrDefault(RecipientType.PLATFORM, Collections.emptyList());
        if (!platform.isEmpty()) {
            log.info("🏢 PLATAFORMA: R$ {}", formatCents(platform.get(0).getAmountCents()));
            log.info("───────────────────────────────────────────────────────────────");
        }
        
        log.info("💰 TOTAL GERAL: R$ {}", formatCents(totalCents));
        log.info("═══════════════════════════════════════════════════════════════");
    }

    /**
     * Formata centavos para reais
     */
    private String formatCents(int cents) {
        return BigDecimal.valueOf(cents)
                .divide(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP)
                .toString();
    }
}
