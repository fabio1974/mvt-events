package com.mvt.mvt_events.payment.exception;

import lombok.Getter;

/**
 * Exceção lançada quando o processamento de pagamento falha no gateway.
 * Contém os payloads de request e response para auditoria.
 */
@Getter
public class PaymentProcessingException extends RuntimeException {
    
    private final String requestPayload;
    private final String responsePayload;
    private final String errorCode;
    
    public PaymentProcessingException(String message, String requestPayload, String responsePayload, String errorCode) {
        super(message);
        this.requestPayload = requestPayload;
        this.responsePayload = responsePayload;
        this.errorCode = errorCode;
    }
    
    public PaymentProcessingException(String message, String requestPayload, String responsePayload, String errorCode, Throwable cause) {
        super(message, cause);
        this.requestPayload = requestPayload;
        this.responsePayload = responsePayload;
        this.errorCode = errorCode;
    }
}
