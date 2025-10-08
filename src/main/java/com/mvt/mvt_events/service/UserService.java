package com.mvt.mvt_events.service;

import com.mvt.mvt_events.controller.UserController.UserUpdateRequest;
import com.mvt.mvt_events.jpa.User;
import com.mvt.mvt_events.repository.UserRepository;
import com.mvt.mvt_events.specification.UserSpecification;
import com.mvt.mvt_events.util.CPFUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class UserService {

    @Autowired
    private UserRepository userRepository;

    public List<User> findAll() {
        return userRepository.findAll();
    }

    /**
     * Lista usuários com filtros dinâmicos
     */
    @Transactional(readOnly = true)
    public Page<User> listWithFilters(
            User.Role role,
            Long organizationId,
            Boolean enabled,
            Pageable pageable) {
        Specification<User> spec = UserSpecification.withFilters(role, organizationId, enabled);
        Page<User> users = userRepository.findAll(spec, pageable);

        // Force load das organizations para evitar lazy loading
        users.getContent().forEach(user -> {
            if (user.getOrganization() != null) {
                user.getOrganization().getName(); // Trigger lazy loading
            }
        });

        return users;
    }

    public User findById(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        // Force load da organization para evitar lazy loading
        if (user.getOrganization() != null) {
            user.getOrganization().getName(); // Trigger lazy loading
        }

        return user;
    }

    public User updateUser(UUID id, UserUpdateRequest request, Authentication authentication) {
        // Verificar se o usuário logado pode atualizar este perfil
        String currentUsername = authentication.getName();
        User currentUser = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new RuntimeException("Usuário logado não encontrado"));

        // Verificar se é o próprio usuário ou se é admin
        if (!currentUser.getId().equals(id) && !currentUser.isAdmin()) {
            throw new RuntimeException("Não autorizado a atualizar este usuário");
        }

        User user = findById(id);

        // Atualizar campos básicos
        if (request.getName() != null && !request.getName().trim().isEmpty()) {
            user.setName(request.getName().trim());
        }

        if (request.getPhone() != null) {
            user.setPhone(request.getPhone().trim());
        }

        if (request.getAddress() != null) {
            user.setAddress(request.getAddress().trim());
        }

        if (request.getCity() != null) {
            user.setCity(request.getCity().trim());
        }

        if (request.getState() != null) {
            user.setState(request.getState().trim());
        }

        if (request.getCountry() != null) {
            user.setCountry(request.getCountry().trim());
        }

        if (request.getEmergencyContact() != null) {
            user.setEmergencyContact(request.getEmergencyContact().trim());
        }

        // Atualizar data de nascimento
        if (request.getBirthDate() != null && !request.getBirthDate().trim().isEmpty()) {
            try {
                LocalDate birthDate = LocalDate.parse(request.getBirthDate(), DateTimeFormatter.ISO_LOCAL_DATE);
                user.setDateOfBirth(birthDate);
            } catch (Exception e) {
                throw new RuntimeException("Data de nascimento inválida. Use o formato YYYY-MM-DD");
            }
        }

        // Atualizar gênero
        if (request.getGender() != null && !request.getGender().trim().isEmpty()) {
            try {
                String gender = request.getGender().trim().toUpperCase();
                // Mapear valores do front-end
                switch (gender) {
                    case "M":
                    case "MALE":
                        user.setGender(User.Gender.MALE);
                        break;
                    case "F":
                    case "FEMALE":
                        user.setGender(User.Gender.FEMALE);
                        break;
                    case "OTHER":
                        user.setGender(User.Gender.OTHER);
                        break;
                    default:
                        throw new RuntimeException("Gênero inválido: " + request.getGender());
                }
            } catch (Exception e) {
                throw new RuntimeException("Gênero inválido: " + request.getGender());
            }
        }

        // Atualizar documento (CPF)
        if (request.getCpf() != null && !request.getCpf().trim().isEmpty()) {
            String cpf = request.getCpf().trim();
            // Clean CPF (remove formatting) and validate
            cpf = CPFUtil.clean(cpf);
            if (CPFUtil.isValid(cpf)) {
                user.setCpf(cpf);
            } else {
                throw new RuntimeException("CPF inválido");
            }
        }

        User savedUser = userRepository.save(user);

        // Force load da organization para evitar lazy loading
        if (savedUser.getOrganization() != null) {
            savedUser.getOrganization().getName(); // Trigger lazy loading
        }

        return savedUser;
    }

    public void deleteUser(UUID id, Authentication authentication) {
        // Verificar se o usuário logado pode deletar este perfil
        String currentUsername = authentication.getName();
        User currentUser = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new RuntimeException("Usuário logado não encontrado"));

        // Apenas admin pode deletar usuários
        if (!currentUser.isAdmin()) {
            throw new RuntimeException("Apenas administradores podem deletar usuários");
        }

        User user = findById(id);
        userRepository.delete(user);
    }

    public User findByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));
    }
}