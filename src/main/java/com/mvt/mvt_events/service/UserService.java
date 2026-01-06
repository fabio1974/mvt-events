package com.mvt.mvt_events.service;

import com.mvt.mvt_events.controller.UserController.UserCreateRequest;
import com.mvt.mvt_events.controller.UserController.UserUpdateRequest;
import com.mvt.mvt_events.jpa.Address;
import com.mvt.mvt_events.jpa.City;
import com.mvt.mvt_events.jpa.User;
import com.mvt.mvt_events.repository.AddressRepository;
import com.mvt.mvt_events.repository.CityRepository;
import com.mvt.mvt_events.repository.UserRepository;
import com.mvt.mvt_events.specification.UserSpecification;
import com.mvt.mvt_events.util.CPFUtil;
import org.hibernate.Hibernate;
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
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CityRepository cityRepository;

    @Autowired
    private AddressRepository addressRepository;

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

        // Force load das cities via address para evitar lazy loading
        users.getContent().forEach(user -> {
            if (user.getAddress() != null && user.getAddress().getCity() != null) {
                user.getAddress().getCity().getName(); // Trigger lazy loading
            }
        });

        return users;
    }

    @Transactional(readOnly = true)
    public User findById(UUID id) {
        // Carrega user com addresses usando query HQL
        User user = userRepository.findByIdWithAddresses(id)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));
        
        // Inicializa contracts manualmente dentro da transação
        Hibernate.initialize(user.getEmploymentContracts());
        Hibernate.initialize(user.getClientContracts());
        
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

        // CPF/CNPJ
        if (request.getDocumentNumber() != null && !request.getDocumentNumber().trim().isEmpty()) {
            String document = request.getDocumentNumber().trim().replaceAll("[^0-9]", "");
            
            // Valida como CPF (11 dígitos) ou CNPJ (14 dígitos)
            boolean isValid = false;
            if (document.length() == 11) {
                isValid = com.mvt.mvt_events.util.CPFUtil.isValid(document);
            } else if (document.length() == 14) {
                isValid = com.mvt.mvt_events.util.CNPJUtil.isValid(document);
            }
            
            if (isValid) {
                user.setDocumentNumber(document);
            } else {
                throw new RuntimeException("CPF ou CNPJ inválido");
            }
        }

        // Campos opcionais
        if (request.getPhoneDdd() != null) {
            user.setPhoneDdd(request.getPhoneDdd().trim());
        }
        if (request.getPhoneNumber() != null) {
            user.setPhoneNumber(request.getPhoneNumber().trim());
        }
        // Note: address is now managed via Address entity, not directly on User

        // City - aceita tanto cityId quanto city.id
        // Agora a cidade está em Address, não diretamente no User
        Long cityIdResolved = request.getCityIdResolved();
        if (cityIdResolved != null) {
            City city = cityRepository.findById(cityIdResolved)
                    .orElseThrow(() -> new RuntimeException("Cidade não encontrada"));
            // A cidade será definida no Address quando ele for criado
            // Por enquanto, não fazemos nada aqui
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

        // Enabled
        if (request.getEnabled() != null) {
            user.setEnabled(request.getEnabled());
        }

        User savedUser = userRepository.save(user);

        // Force load para evitar lazy loading
        if (savedUser.getAddress() != null && savedUser.getAddress().getCity() != null) {
            savedUser.getAddress().getCity().getName();
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

        // Atualizar username (email) se fornecido
        if (request.getUsername() != null && !request.getUsername().trim().isEmpty()) {
            String newUsername = request.getUsername().trim();
            // Verificar se o novo username já existe (exceto se for o mesmo usuário)
            Optional<User> existingUser = userRepository.findByUsername(newUsername);
            if (existingUser.isPresent() && !existingUser.get().getId().equals(id)) {
                throw new RuntimeException("Email já cadastrado no sistema");
            }
            user.setUsername(newUsername);
        }

        // Atualizar campos básicos
        if (request.getName() != null && !request.getName().trim().isEmpty()) {
            user.setName(request.getName().trim());
        }

        if (request.getPhoneDdd() != null) {
            user.setPhoneDdd(request.getPhoneDdd().trim());
        }
        if (request.getPhoneNumber() != null) {
            user.setPhoneNumber(request.getPhoneNumber().trim());
        }

        // Note: address is now managed via Address entity, not directly on User

        // City is now part of Address, not User directly
        // cityId será processado no array de addresses abaixo

        // Atualizar campos deprecated (backward compatibility)
        // Se cpf for fornecido, mapeia para documentNumber
        if (request.getCpf() != null && !request.getCpf().trim().isEmpty() && 
            (request.getDocumentNumber() == null || request.getDocumentNumber().trim().isEmpty())) {
            String document = request.getCpf().trim().replaceAll("[^0-9]", "");
            if (com.mvt.mvt_events.util.CPFUtil.isValid(document)) {
                user.setDocumentNumber(document);
            }
        }
        
        // Se phone for fornecido, mapeia para phoneDdd + phoneNumber
        if (request.getPhone() != null && !request.getPhone().trim().isEmpty()) {
            String phone = request.getPhone().trim().replaceAll("[^0-9]", "");
            if (phone.length() >= 10) {
                // Últimos 8 ou 9 dígitos = número
                user.setPhoneNumber(phone.substring(phone.length() - 8));
                // Primeiros 2 dígitos = DDD
                user.setPhoneDdd(phone.substring(0, 2));
            }
        }

        // Atualizar data de nascimento (suporta ambos: birthDate e dateOfBirth)
        String dateValue = request.getBirthDate() != null ? request.getBirthDate() : null;
        if (dateValue != null && !dateValue.trim().isEmpty()) {
            try {
                LocalDate birthDate = LocalDate.parse(dateValue, DateTimeFormatter.ISO_LOCAL_DATE);
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

        // Atualizar documento (CPF/CNPJ)
        if (request.getDocumentNumber() != null && !request.getDocumentNumber().trim().isEmpty()) {
            String document = request.getDocumentNumber().trim().replaceAll("[^0-9]", "");
            
            // Valida como CPF (11 dígitos) ou CNPJ (14 dígitos)
            boolean isValid = false;
            if (document.length() == 11) {
                isValid = com.mvt.mvt_events.util.CPFUtil.isValid(document);
            } else if (document.length() == 14) {
                isValid = com.mvt.mvt_events.util.CNPJUtil.isValid(document);
            }
            
            if (isValid) {
                user.setDocumentNumber(document);
            } else {
                throw new RuntimeException("CPF ou CNPJ inválido");
            }
        }

        // Nota: latitude e longitude do endereço fixo devem ser atualizados via array de addresses
        // User apenas tem gpsLatitude e gpsLongitude (coordenadas GPS em tempo real)

        // Processar array de endereços (sincronização: update, insert, delete)
        // Usa orphanRemoval=true na coleção user.addresses para deletar automaticamente
        if (request.getAddresses() != null) {
            // Obter a coleção atual de endereços do usuário (já carregada pelo findByIdWithAddresses)
            Set<Address> currentAddresses = user.getAddresses();
            if (currentAddresses == null) {
                currentAddresses = new HashSet<>();
                user.setAddresses(currentAddresses);
            }
            
            // Criar mapa de IDs dos endereços do payload (apenas os que têm ID)
            java.util.Set<Long> payloadAddressIds = request.getAddresses().stream()
                .filter(dto -> dto.getId() != null)
                .map(com.mvt.mvt_events.controller.UserController.AddressDTO::getId)
                .collect(java.util.stream.Collectors.toSet());
            
            // 1. DELETAR: Remover da coleção os endereços que não estão no payload
            // orphanRemoval=true fará o DELETE automaticamente
            currentAddresses.removeIf(addr -> !payloadAddressIds.contains(addr.getId()));
            
            // Criar mapa de endereços existentes por ID para facilitar lookup
            java.util.Map<Long, Address> existingAddressMap = currentAddresses.stream()
                .collect(java.util.stream.Collectors.toMap(Address::getId, addr -> addr));
            
            // 2. UPDATE ou INSERT
            for (com.mvt.mvt_events.controller.UserController.AddressDTO addressDTO : request.getAddresses()) {
                Address address;
                boolean isNewAddress = false;
                
                if (addressDTO.getId() != null && existingAddressMap.containsKey(addressDTO.getId())) {
                    // UPDATE: Endereço existe na coleção
                    address = existingAddressMap.get(addressDTO.getId());
                } else {
                    // INSERT: Novo endereço (sem ID ou ID não encontrado)
                    address = new Address();
                    address.setUser(user);
                    isNewAddress = true;
                }
                
                // Buscar cidade pelo nome
                if (addressDTO.getCity() != null && !addressDTO.getCity().trim().isEmpty()) {
                    String cityName = addressDTO.getCity().trim();
                    String stateName = addressDTO.getState() != null ? addressDTO.getState().trim() 
                                     : (request.getState() != null ? request.getState().trim() : null);
                    
                    if (stateName != null) {
                        cityRepository.findByNameAndState(cityName, stateName)
                            .ifPresent(address::setCity);
                    }
                }
                
                // Campos obrigatórios
                if (addressDTO.getStreet() != null) {
                    address.setStreet(addressDTO.getStreet());
                }
                if (addressDTO.getNumber() != null) {
                    address.setNumber(addressDTO.getNumber());
                }
                if (addressDTO.getNeighborhood() != null) {
                    address.setNeighborhood(addressDTO.getNeighborhood());
                }
                
                // Campos opcionais
                if (addressDTO.getComplement() != null) {
                    address.setComplement(addressDTO.getComplement());
                }
                if (addressDTO.getReferencePoint() != null) {
                    address.setReferencePoint(addressDTO.getReferencePoint());
                }
                if (addressDTO.getZipCode() != null) {
                    address.setZipCode(addressDTO.getZipCode().replaceAll("[^0-9]", ""));
                }
                if (addressDTO.getLatitude() != null && !addressDTO.getLatitude().trim().isEmpty()) {
                    try {
                        address.setLatitude(Double.parseDouble(addressDTO.getLatitude()));
                    } catch (NumberFormatException e) {
                        // Ignorar se não for um número válido
                    }
                }
                if (addressDTO.getLongitude() != null && !addressDTO.getLongitude().trim().isEmpty()) {
                    try {
                        address.setLongitude(Double.parseDouble(addressDTO.getLongitude()));
                    } catch (NumberFormatException e) {
                        // Ignorar se não for um número válido
                    }
                }
                
                // Definir como padrão
                if (addressDTO.getIsDefault() != null) {
                    address.setIsDefault(addressDTO.getIsDefault());
                }
                
                // Adicionar à coleção APÓS preencher todos os campos (para novos endereços)
                if (isNewAddress) {
                    currentAddresses.add(address);
                }
            }
        }

        User savedUser = userRepository.save(user);

        // Force load da city via address para evitar lazy loading
        if (savedUser.getAddress() != null && savedUser.getAddress().getCity() != null) {
            savedUser.getAddress().getCity().getName(); // Trigger lazy loading
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

        // Atualizar localização GPS (em tempo real)
        user.setGpsLatitude(latitude);
        user.setGpsLongitude(longitude);

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

        // Force load da city via address para evitar lazy loading
        if (savedUser.getAddress() != null && savedUser.getAddress().getCity() != null) {
            savedUser.getAddress().getCity().getName(); // Trigger lazy loading
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
     * Busca dados dos Client Contracts de um CLIENT SEM carregar objetos completos
     * Retorna: [organization_id, organization_name, is_primary,
     * status, contract_date, start_date, end_date]
     */
    @Transactional(readOnly = true)
    public java.util.List<Object[]> getClientContractsForUser(UUID userId) {
        return clientContractRepository.findContractDataByClientId(userId);
    }
}