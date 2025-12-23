package com.mvt.mvt_events.controller;

import com.mvt.mvt_events.jpa.BankAccount;
import com.mvt.mvt_events.jpa.BankAccount.BankAccountStatus;
import com.mvt.mvt_events.jpa.User;
import com.mvt.mvt_events.payment.dto.BankAccountRequest;
import com.mvt.mvt_events.payment.dto.BankAccountResponse;
import com.mvt.mvt_events.payment.service.BankAccountService;
import com.mvt.mvt_events.repository.BankAccountRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Controller para gerenciar dados bancários de couriers e organizers
 * 
 * <p>Endpoints REST:
 * <ul>
 *   <li>GET /api/bank-accounts - Lista paginada de todas as contas bancárias</li>
 *   <li>GET /api/bank-accounts/{id} - Busca conta bancária por ID</li>
 *   <li>GET /api/bank-accounts/user/{userId} - Busca conta bancária por User ID</li>
 *   <li>GET /api/bank-accounts/me - Busca conta do usuário autenticado</li>
 *   <li>POST /api/bank-accounts - Cadastrar dados bancários</li>
 *   <li>PUT /api/bank-accounts/{id} - Atualizar dados bancários</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/bank-accounts")
@RequiredArgsConstructor
@Slf4j
public class BankAccountController {

    private final BankAccountService bankAccountService;
    private final BankAccountRepository bankAccountRepository;

    /**
     * Lista todas as contas bancárias (paginado)
     * 
     * GET /api/bank-accounts?page=0&size=10&status=ACTIVE
     * 
     * @param page Número da página (default: 0)
     * @param size Tamanho da página (default: 10)
     * @param status Filtro por status (opcional): ACTIVE, PENDING_VALIDATION, REJECTED
     * @return Page de BankAccountResponse
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<BankAccountResponse>> getAllBankAccounts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) BankAccountStatus status
    ) {
        log.info("📤 GET /api/bank-accounts - page={}, size={}, status={}", page, size, status);
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        
        Page<BankAccount> bankAccountsPage;
        if (status != null) {
            bankAccountsPage = bankAccountRepository.findByStatus(status, pageable);
        } else {
            bankAccountsPage = bankAccountRepository.findAll(pageable);
        }
        
        Page<BankAccountResponse> responsePage = bankAccountsPage.map(BankAccountResponse::from);
        
        log.info("   └─ ✅ Retornando {} registros de {} total", 
                responsePage.getNumberOfElements(), responsePage.getTotalElements());
        
        return ResponseEntity.ok(responsePage);
    }

    /**
     * Busca conta bancária por ID
     * 
     * GET /api/bank-accounts/{id}
     * 
     * @param id ID da conta bancária (Long)
     * @return BankAccountResponse ou 404
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'COURIER', 'ORGANIZER')")
    public ResponseEntity<?> getBankAccountById(@PathVariable Long id) {
        log.info("📤 GET /api/bank-accounts/{}", id);
        
        Optional<BankAccount> bankAccountOpt = bankAccountRepository.findById(id);
        
        if (bankAccountOpt.isEmpty()) {
            log.info("   └─ ℹ️ Conta bancária não encontrada");
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(Map.of(
                        "error", "NOT_FOUND",
                        "message", "Conta bancária não encontrada"
                    ));
        }
        
        return ResponseEntity.ok(BankAccountResponse.from(bankAccountOpt.get()));
    }

    /**
     * Busca conta bancária por User ID
     * 
     * GET /api/bank-accounts/user/{userId}
     * 
     * @param userId UUID do usuário
     * @return BankAccountResponse ou 404
     */
    @GetMapping("/user/{userId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'COURIER', 'ORGANIZER')")
    public ResponseEntity<?> getBankAccountByUserId(@PathVariable UUID userId) {
        log.info("📤 GET /api/bank-accounts/user/{}", userId);
        
        Optional<BankAccount> bankAccountOpt = bankAccountService.getBankAccount(userId);
        
        if (bankAccountOpt.isEmpty()) {
            log.info("   └─ ℹ️ Usuário não possui dados bancários cadastrados");
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(Map.of(
                        "error", "NOT_FOUND",
                        "message", "Dados bancários não cadastrados"
                    ));
        }
        
        return ResponseEntity.ok(BankAccountResponse.from(bankAccountOpt.get()));
    }

