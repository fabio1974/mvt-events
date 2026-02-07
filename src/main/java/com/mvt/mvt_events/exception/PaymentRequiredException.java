package com.mvt.mvt_events.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.math.BigDecimal;

/**
 * Exception lançada quando um pagamento é necessário antes de prosseguir com uma operação.
 * Retorna HTTP 402 Payment Required.
 * 
 * Usado em dois cenários:
 * 1. DELIVERY: Quando motoboy tenta aceitar entrega sem pagamento
 * 2. RIDE: Quando motoboy tenta iniciar viagem sem pagamento
 */
@Getter
@ResponseStatus(HttpStatus.PAYMENT_REQUIRED) // HTTP 402
public class PaymentRequiredException extends RuntimeException {
    
    private final BigDecimal amount;
    private final Long deliveryId;
    
    public PaymentRequiredException(String message, BigDecimal amount, Long deliveryId) {
        super(message);
        this.amount = amount;
        this.deliveryId = deliveryId;
    }
    
    public PaymentRequiredException(String message, BigDecimal amount) {
        super(message);
        this.amount = amount;
        this.deliveryId = null;
    }
    
    public PaymentRequiredException(String message) {
        super(message);
        this.amount = null;
        this.deliveryId = null;
    }
}
