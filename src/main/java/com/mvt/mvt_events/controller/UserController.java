package com.mvt.mvt_events.controller;

import com.mvt.mvt_events.jpa.User;
import com.mvt.mvt_events.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "*")
@Tag(name = "Usuários", description = "Gerenciamento de usuários")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    @Autowired
    private UserService userService;

    @GetMapping
    @Operation(summary = "Listar usuários", description = "Suporta filtros: role, organizationId, enabled, search (nome ou email)")
    @Transactional(readOnly = true)
    public Page<UserResponse> list(
            @RequestParam(required = false) User.Role role,
            @RequestParam(required = false) Long organizationId,
            @RequestParam(required = false) Boolean enabled,
            @RequestParam(required = false) String search,
            Pageable pageable) {
        Page<User> users = userService.listWithFilters(role, organizationId, enabled, search, pageable);
        return users.map(UserResponse::new);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar usuário por ID")
    public UserResponse get(@PathVariable UUID id) {
        User user = userService.findById(id);
        return new UserResponse(user);
    }

    @PostMapping
    @Operation(summary = "Criar novo usuário")
    public ResponseEntity<UserResponse> create(@RequestBody @Valid UserCreateRequest request,
            Authentication authentication) {
        User createdUser = userService.createUser(request, authentication);
        return ResponseEntity.ok(new UserResponse(createdUser));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar usuário")
    public ResponseEntity<UserResponse> update(@PathVariable UUID id, @RequestBody @Valid UserUpdateRequest request,
            Authentication authentication) {
        User updatedUser = userService.updateUser(id, request, authentication);
        return ResponseEntity.ok(new UserResponse(updatedUser));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Excluir usuário")
    public ResponseEntity<Void> delete(@PathVariable UUID id, Authentication authentication) {
        try {
            userService.deleteUser(id, authentication);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // DTO para criação de usuário
    @Data
    @NoArgsConstructor
    public static class UserCreateRequest {
        private String username; // email (obrigatório)
        private String name; // nome completo (obrigatório)
        private String password; // senha (opcional - padrão: "12345678" para ADM, "senha123" para outros)
        private String role; // USER, COURIER, CLIENT, ADM, ADMIN (obrigatório)
        private String cpf; // CPF (obrigatório)
        private String phone;
        private String address;
        private Long cityId; // ID da cidade relacionada (aceita direto)
        private CityIdWrapper city; // Aceita também {"id": 1058}
        private String state;
        private String country;
        private String dateOfBirth; // ISO format: YYYY-MM-DD ou YYYY-MM-DDTHH:mm:ss.sssZ
        private String gender; // MALE, FEMALE, OTHER
        private String emergencyContact;
        private Long organizationId; // ID da organização (opcional, aceita direto)
        private OrganizationIdWrapper organization; // Aceita também {"id": 6}
        private Boolean enabled = true; // default true

        // Método helper para obter o cityId de qualquer formato
        public Long getCityIdResolved() {
            if (cityId != null)
                return cityId;
            if (city != null && city.getId() != null)
                return city.getId();
            return null;
        }

        // Método helper para obter o organizationId de qualquer formato
        public Long getOrganizationIdResolved() {
            if (organizationId != null)
                return organizationId;
            if (organization != null && organization.getId() != null)
                return organization.getId();
            return null;
        }
    }

    // Wrapper para aceitar {"id": valor}
    @Data
    @NoArgsConstructor
    public static class CityIdWrapper {
        private Long id;
    }

    @Data
    @NoArgsConstructor
    public static class OrganizationIdWrapper {
        private Long id;
    }

    // DTO para atualização de usuário
    @Data
    @NoArgsConstructor
    public static class UserUpdateRequest {
        private String name;
        private String phone;
        private String address;
        private Long cityId; // ID da cidade relacionada
        private String state;
        private String country;
        private String birthDate; // Mapeia para "birthDate" do front-end
        private String gender; // M/F/OTHER
        private String cpf; // Mapeia para "cpf" do front-end
        private String emergencyContact;
    }

    // DTO para resposta (evita problemas de lazy loading)
    @Data
    @NoArgsConstructor
    public static class UserResponse {
        private UUID id;
        private String username;
        private String name;
        private String phone;
        private String address;
        private Long cityId;
        private String cityName;
        private String cityState;
        private String state;
        private String country;
        private String dateOfBirth;
        private String gender;
        private String cpf;
        private String emergencyContact;
        private String role;
        private Long organizationId;
        private String organizationName;

        public UserResponse(User user) {
            this.id = user.getId();
            this.username = user.getUsername();
            this.name = user.getName();
            this.phone = user.getPhone();
            this.address = user.getAddress();
            this.state = user.getState();
            this.country = user.getCountry();
            this.dateOfBirth = user.getDateOfBirth() != null ? user.getDateOfBirth().toString() : null;
            this.gender = user.getGender() != null ? user.getGender().toString() : null;
            this.cpf = user.getCpfFormatted();
            this.emergencyContact = user.getEmergencyContact();
            this.role = user.getRole() != null ? user.getRole().toString() : null;

            // Carregar dados da cidade se existir
            if (user.getCity() != null) {
                this.cityId = user.getCity().getId();
                this.cityName = user.getCity().getName();
                this.cityState = user.getCity().getState();
            }

            // Evitar lazy loading da organização
            if (user.getOrganization() != null) {
                this.organizationId = user.getOrganization().getId();
                this.organizationName = user.getOrganization().getName();
            }
        }
    }
}