package com.mvt.mvt_events.controller;

import com.mvt.mvt_events.jpa.ClientWaiter;
import com.mvt.mvt_events.jpa.User;
import com.mvt.mvt_events.service.ClientWaiterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/waiters")
@Tag(name = "Garçons", description = "Gerenciamento de garçons e vínculos com estabelecimentos")
public class WaiterController {

    private final ClientWaiterService clientWaiterService;

    public WaiterController(ClientWaiterService clientWaiterService) {
        this.clientWaiterService = clientWaiterService;
    }

    // ================================================================
    // ENDPOINTS DO CLIENT (gerenciar garçons do seu estabelecimento)
    // ================================================================

    @GetMapping("/by-client")
    @Operation(summary = "Listar garçons de um estabelecimento")
    public List<ClientWaiter> listByClient(
            @RequestParam(required = false) UUID clientId,
            Authentication authentication) {
        UUID resolvedClientId = resolveClientId(clientId, authentication);
        return clientWaiterService.findByClient(resolvedClientId);
    }

    @PostMapping("/link")
    @Operation(summary = "Vincular garçom ao estabelecimento (por email)")
    public ResponseEntity<ClientWaiter> link(
            @RequestBody LinkWaiterRequest request,
            Authentication authentication) {
        UUID clientId = resolveClientId(request.clientId, authentication);
        ClientWaiter cw = clientWaiterService.linkByEmail(clientId, request.waiterEmail, request.pin);
        return ResponseEntity.status(HttpStatus.CREATED).body(cw);
    }

    @PatchMapping("/pin")
    @Operation(summary = "Alterar PIN do garçom em um estabelecimento")
    public ClientWaiter updatePin(
            @RequestBody UpdatePinRequest request,
            Authentication authentication) {
        UUID clientId = resolveClientId(request.clientId, authentication);
        return clientWaiterService.updatePin(clientId, request.waiterId, request.pin);
    }

    @PatchMapping("/toggle")
    @Operation(summary = "Ativar/desativar garçom em um estabelecimento")
    public ClientWaiter toggleActive(
            @RequestBody ToggleRequest request,
            Authentication authentication) {
        UUID clientId = resolveClientId(request.clientId, authentication);
        return clientWaiterService.toggleActive(clientId, request.waiterId, request.active);
    }

    @DeleteMapping("/unlink")
    @Operation(summary = "Desvincular garçom do estabelecimento")
    public ResponseEntity<Void> unlink(
            @RequestParam UUID waiterId,
            @RequestParam(required = false) UUID clientId,
            Authentication authentication) {
        UUID resolvedClientId = resolveClientId(clientId, authentication);
        clientWaiterService.unlink(resolvedClientId, waiterId);
        return ResponseEntity.noContent().build();
    }

    // ================================================================
    // ENDPOINTS DO WAITER (seus estabelecimentos)
    // ================================================================

    @GetMapping("/my-establishments")
    @Operation(summary = "Listar estabelecimentos onde o garçom está ativo")
    public List<ClientWaiter> myEstablishments(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        if (user.getRole() != User.Role.WAITER) {
            throw new RuntimeException("Apenas garçons podem acessar este endpoint");
        }
        return clientWaiterService.findEstablishmentsByWaiter(user.getId());
    }

    // ================================================================
    // HELPERS
    // ================================================================

    private UUID resolveClientId(UUID clientId, Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        if (user.getRole() == User.Role.CLIENT) {
            return user.getId();
        }
        if ((user.getRole() == User.Role.ADMIN || user.getRole() == User.Role.WAITER) && clientId != null) {
            return clientId;
        }
        throw new RuntimeException("clientId é obrigatório para esta role");
    }

    // ================================================================
    // REQUEST DTOs
    // ================================================================

    @Data
    public static class LinkWaiterRequest {
        private UUID clientId;
        private String waiterEmail;
        private String pin;
    }

    @Data
    public static class UpdatePinRequest {
        private UUID clientId;
        private UUID waiterId;
        private String pin;
    }

    @Data
    public static class ToggleRequest {
        private UUID clientId;
        private UUID waiterId;
        private boolean active;
    }
}
