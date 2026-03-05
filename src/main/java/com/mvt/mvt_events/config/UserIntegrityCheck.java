package com.mvt.mvt_events.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Verifica integridade dos dados de usuários no startup.
 * Desativa usuários que estão enabled mas não possuem os requisitos obrigatórios do seu role.
 */
@Component
public class UserIntegrityCheck {

    private static final Logger log = LoggerFactory.getLogger(UserIntegrityCheck.class);

    private final JdbcTemplate jdbcTemplate;

    public UserIntegrityCheck(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void checkUsersIntegrity() {
        int total = 0;

        // === ORGANIZER: conta bancária + dados de saque ===
        int orgNoBankAccount = jdbcTemplate.update(
            "UPDATE users SET enabled = false " +
            "WHERE role = 'ORGANIZER' AND enabled = true " +
            "AND id NOT IN (SELECT user_id FROM bank_accounts)"
        );
        int orgNoTransfer = jdbcTemplate.update(
            "UPDATE users SET enabled = false " +
            "WHERE role = 'ORGANIZER' AND enabled = true " +
            "AND (pagarme_recipient_id IS NULL OR pagarme_status != 'active')"
        );
        total += orgNoBankAccount + orgNoTransfer;
        if (orgNoBankAccount + orgNoTransfer > 0) {
            log.warn("[INTEGRITY] ORGANIZER: {} desativado(s) — {} sem conta bancária, {} sem dados de saque",
                    orgNoBankAccount + orgNoTransfer, orgNoBankAccount, orgNoTransfer);
        }

        // === COURIER: conta bancária + dados de saque + veículo + tipo de serviço ===
        int courNoBankAccount = jdbcTemplate.update(
            "UPDATE users SET enabled = false " +
            "WHERE role = 'COURIER' AND enabled = true " +
            "AND id NOT IN (SELECT user_id FROM bank_accounts)"
        );
        int courNoTransfer = jdbcTemplate.update(
            "UPDATE users SET enabled = false " +
            "WHERE role = 'COURIER' AND enabled = true " +
            "AND (pagarme_recipient_id IS NULL OR pagarme_status != 'active')"
        );
        int courNoVehicle = jdbcTemplate.update(
            "UPDATE users SET enabled = false " +
            "WHERE role = 'COURIER' AND enabled = true " +
            "AND id NOT IN (SELECT owner_id FROM vehicles)"
        );
        int courNoServiceType = jdbcTemplate.update(
            "UPDATE users SET enabled = false " +
            "WHERE role = 'COURIER' AND enabled = true " +
            "AND service_type IS NULL"
        );
        int courTotal = courNoBankAccount + courNoTransfer + courNoVehicle + courNoServiceType;
        total += courTotal;
        if (courTotal > 0) {
            log.warn("[INTEGRITY] COURIER: {} desativado(s) — {} sem conta bancária, {} sem dados de saque, {} sem veículo, {} sem tipo de serviço",
                    courTotal, courNoBankAccount, courNoTransfer, courNoVehicle, courNoServiceType);
        }

        // === CUSTOMER / CLIENT: cartão ativo OU preferência PIX ===
        int custNoPayment = jdbcTemplate.update(
            "UPDATE users SET enabled = false " +
            "WHERE role IN ('CUSTOMER', 'CLIENT') AND enabled = true " +
            "AND id NOT IN (SELECT customer_id FROM customer_cards WHERE is_active = true) " +
            "AND id NOT IN (SELECT user_id FROM customer_payment_preferences WHERE preferred_payment_type = 'PIX')"
        );
        total += custNoPayment;
        if (custNoPayment > 0) {
            log.warn("[INTEGRITY] CUSTOMER/CLIENT: {} desativado(s) — sem cartão ativo e sem preferência PIX", custNoPayment);
        }

        if (total == 0) {
            log.info("[INTEGRITY] Todos os usuários ativos possuem os requisitos obrigatórios");
        }
    }
}
