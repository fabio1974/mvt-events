package com.mvt.mvt_events.jpa;

/**
 * Método de pagamento de uma entrega.
 */
public enum PaymentMethod {
    CREDIT_CARD, // Cartão de crédito
    DEBIT_CARD, // Cartão de débito
    PIX, // PIX
    BANK_SLIP, // Boleto bancário
    CASH, // Dinheiro
    WALLET // Carteira digital (MercadoPago, PicPay, etc)
}
