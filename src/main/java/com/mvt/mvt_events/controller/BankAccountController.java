package com.mvt.mvt_events.controller;

import com.mvt.mvt_events.jpa.BankAccount;
import com.mvt.mvt_events.jpa.User;
import com.mvt.mvt_events.payment.dto.BankAccountRequest;
import com.mvt.mvt_events.payment.dto.BankAccountResponse;
import com.mvt.mvt_events.payment.dto.VerificationStatusResponse;
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

/**
 * Controller para gerenciar dados bancários de couriers e organizers
 * 
 * <p>Endpoints:
 * <ul>
 *   <li>POST /api/motoboy/bank-data - Cadastrar dados bancários</li>
 *   <li>GET /api/motoboy/bank-data - Consultar dados bancários</li>
 *   <li>PUT /api/motoboy/bank-data - Atualizar dados bancários</li>
 *   <li>GET /api/motoboy/bank-data/verification-status - Verificar status de verificação</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/motoboy/bank-data")
@RequiredArgsConstructor
@Slf4j
public class BankAccountController {

    private final BankAccountService bankAccountService;

    /**
     * Cadastra dados bancários e cria subconta no Iugu
     * 
     * <p><strong>POST /api/motoboy/bank-data</strong>
     * 
     * <p>Apenas COURIER e ORGANIZER podem cadastrar dados bancários.
     * 
     * <p>Processo:
     * <ol>
     *   <li>Valida dados bancários (formato, código do banco, etc.)</li>
     *   <li>Cria BankAccount local com status PENDING_VALIDATION</li>
     *   <li>Cria subconta no Iugu (marketplace)</li>
     *   <li>Salva iuguAccountId no User</li>
     *   <li>Retorna dados cadastrados</li>
     * </ol>
     * 
     * @param user Usuário autenticado
     * @param request Dados bancários
     * @return BankAccount criado com status PENDING_VALIDATION
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('COURIER', 'ORGANIZER')")
    public ResponseEntity<?> createBankAccount(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody BankAccountRequest request
    ) {
        log.info("📥 POST /api/motoboy/bank-data - User: {} ({})", user.getUsername(), user.getRole());
        
        try {
            BankAccount bankAccount = bankAccountService.createBankAccount(user.getId(), request);
            
            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(BankAccountResponse.from(bankAccount));
                    
        } catch (IllegalStateException e) {
            // Já existe conta bancária
            log.warn("   └─ ⚠️ Conta bancária já existe: {}", e.getMessage());
            return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body(Map.of(
                        "error", "ALREADY_EXISTS",
                        "message", e.getMessage()
                    ));
                    
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
     * <p><strong>GET /api/motoboy/bank-data</strong>
     * 
     * @param user Usuário autenticado
     * @return Dados bancários cadastrados ou 404 se não cadastrado
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('COURIER', 'ORGANIZER')")
    public ResponseEntity<?> getBankAccount(@AuthenticationPrincipal User user) {
        log.info("📤 GET /api/motoboy/bank-data - User: {}", user.getUsername());
        
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
     * Atualiza dados bancários
     * 
     * <p><strong>PUT /api/motoboy/bank-data</strong>
     * 
     * <p>Processo:
     * <ol>
     *   <li>Valida novos dados</li>
     *   <li>Atualiza BankAccount local</li>
     *   <li>Se estava BLOCKED, volta para PENDING_VALIDATION</li>
     *   <li>Atualiza dados no Iugu (se iuguAccountId existe)</li>
     * </ol>
     * 
     * @param user Usuário autenticado
     * @param request Novos dados bancários
     * @return BankAccount atualizado
     */
    @PutMapping
    @PreAuthorize("hasAnyRole('COURIER', 'ORGANIZER')")
    public ResponseEntity<?> updateBankAccount(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody BankAccountRequest request
    ) {
        log.info("🔄 PUT /api/motoboy/bank-data - User: {}", user.getUsername());
        
        try {
            BankAccount bankAccount = bankAccountService.updateBankAccount(user.getId(), request);
            
            return ResponseEntity.ok(BankAccountResponse.from(bankAccount));
            
        } catch (IllegalStateException e) {
            // Não existe conta bancária para atualizar
            log.warn("   └─ ⚠️ Conta bancária não existe: {}", e.getMessage());
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(Map.of(
                        "error", "NOT_FOUND",
                        "message", e.getMessage()
                    ));
                    
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
     * Verifica status de verificação em tempo real (consulta API Iugu)
     * 
     * <p><strong>GET /api/motoboy/bank-data/verification-status</strong>
     * 
     * <p>Este endpoint consulta diretamente a API Iugu para obter o status
     * atualizado da verificação dos dados bancários. Útil para o usuário
     * verificar manualmente sem esperar o job agendado.
     * 
     * <p>Processo:
     * <ol>
     *   <li>Busca BankAccount e User</li>
     *   <li>Consulta status no Iugu via API</li>
     *   <li>Sincroniza status local se mudou</li>
     *   <li>Retorna status atualizado com mensagem amigável</li>
     * </ol>
     * 
     * @param user Usuário autenticado
     * @return Status de verificação com mensagem
     */
    @GetMapping("/verification-status")
    @PreAuthorize("hasAnyRole('COURIER', 'ORGANIZER')")
    public ResponseEntity<VerificationStatusResponse> checkVerificationStatus(
            @AuthenticationPrincipal User user
    ) {
        log.info("🔍 GET /api/motoboy/bank-data/verification-status - User: {}", user.getUsername());
        
        try {
            VerificationStatusResponse response = bankAccountService.checkVerificationStatus(user.getId());
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            // Usuário não encontrado (não deveria acontecer com @AuthenticationPrincipal)
            log.error("   └─ ❌ Usuário não encontrado: {}", e.getMessage());
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(VerificationStatusResponse.notRegistered());
        }
    }
}
