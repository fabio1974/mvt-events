package com.mvt.mvt_events.payment.service;

import com.mvt.mvt_events.jpa.BankAccount;
import com.mvt.mvt_events.jpa.BankAccount.BankAccountStatus;
import com.mvt.mvt_events.jpa.User;
import com.mvt.mvt_events.payment.dto.SubAccountResponse;
import com.mvt.mvt_events.repository.BankAccountRepository;
import com.mvt.mvt_events.service.PushNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Serviço de sincronização de status de verificação do Iugu
 * 
 * <p>Executa job agendado para consultar o status de subcontas Iugu que estão
 * pendentes de verificação e atualiza o status local no banco de dados.</p>
 * 
 * <p><strong>Processo de verificação Iugu:</strong></p>
 * <ul>
 *   <li>Criação da subconta é instantânea (status: pending)</li>
 *   <li>Verificação bancária demora 2-5 dias úteis (assíncrona)</li>
 *   <li>Status final: verified (aprovado) ou rejected (rejeitado)</li>
 *   <li>Iugu NÃO envia webhook de verificação concluída</li>
 * </ul>
 * 
 * <p><strong>Configuração:</strong></p>
 * <pre>
 * iugu.verification-sync.enabled=true
 * iugu.verification-sync.cron=0 0 *\/6 * * *  # A cada 6 horas
 * iugu.verification-sync.max-pending-days=10
 * </pre>
 * 
 * @see IuguService#getSubAccountStatus(String)
 */
@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(
    prefix = "iugu.verification-sync",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = true // Habilitado por padrão
)
public class IuguVerificationSyncService {

    private final IuguService iuguService;
    private final BankAccountRepository bankAccountRepository;
    private final PushNotificationService pushNotificationService;

    /**
     * Job agendado que sincroniza status de verificação das subcontas Iugu
     * 
     * <p><strong>Execução:</strong> A cada 6 horas (00:00, 06:00, 12:00, 18:00)</p>
     * 
     * <p><strong>Processo:</strong></p>
     * <ol>
     *   <li>Busca todas as BankAccounts com status PENDING_VALIDATION</li>
     *   <li>Consulta status de cada subconta no Iugu via API</li>
     *   <li>Atualiza status local conforme resposta do Iugu</li>
     *   <li>Notifica usuário se status mudou (verificado ou rejeitado)</li>
     * </ol>
     */
    @Scheduled(cron = "${iugu.verification-sync.cron:0 0 */6 * * *}")
    @Transactional
    public void syncPendingVerifications() {
        log.info("🔄 ========================================");
        log.info("🔄 Iniciando sincronização de verificações Iugu...");
        log.info("🔄 ========================================");

        try {
            // 1. Busca todas as contas pendentes de verificação
            List<BankAccount> pendingAccounts = bankAccountRepository
                    .findByStatus(BankAccountStatus.PENDING_VALIDATION);

            if (pendingAccounts.isEmpty()) {
                log.info("✅ Nenhuma conta pendente de verificação");
                return;
            }

            log.info("📋 Encontradas {} conta(s) pendente(s) de verificação", pendingAccounts.size());

            int verified = 0;
            int rejected = 0;
            int stillPending = 0;
            int errors = 0;

            // 2. Processa cada conta
            for (BankAccount bankAccount : pendingAccounts) {
                try {
                    SyncResult result = syncAccountVerification(bankAccount);
                    
                    switch (result) {
                        case VERIFIED -> verified++;
                        case REJECTED -> rejected++;
                        case STILL_PENDING -> stillPending++;
                        case ERROR -> errors++;
                    }

                    // Rate limit: Aguarda 1 segundo entre requests para não sobrecarregar API
                    Thread.sleep(1000);

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.error("❌ Thread interrompida durante sync", e);
                    break;
                } catch (Exception e) {
                    errors++;
                    log.error("❌ Erro inesperado ao sincronizar conta {}: {}",
                            bankAccount.getId(), e.getMessage(), e);
                }
            }

            // 3. Log do resumo
            log.info("🔄 ========================================");
            log.info("✅ Sincronização concluída!");
            log.info("   ├─ ✅ Verificadas: {}", verified);
            log.info("   ├─ ❌ Rejeitadas: {}", rejected);
            log.info("   ├─ ⏳ Ainda pendentes: {}", stillPending);
            log.info("   └─ ⚠️ Erros: {}", errors);
            log.info("🔄 ========================================");

        } catch (Exception e) {
            log.error("❌ Erro fatal durante sincronização de verificações: {}", e.getMessage(), e);
        }
    }

