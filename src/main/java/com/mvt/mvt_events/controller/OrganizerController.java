package com.mvt.mvt_events.controller;

import com.mvt.mvt_events.dto.OrganizerEarningsResponse;
import com.mvt.mvt_events.jpa.User;
import com.mvt.mvt_events.service.OrganizerEarningsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Controller para endpoints específicos de organizers (empresas).
 * Fornece funcionalidades para gerenciar recebimentos e histórico de entregas.
 */
@Slf4j
@RestController
@RequestMapping("/api/organizers")
@CrossOrigin(origins = "*")
@Tag(name = "Organizers", description = "Endpoints para empresas organizadoras")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
public class OrganizerController {

    private final OrganizerEarningsService earningsService;

    /**
     * Lista os recebimentos do organizer logado.
     * Retorna apenas deliveries COMPLETED com pagamento PAID,
     * mostrando o detalhamento da repartição de cada corrida.
     *
     * @param authentication Dados do usuário logado
     * @param recent Se true, filtra apenas corridas recentes (últimos N dias conforme deliveryHistoryDays da config)
     * @return Histórico de recebimentos com detalhamento
     */
    @GetMapping("/me/earnings")
    @Operation(
        summary = "Listar meus recebimentos", 
        description = "Retorna o histórico de recebimentos do organizer logado. " +
                      "Lista todas as corridas completadas com pagamento confirmado (PAID), " +
                      "onde o organizer participou, mostrando o detalhamento completo da repartição de valores: " +
                      "- Valor que o courier recebeu (87% do total) " +
                      "- Valor que o organizer recebeu (5% do total) " +
                      "- Valor que a plataforma recebeu (8% com organizer) " +
                      "Inclui informações da corrida (origem, destino, distância, cliente, courier) " +
                      "e do pagamento (método, status, ID). " +
                      "Parâmetro 'recent' opcional: se true, filtra apenas corridas recentes (número de dias vem do deliveryHistoryDays da configuração)."
    )
    @Transactional(readOnly = true)
    public ResponseEntity<OrganizerEarningsResponse> getMyEarnings(
            Authentication authentication,
            @RequestParam(required = false, defaultValue = "false") Boolean recent) {
        log.info("📊 GET /api/organizers/me/earnings - Buscando recebimentos do organizer");

        // Obter UUID do organizer logado
        User user = (User) authentication.getPrincipal();
        UUID organizerId = user.getId();

        // Validar que o usuário é ORGANIZER
        if (user.getRole() != User.Role.ORGANIZER) {
            log.warn("⚠️ Usuário {} não é ORGANIZER (role: {})", user.getUsername(), user.getRole());
            return ResponseEntity.status(403).build();
        }

        // Buscar recebimentos
        OrganizerEarningsResponse earnings = earningsService.getOrganizerEarnings(organizerId, recent);

        log.info("✅ Retornando {} deliveries com total de R$ {} (recent={})", 
                earnings.getTotalDeliveries(), 
                earnings.getTotalEarnings(),
                recent);

        return ResponseEntity.ok(earnings);
    }
}
