package com.mvt.mvt_events.controller;

import com.mvt.mvt_events.dto.MyRegistrationResponse;
import com.mvt.mvt_events.dto.RegistrationListDTO;
import com.mvt.mvt_events.jpa.Registration;
import com.mvt.mvt_events.jpa.User;
import com.mvt.mvt_events.service.RegistrationService;
import com.mvt.mvt_events.service.RegistrationMapperService;
import com.mvt.mvt_events.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/registrations")
@Tag(name = "Inscrições", description = "Gerenciamento de inscrições em eventos")
@SecurityRequirement(name = "bearerAuth")
public class RegistrationController {

    private final RegistrationService service;
    private final UserService userService;
    private final RegistrationMapperService mapperService;

    public RegistrationController(RegistrationService service, UserService userService,
            RegistrationMapperService mapperService) {
        this.service = service;
        this.userService = userService;
        this.mapperService = mapperService;
    }

    @GetMapping
    @Operation(summary = "Listar inscrições", description = """
            Lista inscrições com suporte a filtros e paginação.

            **Filtros Disponíveis:**
            - `status` - Status da inscrição (PENDING, CONFIRMED, CANCELLED, WAITLISTED)
            - `event` ou `eventId` - ID do evento
            - `user` ou `userId` - UUID do usuário

            **Paginação:**
            - `page` - Número da página (default: 0)
            - `size` - Tamanho da página (default: 20)
            - `sort` - Ordenação (ex: registrationDate,desc)

            **Exemplos:**
            ```
            /api/registrations?status=CONFIRMED
            /api/registrations?event=10&status=PENDING
            /api/registrations?user=742f58ea-5bc1-4bb5-84dc-5ea463d15044
            /api/registrations?sort=registrationDate,desc&size=50
            ```
            """)
    public Page<RegistrationListDTO> list(
            @RequestParam(required = false) Registration.RegistrationStatus status,
            @RequestParam(value = "event", required = false) Long eventId,
            @RequestParam(value = "user", required = false) UUID userId,
            Pageable pageable) {
        return service.listWithFilters(status, eventId, userId, pageable);
    }

    @GetMapping("/my-registrations")
    @Operation(summary = "Minhas inscrições", description = "Retorna inscrições do usuário logado")
    public List<MyRegistrationResponse> getMyRegistrations(Authentication authentication) {
        String currentUsername = authentication.getName();
        User currentUser = userService.findByUsername(currentUsername);
        List<Registration> registrations = service.findByUserId(currentUser.getId());
        return mapperService.toMyRegistrationResponse(registrations);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar inscrição por ID")
    public Registration get(@PathVariable Long id) {
        return service.get(id);
    }

    @GetMapping("/event/{eventId}")
    @Operation(summary = "Listar inscrições de um evento")
    public List<Registration> getByEventId(@PathVariable Long eventId) {
        return service.getByEventId(eventId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Criar nova inscrição")
    public Registration create(@RequestBody @Valid Registration payload) {
        return service.create(payload);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar inscrição")
    public Registration update(@PathVariable Long id, @RequestBody @Valid Registration payload) {
        return service.update(id, payload);
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Atualizar status da inscrição")
    public Registration updateStatus(@PathVariable Long id,
            @RequestParam Registration.RegistrationStatus status) {
        return service.updateStatus(id, status);
    }

    @PatchMapping("/{id}/cancel")
    @Operation(summary = "Cancelar inscrição")
    public void cancelRegistration(@PathVariable Long id) {
        service.cancelRegistration(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}