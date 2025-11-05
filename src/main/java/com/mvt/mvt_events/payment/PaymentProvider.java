package com.mvt.mvt_events.payment;

import java.math.BigDecimal;

/**
 * Interface para providers de pagamento (Stripe, MercadoPago, PayPal).
 * Será usada para processar pagamentos de deliveries no Zapi10.
 */
public interface PaymentProvider {

    /**
     * Processa um pagamento
     * 
     * @param amount             Valor do pagamento
     * @param currency           Moeda (BRL, USD, etc)
     * @param paymentMethodToken Token do método de pagamento
     * @param metadata           Metadados adicionais
     * @return ID da transação no provider
     */
    String processPayment(BigDecimal amount, String currency, String paymentMethodToken, Object metadata)
            throws Exception;

    /**
     * Calcula a taxa do provider sobre o valor
     * 
     * @param amount        Valor base
     * @param paymentMethod Método de pagamento usado
     * @return Valor da taxa
     */
    BigDecimal calculateFee(BigDecimal amount, String paymentMethod);

    /**
     * Verifica se o provider suporta determinado método de pagamento
     * 
     * @param paymentMethod Método a verificar
     * @return true se suportado
     */
    boolean supportsPaymentMethod(String paymentMethod);

    /**
     * Retorna o nome do provider
     * 
     * @return Nome (STRIPE, MERCADOPAGO, PAYPAL)
     */
    String getProviderName();
}
