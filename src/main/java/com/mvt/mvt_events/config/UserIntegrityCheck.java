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
        int totalDisabled = 0;
        int totalReenabled = 0;

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
        int orgReenabled = jdbcTemplate.update(
            "UPDATE users SET enabled = true " +
            "WHERE role = 'ORGANIZER' AND enabled = false AND blocked = false AND deleted_at IS NULL " +
            "AND id IN (SELECT user_id FROM bank_accounts) " +
            "AND pagarme_recipient_id IS NOT NULL AND pagarme_status = 'active'"
        );
        totalDisabled += orgNoBankAccount + orgNoTransfer;
        totalReenabled += orgReenabled;
        if (orgNoBankAccount + orgNoTransfer > 0) {
            log.warn("[INTEGRITY] ORGANIZER: {} desativado(s) — {} sem conta bancária, {} sem dados de saque",
                    orgNoBankAccount + orgNoTransfer, orgNoBankAccount, orgNoTransfer);
        }
        if (orgReenabled > 0) {
            log.warn("[INTEGRITY] ORGANIZER: {} reativado(s) — passaram a cumprir todos os requisitos", orgReenabled);
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
        int courReenabled = jdbcTemplate.update(
            "UPDATE users SET enabled = true " +
            "WHERE role = 'COURIER' AND enabled = false AND blocked = false AND deleted_at IS NULL " +
            "AND id IN (SELECT user_id FROM bank_accounts) " +
            "AND pagarme_recipient_id IS NOT NULL AND pagarme_status = 'active' " +
            "AND id IN (SELECT owner_id FROM vehicles) " +
            "AND service_type IS NOT NULL"
        );
        int courTotal = courNoBankAccount + courNoTransfer + courNoVehicle + courNoServiceType;
        totalDisabled += courTotal;
        totalReenabled += courReenabled;
        if (courTotal > 0) {
            log.warn("[INTEGRITY] COURIER: {} desativado(s) — {} sem conta bancária, {} sem dados de saque, {} sem veículo, {} sem tipo de serviço",
                    courTotal, courNoBankAccount, courNoTransfer, courNoVehicle, courNoServiceType);
        }
        if (courReenabled > 0) {
            log.warn("[INTEGRITY] COURIER: {} reativado(s) — passaram a cumprir todos os requisitos", courReenabled);
        }

        // === CUSTOMER / CLIENT: cartão ativo OU preferência PIX ===
        int custNoPayment = jdbcTemplate.update(
            "UPDATE users SET enabled = false " +
            "WHERE role IN ('CUSTOMER', 'CLIENT') AND enabled = true " +
            "AND id NOT IN (SELECT customer_id FROM customer_cards WHERE is_active = true) " +
            "AND id NOT IN (SELECT user_id FROM customer_payment_preferences WHERE preferred_payment_type = 'PIX')"
        );
        int custReenabled = jdbcTemplate.update(
            "UPDATE users SET enabled = true " +
            "WHERE role IN ('CUSTOMER', 'CLIENT') AND enabled = false AND blocked = false AND deleted_at IS NULL " +
            "AND (id IN (SELECT customer_id FROM customer_cards WHERE is_active = true) " +
            "  OR id IN (SELECT user_id FROM customer_payment_preferences WHERE preferred_payment_type = 'PIX'))"
        );
        totalDisabled += custNoPayment;
        totalReenabled += custReenabled;
        if (custNoPayment > 0) {
            log.warn("[INTEGRITY] CUSTOMER/CLIENT: {} desativado(s) — sem cartão ativo e sem preferência PIX", custNoPayment);
        }
        if (custReenabled > 0) {
            log.warn("[INTEGRITY] CUSTOMER/CLIENT: {} reativado(s) — passaram a ter meio de pagamento", custReenabled);
        }

        if (totalDisabled == 0 && totalReenabled == 0) {
            log.info("[INTEGRITY] Todos os usuários estão consistentes com seus requisitos");
        }

        // === DELIVERY INTEGRITY: PENDING jamais pode ter courier atrelado ===
        int pendingWithCourier = jdbcTemplate.update(
            "UPDATE deliveries SET courier_id = NULL " +
            "WHERE status = 'PENDING' AND courier_id IS NOT NULL"
        );
        if (pendingWithCourier > 0) {
            log.warn("[INTEGRITY] DELIVERY: {} delivery(ies) PENDING tinham courier_id — limpeza realizada", pendingWithCourier);
        } else {
            log.info("[INTEGRITY] DELIVERY: nenhuma delivery PENDING com courier_id órfão encontrada");
        }

        // === COURIER INTEGRITY: currentDeliveryId só pode apontar para delivery ativa ===
        int staleCurrentDelivery = jdbcTemplate.update(
            "UPDATE users SET current_delivery_id = NULL " +
            "WHERE current_delivery_id IN (" +
            "  SELECT id FROM deliveries WHERE status IN ('COMPLETED','CANCELLED','PENDING')" +
            ")"
        );
        if (staleCurrentDelivery > 0) {
            log.warn("[INTEGRITY] COURIER: {} courier(s) com currentDeliveryId apontando para delivery finalizada — limpeza realizada", staleCurrentDelivery);
        } else {
            log.info("[INTEGRITY] COURIER: nenhum currentDeliveryId órfão encontrado");
        }
    }
}
