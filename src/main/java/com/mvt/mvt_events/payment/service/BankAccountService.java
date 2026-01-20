package com.mvt.mvt_events.payment.service;

import com.mvt.mvt_events.jpa.Address;
import com.mvt.mvt_events.jpa.BankAccount;
import com.mvt.mvt_events.jpa.BankAccount.BankAccountStatus;
import com.mvt.mvt_events.jpa.City;
import com.mvt.mvt_events.jpa.User;
import com.mvt.mvt_events.payment.dto.BankAccountRequest;

import com.mvt.mvt_events.payment.dto.RecipientRequest;
import com.mvt.mvt_events.payment.dto.RecipientResponse;
import com.mvt.mvt_events.repository.AddressRepository;
import com.mvt.mvt_events.repository.BankAccountRepository;
import com.mvt.mvt_events.repository.CityRepository;
import com.mvt.mvt_events.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service para gerenciar dados bancários de usuários
 * 
 * <p>Responsabilidades:
 * <ul>
 *   <li>Cadastrar dados bancários + criar recipient Pagar.me</li>
 *   <li>Atualizar dados bancários + atualizar Pagar.me</li>
 *   <li>Consultar dados bancários</li>
 *   <li>Verificar status do recipient</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BankAccountService {

    private final BankAccountRepository bankAccountRepository;
    private final UserRepository userRepository;
    private final AddressRepository addressRepository;
    private final CityRepository cityRepository;
    private final PagarMeService pagarMeService;

    /**
     * Cadastra dados bancários e cria recipient no Pagar.me
     * 
     * @param userId ID do usuário (COURIER ou ORGANIZER)
     * @param request Dados bancários e informações cadastrais
     * @return BankAccount salvo com recipient ID
     * @throws IllegalArgumentException Se usuário não encontrado ou dados inválidos
     * @throws IllegalStateException Se usuário já possui conta bancária
     */
    @Transactional
    public BankAccount createBankAccount(UUID userId, BankAccountRequest request) {
        log.info("🏦 Cadastrando dados bancários para usuário: {}", userId);
        
        // 1. Buscar usuário
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado: " + userId));
        
        log.info("   ├─ Usuário: {} ({})", user.getName(), user.getUsername());
        
        // 2. Verificar se já existe conta bancária
        if (bankAccountRepository.existsByUserId(userId)) {
            throw new IllegalStateException("Usuário já possui conta bancária cadastrada. Use PUT para atualizar.");
        }
        
        // 3. Criar BankAccount local
        BankAccount bankAccount = new BankAccount();
        bankAccount.setUser(user);
        bankAccount.setBankCode(request.bankCode());
        bankAccount.setBankName(request.bankName());
        bankAccount.setAgency(request.agency());
        bankAccount.setAgencyDigit(request.agencyDigit());
        
        // AccountNumber e AccountDigit vêm separados do DTO e são armazenados separados
        bankAccount.setAccountNumber(request.accountNumber());
        bankAccount.setAccountDigit(request.accountDigit());
        
        bankAccount.setAccountType(request.accountType());
        
        // Copiar dados KYC opcionais
        bankAccount.setMotherName(request.motherName());
        bankAccount.setMonthlyIncome(request.monthlyIncome());
        bankAccount.setProfessionalOccupation(request.professionalOccupation());
        
        // Configuração de transferência automática (sempre true agora)
        bankAccount.setAutomaticTransfer(true);
        
        bankAccount.setStatus(BankAccountStatus.PENDING_VALIDATION);
        
        // 4. Salvar primeiro (para gerar ID)
        bankAccount = bankAccountRepository.save(bankAccount);
        log.debug("   ├─ ✅ BankAccount local salvo com ID: {}", bankAccount.getId());
        
        // 5. Verificar duplicidade no Pagar.me ANTES de criar recipient
        // IMPORTANTE: Verificamos CPF + dados bancários (banco, agência, conta)
        try {
            RecipientResponse duplicateRecipient = pagarMeService.findDuplicateRecipient(
                user.getDocumentClean(), // CPF do usuário
                bankAccount.getBankCode(),
                bankAccount.getAgency(),
                bankAccount.getAccountNumber()
            );
            
            if (duplicateRecipient != null) {
                log.warn("   ├─ ⚠️ Recipient duplicado encontrado: {}", duplicateRecipient.getId());
                
                // Marcar como bloqueada
                bankAccount.markAsBlocked("Recipient duplicado no Pagar.me: " + duplicateRecipient.getId());
                bankAccountRepository.save(bankAccount);
                
                throw new IllegalStateException(
                    "Já existe um recipient cadastrado no Pagar.me com este CPF e dados bancários. " +
                    "Recipient ID: " + duplicateRecipient.getId()
                );
            }
            
            log.info("   ├─ ✅ Nenhum recipient duplicado encontrado");
            
        } catch (IllegalStateException e) {
            // Repassa a exceção de duplicidade
            throw e;
        } catch (Exception e) {
            log.warn("   ├─ ⚠️ Erro ao verificar duplicidade (continuando criação): {}", e.getMessage());
            // Continua a criação mesmo se a verificação falhar
        }
        
        // 6. Criar recipient no Pagar.me
        try {
            // Determinar configurações de transferência (defaults: Daily, dia 0)
            String transferInterval = request.transferInterval() != null ? request.transferInterval() : "Daily";
            Integer transferDay = request.transferDay() != null ? request.transferDay() : 0;
            String recipientId = pagarMeService.createRecipient(user, bankAccount, transferInterval, transferDay);
            
            // 7. Atualizar user com recipient ID
            user.markRecipientAsActive(recipientId);
            userRepository.save(user);
            
            // 8. Atualizar status da conta
            bankAccount.setStatus(BankAccountStatus.ACTIVE);
            bankAccount = bankAccountRepository.save(bankAccount);
            
            log.info("   ├─ ✅ Recipient Pagar.me criado: {}", recipientId);
            log.info("   └─ ✅ Status: ACTIVE");
            
        } catch (Exception e) {
            log.error("   └─ ❌ Erro ao criar recipient Pagar.me: {}", e.getMessage());
            // Marca como bloqueada mas mantém o cadastro local
            bankAccount.markAsBlocked("Erro ao criar recipient: " + e.getMessage());
            bankAccount = bankAccountRepository.save(bankAccount);
            throw new RuntimeException("Falha ao criar recipient no Pagar.me", e);
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
     * Cria ou atualiza dados bancários e recipient no Pagar.me (UPSERT)
     * 
     * Processo:
     * 1. Busca BankAccount existente
     * 2. Se NÃO existir → CRIA novo
     * 3. Se existir → ATUALIZA
     * 
     * @param userId ID do usuário
     * @param request Dados bancários
     * @return BankAccount criado ou atualizado
     */
    @Transactional
    public BankAccount updateBankAccount(UUID userId, BankAccountRequest request) {
        log.info("🔄 PUT /api/bank-accounts/{} - Upsert dados bancários", userId);
        
        // 1. Buscar conta existente
        Optional<BankAccount> existingAccount = bankAccountRepository.findByUserId(userId);
        
        // 2. Se não existir, delega para createBankAccount
        if (existingAccount.isEmpty()) {
            log.info("   ├─ ℹ️ Conta não existe - criando nova");
            return createBankAccount(userId, request);
        }
        
        // 3. Se existir, atualiza
        BankAccount bankAccount = existingAccount.get();
        User user = bankAccount.getUser();
        log.info("   ├─ Usuário: {} ({})", user.getName(), user.getUsername());
        
        // 4. Verificar se dados bancários mudaram
        boolean bankDataChanged = !bankAccount.getBankCode().equals(request.bankCode()) ||
                                  !bankAccount.getBankName().equals(request.bankName()) ||
                                  !bankAccount.getAgency().equals(request.agency()) ||
                                  !bankAccount.getAgencyDigit().equals(request.agencyDigit()) ||
                                  !bankAccount.getAccountNumber().equals(request.accountNumber()) ||
                                  !bankAccount.getAccountDigit().equals(request.accountDigit()) ||
                                  !bankAccount.getAccountType().equals(request.accountType());
        
        if (bankDataChanged) {
            log.info("   ├─ ⚠️ Dados bancários alterados - verificando duplicidade no Pagar.me");
            
            // 3. Verificar duplicidade com os NOVOS dados
            try {
                RecipientResponse duplicateRecipient = pagarMeService.findDuplicateRecipient(
                    user.getDocumentClean(),
                    request.bankCode(),
                    request.agency(),
                    request.accountNumber()
                );
                
                if (duplicateRecipient != null) {
                    log.warn("   └─ ❌ Recipient duplicado encontrado: {}", duplicateRecipient.getId());
                    throw new IllegalStateException(
                        "Já existe um recipient cadastrado no Pagar.me com este CPF e dados bancários. " +
                        "Recipient ID: " + duplicateRecipient.getId()
                    );
                }
            } catch (IllegalStateException e) {
                throw e;
            } catch (Exception e) {
                log.warn("   ├─ ⚠️ Erro ao verificar duplicidade (continuando atualização): {}", e.getMessage());
            }
        }
        
        // 4. Verificar se configurações de transferência mudaram
        String newTransferInterval = request.transferInterval() != null ? request.transferInterval() : "Daily";
        Integer newTransferDay = request.transferDay() != null ? request.transferDay() : 0;
        // Para simplificar, sempre assume que pode ter mudado se os campos foram passados
        boolean transferSettingsChanged = request.transferInterval() != null || request.transferDay() != null;
        
        // 5. Atualizar dados locais
        bankAccount.setBankCode(request.bankCode());
        bankAccount.setBankName(request.bankName());
        bankAccount.setAgency(request.agency());
        bankAccount.setAgencyDigit(request.agencyDigit());
        
        // AccountNumber e AccountDigit vêm separados do DTO e são armazenados separados
        bankAccount.setAccountNumber(request.accountNumber());
        bankAccount.setAccountDigit(request.accountDigit());
        
        bankAccount.setAccountType(request.accountType());
        bankAccount.setAutomaticTransfer(true); // Sempre true agora
        
        bankAccount = bankAccountRepository.save(bankAccount);
        log.info("   ├─ ✅ Dados bancários locais atualizados");
        
        // 6. Atualizar conta bancária no Pagar.me (se dados mudaram E já tem recipient)
        log.info("   ├─ 🔍 Verificando necessidade de atualizar Pagar.me:");
        log.info("   │  ├─ Dados bancários mudaram: {}", bankDataChanged);
        log.info("   │  ├─ Transfer settings mudaram: {}", transferSettingsChanged);
        log.info("   │  └─ Recipient ID existe: {}", user.getPagarmeRecipientId() != null);
        
        if (bankDataChanged && user.getPagarmeRecipientId() != null) {
            try {
                log.info("   ├─ 🔄 Atualizando conta bancária padrão no Pagar.me");
                log.info("   │  └─ Recipient ID: {}", user.getPagarmeRecipientId());
                pagarMeService.updateDefaultBankAccount(
                    user.getPagarmeRecipientId(),
                    bankAccount,
                    user
                );
                
                // Atualizar status da conta para ACTIVE
                bankAccount.setStatus(BankAccountStatus.ACTIVE);
                bankAccount = bankAccountRepository.save(bankAccount);
                log.info("   ├─ ✅ Conta bancária atualizada no Pagar.me");
                
            } catch (Exception e) {
                log.error("   └─ ❌ Erro ao atualizar conta no Pagar.me (dados locais já foram salvos)", e);
                // Não lançar exceção - dados locais já foram salvos
                // Se falhar no Pagar.me, usuário pode tentar novamente
            }
        }
        
        // 7. Atualizar transfer settings se mudou E já tem recipient
        if (transferSettingsChanged && user.getPagarmeRecipientId() != null) {
            try {
                log.info("   ├─ 💰 Atualizando transfer settings no Pagar.me");
                log.info("   │  └─ Nova configuração: Intervalo={}, Dia={}", newTransferInterval, newTransferDay);
                pagarMeService.updateTransferSettings(
                    user.getPagarmeRecipientId(),
                    newTransferInterval,
                    newTransferDay
                );
                log.info("   ├─ ✅ Transfer settings atualizados no Pagar.me");
            } catch (Exception e) {
                log.error("   └─ ❌ Erro ao atualizar transfer settings no Pagar.me (dados locais já foram salvos)", e);
                // Não lançar exceção - dados locais já foram salvos
            }
            
        } else if (user.getPagarmeRecipientId() == null) {
            // 6. Se não tem recipient ainda, criar um novo
            try {
                log.info("   ├─ 🆕 Criando recipient no Pagar.me (não existia anteriormente)");
                String newRecipientId = pagarMeService.createRecipient(user, bankAccount);
                
                // Atualizar o recipientId no User
                user.setPagarmeRecipientId(newRecipientId);
                userRepository.save(user);
                
                // Atualizar status da conta para ACTIVE
                bankAccount.setStatus(BankAccountStatus.ACTIVE);
                bankAccount = bankAccountRepository.save(bankAccount);
                
                log.info("   ├─ ✅ Recipient criado no Pagar.me: {}", newRecipientId);
                
            } catch (Exception e) {
                log.error("   └─ ❌ Erro ao criar recipient no Pagar.me", e);
                throw new RuntimeException("Erro ao criar recipient no Pagar.me: " + e.getMessage(), e);
            }
        }
        
        log.info("   └─ ✅ Dados bancários atualizados com sucesso");
        return bankAccount;
    }
}
