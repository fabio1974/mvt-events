package com.mvt.mvt_events.controller;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.mvt.mvt_events.common.JwtUtil;
import com.mvt.mvt_events.exception.EmailAlreadyExistsException;
import com.mvt.mvt_events.jpa.User;
import com.mvt.mvt_events.jpa.Organization;
import com.mvt.mvt_events.repository.UserRepository;
import com.mvt.mvt_events.repository.OrganizationRepository;
import com.mvt.mvt_events.service.EmailService;
import com.mvt.mvt_events.validation.CPF;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping({"/api/auth", "/auth"})
@Tag(name = "Autenticação", description = "Login, registro e gerenciamento de usuários")
public class AuthController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private EmailService emailService;

    @Value("${app.frontend.url:http://localhost:5173}")
    private String frontendUrl;

    @PostMapping("/login")
    @Operation(summary = "Login de usuário", description = "Autenticação via email ou CPF e senha, retorna token JWT")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
        try {
            // Primeiro verificar se o usuário existe e está confirmado
            Optional<User> userOpt = userRepository.findByUsername(loginRequest.getUsername());
            if (userOpt.isEmpty()) {
                // Tentar buscar por CPF
                userOpt = userRepository.findByDocumentNumberForAuth(loginRequest.getUsername());
            }
            
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                // Verificar se o email foi confirmado
                if (!user.isConfirmed()) {
                    return ResponseEntity.status(403).body(Map.of(
                        "error", "EMAIL_NOT_CONFIRMED",
                        "message", "Por favor, confirme seu email antes de fazer login. Verifique sua caixa de entrada.",
                        "username", user.getUsername()
                    ));
                }
            }

            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword()));

            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            String token = jwtUtil.generateToken(userDetails);

            Map<String, Object> response = new HashMap<>();
            response.put("token", token);
            response.put("type", "Bearer");
            response.put("username", userDetails.getUsername());

            // Add user data from token
            Map<String, Object> userData = jwtUtil.getUserDataFromToken(token);
            response.put("user", userData);

            return ResponseEntity.ok(response);
        } catch (BadCredentialsException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "INVALID_CREDENTIALS", "message", "Email/CPF ou senha inválidos"));
        }
    }

    @PostMapping("/register")
    @Operation(summary = "Registrar novo usuário", description = "Criação de conta (acesso público). Envia email de confirmação.")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest registerRequest) {
        if (userRepository.findByUsername(registerRequest.getUsername()).isPresent()) {
            throw new EmailAlreadyExistsException(registerRequest.getUsername(), "Email");
        }

        User user = new User();
        user.setUsername(registerRequest.getUsername()); // usar email como username
        user.setPassword(passwordEncoder.encode(registerRequest.getPassword()));
        user.setName(registerRequest.getName()); // Set the name field
        user.setRole(User.Role.valueOf(registerRequest.getRole().toUpperCase()));

        // Set CPF if provided
        if (registerRequest.getDocumentNumber() != null && !registerRequest.getDocumentNumber().trim().isEmpty()) {
            user.setDocumentNumber(registerRequest.getDocumentNumber());
        }

        // Gerar token de confirmação de email
        String confirmationToken = UUID.randomUUID().toString();
        user.setConfirmationToken(confirmationToken);
        user.setConfirmationTokenExpiresAt(LocalDateTime.now().plusHours(24)); // Expira em 24h
        user.setConfirmed(false); // Não confirmado até clicar no link

        userRepository.save(user);

        // Enviar email de confirmação (async)
        emailService.sendConfirmationEmail(user);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Cadastro realizado! Verifique seu email para confirmar a conta.");
        response.put("username", user.getUsername());
        response.put("role", user.getRole().toString());
        response.put("confirmationRequired", true);

        return ResponseEntity.ok(response);
    }

    /**
     * Confirma o email do usuário através do token enviado por email.
     * Pode ser acessado diretamente (redirect) ou via frontend.
     */
    @GetMapping("/confirm")
    @Operation(summary = "Confirmar email", description = "Confirma o email do usuário através do token enviado")
    public ResponseEntity<?> confirmEmail(@RequestParam String token) {
        Optional<User> userOpt = userRepository.findByConfirmationToken(token);
        
        if (userOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "INVALID_TOKEN",
                "message", "Token de confirmação inválido ou já utilizado"
            ));
        }

        User user = userOpt.get();

        // Verificar se token expirou
        if (user.getConfirmationTokenExpiresAt() != null && 
            user.getConfirmationTokenExpiresAt().isBefore(LocalDateTime.now())) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "TOKEN_EXPIRED",
                "message", "Token de confirmação expirado. Solicite um novo email de confirmação.",
                "username", user.getUsername()
            ));
        }

        // Confirmar email
        user.setConfirmed(true);
        user.setConfirmationToken(null); // Limpar token após uso
        user.setConfirmationTokenExpiresAt(null);
        userRepository.save(user);

        // Redirecionar para o frontend ou retornar JSON
        return ResponseEntity.ok(Map.of(
            "success", true,
            "message", "Email confirmado com sucesso! Você já pode fazer login.",
            "username", user.getUsername()
        ));
    }

    /**
     * Reenvia email de confirmação para usuários que não receberam ou cujo token expirou.
     */
    @PostMapping("/resend-confirmation")
    @Operation(summary = "Reenviar email de confirmação", description = "Reenvia o email de confirmação para o usuário")
    public ResponseEntity<?> resendConfirmation(@RequestBody Map<String, String> request) {
        String username = request.get("username");
        if (username == null || username.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "MISSING_USERNAME",
                "message", "Email é obrigatório"
            ));
        }

        Optional<User> userOpt = userRepository.findByUsername(username.trim());
        if (userOpt.isEmpty()) {
            // Não revelar se o email existe ou não (segurança)
            return ResponseEntity.ok(Map.of(
                "message", "Se o email estiver cadastrado, você receberá um novo link de confirmação."
            ));
        }

        User user = userOpt.get();

        // Se já confirmado, não reenviar
        if (user.isConfirmed()) {
            return ResponseEntity.ok(Map.of(
                "message", "Este email já foi confirmado. Você pode fazer login."
            ));
        }

        // Gerar novo token
        String newToken = UUID.randomUUID().toString();
        user.setConfirmationToken(newToken);
        user.setConfirmationTokenExpiresAt(LocalDateTime.now().plusHours(24));
        userRepository.save(user);

        // Enviar email
        emailService.sendConfirmationEmail(user);

        return ResponseEntity.ok(Map.of(
            "message", "Email de confirmação reenviado! Verifique sua caixa de entrada."
        ));
    }

    @GetMapping("/validate")
    @Operation(summary = "Valida o token JWT e retorna informações básicas do usuário")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<?> validateToken(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).body(Map.of("valid", false, "message", "Token inválido ou expirado"));
        }
        
        String username = authentication.getName();
        User user = userRepository.findByUsername(username)
                .orElse(null);
                
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("valid", false, "message", "Usuário não encontrado"));
        }
        
        // Find organization where user is owner
        Long organizationId = null;
        String organizationName = null;
        Optional<com.mvt.mvt_events.jpa.Organization> orgOpt = organizationRepository.findByOwner(user);
        if (orgOpt.isPresent()) {
            organizationId = orgOpt.get().getId();
            organizationName = orgOpt.get().getName();
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("valid", true);
        response.put("userId", user.getId());
        response.put("username", user.getUsername());
        response.put("name", user.getName());
        response.put("role", user.getRole());
        response.put("organizationId", organizationId);
        response.put("organizationName", organizationName);
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    @Operation(summary = "Dados do usuário autenticado")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<?> getCurrentUser(Authentication authentication) {
        Map<String, Object> response = new HashMap<>();
        response.put("username", authentication.getName());
        response.put("authorities", authentication.getAuthorities());
        response.put("authenticated", authentication.isAuthenticated());
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/user-info")
    @Operation(summary = "Informações completas do usuário autenticado")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<?> getUserInfo(Authentication authentication) {
        String username = authentication.getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Find organization where user is owner
        Long organizationId = null;
        String organizationName = null;
        Optional<Organization> orgOpt = organizationRepository.findByOwner(user);
        if (orgOpt.isPresent()) {
            organizationId = orgOpt.get().getId();
            organizationName = orgOpt.get().getName();
        }

        Map<String, Object> response = new HashMap<>();
        response.put("id", user.getId());
        response.put("username", user.getUsername());
        response.put("name", user.getName());
        response.put("role", user.getRole());
        response.put("organizationId", organizationId);
        response.put("organizationName", organizationName);

        return ResponseEntity.ok(response);
    }

    @PutMapping("/users/{userId}/organization")
    public ResponseEntity<?> updateUserOrganization(@PathVariable String userId,
            @RequestBody UpdateOrganizationRequest request) {
        try {
            java.util.UUID userUuid = java.util.UUID.fromString(userId);
            User user = userRepository.findById(userUuid)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // Update organization owner
            Long organizationId = null;
            String organizationName = null;
            
            if (request.getOrganizationId() != null) {
                Organization organization = organizationRepository.findById(request.getOrganizationId())
                        .orElseThrow(() -> new RuntimeException("Organization not found"));
                organization.setOwner(user);
                organizationRepository.save(organization);
                organizationId = organization.getId();
                organizationName = organization.getName();
            } else {
                // Remove ownership - find organization where user is owner and set owner to null
                Optional<Organization> orgOpt = organizationRepository.findByOwner(user);
                if (orgOpt.isPresent()) {
                    Organization org = orgOpt.get();
                    org.setOwner(null);
                    organizationRepository.save(org);
                }
            }

            Map<String, Object> response = new HashMap<>();
            response.put("message", "User organization updated successfully");
            response.put("userId", user.getId());
            response.put("organizationId", organizationId);
            response.put("organizationName", organizationName);

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("Invalid user ID format");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/users/email/{email}/organization")
    public ResponseEntity<?> updateUserOrganizationByEmail(@PathVariable String email,
            @RequestBody UpdateOrganizationRequest request) {
        try {
            User user = userRepository.findByUsername(email)
                    .orElseThrow(() -> new RuntimeException("User not found with email: " + email));

            // Update organization owner
            Long organizationId = null;
            String organizationName = null;
            
            if (request.getOrganizationId() != null) {
                Organization organization = organizationRepository.findById(request.getOrganizationId())
                        .orElseThrow(() -> new RuntimeException("Organization not found"));
                organization.setOwner(user);
                organizationRepository.save(organization);
                organizationId = organization.getId();
                organizationName = organization.getName();
            } else {
                // Remove ownership
                Optional<Organization> orgOpt = organizationRepository.findByOwner(user);
                if (orgOpt.isPresent()) {
                    Organization org = orgOpt.get();
                    org.setOwner(null);
                    organizationRepository.save(org);
                }
            }

            Map<String, Object> response = new HashMap<>();
            response.put("message", "User organization updated successfully");
            response.put("userId", user.getId());
            response.put("username", user.getUsername());
            response.put("organizationId", organizationId);
            response.put("organizationName", organizationName);

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/me/organization")
    public ResponseEntity<?> updateMyOrganization(Authentication authentication,
            @RequestBody UpdateOrganizationRequest request) {
        try {
            String username = authentication.getName();
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // Update organization owner
            Long organizationId = null;
            String organizationName = null;
            
            if (request.getOrganizationId() != null) {
                Organization organization = organizationRepository.findById(request.getOrganizationId())
                        .orElseThrow(() -> new RuntimeException("Organization not found"));
                organization.setOwner(user);
                organizationRepository.save(organization);
                organizationId = organization.getId();
                organizationName = organization.getName();
            } else {
                // Remove ownership
                Optional<Organization> orgOpt = organizationRepository.findByOwner(user);
                if (orgOpt.isPresent()) {
                    Organization org = orgOpt.get();
                    org.setOwner(null);
                    organizationRepository.save(org);
                }
            }

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Your organization updated successfully");
            response.put("userId", user.getId());
            response.put("username", user.getUsername());
            response.put("organizationId", organizationId);
            response.put("organizationName", organizationName);

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(Authentication authentication,
            @RequestBody ChangePasswordRequest request) {
        try {
            String username = authentication.getName();
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // Verify current password
            if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
                return ResponseEntity.badRequest().body("Current password is incorrect");
            }

            // Validate new password
            if (request.getNewPassword() == null || request.getNewPassword().trim().isEmpty()) {
                return ResponseEntity.badRequest().body("New password cannot be empty");
            }

            if (request.getNewPassword().length() < 4) {
                return ResponseEntity.badRequest().body("New password must be at least 4 characters long");
            }

            // Update password
            user.setPassword(passwordEncoder.encode(request.getNewPassword()));
            userRepository.save(user);

            Map<String, String> response = new HashMap<>();
            response.put("message", "Password changed successfully");
            response.put("username", user.getUsername());

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody ResetPasswordRequest request) {
        try {
            // Log the request for debugging
            System.out.println("Reset password request received for: " + request.getUsername());
            System.out.println("New password provided: " + (request.getNewPassword() != null ? "Yes" : "No"));

            // Validate request body
            if (request.getUsername() == null || request.getUsername().trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Username cannot be empty");
            }

            // Validate new password
            if (request.getNewPassword() == null || request.getNewPassword().trim().isEmpty()) {
                return ResponseEntity.badRequest().body("New password cannot be empty");
            }

            if (request.getNewPassword().length() < 4) {
                return ResponseEntity.badRequest().body("New password must be at least 4 characters long");
            }

            // Check if user exists (lightweight check)
            if (!userRepository.existsByUsername(request.getUsername())) {
                return ResponseEntity.badRequest().body("User not found with email: " + request.getUsername());
            }

            // Update password directly (avoiding collection loading issues)
            String encodedPassword = passwordEncoder.encode(request.getNewPassword());
            int updatedRows = userRepository.updatePasswordByUsername(request.getUsername(), encodedPassword);

            if (updatedRows == 0) {
                return ResponseEntity.badRequest().body("Failed to update password");
            }

            Map<String, String> response = new HashMap<>();
            response.put("message", "Password reset successfully");
            response.put("username", request.getUsername());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.out.println("Error in reset password: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    } // DTOs

    public static class LoginRequest {
        private String username;
        private String password;

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }
    }

    public static class RegisterRequest {
        @NotBlank(message = "Nome é obrigatório")
        private String name;

        @NotBlank(message = "Email é obrigatório")
        @Email(message = "Email deve ser válido")
        private String username;

        @NotBlank(message = "Senha é obrigatória")
        @Size(min = 6, message = "Senha deve ter no mínimo 6 caracteres")
        private String password;

        @NotBlank(message = "Role é obrigatório")
        private String role;

        @com.mvt.mvt_events.validation.Document(message = "CPF ou CNPJ inválido", required = true)
        @JsonAlias({"cpf", "cnpj", "document"})
        private String documentNumber;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getRole() {
            return role;
        }

        public void setRole(String role) {
            this.role = role;
        }

        public String getDocumentNumber() {
            return documentNumber;
        }

        public void setDocumentNumber(String documentNumber) {
            this.documentNumber = documentNumber;
        }
    }

    public static class UpdateOrganizationRequest {
        private Long organizationId;

        public Long getOrganizationId() {
            return organizationId;
        }

        public void setOrganizationId(Long organizationId) {
            this.organizationId = organizationId;
        }
    }

    public static class ChangePasswordRequest {
        private String currentPassword;
        private String newPassword;

        public String getCurrentPassword() {
            return currentPassword;
        }

        public void setCurrentPassword(String currentPassword) {
            this.currentPassword = currentPassword;
        }

        public String getNewPassword() {
            return newPassword;
        }

        public void setNewPassword(String newPassword) {
            this.newPassword = newPassword;
        }
    }

    public static class ResetPasswordRequest {
        private String username;
        private String newPassword;

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getNewPassword() {
            return newPassword;
        }

        public void setNewPassword(String newPassword) {
            this.newPassword = newPassword;
        }
    }
}