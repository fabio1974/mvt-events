package com.mvt.mvt_events.payment.service;

import com.mvt.mvt_events.jpa.BankAccount;
import com.mvt.mvt_events.jpa.BankAccount.BankAccountStatus;
import com.mvt.mvt_events.jpa.User;
import com.mvt.mvt_events.payment.dto.BankAccountRequest;
import com.mvt.mvt_events.payment.dto.SubAccountResponse;
import com.mvt.mvt_events.payment.dto.VerificationStatusResponse;
import com.mvt.mvt_events.repository.BankAccountRepository;
import com.mvt.mvt_events.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

/**
 * Service para gerenciar dados bancários de usuários
 * 
 * <p>Responsabilidades:
 * <ul>
 *   <li>Cadastrar dados bancários + criar subconta Iugu</li>
 *   <li>Atualizar dados bancários + atualizar Iugu</li>
 *   <li>Consultar dados bancários</li>
 *   <li>Verificar status de verificação em tempo real</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BankAccountService {

    private final BankAccountRepository bankAccountRepository;
    private final UserRepository userRepository;
    private final IuguService iuguService;

    /**
     * Cadastra dados bancários e cria subconta no Iugu
     * 
     * @param userId ID do usuário (COURIER ou ORGANIZER)
     * @param request Dados bancários
     * @return BankAccount salvo com iuguAccountId
     * @throws IllegalArgumentException Se usuário não encontrado ou dados inválidos
     * @throws IllegalStateException Se usuário já possui conta bancária
     */
    @Transactional
    public BankAccount createBankAccount(UUID userId, BankAccountRequest request) {
        log.info("🏦 Cadastrando dados bancários para usuário: {}", userId);
        
        // 1. Validar request
        request.validate();
        
        // 2. Buscar usuário
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado: " + userId));
        
        // 3. Verificar se já existe conta bancária
        if (bankAccountRepository.existsByUserId(userId)) {
            throw new IllegalStateException("Usuário já possui conta bancária cadastrada. Use PUT para atualizar.");
        }
        
        // 4. Criar BankAccount local
        BankAccount bankAccount = new BankAccount();
        bankAccount.setUser(user);
        bankAccount.setBankCode(request.bankCode());
        bankAccount.setBankName(request.bankName());
        bankAccount.setAgency(request.agency());
        bankAccount.setAccountNumber(request.accountNumber());
        bankAccount.setAccountType(request.accountType());
        bankAccount.setStatus(BankAccountStatus.PENDING_VALIDATION);
        
        // 5. Salvar primeiro (para gerar ID)
        bankAccount = bankAccountRepository.save(bankAccount);
        log.debug("   ├─ ✅ BankAccount local salvo com ID: {}", bankAccount.getId());
        
        // 6. Criar subconta no Iugu
        try {
            SubAccountResponse iuguResponse = iuguService.createSubAccount(user, bankAccount);
            
            // 7. Atualizar user com iuguAccountId
            user.setIuguAccountId(iuguResponse.accountId());
            userRepository.save(user);
            
            log.info("   ├─ ✅ Subconta Iugu criada: {}", iuguResponse.accountId());
            log.info("   └─ ⏳ Status: {} (verificação em 2-5 dias)", iuguResponse.verificationStatus());
            
        } catch (Exception e) {
            log.error("   └─ ❌ Erro ao criar subconta Iugu: {}", e.getMessage());
            // Não falha o cadastro local, mas registra o erro
            bankAccount.setNotes("Erro ao criar subconta Iugu: " + e.getMessage());
            bankAccount = bankAccountRepository.save(bankAccount);
        }
        
        return bankAccount;
    }

    /**
     * Atualiza dados bancários e sincroniza com Iugu
     * 
     * @param userId ID do usuário
     * @param request Novos dados bancários
     * @return BankAccount atualizado
     * @throws IllegalArgumentException Se usuário não encontrado
     * @throws IllegalStateException Se não existe conta bancária para atualizar
     */
    @Transactional
    public BankAccount updateBankAccount(UUID userId, BankAccountRequest request) {
        log.info("🔄 Atualizando dados bancários para usuário: {}", userId);
        
        // 1. Validar request
        request.validate();
        
        // 2. Buscar BankAccount existente
        BankAccount bankAccount = bankAccountRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalStateException(
                    "Não existe conta bancária cadastrada. Use POST para criar."
                ));
        
        User user = bankAccount.getUser();
        
        // 3. Atualizar dados locais
        bankAccount.setBankCode(request.bankCode());
        bankAccount.setBankName(request.bankName());
        bankAccount.setAgency(request.agency());
        bankAccount.setAccountNumber(request.accountNumber());
        bankAccount.setAccountType(request.accountType());
        
        // Se estava bloqueada, volta para pendente após atualização
        if (bankAccount.getStatus() == BankAccountStatus.BLOCKED) {
            bankAccount.setStatus(BankAccountStatus.PENDING_VALIDATION);
            log.info("   ├─ Status alterado de BLOCKED → PENDING_VALIDATION");
        }
        
        bankAccount = bankAccountRepository.save(bankAccount);
        log.debug("   ├─ ✅ BankAccount local atualizado");
        
        // 4. Atualizar no Iugu (se já existe subconta)
        if (user.getIuguAccountId() != null) {
            try {
                iuguService.updateBankAccount(user.getIuguAccountId(), bankAccount);
                log.info("   └─ ✅ Dados atualizados no Iugu: {}", user.getIuguAccountId());
            } catch (Exception e) {
                log.error("   └─ ⚠️ Erro ao atualizar Iugu: {}", e.getMessage());
                bankAccount.setNotes("Erro ao atualizar Iugu: " + e.getMessage());
                bankAccount = bankAccountRepository.save(bankAccount);
            }
        } else {
            log.warn("   └─ ⚠️ Sem iuguAccountId, apenas atualização local");
        }
        
        return bankAccount;
    }

    /**
     * Busca dados bancários do usuário
     * 
     * @param userId ID do usuário
     * @return BankAccount se existir
     */
    @Transactional(readOnly = true)
    public Optional<BankAccount> getBankAccount(UUID userId) {
        return bankAccountRepository.findByUserId(userId);
    }

    /**
     * Verifica status de verificação em tempo real consultando API Iugu
     * 
     * @param userId ID do usuário
     * @return Status de verificação atualizado
     */
    @Transactional
    public VerificationStatusResponse checkVerificationStatus(UUID userId) {
        log.info("🔍 Verificando status de verificação para usuário: {}", userId);
        
        // 1. Buscar usuário
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado: " + userId));
        
        // 2. Buscar BankAccount
        Optional<BankAccount> bankAccountOpt = bankAccountRepository.findByUserId(userId);
        
        if (bankAccountOpt.isEmpty()) {
            log.warn("   └─ ❌ Usuário não possui dados bancários cadastrados");
            return VerificationStatusResponse.notRegistered();
        }
        
        BankAccount bankAccount = bankAccountOpt.get();
        
        // 3. Verificar se tem iuguAccountId
        if (user.getIuguAccountId() == null) {
            log.warn("   └─ ⚠️ Sem iuguAccountId, subconta ainda não criada");
            return VerificationStatusResponse.notLinkedToIugu();
        }
        
        // 4. Consultar status no Iugu
        try {
            SubAccountResponse iuguResponse = iuguService.getSubAccountStatus(user.getIuguAccountId());
            
            log.info("   ├─ Status Iugu: {}", iuguResponse.verificationStatus());
            log.info("   └─ Status local: {}", bankAccount.getStatus());
            
            // 5. Sincronizar status se mudou
            String iuguStatus = iuguResponse.verificationStatus();
            if ("verified".equalsIgnoreCase(iuguStatus) && 
                bankAccount.getStatus() != BankAccountStatus.ACTIVE) {
                
                bankAccount.markAsActive();
                bankAccountRepository.save(bankAccount);
                log.info("   └─ ✅ Status atualizado para ACTIVE");
                
            } else if ("rejected".equalsIgnoreCase(iuguStatus) && 
                       bankAccount.getStatus() != BankAccountStatus.BLOCKED) {
                
                bankAccount.setStatus(BankAccountStatus.BLOCKED);
                bankAccountRepository.save(bankAccount);
                log.warn("   └─ ❌ Status atualizado para BLOCKED");
            }
            
            return VerificationStatusResponse.of(
                user.getIuguAccountId(),
                bankAccount.getStatus(),
                iuguResponse.verificationStatus()
            );
            
        } catch (Exception e) {
            log.error("   └─ ❌ Erro ao consultar Iugu: {}", e.getMessage());
            
            // Retorna status local se não conseguiu consultar Iugu
            return VerificationStatusResponse.of(
                user.getIuguAccountId(),
                bankAccount.getStatus(),
                "unknown"
            );
        }
    }
}
