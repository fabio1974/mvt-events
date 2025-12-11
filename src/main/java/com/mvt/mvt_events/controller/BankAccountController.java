package com.mvt.mvt_events.controller;

import com.mvt.mvt_events.jpa.BankAccount;
import com.mvt.mvt_events.jpa.User;
import com.mvt.mvt_events.payment.dto.BankAccountRequest;
import com.mvt.mvt_events.payment.dto.BankAccountResponse;
import com.mvt.mvt_events.payment.service.BankAccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
 * <p>Endpoints:
 * <ul>
 *   <li>POST /api/bank-accounts - Cadastrar dados bancários e criar recipient Pagar.me</li>
 *   <li>GET /api/bank-accounts - Consultar dados bancários do usuário autenticado</li>
 *   <li>GET /api/bank-accounts/{userId} - Buscar dados bancários por userId</li>
 *   <li>PUT /api/bank-accounts/{userId} - Atualizar dados bancários</li>
 * </ul>
 */
@RestController
@RequiredArgsConstructor
@Slf4j
public class BankAccountController {

    private final BankAccountService bankAccountService;

    /**
     * Cadastra dados bancários e cria recipient no Pagar.me
     * 
     * <p><strong>POST /api/bank-accounts</strong>
     * 
     * <p>Apenas COURIER e ORGANIZER podem cadastrar dados bancários.
     * 
     * <p>Processo:
     * <ol>
     *   <li>Busca dados do User (nome, CPF, email já cadastrados)</li>
     *   <li>Cria BankAccount local com dados bancários</li>
     *   <li>Verifica duplicidade no Pagar.me (CPF + dados bancários)</li>
     *   <li>Cria recipient no Pagar.me com dados mínimos necessários</li>
     *   <li>Salva pagarmeRecipientId no User</li>
     * </ol>
     * 
     * @param user Usuário autenticado
     * @param request Dados bancários (apenas campos essenciais)
     * @return BankAccount criado
     */
    @PostMapping("/api/bank-accounts")
    @PreAuthorize("hasAnyRole('COURIER', 'ORGANIZER')")
    public ResponseEntity<?> createBankAccount(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody BankAccountRequest request
    ) {
        log.info("📥 POST /api/bank-accounts - User: {} ({})", user.getUsername(), user.getRole());
        
        try {
            BankAccount bankAccount = bankAccountService.createBankAccount(user.getId(), request);
            
            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(BankAccountResponse.from(bankAccount));
                    
        } catch (IllegalStateException e) {
            // Verifica se é duplicidade no Pagar.me ou se usuário já tem conta
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
                // Já existe conta bancária local
                log.warn("   └─ ⚠️ Conta bancária já existe: {}", e.getMessage());
                return ResponseEntity
                        .status(HttpStatus.CONFLICT)
                        .body(Map.of(
                            "error", "ALREADY_EXISTS",
                            "message", e.getMessage()
                        ));
            }
                    
        } catch (IllegalArgumentException e) {
            // Dados inválidos
            log.warn("   └─ ❌ Dados inválidos: {}", e.getMessage());
            return ResponseEntity
                    .badRequest()
                    .body(Map.of(
                        "error", "INVALID_DATA",
                        "message", e.getMessage()
                    ));
                    
        } catch (Exception e) {
            // Erro inesperado
            log.error("   └─ ❌ Erro ao cadastrar dados bancários", e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                        "error", "INTERNAL_ERROR",
                        "message", "Erro ao cadastrar dados bancários: " + e.getMessage()
                    ));
        }
    }

    /**
     * Consulta dados bancários do usuário autenticado
     * 
     * <p><strong>GET /api/bank-accounts</strong>
     * 
     * @param user Usuário autenticado
     * @return Dados bancários cadastrados ou 404 se não cadastrado
     */
    @GetMapping("/api/bank-accounts")
    @PreAuthorize("hasAnyRole('COURIER', 'ORGANIZER')")
    public ResponseEntity<?> getBankAccount(@AuthenticationPrincipal User user) {
        log.info("📤 GET /api/bank-accounts - User: {}", user.getUsername());
        
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
     * Busca dados bancários por User ID
     * 
     * Endpoint: GET /api/bank-accounts/{userId}
     * 
     * @param userId UUID do usuário
     * @return Dados bancários ou 404
     */
    @GetMapping("/api/bank-accounts/{userId}")
    public ResponseEntity<?> getBankAccountByUserId(@PathVariable UUID userId) {
        log.info("📤 GET /api/bank-accounts/{} - Buscando por User ID", userId);
        
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
     * Atualiza dados bancários e recipient no Pagar.me
     * 
     * <p><strong>PUT /api/bank-accounts/{userId}</strong>
     * 
     * <p>Processo:
     * <ol>
     *   <li>Verifica se dados bancários mudaram</li>
     *   <li>Se mudaram, verifica duplicidade no Pagar.me (CPF + dados bancários)</li>
     *   <li>Atualiza dados locais</li>
     *   <li>Cria novo recipient no Pagar.me (se dados bancários mudaram)</li>
     * </ol>
     * 
     * @param userId ID do usuário
     * @param request Novos dados bancários (apenas campos essenciais)
     * @return BankAccount atualizado
     */
    @PutMapping("/api/bank-accounts/{userId}")
    @PreAuthorize("hasAnyRole('COURIER', 'ORGANIZER', 'ADMIN')")
    public ResponseEntity<?> updateBankAccount(
            @PathVariable UUID userId,
            @Valid @RequestBody BankAccountRequest request
    ) {
        log.info("🔄 PUT /api/bank-accounts/{} - Atualizando dados bancários", userId);
        
        try {
            BankAccount bankAccount = bankAccountService.updateBankAccount(userId, request);
            
            return ResponseEntity.ok(BankAccountResponse.from(bankAccount));
            
        } catch (IllegalArgumentException e) {
            // Dados não encontrados ou inválidos
            log.warn("   └─ ❌ Dados inválidos: {}", e.getMessage());
            return ResponseEntity
                    .badRequest()
                    .body(Map.of(
                        "error", "INVALID_DATA",
                        "message", e.getMessage()
                    ));
                    
        } catch (IllegalStateException e) {
            // Recipient duplicado
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
            // Erro inesperado
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
