package com.mvt.mvt_events.service;

import com.mvt.mvt_events.controller.UserController.UserCreateRequest;
import com.mvt.mvt_events.controller.UserController.UserUpdateRequest;
import com.mvt.mvt_events.jpa.City;
import com.mvt.mvt_events.jpa.Organization;
import com.mvt.mvt_events.jpa.User;
import com.mvt.mvt_events.repository.CityRepository;
import com.mvt.mvt_events.repository.OrganizationRepository;
import com.mvt.mvt_events.repository.UserRepository;
import com.mvt.mvt_events.specification.UserSpecification;
import com.mvt.mvt_events.util.CPFUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CityRepository cityRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private com.mvt.mvt_events.repository.EmploymentContractRepository employmentContractRepository;

    @Autowired
    private com.mvt.mvt_events.repository.ClientContractRepository clientContractRepository;

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
            String search,
            Pageable pageable) {
        Specification<User> spec = UserSpecification.withFilters(role, organizationId, enabled, search);
        Page<User> users = userRepository.findAll(spec, pageable);

        // Force load das organizations e cities para evitar lazy loading
        users.getContent().forEach(user -> {
            if (user.getOrganization() != null) {
                user.getOrganization().getName(); // Trigger lazy loading
            }
            if (user.getCity() != null) {
                user.getCity().getName(); // Trigger lazy loading
            }
        });

        return users;
    }

    public User findById(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        // Force load da organization e city para evitar lazy loading
        if (user.getOrganization() != null) {
            user.getOrganization().getName(); // Trigger lazy loading
        }
        if (user.getCity() != null) {
            user.getCity().getName(); // Trigger lazy loading
        }

        return user;
    }

    public User createUser(UserCreateRequest request, Authentication authentication) {
        // Verificar se usuário logado é admin
        String currentUsername = authentication.getName();
        User currentUser = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new RuntimeException("Usuário logado não encontrado"));

        if (!currentUser.isAdmin()) {
            throw new RuntimeException("Apenas administradores podem criar usuários");
        }

        // Verificar se email já existe
        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new RuntimeException("Email já cadastrado: " + request.getUsername());
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setName(request.getName());

        // Role
        User.Role role;
        try {
            role = User.Role.valueOf(request.getRole().toUpperCase());
            user.setRole(role);
        } catch (Exception e) {
            throw new RuntimeException("Role inválida: " + request.getRole());
        }

        // Senha padrão
        String password;
        if (request.getPassword() != null && !request.getPassword().trim().isEmpty()) {
            password = request.getPassword();
        } else {
            // Senha padrão para todos os roles
            password = "senha123";
        }
        user.setPassword(passwordEncoder.encode(password));

        // CPF
        if (request.getCpf() != null && !request.getCpf().trim().isEmpty()) {
            String cpf = CPFUtil.clean(request.getCpf().trim());
            if (CPFUtil.isValid(cpf)) {
                user.setCpf(cpf);
            } else {
                throw new RuntimeException("CPF inválido");
            }
        }

        // Campos opcionais
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone().trim());
        }
        if (request.getAddress() != null) {
            user.setAddress(request.getAddress().trim());
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

        // City - aceita tanto cityId quanto city.id
        Long cityIdResolved = request.getCityIdResolved();
        if (cityIdResolved != null) {
            City city = cityRepository.findById(cityIdResolved)
                    .orElseThrow(() -> new RuntimeException("Cidade não encontrada"));
            user.setCity(city);
        }

        // Data de nascimento
        if (request.getDateOfBirth() != null && !request.getDateOfBirth().trim().isEmpty()) {
            try {
                // Tenta parsear como ISO 8601 com timezone (ex: "2025-10-20T03:00:00.000Z")
                if (request.getDateOfBirth().contains("T")) {
                    ZonedDateTime zdt = ZonedDateTime.parse(request.getDateOfBirth());
                    user.setDateOfBirth(zdt.toLocalDate());
                } else {
                    // Formato simples: YYYY-MM-DD
                    LocalDate birthDate = LocalDate.parse(request.getDateOfBirth(), DateTimeFormatter.ISO_LOCAL_DATE);
                    user.setDateOfBirth(birthDate);
                }
            } catch (Exception e) {
                throw new RuntimeException("Data de nascimento inválida. Use o formato YYYY-MM-DD ou ISO 8601");
            }
        }

        // Gender
        if (request.getGender() != null && !request.getGender().trim().isEmpty()) {
            try {
                User.Gender gender = User.Gender.valueOf(request.getGender().toUpperCase());
                user.setGender(gender);
            } catch (Exception e) {
                throw new RuntimeException("Gênero inválido: " + request.getGender());
            }
        }

        // Organization - aceita tanto organizationId quanto organization.id
        Long organizationIdResolved = request.getOrganizationIdResolved();
        if (organizationIdResolved != null) {
            Organization organization = organizationRepository.findById(organizationIdResolved)
                    .orElseThrow(() -> new RuntimeException("Organização não encontrada"));
            user.setOrganization(organization);
        }

        // Enabled
        if (request.getEnabled() != null) {
            user.setEnabled(request.getEnabled());
        }

        User savedUser = userRepository.save(user);

        // Force load para evitar lazy loading
        if (savedUser.getOrganization() != null) {
            savedUser.getOrganization().getName();
        }
        if (savedUser.getCity() != null) {
            savedUser.getCity().getName();
        }

        return savedUser;
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

        if (request.getCityId() != null) {
            City city = cityRepository.findById(request.getCityId())
                    .orElseThrow(() -> new RuntimeException("Cidade não encontrada"));
            user.setCity(city);
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

        // Force load da organization e city para evitar lazy loading
        if (savedUser.getOrganization() != null) {
            savedUser.getOrganization().getName(); // Trigger lazy loading
        }
        if (savedUser.getCity() != null) {
            savedUser.getCity().getName(); // Trigger lazy loading
        }

        return savedUser;
    }

    /**
     * Atualizar localização (latitude/longitude) do usuário
     */
    public User updateUserLocation(UUID id, Double latitude, Double longitude, String updatedAtString,
            Authentication authentication) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        // Verificar permissões: usuário só pode atualizar própria localização, ou admin
        // pode atualizar qualquer um
        String currentUsername = authentication.getName();
        User currentUser = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new RuntimeException("Usuário logado não encontrado"));

        if (!currentUser.isAdmin() && !user.getUsername().equals(currentUsername)) {
            throw new RuntimeException("Você só pode atualizar sua própria localização");
        }

        // Atualizar localização
        user.setLatitude(latitude);
        user.setLongitude(longitude);

        // Atualizar timestamp - se fornecido pelo GPS, usar esse; senão usar timestamp
        // atual
        if (updatedAtString != null && !updatedAtString.trim().isEmpty()) {
            try {
                // Tentar parsear diferentes formatos de data
                LocalDateTime gpsTimestamp;

                if (updatedAtString.contains("T")) {
                    // Formato ISO com T (ex: "2025-10-31T15:30:45.123Z" ou "2025-10-31T15:30:45")
                    if (updatedAtString.endsWith("Z")) {
                        gpsTimestamp = LocalDateTime.parse(updatedAtString.substring(0, updatedAtString.length() - 1));
                    } else {
                        gpsTimestamp = LocalDateTime.parse(updatedAtString);
                    }
                } else {
                    // Formato simples (ex: "2025-10-31 15:30:45")
                    gpsTimestamp = LocalDateTime.parse(updatedAtString.replace(" ", "T"));
                }

                user.setUpdatedAt(gpsTimestamp);
            } catch (Exception e) {
                // Se não conseguir parsear, usar timestamp atual e logar warning
                System.err
                        .println("Erro ao parsear timestamp do GPS: " + updatedAtString + ". Usando timestamp atual.");
                user.setUpdatedAt(LocalDateTime.now());
            }
        } else {
            // Se não fornecido, usar timestamp atual
            user.setUpdatedAt(LocalDateTime.now());
        }

        User savedUser = userRepository.save(user);

        // Force load da organization e city para evitar lazy loading
        if (savedUser.getOrganization() != null) {
            savedUser.getOrganization().getName(); // Trigger lazy loading
        }
        if (savedUser.getCity() != null) {
            savedUser.getCity().getName(); // Trigger lazy loading
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

    // ============================================================================
    // MÉTODOS PARA CONTRATOS (sem lazy loading)
    // ============================================================================

    /**
     * Busca dados dos Employment Contracts de um COURIER SEM carregar objetos
     * completos
     * Retorna: [organization_id, organization_name, linked_at, is_active]
     */
    @Transactional(readOnly = true)
    public java.util.List<Object[]> getEmploymentContractsForUser(UUID userId) {
        return employmentContractRepository.findContractDataByCourierId(userId);
    }

    /**
     * Busca dados dos Service Contracts de um CLIENT SEM carregar objetos completos
     * Retorna: [organization_id, organization_name, contract_number, is_primary,
     * status, contract_date, start_date, end_date]
     */
    @Transactional(readOnly = true)
    public java.util.List<Object[]> getServiceContractsForUser(UUID userId) {
        return clientContractRepository.findContractDataByClientId(userId);
    }
}