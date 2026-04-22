package com.mvt.mvt_events.controller;

import com.mvt.mvt_events.dto.CashReportDto;
import com.mvt.mvt_events.jpa.User;
import com.mvt.mvt_events.repository.UserRepository;
import com.mvt.mvt_events.service.CashReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.UUID;

@RestController
@RequestMapping("/api/reports")
@Tag(name = "Relatórios", description = "Relatórios on-demand do estabelecimento")
public class ReportController {

    private static final ZoneId TZ = ZoneId.of("America/Fortaleza");

    private final CashReportService cashReportService;
    private final UserRepository userRepository;

    public ReportController(CashReportService cashReportService, UserRepository userRepository) {
        this.cashReportService = cashReportService;
        this.userRepository = userRepository;
    }

    /**
     * Relatório de caixa para um intervalo do dia (start/end no formato HH:mm).
     * Default: hoje, 00:00 → agora.
     */
    @GetMapping("/cash")
    @Operation(summary = "Relatório de caixa do dia (filtro HH:mm)")
    public ResponseEntity<CashReportDto> cashReport(
            @RequestParam(required = false) String startTime,  // HH:mm
            @RequestParam(required = false) String endTime,    // HH:mm
            @RequestParam(required = false) UUID clientId,
            Authentication authentication) {

        User user = (User) authentication.getPrincipal();
        UUID resolvedClientId = resolveClientId(clientId, user);
        User client = userRepository.findById(resolvedClientId)
                .orElseThrow(() -> new RuntimeException("Estabelecimento não encontrado"));

        LocalDate today = LocalDate.now(TZ);
        LocalTime startT = parseTime(startTime, LocalTime.MIDNIGHT);
        LocalTime endT = parseTime(endTime, LocalTime.now(TZ));

        OffsetDateTime start = today.atTime(startT).atZone(TZ).toOffsetDateTime();
        OffsetDateTime end = today.atTime(endT).atZone(TZ).toOffsetDateTime();
        if (!end.isAfter(start)) {
            throw new RuntimeException("Hora final deve ser maior que a inicial");
        }

        CashReportDto dto = cashReportService.generateForRange(client, today, start, end);
        return ResponseEntity.ok(dto);
    }

    private LocalTime parseTime(String hhmm, LocalTime fallback) {
        if (hhmm == null || hhmm.isBlank()) return fallback;
        try {
            return LocalTime.parse(hhmm);
        } catch (Exception e) {
            throw new RuntimeException("Formato de hora inválido (use HH:mm): " + hhmm);
        }
    }

    private UUID resolveClientId(UUID clientId, User user) {
        if (user.getRole() == User.Role.CLIENT) return user.getId();
        if (clientId != null && user.getRole() == User.Role.ADMIN) return clientId;
        throw new RuntimeException("Apenas CLIENT ou ADMIN com clientId podem acessar relatórios");
    }
}
