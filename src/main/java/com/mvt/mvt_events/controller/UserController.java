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

import java.util.List;
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
    @Operation(summary = "Listar usuários", description = "Suporta filtros: role, organizationId, enabled")
    @Transactional(readOnly = true)
    public Page<UserResponse> list(
            @RequestParam(required = false) User.Role role,
            @RequestParam(required = false) Long organizationId,
            @RequestParam(required = false) Boolean enabled,
            Pageable pageable) {
        Page<User> users = userService.listWithFilters(role, organizationId, enabled, pageable);
        return users.map(UserResponse::new);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar usuário por ID")
    public UserResponse get(@PathVariable UUID id) {
        User user = userService.findById(id);
        return new UserResponse(user);
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

    // DTO para atualização de usuário
    @Data
    @NoArgsConstructor
    public static class UserUpdateRequest {
        private String name;
        private String phone;
        private String address;
        private String city;
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
        private String city;
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
            this.city = user.getCity();
            this.state = user.getState();
            this.country = user.getCountry();
            this.dateOfBirth = user.getDateOfBirth() != null ? user.getDateOfBirth().toString() : null;
            this.gender = user.getGender() != null ? user.getGender().toString() : null;
            this.cpf = user.getCpfFormatted();
            this.emergencyContact = user.getEmergencyContact();
            this.role = user.getRole() != null ? user.getRole().toString() : null;

            // Evitar lazy loading da organização
            if (user.getOrganization() != null) {
                this.organizationId = user.getOrganization().getId();
                this.organizationName = user.getOrganization().getName();
            }
        }
    }
}