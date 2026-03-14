package com.mvt.mvt_events.controller;

import com.mvt.mvt_events.dto.CourierEarningsResponse;
import com.mvt.mvt_events.jpa.User;
import com.mvt.mvt_events.service.CourierEarningsService;
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
 * Controller para endpoints específicos de couriers (motoboys).
 * Fornece funcionalidades para gerenciar recebimentos e histórico de entregas.
 */
@Slf4j
@RestController
@RequestMapping("/api/couriers")
@CrossOrigin(origins = "*")
@Tag(name = "Couriers", description = "Endpoints para motoboys")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
public class CourierController {

    private final CourierEarningsService earningsService;

    /**
     * Lista os recebimentos do courier logado.
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
        description = "Retorna o histórico de recebimentos do courier logado. " +
                      "Lista todas as corridas completadas com pagamento confirmado (PAID), " +
                      "mostrando o detalhamento completo da repartição de valores: " +
                      "- Valor que o courier recebeu (87% do total) " +
                      "- Valor que o organizer recebeu (5% se houver, 0 caso contrário) " +
                      "- Valor que a plataforma recebeu (8% com organizer, 13% sem organizer) " +
                      "Inclui informações da corrida (origem, destino, distância, cliente) " +
                      "e do pagamento (método, status, ID). " +
                      "Parâmetro 'recent' opcional: se true, filtra apenas corridas recentes (número de dias vem do deliveryHistoryDays da configuração)."
    )
    @Transactional(readOnly = true)
    public ResponseEntity<CourierEarningsResponse> getMyEarnings(
            Authentication authentication,
            @RequestParam(required = false, defaultValue = "false") Boolean recent) {
        log.info("📊 GET /api/couriers/me/earnings - Buscando recebimentos do courier");

        // Obter UUID do courier logado
        User user = (User) authentication.getPrincipal();
        UUID courierId = user.getId();

        // Validar que o usuário é COURIER
        if (user.getRole() != User.Role.COURIER) {
            log.warn("⚠️ Usuário {} não é COURIER (role: {})", user.getUsername(), user.getRole());
            return ResponseEntity.status(403).build();
        }

        // Buscar recebimentos
        CourierEarningsResponse earnings = earningsService.getCourierEarnings(courierId, recent);

        log.info("✅ Retornando {} deliveries com total de R$ {} (recent={})", 
                earnings.getTotalDeliveries(), 
                earnings.getTotalEarnings(),
                recent);

        return ResponseEntity.ok(earnings);
    }
}
