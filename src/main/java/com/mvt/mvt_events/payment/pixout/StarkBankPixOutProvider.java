package com.mvt.mvt_events.payment.pixout;

import com.mvt.mvt_events.jpa.User;
import com.starkbank.DictKey;
import com.starkbank.Project;
import com.starkbank.Transfer;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Implementação de {@link PixOutProvider} via Stark Bank SDK.
 *
 * Fluxo:
 *  1. {@link DictKey#get} resolve a chave PIX em dados da conta (ispb, agência, conta, nome, taxId)
 *  2. {@link Transfer#create} cria a transferência com os dados resolvidos
 *
 * Ativa via `pix.out.provider=stark`. Requer:
 *  - pix.out.stark.environment (sandbox|production)
 *  - pix.out.stark.project-id
 *  - pix.out.stark.private-key (PEM, \n escapado se vem de env)
 */
@Component
@Slf4j
@ConditionalOnProperty(name = "pix.out.provider", havingValue = "stark")
public class StarkBankPixOutProvider implements PixOutProvider {

    @Value("${pix.out.stark.environment:sandbox}")
    private String environment;

    @Value("${pix.out.stark.project-id:}")
    private String projectId;

    @Value("${pix.out.stark.private-key:}")
    private String privateKey;

    private Project project;

    @PostConstruct
    public void init() {
        if (projectId == null || projectId.isBlank() || privateKey == null || privateKey.isBlank()) {
            log.warn("⚠️ Stark Bank provider ativo mas credenciais incompletas — todas as chamadas vão falhar");
            return;
        }
        String pem = privateKey.replace("\\n", "\n");
        try {
            this.project = new Project(environment, projectId, pem);
            log.info("✅ StarkBankPixOutProvider inicializado (env={}, projectId={})", environment, projectId);
        } catch (Exception e) {
            log.error("❌ Falha ao inicializar Stark Bank Project: {}", e.getMessage(), e);
            this.project = null;
        }
    }

    @Override
    public PixOutResult send(User to, long amountCents, String externalId) {
        if (project == null) {
            return PixOutResult.failed("Stark Bank não configurado (credenciais ausentes)");
        }
        if (to == null || to.getPixKey() == null || to.getPixKey().isBlank()) {
            return PixOutResult.failed("Destinatário sem chave PIX");
        }
        if (amountCents <= 0) {
            return PixOutResult.failed("Valor inválido");
        }

        String formattedKey = formatPixKey(to.getPixKey(), to.getPixKeyType());

        // 1. Resolver chave PIX → dados da conta do recebedor
        DictKey dict;
        try {
            dict = DictKey.get(formattedKey, project);
        } catch (Exception e) {
            log.error("❌ Falha ao resolver chave PIX '{}' no DICT: {}", mask(formattedKey), e.getMessage());
            return PixOutResult.failed("Chave PIX não encontrada no DICT: " + e.getMessage());
        }
        if (dict == null) {
            return PixOutResult.failed("Chave PIX inválida (DICT retornou null)");
        }
        if (dict.status != null && !"registered".equalsIgnoreCase(dict.status)) {
            return PixOutResult.failed("Chave PIX com status inválido: " + dict.status);
        }

        log.info("🔍 Chave resolvida: name={} bank={} branch={} account={}",
                dict.name, dict.ispb, dict.branchCode, dict.accountNumber);

        // 2. Criar Transfer usando ispb (bankCode)
        try {
            Map<String, Object> data = new HashMap<>();
            data.put("amount", amountCents);
            data.put("name", dict.name);
            data.put("taxId", dict.taxId);
            data.put("bankCode", dict.ispb);
            data.put("branchCode", dict.branchCode);
            data.put("accountNumber", dict.accountNumber);
            data.put("accountType", dict.accountType);
            data.put("externalId", externalId);
            data.put("description", "Zapi-Food courier payout");

            Transfer t = new Transfer(data);
            List<Transfer> created = Transfer.create(
                    new ArrayList<Object>(Arrays.asList(t)), project);

            if (created.isEmpty()) {
                return PixOutResult.failed("Stark Bank retornou lista vazia");
            }
            Transfer created0 = created.get(0);
            log.info("✅ Transfer Stark criado: id={} status={} externalId={}",
                    created0.id, created0.status, externalId);

            // Status inicial: "created" → "processing" → "success" (assíncrono)
            if ("success".equalsIgnoreCase(created0.status)) {
                return PixOutResult.succeeded(created0.id);
            } else if ("failed".equalsIgnoreCase(created0.status)) {
                return PixOutResult.failed("Stark retornou failed");
            }
            return PixOutResult.pending(created0.id);

        } catch (Exception e) {
            log.error("❌ Erro no Stark Transfer.create (externalId={}): {}", externalId, e.getMessage(), e);
            return PixOutResult.failed("Stark erro: " + e.getMessage());
        }
    }

    /**
     * Formata a chave PIX pro padrão que o DICT aceita.
     *  - CPF/CNPJ: apenas dígitos
     *  - EMAIL: lowercase, trim
     *  - PHONE: +55DDDNNNNNNNN (E.164)
     *  - EVP: UUID como veio
     */
    static String formatPixKey(String key, User.PixKeyType type) {
        if (key == null) return null;
        String trimmed = key.trim();
        if (type == null) return trimmed;
        return switch (type) {
            case CPF, CNPJ -> trimmed.replaceAll("[^0-9]", "");
            case EMAIL -> trimmed.toLowerCase();
            case PHONE -> formatPhone(trimmed);
            case EVP -> trimmed;
        };
    }

    private static String formatPhone(String raw) {
        String digits = raw.replaceAll("[^0-9]", "");
        if (digits.startsWith("55") && digits.length() >= 12) return "+" + digits;
        if (digits.length() == 11 || digits.length() == 10) return "+55" + digits;
        return raw.startsWith("+") ? raw : "+" + digits;
    }

    private String mask(String key) {
        if (key == null || key.length() <= 4) return key;
        return key.substring(0, 3) + "***" + key.substring(key.length() - 2);
    }
}