    /**
     * Sincroniza o status de verificação de uma única conta bancária
     * 
     * @param bankAccount Conta bancária a sincronizar
     * @return Resultado da sincronização
     */
    private SyncResult syncAccountVerification(BankAccount bankAccount) {
        User user = bankAccount.getUser();
        String iuguAccountId = user.getIuguAccountId();

        // Validações básicas
        if (iuguAccountId == null || iuguAccountId.isBlank()) {
            log.warn("⚠️ User {} não tem iuguAccountId (BankAccount {})",
                    user.getId(), bankAccount.getId());
            return SyncResult.ERROR;
        }

        // Verifica se está travada há muito tempo (> 10 dias)
        checkIfStuck(bankAccount);

        try {
            // Consulta status no Iugu
            log.debug("🔍 Consultando status da subconta: {} (User: {})",
                    iuguAccountId, user.getUsername());

            SubAccountResponse iuguStatus = iuguService.getSubAccountStatus(iuguAccountId);

            // Processa conforme status retornado
            String status = iuguStatus.verificationStatus();
            
            if ("verified".equalsIgnoreCase(status)) {
                return handleVerified(bankAccount, user, iuguAccountId);
            } else if ("rejected".equalsIgnoreCase(status)) {
                return handleRejected(bankAccount, user, iuguAccountId);
            } else {
                return handleStillPending(bankAccount, user, iuguAccountId);
            }

        } catch (IuguService.IuguApiException e) {
            log.error("❌ Erro ao consultar Iugu para conta {}: {}",
                    iuguAccountId, e.getMessage());
            return SyncResult.ERROR;
        }
    }

    /**
     * Processa conta verificada
     */
    private SyncResult handleVerified(BankAccount bankAccount, User user, String iuguAccountId) {
        log.info("✅ Conta bancária VERIFICADA: {} (User: {})", iuguAccountId, user.getUsername());

        // Atualiza status local
        bankAccount.markAsActive();
        bankAccountRepository.save(bankAccount);

        // Notifica usuário via Push Notification
        try {
            pushNotificationService.notifyBankDataVerified(
                    user.getId(),
                    bankAccount.getBankName(),
                    bankAccount.getAccountNumberMasked()
            );
            log.info("   ├─ 📱 Push notification enviada com sucesso");
        } catch (Exception e) {
            log.error("   ├─ ⚠️ Erro ao enviar push notification: {}", e.getMessage());
        }

        log.info("   └─ ✅ Status atualizado para ACTIVE no banco local");

        return SyncResult.VERIFIED;
    }

    /**
     * Processa conta rejeitada
     */
    private SyncResult handleRejected(BankAccount bankAccount, User user, String iuguAccountId) {
        log.warn("❌ Conta bancária REJEITADA: {} (User: {})", iuguAccountId, user.getUsername());

        // Atualiza status local
        bankAccount.setStatus(BankAccountStatus.BLOCKED);
        bankAccountRepository.save(bankAccount);

        // Notifica usuário via Push Notification para corrigir dados
        try {
            pushNotificationService.notifyBankDataRejected(
                    user.getId(),
                    "Dados bancários incorretos ou conta inválida. Verifique CPF, agência e conta."
            );
            log.warn("   ├─ 📱 Push notification de rejeição enviada");
        } catch (Exception e) {
            log.error("   ├─ ⚠️ Erro ao enviar push notification: {}", e.getMessage());
        }

        log.warn("   └─ ❌ Status atualizado para BLOCKED no banco local");

        return SyncResult.REJECTED;
    }

    /**
     * Processa conta ainda pendente
     */
    private SyncResult handleStillPending(BankAccount bankAccount, User user, String iuguAccountId) {
        long daysPending = ChronoUnit.DAYS.between(bankAccount.getCreatedAt(), LocalDateTime.now());

        log.debug("⏳ Conta ainda PENDENTE: {} (User: {}, {} dias)",
                iuguAccountId, user.getUsername(), daysPending);

        return SyncResult.STILL_PENDING;
    }

    /**
     * Verifica se a conta está travada há muito tempo
     */
    private void checkIfStuck(BankAccount bankAccount) {
        long daysPending = ChronoUnit.DAYS.between(bankAccount.getCreatedAt(), LocalDateTime.now());
        int maxDays = 10; // TODO: Mover para configuração

        if (daysPending > maxDays) {
            log.warn("⚠️ ALERTA: Conta {} pendente há {} dias (max: {} dias) - User: {}",
                    bankAccount.getId(),
                    daysPending,
                    maxDays,
                    bankAccount.getUser().getUsername()
            );
            // TODO: Notificar admin ou criar ticket de suporte
        }
    }

    /**
     * Resultado da sincronização de uma conta
     */
    private enum SyncResult {
        VERIFIED,      // Conta foi verificada (pending → verified)
        REJECTED,      // Conta foi rejeitada (pending → rejected)
        STILL_PENDING, // Conta ainda está pendente
        ERROR          // Erro ao sincronizar
    }
}
