package com.mvt.mvt_events.payment.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Request para criação de invoice no Iugu
 * 
 * <p>Estrutura para criar uma fatura com split de pagamento.</p>
 * 
 * @see <a href="https://dev.iugu.com/reference/criar-invoice">Documentação Criar Invoice</a>
 */
@Data
public class InvoiceRequest {

    /**
     * Email do cliente (recebe a fatura)
     */
    @JsonProperty("email")
    private String email;

    /**
     * Data de vencimento (ISO 8601: "2025-12-03T23:59:59")
     */
    @JsonProperty("due_date")
    private LocalDateTime dueDate;

    /**
     * Métodos de pagamento permitidos
     * Valores: "all", "bank_slip", "credit_card", "pix"
     */
    @JsonProperty("payable_with")
    private String payableWith;

    /**
     * Garantir que a data de vencimento seja em dia útil
     */
    @JsonProperty("ensure_workday_due_date")
    private Boolean ensureWorkdayDueDate;

    /**
     * Itens da fatura
     */
    @JsonProperty("items")
    private List<InvoiceItemRequest> items;

    /**
     * Regras de split de pagamento
     */
    @JsonProperty("splits")
    private List<SplitRequest> splits;
}
