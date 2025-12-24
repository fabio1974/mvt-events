package com.mvt.mvt_events.dto;

import com.mvt.mvt_events.jpa.Payment;
import com.mvt.mvt_events.jpa.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO para retornar dados de pagamentos PIX via Pagar.me
 * 
 * Este DTO combina dados do Payment local com dados do pedido Pagar.me,
 * fornecendo todas as informações necessárias para o cliente pagar.
 * 
 * Campos principais:
 * - paymentId: ID local do pagamento
 * - pagarmeOrderId: ID do pedido no Pagar.me
 * - pixQrCode: Código PIX copia-e-cola
 * - pixQrCodeUrl: URL da imagem do QR Code
 * - amount: Valor total
 * - status: Status do pagamento (PENDING, COMPLETED, etc)
 * - expiresAt: Data/hora de expiração
 * 
 * Exemplo de resposta JSON:
 * <pre>
 * {
 *   "paymentId": 123,
 *   "pagarmeOrderId": "or_abc123xyz",
 *   "pixQrCode": "00020126360014BR.GOV.BCB.PIX...",
 *   "pixQrCodeUrl": "https://api.pagar.me/qr/123.png",
 *   "amount": 50.00,
 *   "status": "PENDING",
 *   "expiresAt": "2025-12-03T23:59:59",
 *   "deliveryId": 456,
 *   "clientEmail": "cliente@example.com"
 * }
 * </pre>
 * 
 * @see PaymentRequest
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponse {

    /**
     * ID do pagamento local
     */
    private Long id;

    /**
     * ID do pedido no Pagar.me
     */
    private String pagarmeOrderId;

    /**
     * Código PIX copia-e-cola
     * Cliente pode copiar este texto e colar no app do banco
     */
    private String pixQrCode;

    /**
     * URL da imagem do QR Code PIX
     * Cliente pode escanear este QR Code com o app do banco
     */
    private String pixQrCodeUrl;

    /**
     * Valor total em centavos
     */
    private BigDecimal amount;

    /**
     * Status do pagamento
     */
    private PaymentStatus status;

    /**
     * Data/hora de expiração da fatura
     */
    private LocalDateTime expiresAt;

    /**
     * Data/hora de criação
     */
    private LocalDateTime createdAt;

    /**
     * Data/hora do pagamento (quando foi pago)
     */
    private LocalDateTime paymentDate;

    /**
     * ID da entrega
     */
    private Long deliveryId;

    /**
     * Email do cliente
     */
    private String clientEmail;

    /**
     * Indica se o pagamento expirou
     */
    private boolean expired;

    /**
     * Mensagem amigável sobre o status
     */
    private String statusMessage;

    /**
     * Request completo enviado ao Pagar.me (JSON)
     */
    private String request;

    /**
     * Response completo retornado pelo Pagar.me (JSON)
     */
    private String response;

    /**
     * Gateway response com códigos de erro (JSON)
     */
    private String gatewayResponse;

    /**
     * Cria um PaymentResponse a partir de um Payment
     * 
     * @param payment Payment local
     * @return PaymentResponse completo
     */
    public static PaymentResponse from(Payment payment) {
        if (payment == null) {
            throw new IllegalArgumentException("Payment não pode ser null");
        }

        PaymentResponseBuilder builder = PaymentResponse.builder()
                .id(payment.getId())
                .pagarmeOrderId(payment.getProviderPaymentId())
                .pixQrCode(payment.getPixQrCode())
                .pixQrCodeUrl(payment.getPixQrCodeUrl())
                .amount(payment.getAmount())
                .status(payment.getStatus())
                .expiresAt(payment.getExpiresAt())
                .createdAt(payment.getCreatedAt())
                .paymentDate(payment.getPaymentDate())
                .expired(payment.isExpired())
                .request(payment.getRequest())
                .response(payment.getResponse())
                .gatewayResponse(payment.getGatewayResponse());

        // Dados da entrega (se disponível) - pegando a primeira (temporário até refatorar para N:M completo)
        if (payment.getDeliveries() != null && !payment.getDeliveries().isEmpty()) {
            builder.deliveryId(payment.getDeliveries().iterator().next().getId());
        }

        // Email do cliente (pode estar no payer ou na metadata)
        if (payment.getPayer() != null && payment.getPayer().getUsername() != null) {
            builder.clientEmail(payment.getPayer().getUsername());
        }

        // Mensagem de status
        builder.statusMessage(getStatusMessage(payment));

        return builder.build();
    }

    /**
     * Gera mensagem amigável sobre o status do pagamento
     */
    private static String getStatusMessage(Payment payment) {
        if (payment.isExpired() && payment.isPending()) {
            return "⏱️ Pagamento expirado. Por favor, gere uma nova fatura.";
        }

        return switch (payment.getStatus()) {
            case PENDING -> "⏳ Aguardando pagamento. Escaneie o QR Code ou use o código PIX.";
            case COMPLETED -> "✅ Pagamento confirmado! Obrigado.";
            case FAILED -> "❌ Pagamento falhou. Tente novamente.";
            case CANCELLED -> "🚫 Pagamento cancelado.";
            case REFUNDED -> "↩️ Pagamento reembolsado.";
            default -> "ℹ️ Status: " + payment.getStatus();
        };
    }

    /**
     * Cria uma resposta de erro
     */
    public static PaymentResponse error(String message) {
        return PaymentResponse.builder()
                .statusMessage("❌ " + message)
                .build();
    }
}
