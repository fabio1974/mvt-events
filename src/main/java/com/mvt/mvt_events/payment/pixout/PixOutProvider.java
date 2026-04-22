package com.mvt.mvt_events.payment.pixout;

import com.mvt.mvt_events.jpa.User;

/**
 * Abstração de provedor de PIX out (saída de dinheiro para chave PIX de terceiro).
 *
 * Implementações previstas:
 *  - {@link LogOnlyPixOutProvider}  — fase 1, só loga (stub)
 *  - StarkBankPixOutProvider         — para produção (futuro)
 *  - InterPixOutProvider             — melhor custo (futuro)
 *
 * Configurar via property `pix.out.provider` (log|stark|inter).
 */
public interface PixOutProvider {

    /**
     * Envia PIX para a chave de um usuário.
     *
     * @param to Destinatário (deve ter pixKey + pixKeyType setados)
     * @param amountCents Valor em centavos (>0)
     * @param externalId ID interno para correlação (ex: "transfer-42")
     * @return Resultado síncrono. O status final pode vir assíncrono via webhook.
     */
    PixOutResult send(User to, long amountCents, String externalId);

    enum Status {
        PENDING,    // Aceito, aguarda confirmação assíncrona
        SUCCEEDED,  // Confirmado
        FAILED      // Rejeitado (ver errorMessage)
    }

    record PixOutResult(Status status, String providerTransactionId, String errorMessage) {
        public static PixOutResult pending(String id) { return new PixOutResult(Status.PENDING, id, null); }
        public static PixOutResult succeeded(String id) { return new PixOutResult(Status.SUCCEEDED, id, null); }
        public static PixOutResult failed(String message) { return new PixOutResult(Status.FAILED, null, message); }
    }
}
