package com.mvt.mvt_events.payment.pixout;

import com.mvt.mvt_events.jpa.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Implementação stub — apenas loga e retorna SUCCEEDED fake.
 *
 * ⚠️ NÃO USE EM PRODUÇÃO. Serve pra destravar o fluxo end-to-end enquanto
 * Stark Bank / Inter não estiverem ligados.
 *
 * Ativa por padrão (quando `pix.out.provider` não está setado ou = "log").
 */
@Component
@Slf4j
@ConditionalOnProperty(name = "pix.out.provider", havingValue = "log", matchIfMissing = true)
public class LogOnlyPixOutProvider implements PixOutProvider {

    @Override
    public PixOutResult send(User to, long amountCents, String externalId) {
        if (to == null || to.getPixKey() == null || to.getPixKey().isBlank()) {
            log.warn("💸 [PIX-OUT LOG] recusado — destinatário sem pixKey (externalId={})", externalId);
            return PixOutResult.failed("Destinatário sem chave PIX cadastrada");
        }
        if (amountCents <= 0) {
            log.warn("💸 [PIX-OUT LOG] recusado — amount inválido ({})", amountCents);
            return PixOutResult.failed("Valor inválido");
        }
        log.info("💸 [PIX-OUT LOG-STUB] ▶ to={} pixKey={} ({}) amount={}¢ externalId={}",
                to.getId(), mask(to.getPixKey()), to.getPixKeyType(), amountCents, externalId);
        log.info("💸 [PIX-OUT LOG-STUB] ✅ simulado como SUCCEEDED (substitua provider em prod)");
        return PixOutResult.succeeded("log-" + externalId);
    }

    private String mask(String key) {
        if (key == null || key.length() <= 4) return key;
        return key.substring(0, 3) + "***" + key.substring(key.length() - 2);
    }
}