    /**
     * Busca conta bancária do usuário autenticado
     * 
     * GET /api/bank-accounts/me
     * 
     * @param user Usuário autenticado
     * @return BankAccountResponse ou 404
     */
    @GetMapping("/me")
    @PreAuthorize("hasAnyRole('COURIER', 'ORGANIZER')")
    public ResponseEntity<?> getMyBankAccount(@AuthenticationPrincipal User user) {
        log.info("📤 GET /api/bank-accounts/me - User: {}", user.getUsername());
        
        Optional<BankAccount> bankAccountOpt = bankAccountService.getBankAccount(user.getId());
        
        if (bankAccountOpt.isEmpty()) {
            log.info("   └─ ℹ️ Usuário não possui dados bancários cadastrados");
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(Map.of(
                        "error", "NOT_FOUND",
                        "message", "Dados bancários não cadastrados"
                    ));
        }
        
        return ResponseEntity.ok(BankAccountResponse.from(bankAccountOpt.get()));
    }

    /**
     * Cadastra dados bancários e cria recipient no Pagar.me
     * 
     * POST /api/bank-accounts
     * 
     * @param request Dados bancários (inclui userId)
     * @return BankAccount criado (201) ou erro
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'COURIER', 'ORGANIZER')")
    public ResponseEntity<?> createBankAccount(@Valid @RequestBody BankAccountRequest request) {
        log.info("📥 POST /api/bank-accounts - UserId: {}", request.user().id());
        
        try {
            BankAccount bankAccount = bankAccountService.createBankAccount(request.user().id(), request);
            
            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(BankAccountResponse.from(bankAccount));
                    
        } catch (IllegalStateException e) {
            if (e.getMessage().contains("Recipient duplicado") || e.getMessage().contains("recipient cadastrado")) {
                log.warn("   └─ ⚠️ Recipient duplicado no Pagar.me: {}", e.getMessage());
                return ResponseEntity
                        .status(HttpStatus.CONFLICT)
                        .body(Map.of(
                            "error", "DUPLICATE_RECIPIENT",
                            "message", "Já existe um recipient cadastrado no Pagar.me com este CPF e conta bancária",
                            "details", e.getMessage()
                        ));
            } else {
                log.warn("   └─ ⚠️ Conta bancária já existe: {}", e.getMessage());
                return ResponseEntity
                        .status(HttpStatus.CONFLICT)
                        .body(Map.of(
                            "error", "ALREADY_EXISTS",
                            "message", e.getMessage()
                        ));
            }
                    
        } catch (IllegalArgumentException e) {
            log.warn("   └─ ❌ Dados inválidos: {}", e.getMessage());
            return ResponseEntity
                    .badRequest()
                    .body(Map.of(
                        "error", "INVALID_DATA",
                        "message", e.getMessage()
                    ));
                    
        } catch (Exception e) {
            log.error("   └─ ❌ Erro ao cadastrar dados bancários", e);
            
            // Extrai mensagem detalhada do Pagar.me se disponível
            String details = e.getMessage();
            if (e.getCause() != null && e.getCause().getMessage() != null) {
                details = e.getCause().getMessage();
            }
            
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                        "error", "INTERNAL_ERROR",
                        "message", "Erro ao cadastrar dados bancários: " + e.getMessage(),
                        "details", details
                    ));
        }
    }

    /**
     * Atualiza dados bancários e recipient no Pagar.me
     * 
     * PUT /api/bank-accounts/{id}
     * 
     * @param id ID da conta bancária
     * @param request Novos dados bancários
     * @return BankAccount atualizado
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('COURIER', 'ORGANIZER', 'ADMIN')")
    public ResponseEntity<?> updateBankAccount(
            @PathVariable Long id,
            @Valid @RequestBody BankAccountRequest request
    ) {
        log.info("🔄 PUT /api/bank-accounts/{}", id);
        
        try {
            // Buscar conta bancária pelo ID
            Optional<BankAccount> bankAccountOpt = bankAccountRepository.findById(id);
            if (bankAccountOpt.isEmpty()) {
                return ResponseEntity
                        .status(HttpStatus.NOT_FOUND)
                        .body(Map.of(
                            "error", "NOT_FOUND",
                            "message", "Conta bancária não encontrada"
                        ));
            }
            
            UUID userId = bankAccountOpt.get().getUser().getId();
            BankAccount bankAccount = bankAccountService.updateBankAccount(userId, request);
            
            return ResponseEntity.ok(BankAccountResponse.from(bankAccount));
            
        } catch (IllegalArgumentException e) {
            log.warn("   └─ ❌ Dados inválidos: {}", e.getMessage());
            return ResponseEntity
                    .badRequest()
                    .body(Map.of(
                        "error", "INVALID_DATA",
                        "message", e.getMessage()
                    ));
                    
        } catch (IllegalStateException e) {
            if (e.getMessage().contains("Recipient duplicado") || e.getMessage().contains("recipient cadastrado")) {
                log.warn("   └─ ⚠️ Recipient duplicado no Pagar.me: {}", e.getMessage());
                return ResponseEntity
                        .status(HttpStatus.CONFLICT)
                        .body(Map.of(
                            "error", "DUPLICATE_RECIPIENT",
                            "message", "Já existe um recipient cadastrado no Pagar.me com este CPF e conta bancária",
                            "details", e.getMessage()
                        ));
            }
            throw e;
                    
        } catch (Exception e) {
            log.error("   └─ ❌ Erro ao atualizar dados bancários", e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                        "error", "INTERNAL_ERROR",
                        "message", "Erro ao atualizar dados bancários: " + e.getMessage()
                    ));
        }
    }

    /**
     * Atualiza dados bancários por User ID (backward compatibility)
     * 
     * PUT /api/bank-accounts/user/{userId}
     * 
     * @param userId UUID do usuário
     * @param request Novos dados bancários
     * @return BankAccount atualizado
     */
    @PutMapping("/user/{userId}")
    @PreAuthorize("hasAnyRole('COURIER', 'ORGANIZER', 'ADMIN')")
    public ResponseEntity<?> updateBankAccountByUserId(
            @PathVariable UUID userId,
            @Valid @RequestBody BankAccountRequest request
    ) {
        log.info("🔄 PUT /api/bank-accounts/user/{}", userId);
        
        try {
            BankAccount bankAccount = bankAccountService.updateBankAccount(userId, request);
            return ResponseEntity.ok(BankAccountResponse.from(bankAccount));
            
        } catch (IllegalArgumentException e) {
            log.warn("   └─ ❌ Dados inválidos: {}", e.getMessage());
            return ResponseEntity
                    .badRequest()
                    .body(Map.of(
                        "error", "INVALID_DATA",
                        "message", e.getMessage()
                    ));
                    
        } catch (IllegalStateException e) {
            if (e.getMessage().contains("Recipient duplicado") || e.getMessage().contains("recipient cadastrado")) {
                log.warn("   └─ ⚠️ Recipient duplicado no Pagar.me: {}", e.getMessage());
                return ResponseEntity
                        .status(HttpStatus.CONFLICT)
                        .body(Map.of(
                            "error", "DUPLICATE_RECIPIENT",
                            "message", "Já existe um recipient cadastrado no Pagar.me com este CPF e conta bancária",
                            "details", e.getMessage()
                        ));
            }
            throw e;
                    
        } catch (Exception e) {
            log.error("   └─ ❌ Erro ao atualizar dados bancários", e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                        "error", "INTERNAL_ERROR",
                        "message", "Erro ao atualizar dados bancários: " + e.getMessage()
                    ));
        }
    }
}
