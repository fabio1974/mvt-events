package com.mvt.mvt_events.controller;

import com.mvt.mvt_events.jpa.CashRegisterMovement;
import com.mvt.mvt_events.jpa.CashRegisterSession;
import com.mvt.mvt_events.jpa.User;
import com.mvt.mvt_events.service.CashRegisterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/cash-register")
@Tag(name = "Caixa", description = "Sessão de caixa do estabelecimento (abertura, movimentação, fechamento)")
public class CashRegisterController {

    private final CashRegisterService service;

    public CashRegisterController(CashRegisterService service) {
        this.service = service;
    }

    @GetMapping("/current")
    @Operation(summary = "Sessão de caixa aberta (se houver)")
    public ResponseEntity<?> current(
            @RequestParam(required = false) UUID clientId,
            Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        UUID resolved = resolveClientId(clientId, user);
        return service.getOpenSession(resolved)
                .map(s -> {
                    Map<String, Object> body = serialize(s);
                    body.put("expectedBalance", service.computeExpected(s, OffsetDateTime.now()));
                    return ResponseEntity.ok(body);
                })
                .orElse(ResponseEntity.ok(Map.of("status", "NONE")));
    }

    @PostMapping("/open")
    @Operation(summary = "Abrir sessão de caixa")
    public ResponseEntity<CashRegisterSession> open(
            @RequestBody OpenRequest request,
            Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        UUID resolved = resolveClientId(request.clientId, user);
        CashRegisterSession s = service.open(resolved, user.getId(), request.openingBalance, request.notes);
        return ResponseEntity.status(HttpStatus.CREATED).body(s);
    }

    @PostMapping("/close")
    @Operation(summary = "Fechar sessão de caixa")
    public CashRegisterSession close(
            @RequestBody CloseRequest request,
            Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        UUID resolved = resolveClientId(request.clientId, user);
        return service.close(resolved, user.getId(), request.closingBalanceActual, request.notes);
    }

    @PostMapping("/movements")
    @Operation(summary = "Adicionar movimentação (suprimento, retirada ou sangria)")
    public ResponseEntity<CashRegisterMovement> addMovement(
            @RequestBody MovementRequest request,
            Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        UUID resolved = resolveClientId(request.clientId, user);
        CashRegisterMovement.Type type;
        try {
            type = CashRegisterMovement.Type.valueOf(request.type.toUpperCase());
        } catch (Exception e) {
            throw new RuntimeException("Tipo inválido. Use ADDITION, WITHDRAWAL ou SANGRIA");
        }
        CashRegisterMovement m = service.addMovement(resolved, user.getId(), type, request.amount, request.reason);
        return ResponseEntity.status(HttpStatus.CREATED).body(m);
    }

    private UUID resolveClientId(UUID clientId, User user) {
        if (user.getRole() == User.Role.CLIENT) return user.getId();
        if (clientId != null && (user.getRole() == User.Role.ADMIN || user.getRole() == User.Role.WAITER)) {
            return clientId;
        }
        throw new RuntimeException("clientId é obrigatório para esta role");
    }

    private Map<String, Object> serialize(CashRegisterSession s) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", s.getId());
        m.put("status", s.getStatus().name());
        m.put("openingBalance", s.getOpeningBalance());
        m.put("openedAt", s.getOpenedAt());
        m.put("closedAt", s.getClosedAt());
        m.put("openedByName", s.getOpenedByName());
        m.put("closedByName", s.getClosedByName());
        m.put("closingBalanceActual", s.getClosingBalanceActual());
        m.put("closingBalanceExpected", s.getClosingBalanceExpected());
        m.put("notes", s.getNotes());
        m.put("movements", s.getMovements());
        return m;
    }

    @Data public static class OpenRequest {
        private UUID clientId;
        private BigDecimal openingBalance;
        private String notes;
    }

    @Data public static class CloseRequest {
        private UUID clientId;
        private BigDecimal closingBalanceActual;
        private String notes;
    }

    @Data public static class MovementRequest {
        private UUID clientId;
        private String type; // ADDITION | WITHDRAWAL | SANGRIA
        private BigDecimal amount;
        private String reason;
    }
}
