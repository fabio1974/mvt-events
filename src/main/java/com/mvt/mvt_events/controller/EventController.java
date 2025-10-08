package com.mvt.mvt_events.controller;

import com.mvt.mvt_events.dto.EventCreateRequest;
import com.mvt.mvt_events.dto.EventUpdateRequest;
import com.mvt.mvt_events.jpa.Event;
import com.mvt.mvt_events.service.EventService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/events")
@Tag(name = "Eventos", description = "Gerenciamento de eventos esportivos")
public class EventController {

    private final EventService service;

    public EventController(EventService service) {
        this.service = service;
    }

    @GetMapping("/stats")
    @Operation(summary = "Estatísticas de eventos")
    @SecurityRequirement(name = "bearerAuth")
    public Object getStats() {
        return service.getStats();
    }

    @GetMapping
    @Operation(summary = "Listar eventos (paginado)", description = "Suporta filtros: status, organizationId, categoryId, city, state")
    @SecurityRequirement(name = "bearerAuth")
    public Page<Event> list(
            @RequestParam(required = false) Event.EventStatus status,
            @RequestParam(required = false) Long organizationId,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String state,
            Pageable pageable) {

        // Se houver algum filtro, usa o método com filtros
        if (status != null || organizationId != null || categoryId != null ||
                city != null || state != null) {
            return service.listWithFilters(status, organizationId, categoryId, city, state, pageable);
        }

        return service.list(pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar evento por ID")
    @SecurityRequirement(name = "bearerAuth")
    public Event get(@PathVariable Long id) {
        return service.get(id);
    }

    @GetMapping("/organization/{organizationId}")
    @Operation(summary = "Listar eventos por organização")
    @SecurityRequirement(name = "bearerAuth")
    public List<Event> getByOrganizationId(@PathVariable Long organizationId) {
        return service.findByOrganizationId(organizationId);
    }

    @GetMapping("/public")
    @Operation(summary = "Listar eventos públicos", description = "Acesso sem autenticação")
    public List<Event> getPublicEvents() {
        return service.findPublishedEvents();
    }

    @GetMapping("/public/{id}")
    @Operation(summary = "Buscar evento público por ID", description = "Acesso sem autenticação")
    public Event getPublicEventById(@PathVariable Long id) {
        return service.findPublishedEventById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Criar novo evento")
    @SecurityRequirement(name = "bearerAuth")
    public Event create(@RequestBody @Valid EventCreateRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar evento")
    @SecurityRequirement(name = "bearerAuth")
    public Event update(@PathVariable Long id, @RequestBody @Valid EventUpdateRequest request) {
        return service.updateWithCategories(id, request);
    }

    @PutMapping("/{id}/legacy")
    @Operation(summary = "Atualizar evento (legacy)", description = "Endpoint legado para compatibilidade")
    @SecurityRequirement(name = "bearerAuth")
    public Event updateLegacy(@PathVariable Long id, @RequestBody @Valid Event payload) {
        return service.update(id, payload);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Excluir evento")
    @SecurityRequirement(name = "bearerAuth")
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}