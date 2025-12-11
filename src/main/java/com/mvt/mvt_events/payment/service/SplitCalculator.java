package com.mvt.mvt_events.payment.service;

import com.mvt.mvt_events.jpa.Delivery;
import com.mvt.mvt_events.payment.dto.PagarMeSplitRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Calculadora de split de pagamento para Pagar.me
 * 
 * Modelo de negócio Zapi10:
 * - 87% para o motoboy (entregador)
 * - 5% para o gerente (manager)
 * - 8% para a plataforma Zapi10 (automático - remainder)
 * 
 * Regras de liable e taxas:
 * - ZAPI10 (plataforma) é LIABLE: assume risco de chargebacks
 * - ZAPI10 paga TODAS as taxas de processamento
 * - Motoboy e Manager recebem valores líquidos (sem desconto de taxas)
 */
@Service
public class SplitCalculator {

    @Value("${pagarme.split.courier-percentage:8700}")
    private Integer courierPercentage; // 8700 = 87.00%

    @Value("${pagarme.split.manager-percentage:500}")
    private Integer managerPercentage; // 500 = 5.00%

    @Value("${pagarme.split.courier-liable:false}")
    private Boolean courierLiable; // false - plataforma é liable

    @Value("${pagarme.split.courier-charge-processing-fee:false}")
    private Boolean courierChargeFee; // false - plataforma paga

    @Value("${pagarme.split.manager-charge-processing-fee:false}")
    private Boolean managerChargeFee; // false - plataforma paga

    /**
     * Calcula o split de pagamento para Pagar.me
     * 
     * Formato Pagar.me:
     * {
     *   "amount": 8700,  // 87% em inteiro (8700 = 87.00%)
     *   "recipient_id": "re_ckl9k45a60001og6hdqb9mqc7",
     *   "type": "percentage",
     *   "options": {
     *     "liable": false,  // SEMPRE false - plataforma é liable
     *     "charge_processing_fee": false,  // SEMPRE false - plataforma paga
     *     "charge_remainder_fee": false
     *   }
     * }
     * 
     * IMPORTANTE:
     * - A plataforma (8%) NÃO vai no array de splits
     * - O Pagar.me calcula automaticamente como "remainder"
     * - Apenas motoboy (87%) e manager (5%) vão no array
     * 
     * @param delivery Entrega com informações de motoboy e manager
     * @return Lista de splits para o Pagar.me (2 itens: courier + manager)
     */
    public List<PagarMeSplitRequest> calculatePagarmeSplit(Delivery delivery) {
        List<PagarMeSplitRequest> splits = new ArrayList<>();

        // Validação básica
        if (delivery == null) {
            throw new IllegalArgumentException("Delivery cannot be null");
        }

        // Validação: soma dos splits não pode ultrapassar 100%
        int totalPercentage = courierPercentage + managerPercentage;
        if (totalPercentage > 10000) {
            throw new IllegalStateException(
                String.format("Split percentages exceed 100%%. Courier: %d%%, Manager: %d%%, Total: %d%%",
                    courierPercentage / 100, managerPercentage / 100, totalPercentage / 100)
            );
        }

        // 1. Split do MOTOBOY (87%)
        if (delivery.getCourier() != null) {
            String courierRecipientId = delivery.getCourier().getPagarmeRecipientId();
            
            if (courierRecipientId == null || courierRecipientId.isEmpty()) {
                throw new IllegalStateException(
                    "Courier does not have a recipient ID. Bank account must be registered first."
                );
            }

            PagarMeSplitRequest courierSplit = PagarMeSplitRequest.builder()
                .amount(courierPercentage) // 8700 = 87%
                .recipientId(courierRecipientId)
                .type("percentage")
                .options(PagarMeSplitRequest.SplitOptions.builder()
                    .liable(courierLiable) // false - plataforma é liable
                    .chargeProcessingFee(courierChargeFee) // false - plataforma paga
                    .chargeRemainderFee(false)
                    .build())
                .build();

            splits.add(courierSplit);
        } else {
            throw new IllegalStateException("Courier is missing");
        }

        // 2. Split do MANAGER (5%)
        if (delivery.getOrganizer() != null) {
            String managerRecipientId = delivery.getOrganizer().getPagarmeRecipientId();
            
            if (managerRecipientId == null || managerRecipientId.isEmpty()) {
                throw new IllegalStateException(
                    "Manager does not have a recipient ID. Bank account must be registered first."
                );
            }

            PagarMeSplitRequest managerSplit = PagarMeSplitRequest.builder()
                .amount(managerPercentage) // 500 = 5%
                .recipientId(managerRecipientId)
                .type("percentage")
                .options(PagarMeSplitRequest.SplitOptions.builder()
                    .liable(false) // false - plataforma é liable
                    .chargeProcessingFee(managerChargeFee) // false - plataforma paga
                    .chargeRemainderFee(false)
                    .build())
                .build();

            splits.add(managerSplit);
        } else {
            throw new IllegalStateException("Manager (organizer) is missing");
        }

        // 3. PLATAFORMA (8%) - NÃO vai no array
        // O Pagar.me calcula automaticamente como remainder
        // 100% - 87% - 5% = 8% (automático para a conta principal)

        return splits;
    }

    /**
     * Valida se uma entrega está pronta para split de pagamento
     * 
     * @param delivery Entrega a validar
     * @return true se a entrega tem todos os recipient IDs necessários
     */
    public boolean isReadyForSplit(Delivery delivery) {
        if (delivery == null) {
            return false;
        }

        // Valida courier
        if (delivery.getCourier() == null || 
            delivery.getCourier().getPagarmeRecipientId() == null) {
            return false;
        }

        // Valida manager (organizer)
        if (delivery.getOrganizer() == null || 
            delivery.getOrganizer().getPagarmeRecipientId() == null) {
            return false;
        }

        return true;
    }

    /**
     * Calcula o valor que cada parte receberá (para exibição/log)
     * 
     * @param totalAmount Valor total em centavos (ex: 10000 = R$ 100,00)
     * @param delivery Entrega com informações de split
     * @return Mapa com valores calculados
     */
    public SplitBreakdown calculateSplitBreakdown(Long totalAmount, Delivery delivery) {
        if (totalAmount == null || totalAmount <= 0) {
            throw new IllegalArgumentException("Total amount must be positive");
        }

        // Calcula valores em centavos
        long courierAmount = (totalAmount * courierPercentage) / 10000;
        long managerAmount = (totalAmount * managerPercentage) / 10000;
        long platformAmount = totalAmount - courierAmount - managerAmount; // Resto = 8%

        return SplitBreakdown.builder()
            .totalAmount(totalAmount)
            .courierAmount(courierAmount)
            .managerAmount(managerAmount)
            .platformAmount(platformAmount)
            .courierPercentage(new BigDecimal(courierPercentage).divide(new BigDecimal(100)))
            .managerPercentage(new BigDecimal(managerPercentage).divide(new BigDecimal(100)))
            .platformPercentage(new BigDecimal(10000 - courierPercentage - managerPercentage).divide(new BigDecimal(100)))
            .build();
    }

    /**
     * Classe para detalhamento do split (para logs/debug)
     */
    @lombok.Builder
    @lombok.Data
    public static class SplitBreakdown {
        private Long totalAmount;
        private Long courierAmount;
        private Long managerAmount;
        private Long platformAmount;
        private BigDecimal courierPercentage;
        private BigDecimal managerPercentage;
        private BigDecimal platformPercentage;
    }
}
