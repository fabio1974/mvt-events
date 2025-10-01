package com.mvt.mvt_events.controller;

import com.mvt.mvt_events.jpa.User;
import com.mvt.mvt_events.service.UserService;
import jakarta.validation.Valid;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "*")
public class UserController {

    @Autowired
    private UserService userService;

    @GetMapping
    public List<User> list() {
        return userService.findAll();
    }

    @GetMapping("/{id}")
    public UserResponse get(@PathVariable UUID id) {
        User user = userService.findById(id);
        return new UserResponse(user);
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserResponse> update(@PathVariable UUID id, @RequestBody @Valid UserUpdateRequest request,
            Authentication authentication) {
        try {
            User updatedUser = userService.updateUser(id, request, authentication);
            return ResponseEntity.ok(new UserResponse(updatedUser));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/{id}")
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
        private String documentNumber;
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
            this.documentNumber = user.getDocumentNumber();
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