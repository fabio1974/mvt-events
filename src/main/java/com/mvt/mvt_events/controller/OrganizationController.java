package com.mvt.mvt_events.controller;

import com.mvt.mvt_events.jpa.Organization;
import com.mvt.mvt_events.service.OrganizationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/organizations")
@Tag(name = "Organizações", description = "Gerenciamento de organizações")
@SecurityRequirement(name = "bearerAuth")
public class OrganizationController {

    private final OrganizationService service;

    public OrganizationController(OrganizationService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Listar organizações")
    public List<Organization> list() {
        return service.list();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar organização por ID")
    public Organization get(@PathVariable Long id) {
        return service.get(id);
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Buscar organização do usuário")
    public Organization getByUserId(@PathVariable UUID userId) {
        return service.getByUserId(userId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Organization create(@RequestBody @Valid OrganizationCreateRequest request) {
        return service.createWithUser(request);
    }

    @PutMapping("/{id}")
    public Organization update(@PathVariable Long id, @RequestBody @Valid Organization payload) {
        return service.update(id, payload);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }

    /**
     * DTO for creating organization with user assignment
     */
    @Data
    public static class OrganizationCreateRequest {
        @NotBlank(message = "Nome é obrigatório")
        @Size(max = 255, message = "Nome deve ter no máximo 255 caracteres")
        private String name;

        @Size(max = 100, message = "Slug deve ter no máximo 100 caracteres")
        private String slug;

        @NotBlank(message = "Email de contato é obrigatório")
        @Email(message = "Email de contato deve ser válido")
        private String contactEmail;

        private String phone;
        private String website;
        private String description;
        private String logoUrl;

        // ID do usuário organizador que será vinculado à organização
        private UUID userId;
    }
}