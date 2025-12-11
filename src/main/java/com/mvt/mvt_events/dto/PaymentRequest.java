package com.mvt.mvt_events.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * DTO para criação de pagamentos PIX via Pagar.me
 * 
 * Este DTO encapsula todos os dados necessários para criar uma fatura PIX
 * com split automático entre motoboy (87%), gestor (5%) e plataforma (8%).
 * 
 * IMPORTANTE: Agora suporta MÚLTIPLAS DELIVERIES em um único pagamento!
 * Isso permite que o cliente pague várias entregas com um único QR Code PIX,
 * economizando taxas (R$ 0,59 vs múltiplos R$ 0,59) e melhorando a UX.
 * 
 * Validações:
 * - deliveryIds: obrigatório, mínimo 1, máximo 10 deliveries
 * - amount: obrigatório, mínimo R$ 1,00
 * - clientEmail: obrigatório e válido (para enviar a fatura)
 * 
 * Exemplo de uso (múltiplas deliveries):
 * <pre>
 * PaymentRequest request = new PaymentRequest();
 * request.setDeliveryIds(List.of(1L, 2L, 3L)); // 3 entregas
 * request.setAmount(new BigDecimal("150.00")); // Soma das 3 entregas
 * request.setClientEmail("cliente@example.com");
 * </pre>
 * 
 * @see com.mvt.mvt_events.service.PaymentService#createPaymentWithSplit
 */
@Data
public class PaymentRequest {

    /**
     * IDs das entregas a serem pagas (mínimo 1, máximo 10)
     * Todas as deliveries devem:
     * - Estar com status COMPLETED
     * - Não ter payment PAID ainda
     * - Pertencer ao mesmo cliente (payer)
     */
    @NotEmpty(message = "Ao menos uma entrega deve ser informada")
    @Size(min = 1, max = 10, message = "Mínimo 1 e máximo 10 entregas por pagamento")
    private List<Long> deliveryIds;

    /**
     * Valor total do pagamento (em reais)
     * Mínimo: R$ 1,00
     */
    @NotNull(message = "Valor é obrigatório")
    @DecimalMin(value = "1.00", message = "Valor mínimo é R$ 1,00")
    private BigDecimal amount;

    /**
     * Email do cliente que vai pagar
     * Usado para enviar a fatura
     */
    @NotBlank(message = "Email do cliente é obrigatório")
    @Email(message = "Email inválido")
    private String clientEmail;

    /**
     * ID do recipient Pagar.me do motoboy
     * Deve ser o campo `pagarmeRecipientId` do User
     * Receberá 87% do valor
     */
    @NotBlank(message = "ID do recipient do motoboy é obrigatório")
    private String motoboyAccountId;

    /**
     * ID do recipient Pagar.me do gestor da organização
     * Deve ser o campo `pagarmeRecipientId` do User
     * Receberá 5% do valor
     * Opcional - se não informado, os 5% vão para a plataforma
     */
    private String managerAccountId;

    /**
     * Descrição opcional do pagamento
     * Aparecerá na fatura do cliente
     */
    private String description;

    /**
     * Tempo de expiração da fatura em horas
     * Padrão: 24 horas
     * Mínimo: 1 hora
     * Máximo: 720 horas (30 dias)
     */
    @Min(value = 1, message = "Tempo de expiração mínimo é 1 hora")
    @Max(value = 720, message = "Tempo de expiração máximo é 720 horas (30 dias)")
    private Integer expirationHours = 24;

    /**
     * Valida se os dados estão corretos antes de criar a fatura
     */
    public void validate() {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Valor deve ser maior que zero");
        }

        if (deliveryIds == null || deliveryIds.isEmpty()) {
            throw new IllegalArgumentException("Ao menos uma entrega deve ser informada");
        }

        if (deliveryIds.size() > 10) {
            throw new IllegalArgumentException("Máximo de 10 entregas por pagamento");
        }

        if (motoboyAccountId == null || motoboyAccountId.isBlank()) {
            throw new IllegalArgumentException("ID do recipient do motoboy é obrigatório");
        }
    }

    /**
     * Retorna a descrição padrão se não foi informada
     */
    public String getDescriptionOrDefault() {
        if (description != null && !description.isBlank()) {
            return description;
        }
        int count = deliveryIds != null ? deliveryIds.size() : 0;
        if (count == 1) {
            return "Pagamento de entrega";
        }
        return "Pagamento de " + count + " entregas";
    }
}
