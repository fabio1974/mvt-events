package com.mvt.mvt_events.jpa;

/**
 * Status do pagamento de uma entrega.
 */
public enum PaymentStatus {
    PENDING, // Aguardando pagamento
    PROCESSING, // Processando pagamento
    PAID, // Pago
    FAILED, // Pagamento falhou
    REFUNDED, // Pagamento reembolsado
    CANCELLED, // Pagamento cancelado
    EXPIRED // Pagamento expirado (prazo de validade vencido)
}
