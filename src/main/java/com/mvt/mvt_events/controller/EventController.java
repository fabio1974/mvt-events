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

    @GetMapping("/debug/tenant")
    @Operation(summary = "Debug tenant context")
    @SecurityRequirement(name = "bearerAuth")
    public Object debugTenant() {
        Long tenantId = com.mvt.mvt_events.tenant.TenantContext.getCurrentTenantId();
        Long count = tenantId != null ? service.getRepository().countByOrganizationIdNative(tenantId) : 0;

        return java.util.Map.of(
                "tenantId", tenantId != null ? tenantId : "null",
                "hasTenant", com.mvt.mvt_events.tenant.TenantContext.hasTenant(),
                "eventsCountNative", count,
                "authentication",
                org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication() != null
                        ? org.springframework.security.core.context.SecurityContextHolder
                                .getContext().getAuthentication().getPrincipal().getClass().getName()
                        : "null");
    }

    @GetMapping
    @Operation(summary = "Listar eventos (paginado)", description = """
            Lista eventos com suporte a múltiplos filtros e paginação.

            **Filtros Disponíveis:**
            - `status` - Status do evento (DRAFT, PUBLISHED, CANCELLED, COMPLETED)
            - `organization` ou `organizationId` - ID da organização
            - `category` ou `categoryId` - ID da categoria
            - `city` - Nome da cidade (busca parcial, case-insensitive)
            - `state` - Sigla do estado (ex: SP, RJ, MG)
            - `eventType` - Tipo do evento (RUNNING, CYCLING, SWIMMING, TRIATHLON, WALKING, OTHER)
            - `name` - Nome do evento (busca parcial, case-insensitive)

            **Paginação:**
            - `page` - Número da página (default: 0)
            - `size` - Tamanho da página (default: 20)
            - `sort` - Ordenação (ex: eventDate,desc ou name,asc)

            **Exemplos:**
            ```
            /api/events?eventType=RUNNING&name=maratona
            /api/events?city=São Paulo&state=SP&status=PUBLISHED
            /api/events?organization=5&registrationOpen=true&page=0&size=10
            /api/events?sort=eventDate,desc&size=50
            ```

            **Nota:** Todos os filtros podem ser combinados usando lógica AND.
            """)
    @SecurityRequirement(name = "bearerAuth")
    public Page<Event> list(
            @RequestParam(required = false) Event.EventStatus status,
            @RequestParam(value = "organization", required = false) Long organizationId,
            @RequestParam(value = "category", required = false) Long categoryId,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) Event.EventType eventType,
            @RequestParam(required = false) String name,
            Pageable pageable) {

        // Se houver algum filtro, usa o método com filtros
        if (status != null || organizationId != null || categoryId != null ||
                city != null || state != null || eventType != null || name != null) {
            return service.listWithFilters(status, organizationId, categoryId, city, state, eventType, name, pageable);
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
    public Event create(@RequestBody @Valid Event event) {
        return service.create(event);
    }

    @PostMapping("/with-dto")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Criar novo evento (DTO)", description = "Endpoint com DTO específico para compatibilidade")
    @SecurityRequirement(name = "bearerAuth")
    public Event createWithDto(@RequestBody @Valid EventCreateRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar evento")
    @SecurityRequirement(name = "bearerAuth")
    public Event update(@PathVariable Long id, @RequestBody @Valid Event payload) {
        return service.update(id, payload);
    }

    @PutMapping("/{id}/with-categories")
    @Operation(summary = "Atualizar evento com categorias (DTO)", description = "Endpoint com DTO específico para updates complexos")
    @SecurityRequirement(name = "bearerAuth")
    public Event updateWithCategories(@PathVariable Long id, @RequestBody @Valid EventUpdateRequest request) {
        return service.updateWithCategories(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Excluir evento")
    @SecurityRequirement(name = "bearerAuth")
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}