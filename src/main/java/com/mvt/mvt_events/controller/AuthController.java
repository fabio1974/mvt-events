package com.mvt.mvt_events.controller;

import com.mvt.mvt_events.common.JwtUtil;
import com.mvt.mvt_events.jpa.User;
import com.mvt.mvt_events.jpa.Organization;
import com.mvt.mvt_events.repository.UserRepository;
import com.mvt.mvt_events.repository.OrganizationRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
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

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword()));

            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            String token = jwtUtil.generateToken(userDetails);

            Map<String, String> response = new HashMap<>();
            response.put("token", token);
            response.put("type", "Bearer");
            response.put("username", userDetails.getUsername());

            return ResponseEntity.ok(response);
        } catch (BadCredentialsException e) {
            return ResponseEntity.badRequest().body("Invalid username or password");
        }
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest registerRequest) {
        if (userRepository.findByUsername(registerRequest.getUsername()).isPresent()) {
            return ResponseEntity.badRequest().body("Email is already taken!");
        }

        User user = new User();
        user.setUsername(registerRequest.getUsername()); // usar email como username
        user.setPassword(passwordEncoder.encode(registerRequest.getPassword()));
        user.setName(registerRequest.getName()); // Set the name field
        user.setRole(User.Role.valueOf(registerRequest.getRole().toUpperCase()));

        userRepository.save(user);

        Map<String, String> response = new HashMap<>();
        response.put("message", "User registered successfully!");
        response.put("username", user.getUsername());
        response.put("role", user.getRole().toString());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(Authentication authentication) {
        Map<String, Object> response = new HashMap<>();
        response.put("username", authentication.getName());
        response.put("authorities", authentication.getAuthorities());
        response.put("principal", authentication.getPrincipal());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/user-info")
    public ResponseEntity<?> getUserInfo(Authentication authentication) {
        String username = authentication.getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Map<String, Object> response = new HashMap<>();
        response.put("id", user.getId());
        response.put("username", user.getUsername());
        response.put("name", user.getName());
        response.put("role", user.getRole());
        response.put("organizationId", user.getOrganization() != null ? user.getOrganization().getId() : null);
        response.put("organizationName", user.getOrganization() != null ? user.getOrganization().getName() : null);

        return ResponseEntity.ok(response);
    }

    @PutMapping("/users/{userId}/organization")
    public ResponseEntity<?> updateUserOrganization(@PathVariable String userId,
            @RequestBody UpdateOrganizationRequest request) {
        try {
            java.util.UUID userUuid = java.util.UUID.fromString(userId);
            User user = userRepository.findById(userUuid)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // Update organization
            if (request.getOrganizationId() != null) {
                Organization organization = organizationRepository.findById(request.getOrganizationId())
                        .orElseThrow(() -> new RuntimeException("Organization not found"));
                user.setOrganization(organization);
            } else {
                user.setOrganization(null); // Remove organization
            }

            userRepository.save(user);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "User organization updated successfully");
            response.put("userId", user.getId());
            response.put("organizationId", user.getOrganization() != null ? user.getOrganization().getId() : null);
            response.put("organizationName", user.getOrganization() != null ? user.getOrganization().getName() : null);

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

            // Update organization
            if (request.getOrganizationId() != null) {
                Organization organization = organizationRepository.findById(request.getOrganizationId())
                        .orElseThrow(() -> new RuntimeException("Organization not found"));
                user.setOrganization(organization);
            } else {
                user.setOrganization(null); // Remove organization
            }

            userRepository.save(user);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "User organization updated successfully");
            response.put("userId", user.getId());
            response.put("username", user.getUsername());
            response.put("organizationId", user.getOrganization() != null ? user.getOrganization().getId() : null);
            response.put("organizationName", user.getOrganization() != null ? user.getOrganization().getName() : null);

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

            // Update organization
            if (request.getOrganizationId() != null) {
                Organization organization = organizationRepository.findById(request.getOrganizationId())
                        .orElseThrow(() -> new RuntimeException("Organization not found"));
                user.setOrganization(organization);
            } else {
                user.setOrganization(null); // Remove organization
            }

            userRepository.save(user);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Your organization updated successfully");
            response.put("userId", user.getId());
            response.put("username", user.getUsername());
            response.put("organizationId", user.getOrganization() != null ? user.getOrganization().getId() : null);
            response.put("organizationName", user.getOrganization() != null ? user.getOrganization().getName() : null);

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
            // Find user by email
            User user = userRepository.findByUsername(request.getUsername())
                    .orElseThrow(() -> new RuntimeException("User not found with email: " + request.getUsername()));

            // Validate new password
            if (request.getNewPassword() == null || request.getNewPassword().trim().isEmpty()) {
                return ResponseEntity.badRequest().body("New password cannot be empty");
            }

            if (request.getNewPassword().length() < 4) {
                return ResponseEntity.badRequest().body("New password must be at least 4 characters long");
            }

            // Update password (without requiring current password)
            user.setPassword(passwordEncoder.encode(request.getNewPassword()));
            userRepository.save(user);

            Map<String, String> response = new HashMap<>();
            response.put("message", "Password reset successfully");
            response.put("username", user.getUsername());

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // DTOs
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
        private String name;
        private String username;
        private String password;
        private String role;

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