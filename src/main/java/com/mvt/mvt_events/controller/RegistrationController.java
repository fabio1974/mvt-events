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
    @Operation(summary = "Listar inscrições", description = "Lista paginada com filtros opcionais: status, eventId (ID do evento), userId (UUID do usuário)")
    public Page<RegistrationListDTO> list(
            @RequestParam(required = false) Registration.RegistrationStatus status,
            @RequestParam(required = false) Long eventId,
            @RequestParam(required = false) UUID userId,
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