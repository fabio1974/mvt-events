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
    
    @Autowired
    private com.mvt.mvt_events.repository.VehicleRepository vehicleRepository;
    
    @Autowired
    private com.mvt.mvt_events.repository.BankAccountRepository bankAccountRepository;

    @Autowired
    private com.mvt.mvt_events.repository.StoreProfileRepository storeProfileRepository;
    
    @Autowired
    private com.mvt.mvt_events.repository.CustomerCardRepository customerCardRepository;

    @Autowired
    private com.mvt.mvt_events.repository.CustomerPaymentPreferenceRepository customerPaymentPreferenceRepository;

    @Autowired
    private com.mvt.mvt_events.repository.DeliveryRepository deliveryRepository;

    @Autowired
    private PlannedRouteService plannedRouteService;

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
                String dateStr = request.getDateOfBirth().trim();
                
                // Extrai apenas a parte da data (YYYY-MM-DD) ignorando timezone
                // Se formato: "1974-09-02T00:00:00.000Z" -> extrai "1974-09-02"
                // Se formato: "1974-09-02" -> usa direto
                String datePart = dateStr.substring(0, Math.min(10, dateStr.length()));
                
                LocalDate birthDate = LocalDate.parse(datePart, DateTimeFormatter.ISO_LOCAL_DATE);
                user.setDateOfBirth(birthDate);
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

        // Enabled — regras de preenchimento mínimo por role
        if (request.getEnabled() != null) {
            user.setEnabled(request.getEnabled());
        }

        // Blocked — bloqueio de segurança (impede login)
        if (request.getBlocked() != null) {
            user.setBlocked(request.getBlocked());
        }

        User savedUser = userRepository.save(user);

        // Se for ORGANIZER, criar Organization automaticamente com owner = este usuário
        if (savedUser.getRole() == User.Role.ORGANIZER) {
            com.mvt.mvt_events.jpa.Organization organization = new com.mvt.mvt_events.jpa.Organization();
            organization.setName("Grupo de " + savedUser.getName());
            organization.setOwner(savedUser);
            organizationRepository.save(organization);
            System.out.println("✅ Organization criada automaticamente para ORGANIZER: " + savedUser.getUsername() + " → org '" + organization.getName() + "'");
        }

        // Se for CLIENT, criar automaticamente preferência de pagamento como PIX
        if (savedUser.getRole() == User.Role.CLIENT) {
            com.mvt.mvt_events.jpa.CustomerPaymentPreference preference =
                com.mvt.mvt_events.jpa.CustomerPaymentPreference.builder()
                    .user(savedUser)
                    .preferredPaymentType(com.mvt.mvt_events.jpa.CustomerPaymentPreference.PreferredPaymentType.PIX)
                    .defaultCard(null)
                    .build();
            customerPaymentPreferenceRepository.save(preference);
            System.out.println("✅ Preferência de pagamento PIX criada automaticamente para CLIENT: " + savedUser.getUsername());
        }

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

        // Atualizar data de nascimento
        if (request.getDateOfBirth() != null && !request.getDateOfBirth().trim().isEmpty()) {
            try {
                String dateStr = request.getDateOfBirth().trim();
                
                // Extrai apenas a parte da data (YYYY-MM-DD) ignorando timezone
                // Se formato: "1974-09-02T00:00:00.000Z" -> extrai "1974-09-02"
                // Se formato: "1974-09-02" -> usa direto
                String datePart = dateStr.substring(0, Math.min(10, dateStr.length()));
                
                LocalDate birthDate = LocalDate.parse(datePart, DateTimeFormatter.ISO_LOCAL_DATE);
                user.setDateOfBirth(birthDate);
            } catch (Exception e) {
                throw new RuntimeException("Data de nascimento inválida. Use o formato YYYY-MM-DD ou YYYY-MM-DDTHH:mm:ss.sssZ");
            }
        }

        // Atualizar gênero
        if (request.getGender() != null && !request.getGender().trim().isEmpty()) {
            try {
                String gender = request.getGender().trim().toUpperCase();
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

        // Atualizar coordenadas GPS (posição em tempo real)
        if (request.getGpsLatitude() != null) {
            user.setGpsLatitude(request.getGpsLatitude());
        }
        if (request.getGpsLongitude() != null) {
            user.setGpsLongitude(request.getGpsLongitude());
        }

        // Atualizar tipo de serviço (apenas para COURIER)
        if (request.getServiceType() != null && !request.getServiceType().trim().isEmpty()) {
            try {
                user.setServiceType(com.mvt.mvt_events.jpa.ServiceType.valueOf(request.getServiceType().trim().toUpperCase()));
            } catch (IllegalArgumentException e) {
                throw new RuntimeException("serviceType inválido. Valores aceitos: DELIVERY, PASSENGER_TRANSPORT, BOTH");
            }
        }

        // Enabled — validações obrigatórias por role para ativação
        if (request.getEnabled() != null) {
            if (request.getEnabled()) {
                validateActivationRequirements(user);
            }
            user.setEnabled(request.getEnabled());
        }

        // Blocked — bloqueio de segurança (impede login)
        if (request.getBlocked() != null) {
            user.setBlocked(request.getBlocked());
        }

        // Perfil da loja (nested OneToOne — apenas para CLIENT)
        if (request.getStoreProfile() != null && user.getRole() == User.Role.CLIENT) {
            var spReq = request.getStoreProfile();
            com.mvt.mvt_events.jpa.StoreProfile store = storeProfileRepository.findByUserId(user.getId())
                    .orElseGet(() -> {
                        var sp = new com.mvt.mvt_events.jpa.StoreProfile();
                        sp.setUser(user);
                        return sp;
                    });

            if (spReq.getIsOpen() != null) store.setIsOpen(spReq.getIsOpen());
            if (spReq.getTableOrdersEnabled() != null) store.setTableOrdersEnabled(spReq.getTableOrdersEnabled());
            if (spReq.getDescription() != null) store.setDescription(spReq.getDescription());
            if (spReq.getMinOrder() != null) store.setMinOrder(spReq.getMinOrder());
            if (spReq.getAvgPreparationMinutes() != null) store.setAvgPreparationMinutes(spReq.getAvgPreparationMinutes());
            if (spReq.getLogoUrl() != null) store.setLogoUrl(spReq.getLogoUrl());
            if (spReq.getCoverUrl() != null) store.setCoverUrl(spReq.getCoverUrl());

            storeProfileRepository.save(store);
        }

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
                
                // Buscar cidade pelo ID ou pelo nome
                if (addressDTO.getCityId() != null) {
                    // Usar ID da cidade diretamente
                    cityRepository.findById(addressDTO.getCityId())
                        .ifPresent(address::setCity);
                } else if (addressDTO.getCityName() != null) {
                    // Fallback: buscar por nome e estado
                    String cityName = addressDTO.getCityName().trim();
                    String stateName = addressDTO.getState() != null ? addressDTO.getState().trim() : 
                                      (addressDTO.getCityState() != null ? addressDTO.getCityState().trim() : null);
                    
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
                if (addressDTO.getLatitude() != null) {
                    address.setLatitude(addressDTO.getLatitude());
                }
                if (addressDTO.getLongitude() != null) {
                    address.setLongitude(addressDTO.getLongitude());
                }
                
                // Definir como padrão (e desmarcar outros se necessário)
                if (addressDTO.getIsDefault() != null && addressDTO.getIsDefault()) {
                    // Desmarcar todos os outros endereços do usuário como não-default
                    currentAddresses.forEach(addr -> addr.setIsDefault(false));
                    address.setIsDefault(true);
                } else if (addressDTO.getIsDefault() != null) {
                    address.setIsDefault(false);
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

                user.setUpdatedAt(java.time.OffsetDateTime.ofInstant(gpsTimestamp.toInstant(java.time.ZoneOffset.UTC), java.time.ZoneId.of("America/Fortaleza")));
            } catch (Exception e) {
                // Se não conseguir parsear, usar timestamp atual e logar warning
                System.err
                        .println("Erro ao parsear timestamp do GPS: " + updatedAtString + ". Usando timestamp atual.");
                user.setUpdatedAt(java.time.OffsetDateTime.now(java.time.ZoneId.of("America/Fortaleza")));
            }
        } else {
            // Se não fornecido, usar timestamp atual
            user.setUpdatedAt(java.time.OffsetDateTime.now(java.time.ZoneId.of("America/Fortaleza")));
        }

        User savedUser = userRepository.save(user);

        // 📍 ROUTE TRACKING: ACCEPTED → approach_route | IN_TRANSIT → actual_route
        // M = epoch seconds do timestamp real de captura GPS
        if (savedUser.getCurrentDeliveryId() != null) {
            try {
                double epochSec = savedUser.getUpdatedAt().toEpochSecond();
                deliveryRepository.findByIdWithJoins(savedUser.getCurrentDeliveryId()).ifPresent(delivery -> {
                    var status = delivery.getStatus();
                    if (status == com.mvt.mvt_events.jpa.Delivery.DeliveryStatus.ACCEPTED) {
                        String existingApproach = deliveryRepository.getApproachRouteAsGeoJson(delivery.getId());
                        if (existingApproach == null) {
                            deliveryRepository.initializeApproachRoute(delivery.getId(), latitude, longitude, epochSec);
                        } else {
                            deliveryRepository.appendApproachRoutePoint(delivery.getId(), latitude, longitude, epochSec);
                        }
                    } else if (status == com.mvt.mvt_events.jpa.Delivery.DeliveryStatus.IN_TRANSIT) {
                        deliveryRepository.appendRoutePoint(delivery.getId(), latitude, longitude, epochSec);
                    }
                });
            } catch (Exception e) {
                System.err.println("⚠️ Failed to append route point: " + e.getMessage());
            }
        }

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

    /**
     * Soft-delete da própria conta do usuário logado (DELETE /api/users/me).
     * - Contas demo (demo.*@zapi10.com) não podem ser excluídas.
     * - Anonimiza dados pessoais e bloqueia login.
     */
    public void softDeleteMyAccount(Authentication authentication) {
        String username = authentication.getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        if (user.isDemoAccount()) {
            throw new RuntimeException("Contas de demonstração não podem ser excluídas");
        }

        // Anonymize personal data
        user.setUsername("deleted_" + user.getId() + "@removed.com");
        user.setName("Usuário Removido");
        user.setDocumentNumber(generateDeletedCpf(user.getId()));
        user.setPhoneDdd(null);
        user.setPhoneNumber(null);

        // Block login and mark as deleted
        user.setBlocked(true);
        user.setEnabled(false);
        user.setDeletedAt(LocalDateTime.now());

        userRepository.save(user);
    }

    /**
     * Generates a unique valid CPF from UUID for anonymized deleted accounts.
     */
    private String generateDeletedCpf(UUID userId) {
        // Use first 9 hex digits of UUID, convert to decimal base digits
        String hex = userId.toString().replace("-", "");
        long num = Long.parseLong(hex.substring(0, 9), 16) % 999999999L;
        String base = String.format("%09d", num);
        int[] digits = new int[11];
        for (int i = 0; i < 9; i++) digits[i] = base.charAt(i) - '0';
        // First check digit
        int sum = 0;
        for (int i = 0; i < 9; i++) sum += digits[i] * (10 - i);
        int rem = sum % 11;
        digits[9] = rem < 2 ? 0 : 11 - rem;
        // Second check digit
        sum = 0;
        for (int i = 0; i < 10; i++) sum += digits[i] * (11 - i);
        rem = sum % 11;
        digits[10] = rem < 2 ? 0 : 11 - rem;
        StringBuilder sb = new StringBuilder();
        for (int d : digits) sb.append(d);
        return sb.toString();
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

    // ============================================================================
    // MÉTODOS PARA BUSCA DE COURIERS (typeahead mobile)
    // ============================================================================

    @Autowired
    private com.mvt.mvt_events.repository.OrganizationRepository organizationRepository;

    /**
     * Busca motoboys para typeahead mobile.
     * Retorna lista leve com apenas id, nome, email e telefone.
     * Exclui motoboys que já estão vinculados à organização do usuário logado.
     * 
     * @param search Termo de busca (parte do nome ou email)
     * @param limit Limite de resultados (padrão: 10)
     * @param authentication Autenticação do usuário logado
     * @return Lista de CourierSearchResponse
     */
    @Transactional(readOnly = true)
    public java.util.List<com.mvt.mvt_events.controller.UserController.CourierSearchResponse> searchCouriersForTypeahead(
            String search, 
            Integer limit, 
            Authentication authentication) {
        
        // Buscar usuário logado
        String currentUsername = authentication.getName();
        User currentUser = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new RuntimeException("Usuário logado não encontrado"));
        
        // Buscar organização do usuário logado (ORGANIZER/ADM)
        Long organizationId = null;
        Optional<com.mvt.mvt_events.jpa.Organization> orgOpt = organizationRepository.findByOwner(currentUser);
        if (orgOpt.isPresent()) {
            organizationId = orgOpt.get().getId();
        }
        
        // Buscar motoboys que NÃO estão na organização do usuário
        java.util.List<User> couriers;
        if (organizationId != null) {
            couriers = userRepository.searchCouriersNotInOrganization(
                    search != null ? search.toLowerCase().trim() : "", 
                    organizationId, 
                    limit != null ? limit : 10);
        } else {
            // Se não tem organização, busca todos os couriers
            couriers = userRepository.searchCouriersWithLimit(
                    search != null ? search.toLowerCase().trim() : "", 
                    limit != null ? limit : 10);
        }
        
        return couriers.stream()
                .map(com.mvt.mvt_events.controller.UserController.CourierSearchResponse::new)
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * Lista todos os motoboys vinculados à organização do ORGANIZER logado.
     * Retorna dados completos do motoboy + status do contrato.
     * 
     * @param authentication Autenticação do usuário logado
     * @return Lista de CourierForOrganizerResponse
     */
    @Transactional(readOnly = true)
    public java.util.List<com.mvt.mvt_events.controller.UserController.CourierForOrganizerResponse> getCouriersForLoggedOrganizer(
            Authentication authentication) {
        
        // Buscar usuário logado
        String currentUsername = authentication.getName();
        User currentUser = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new RuntimeException("Usuário logado não encontrado"));
        
        // Verificar se é ORGANIZER
        if (!currentUser.isOrganizer() && !currentUser.isAdmin()) {
            throw new RuntimeException("Apenas organizadores podem listar seus motoboys");
        }
        
        // Buscar organização do usuário
        com.mvt.mvt_events.jpa.Organization organization = organizationRepository.findByOwner(currentUser)
                .orElseThrow(() -> new RuntimeException("Organização não encontrada para o usuário logado"));
        
        // Buscar contratos de trabalho ATIVOS da organização
        java.util.List<com.mvt.mvt_events.jpa.EmploymentContract> contracts = 
                employmentContractRepository.findActiveByOrganizationId(organization.getId());
        
        // Mapear para DTO
        return contracts.stream()
                .map(contract -> new com.mvt.mvt_events.controller.UserController.CourierForOrganizerResponse(
                        contract.getCourier(), contract))
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * Adiciona um motoboy à organização do ORGANIZER logado.
     * Cria um contrato de trabalho (EmploymentContract) ativo.
     * 
     * @param courierId ID do motoboy a ser adicionado
     * @param authentication Autenticação do usuário logado
     * @return CourierForOrganizerResponse com dados do motoboy adicionado
     */
    public com.mvt.mvt_events.controller.UserController.CourierForOrganizerResponse addCourierToOrganization(
            UUID courierId,
            Authentication authentication) {
        
        // Buscar usuário logado
        String currentUsername = authentication.getName();
        User currentUser = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new RuntimeException("Usuário logado não encontrado"));
        
        // Verificar se é ORGANIZER
        if (!currentUser.isOrganizer() && !currentUser.isAdmin()) {
            throw new RuntimeException("Apenas organizadores podem adicionar motoboys ao grupo");
        }
        
        // Buscar organização do usuário
        com.mvt.mvt_events.jpa.Organization organization = organizationRepository.findByOwner(currentUser)
                .orElseThrow(() -> new RuntimeException("Organização não encontrada para o usuário logado"));
        
        // Buscar o motoboy
        User courier = userRepository.findById(courierId)
                .orElseThrow(() -> new RuntimeException("Motoboy não encontrado"));
        
        // Verificar se é COURIER
        if (courier.getRole() != User.Role.COURIER) {
            throw new RuntimeException("Usuário não é um motoboy (COURIER)");
        }
        
        // Verificar se já existe contrato
        java.util.Optional<com.mvt.mvt_events.jpa.EmploymentContract> existingContract = 
                employmentContractRepository.findByCourierAndOrganization(courier, organization);
        
        com.mvt.mvt_events.jpa.EmploymentContract contract;
        
        if (existingContract.isPresent()) {
            // Se já existe, reativar
            contract = existingContract.get();
            if (contract.isActive()) {
                throw new RuntimeException("Motoboy já está vinculado à sua organização");
            }
            contract.activate();
            contract = employmentContractRepository.save(contract);
        } else {
            // Criar novo contrato
            contract = new com.mvt.mvt_events.jpa.EmploymentContract();
            contract.setCourier(courier);
            contract.setOrganization(organization);
            contract.setActive(true);
            contract.setLinkedAt(java.time.OffsetDateTime.now(java.time.ZoneId.of("America/Fortaleza")));
            contract = employmentContractRepository.save(contract);
        }
        
        return new com.mvt.mvt_events.controller.UserController.CourierForOrganizerResponse(courier, contract);
    }

    /**
     * Remove um motoboy da organização do ORGANIZER logado.
     * Deleta o contrato de trabalho (EmploymentContract).
     * 
     * @param courierId ID do motoboy a ser removido
     * @param authentication Autenticação do usuário logado
     */
    public void removeCourierFromOrganization(
            UUID courierId,
            Authentication authentication) {
        
        // Buscar usuário logado
        String currentUsername = authentication.getName();
        User currentUser = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new RuntimeException("Usuário logado não encontrado"));
        
        // Verificar se é ORGANIZER
        if (!currentUser.isOrganizer() && !currentUser.isAdmin()) {
            throw new RuntimeException("Apenas organizadores podem remover motoboys do grupo");
        }
        
        // Buscar organização do usuário
        com.mvt.mvt_events.jpa.Organization organization = organizationRepository.findByOwner(currentUser)
                .orElseThrow(() -> new RuntimeException("Organização não encontrada para o usuário logado"));
        
        // Buscar o motoboy
        User courier = userRepository.findById(courierId)
                .orElseThrow(() -> new RuntimeException("Motoboy não encontrado"));
        
        // Buscar contrato
        com.mvt.mvt_events.jpa.EmploymentContract contract = 
                employmentContractRepository.findByCourierAndOrganization(courier, organization)
                .orElseThrow(() -> new RuntimeException("Motoboy não está vinculado à sua organização"));
        
        // Deletar contrato do banco
        employmentContractRepository.delete(contract);
    }

    // ============================================================================
    // CLIENT GROUP MANAGEMENT
    // ============================================================================

    /**
     * Busca clientes por nome ou email para adicionar ao grupo (typeahead).
     * Exclui clientes que já têm contrato ativo com a organização do usuário logado.
     * 
     * @param search Termo de busca (parte do nome ou email)
     * @param limit Limite de resultados (padrão: 10)
     * @param authentication Autenticação do usuário logado
     * @return Lista de ClientSearchResponse
     */
    @Transactional(readOnly = true)
    public java.util.List<com.mvt.mvt_events.controller.UserController.ClientSearchResponse> searchClientsForTypeahead(
            String search, 
            Integer limit, 
            Authentication authentication) {
        
        // Buscar usuário logado
        String currentUsername = authentication.getName();
        User currentUser = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new RuntimeException("Usuário logado não encontrado"));
        
        // Buscar organização do usuário logado (ORGANIZER/ADM)
        Long organizationId = null;
        Optional<com.mvt.mvt_events.jpa.Organization> orgOpt = organizationRepository.findByOwner(currentUser);
        if (orgOpt.isPresent()) {
            organizationId = orgOpt.get().getId();
        }
        
        // Buscar clientes que NÃO estão na organização do usuário
        java.util.List<User> clients;
        if (organizationId != null) {
            clients = userRepository.searchClientsNotInOrganization(
                    search != null ? search.toLowerCase().trim() : "", 
                    organizationId, 
                    limit != null ? limit : 10);
        } else {
            // Se não tem organização, busca todos os clientes
            clients = userRepository.searchClientsWithLimit(
                    search != null ? search.toLowerCase().trim() : "", 
                    limit != null ? limit : 10);
        }
        
        return clients.stream()
                .map(com.mvt.mvt_events.controller.UserController.ClientSearchResponse::new)
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * Lista todos os clientes vinculados à organização do ORGANIZER logado.
     * Retorna dados completos do cliente + status do contrato.
     * 
     * @param authentication Autenticação do usuário logado
     * @return Lista de ClientForOrganizerResponse
     */
    @Transactional(readOnly = true)
    public java.util.List<com.mvt.mvt_events.controller.UserController.ClientForOrganizerResponse> getClientsForLoggedOrganizer(
            Authentication authentication) {
        
        // Buscar usuário logado
        String currentUsername = authentication.getName();
        User currentUser = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new RuntimeException("Usuário logado não encontrado"));
        
        // Verificar se é ORGANIZER
        if (!currentUser.isOrganizer() && !currentUser.isAdmin()) {
            throw new RuntimeException("Apenas organizadores podem listar seus clientes");
        }
        
        // Buscar organização do usuário
        com.mvt.mvt_events.jpa.Organization organization = organizationRepository.findByOwner(currentUser)
                .orElseThrow(() -> new RuntimeException("Organização não encontrada para o usuário logado"));
        
        // Buscar contratos de serviço ATIVOS da organização
        java.util.List<com.mvt.mvt_events.jpa.ClientContract> contracts = 
                clientContractRepository.findActiveByOrganizationId(organization.getId());
        
        // Mapear para DTO
        return contracts.stream()
                .map(contract -> new com.mvt.mvt_events.controller.UserController.ClientForOrganizerResponse(
                        contract.getClient(), contract))
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * Adiciona um cliente à organização do ORGANIZER logado.
     * Cria um contrato de serviço (ClientContract) ativo.
     * 
     * @param clientId ID do cliente a ser adicionado
     * @param authentication Autenticação do usuário logado
     * @return ClientForOrganizerResponse com dados do cliente adicionado
     */
    public com.mvt.mvt_events.controller.UserController.ClientForOrganizerResponse addClientToOrganization(
            UUID clientId,
            Authentication authentication) {
        
        // Buscar usuário logado
        String currentUsername = authentication.getName();
        User currentUser = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new RuntimeException("Usuário logado não encontrado"));
        
        // Verificar se é ORGANIZER
        if (!currentUser.isOrganizer() && !currentUser.isAdmin()) {
            throw new RuntimeException("Apenas organizadores podem adicionar clientes ao grupo");
        }
        
        // Buscar organização do usuário
        com.mvt.mvt_events.jpa.Organization organization = organizationRepository.findByOwner(currentUser)
                .orElseThrow(() -> new RuntimeException("Organização não encontrada para o usuário logado"));
        
        // Buscar o cliente
        User client = userRepository.findById(clientId)
                .orElseThrow(() -> new RuntimeException("Cliente não encontrado"));
        
        // Verificar se é CLIENT ou CUSTOMER
        if (client.getRole() != User.Role.CLIENT && client.getRole() != User.Role.CUSTOMER) {
            throw new RuntimeException("Usuário não é um cliente (CLIENT/CUSTOMER)");
        }
        
        // Verificar se já existe contrato
        java.util.Optional<com.mvt.mvt_events.jpa.ClientContract> existingContract = 
                clientContractRepository.findByClientAndOrganization(client, organization);
        
        com.mvt.mvt_events.jpa.ClientContract contract;
        
        if (existingContract.isPresent()) {
            // Se já existe, reativar
            contract = existingContract.get();
            if (contract.isActive()) {
                throw new RuntimeException("Cliente já está vinculado à sua organização");
            }
            contract.setStatus(com.mvt.mvt_events.jpa.ClientContract.ContractStatus.ACTIVE);
            contract.setStartDate(java.time.LocalDate.now());
            contract.setEndDate(null);
            contract = clientContractRepository.save(contract);
        } else {
            // Criar novo contrato
            contract = new com.mvt.mvt_events.jpa.ClientContract();
            contract.setClient(client);
            contract.setOrganization(organization);
            contract.setStatus(com.mvt.mvt_events.jpa.ClientContract.ContractStatus.ACTIVE);
            contract.setStartDate(java.time.LocalDate.now());
            contract.setPrimary(false);
            contract = clientContractRepository.save(contract);
        }
        
        return new com.mvt.mvt_events.controller.UserController.ClientForOrganizerResponse(client, contract);
    }

    /**
     * Remove um cliente da organização do ORGANIZER logado.
     * Deleta o contrato de serviço (ClientContract).
     * 
     * @param clientId ID do cliente a ser removido
     * @param authentication Autenticação do usuário logado
     */
    public void removeClientFromOrganization(
            UUID clientId,
            Authentication authentication) {
        
        // Buscar usuário logado
        String currentUsername = authentication.getName();
        User currentUser = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new RuntimeException("Usuário logado não encontrado"));
        
        // Verificar se é ORGANIZER
        if (!currentUser.isOrganizer() && !currentUser.isAdmin()) {
            throw new RuntimeException("Apenas organizadores podem remover clientes do grupo");
        }
        
        // Buscar organização do usuário
        com.mvt.mvt_events.jpa.Organization organization = organizationRepository.findByOwner(currentUser)
                .orElseThrow(() -> new RuntimeException("Organização não encontrada para o usuário logado"));
        
        // Buscar o cliente
        User client = userRepository.findById(clientId)
                .orElseThrow(() -> new RuntimeException("Cliente não encontrado"));
        
        // Buscar contrato
        com.mvt.mvt_events.jpa.ClientContract contract = 
                clientContractRepository.findByClientAndOrganization(client, organization)
                .orElseThrow(() -> new RuntimeException("Cliente não está vinculado à sua organização"));
        
        // Deletar contrato do banco
        clientContractRepository.delete(contract);
    }

    // ============================================================================
    // WAITER MANAGEMENT (CLIENT ↔ WAITER direto)
    // ============================================================================

    @Autowired
    private com.mvt.mvt_events.repository.ClientWaiterRepository clientWaiterRepository;

    /** Busca garçons que ainda não estão vinculados ao CLIENT logado */
    public java.util.List<com.mvt.mvt_events.controller.UserController.WaiterSearchResponse> searchWaitersForTypeahead(
            String search, Integer limit, Authentication authentication) {

        String currentUsername = authentication.getName();
        User currentUser = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new RuntimeException("Usuário logado não encontrado"));

        java.util.List<User> waiters = userRepository.searchWaitersNotLinkedToClient(
                search != null ? search.toLowerCase().trim() : "",
                currentUser.getId(),
                limit != null ? limit : 10);

        return waiters.stream()
                .map(com.mvt.mvt_events.controller.UserController.WaiterSearchResponse::new)
                .collect(java.util.stream.Collectors.toList());
    }

    /** Lista garçons ativos do CLIENT logado */
    @Transactional(readOnly = true)
    public java.util.List<com.mvt.mvt_events.controller.UserController.WaiterForClientResponse> getWaitersForLoggedClient(
            Authentication authentication) {

        String currentUsername = authentication.getName();
        User currentUser = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new RuntimeException("Usuário logado não encontrado"));

        if (currentUser.getRole() != User.Role.CLIENT && !currentUser.isAdmin()) {
            throw new RuntimeException("Apenas estabelecimentos (CLIENT) podem listar seus garçons");
        }

        java.util.List<com.mvt.mvt_events.jpa.ClientWaiter> links =
                clientWaiterRepository.findActiveByClientId(currentUser.getId());

        return links.stream()
                .map(link -> new com.mvt.mvt_events.controller.UserController.WaiterForClientResponse(
                        link.getWaiter(), link))
                .collect(java.util.stream.Collectors.toList());
    }

    /** CLIENT adiciona garçom ao seu estabelecimento */
    public com.mvt.mvt_events.controller.UserController.WaiterForClientResponse addWaiterToClient(
            UUID waiterId, Authentication authentication) {

        String currentUsername = authentication.getName();
        User currentUser = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new RuntimeException("Usuário logado não encontrado"));

        if (currentUser.getRole() != User.Role.CLIENT && !currentUser.isAdmin()) {
            throw new RuntimeException("Apenas estabelecimentos (CLIENT) podem adicionar garçons");
        }

        User waiter = userRepository.findById(waiterId)
                .orElseThrow(() -> new RuntimeException("Garçom não encontrado"));

        if (waiter.getRole() != User.Role.WAITER) {
            throw new RuntimeException("Usuário não é um garçom (WAITER)");
        }

        java.util.Optional<com.mvt.mvt_events.jpa.ClientWaiter> existing =
                clientWaiterRepository.findByClientAndWaiter(currentUser, waiter);

        com.mvt.mvt_events.jpa.ClientWaiter link;

        if (existing.isPresent()) {
            link = existing.get();
            if (link.isActive()) {
                throw new RuntimeException("Garçom já está vinculado ao seu estabelecimento");
            }
            link.setActive(true);
            link = clientWaiterRepository.save(link);
        } else {
            link = new com.mvt.mvt_events.jpa.ClientWaiter();
            link.setClient(currentUser);
            link.setWaiter(waiter);
            link.setActive(true);
            link = clientWaiterRepository.save(link);
        }

        return new com.mvt.mvt_events.controller.UserController.WaiterForClientResponse(waiter, link);
    }

    /** CLIENT remove garçom do seu estabelecimento */
    public void removeWaiterFromClient(UUID waiterId, Authentication authentication) {

        String currentUsername = authentication.getName();
        User currentUser = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new RuntimeException("Usuário logado não encontrado"));

        if (currentUser.getRole() != User.Role.CLIENT && !currentUser.isAdmin()) {
            throw new RuntimeException("Apenas estabelecimentos (CLIENT) podem remover garçons");
        }

        User waiter = userRepository.findById(waiterId)
                .orElseThrow(() -> new RuntimeException("Garçom não encontrado"));

        com.mvt.mvt_events.jpa.ClientWaiter link =
                clientWaiterRepository.findByClientAndWaiter(currentUser, waiter)
                .orElseThrow(() -> new RuntimeException("Garçom não está vinculado ao seu estabelecimento"));

        clientWaiterRepository.delete(link);
    }

    /** Retorna os CLIENTs (estabelecimentos) vinculados diretamente ao WAITER logado */
    @Transactional(readOnly = true)
    public java.util.List<com.mvt.mvt_events.controller.UserController.ClientForOrganizerResponse> getEstablishmentsForWaiter(
            Authentication authentication) {

        String currentUsername = authentication.getName();
        User waiter = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new RuntimeException("Usuário logado não encontrado"));

        if (waiter.getRole() != User.Role.WAITER) {
            throw new RuntimeException("Apenas garçons podem acessar este endpoint");
        }

        java.util.List<com.mvt.mvt_events.jpa.ClientWaiter> links =
                clientWaiterRepository.findActiveByWaiterId(waiter.getId());

        return links.stream()
                .filter(link -> {
                    // Só retorna estabelecimentos com módulo de mesas habilitado
                    return storeProfileRepository.findByUserId(link.getClient().getId())
                            .map(sp -> Boolean.TRUE.equals(sp.getTableOrdersEnabled()))
                            .orElse(false);
                })
                .map(link -> {
                    java.util.List<com.mvt.mvt_events.jpa.ClientContract> contracts =
                            clientContractRepository.findActiveByClientId(link.getClient().getId());
                    com.mvt.mvt_events.jpa.ClientContract cc = contracts.isEmpty() ? null : contracts.get(0);
                    return new com.mvt.mvt_events.controller.UserController.ClientForOrganizerResponse(
                            link.getClient(), cc);
                })
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * Retorna o status de ativação do usuário, detalhando o que está faltando
     * para ele estar completamente habilitado no sistema.
     * 
     * Requisitos por role:
     * - COURIER: veículo, conta bancária, telefone
     * - CUSTOMER: meio de pagamento, telefone
     * - Ambos: endereço padrão (sugerido)
     * 
     * @param authentication Autenticação do usuário logado
     * @return ActivationStatusResponse com status detalhado
     */
    @Transactional(readOnly = true)
    public com.mvt.mvt_events.dto.ActivationStatusResponse getActivationStatus(Authentication authentication) {
        String currentUsername = authentication.getName();
        User user = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));
        
        java.util.List<String> missing = new java.util.ArrayList<>();
        java.util.Map<String, String> messages = new java.util.HashMap<>();
        java.util.List<String> suggested = new java.util.ArrayList<>();
        
        // Verificar telefone (obrigatório para todos)
        if (user.getPhone() == null || user.getPhone().trim().isEmpty()) {
            missing.add("phone");
            messages.put("phone", "Preencha seu telefone nas informações pessoais");
        }
        
        // Verificações específicas por role
        if (user.getRole() == User.Role.ORGANIZER) {
            // Verificar conta bancária
            boolean hasBankAccount = bankAccountRepository.existsByUserId(user.getId());
            if (!hasBankAccount) {
                missing.add("bankAccount");
                messages.put("bankAccount", "Cadastre sua conta bancária");
            }
            // Verificar dados de saque (Pagar.me)
            if (user.getPagarmeRecipientId() == null || !"active".equals(user.getPagarmeStatus())) {
                missing.add("transferSettings");
                messages.put("transferSettings", "Configure seus dados de saque");
            }

        } else if (user.getRole() == User.Role.COURIER) {
            // Verificar conta bancária
            boolean hasBankAccount = bankAccountRepository.existsByUserId(user.getId());
            if (!hasBankAccount) {
                missing.add("bankAccount");
                messages.put("bankAccount", "Cadastre sua conta bancária");
            }
            // Verificar dados de saque (Pagar.me)
            if (user.getPagarmeRecipientId() == null || !"active".equals(user.getPagarmeStatus())) {
                missing.add("transferSettings");
                messages.put("transferSettings", "Configure seus dados de saque");
            }
            // Verificar veículo
            List<com.mvt.mvt_events.jpa.Vehicle> vehicles = vehicleRepository.findByOwnerId(user.getId());
            if (vehicles.isEmpty()) {
                missing.add("vehicle");
                messages.put("vehicle", "Cadastre um veículo");
            }
            // Verificar tipo de serviço
            if (user.getServiceType() == null) {
                missing.add("serviceType");
                messages.put("serviceType", "Defina seu tipo de serviço");
            }

        } else if (user.getRole() == User.Role.CUSTOMER || user.getRole() == User.Role.CLIENT) {
            // Verificar meio de pagamento (cartão ativo OU preferência PIX)
            long activeCards = customerCardRepository.countActiveCardsByCustomerId(user.getId());
            boolean hasPixPreference = customerPaymentPreferenceRepository.findByUserId(user.getId())
                    .map(pref -> pref.getPreferredPaymentType() == com.mvt.mvt_events.jpa.CustomerPaymentPreference.PreferredPaymentType.PIX)
                    .orElse(false);
            if (activeCards == 0 && !hasPixPreference) {
                missing.add("paymentMethod");
                messages.put("paymentMethod", "Cadastre um meio de pagamento (cartão ou PIX)");
            }
        }
        
        // Verificar endereço padrão (sugerido para todos)
        if (user.getAddress() == null) {
            suggested.add("defaultAddress");
        }
        
        boolean enabled = missing.isEmpty();
        
        return com.mvt.mvt_events.dto.ActivationStatusResponse.builder()
                .enabled(enabled)
                .role(user.getRole())
                .missing(missing)
                .messages(messages)
                .suggested(suggested)
                .build();
    }

    /**
     * Valida requisitos obrigatórios para ativação por role.
     * Lança RuntimeException se algum requisito não for atendido.
     */
    private void validateActivationRequirements(User user) {
        switch (user.getRole()) {
            case ORGANIZER:
                if (!bankAccountRepository.existsByUserId(user.getId())) {
                    throw new RuntimeException("Organizer precisa ter conta bancária cadastrada para ser ativado");
                }
                if (user.getPagarmeRecipientId() == null || !"active".equals(user.getPagarmeStatus())) {
                    throw new RuntimeException("Organizer precisa ter dados de saque configurados no Pagar.me para ser ativado");
                }
                break;

            case COURIER:
                if (!bankAccountRepository.existsByUserId(user.getId())) {
                    throw new RuntimeException("Courier precisa ter conta bancária cadastrada para ser ativado");
                }
                if (user.getPagarmeRecipientId() == null || !"active".equals(user.getPagarmeStatus())) {
                    throw new RuntimeException("Courier precisa ter dados de saque configurados no Pagar.me para ser ativado");
                }
                if (vehicleRepository.findByOwnerId(user.getId()).isEmpty()) {
                    throw new RuntimeException("Courier precisa ter veículo cadastrado para ser ativado");
                }
                if (user.getServiceType() == null) {
                    throw new RuntimeException("Courier precisa ter tipo de serviço definido para ser ativado");
                }
                break;

            case CUSTOMER:
            case CLIENT:
                boolean hasActiveCard = customerCardRepository.countActiveCardsByCustomerId(user.getId()) > 0;
                boolean hasPixPreference = customerPaymentPreferenceRepository.findByUserId(user.getId())
                        .map(pref -> pref.getPreferredPaymentType() == com.mvt.mvt_events.jpa.CustomerPaymentPreference.PreferredPaymentType.PIX)
                        .orElse(false);
                if (!hasActiveCard && !hasPixPreference) {
                    throw new RuntimeException("Cliente precisa ter cartão ativo ou preferência PIX cadastrada para ser ativado");
                }
                break;

            default:
                break;
        }
    }
}
