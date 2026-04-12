package com.mvt.mvt_events.service;

import com.mvt.mvt_events.controller.UserController.UserCreateRequest;
import com.mvt.mvt_events.controller.UserController.UserUpdateRequest;
import com.mvt.mvt_events.jpa.*;
import com.mvt.mvt_events.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Testes unitários do UserService — cobre criação de usuários por role,
 * validações, soft-delete, atualização de dados, localização GPS e ativação/desativação.
 */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private CityRepository cityRepository;
    @Mock private AddressRepository addressRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private EmploymentContractRepository employmentContractRepository;
    @Mock private ClientContractRepository clientContractRepository;
    @Mock private VehicleRepository vehicleRepository;
    @Mock private BankAccountRepository bankAccountRepository;
    @Mock private CustomerCardRepository customerCardRepository;
    @Mock private CustomerPaymentPreferenceRepository customerPaymentPreferenceRepository;
    @Mock private DeliveryRepository deliveryRepository;
    @Mock private PlannedRouteService plannedRouteService;
    @Mock private OrganizationRepository organizationRepository;

    @InjectMocks
    private UserService userService;

    // ========== Helpers ==========

    private User makeUser(UUID id, String name, User.Role role) {
        User u = new User();
        u.setId(id);
        u.setName(name);
        u.setRole(role);
        u.setUsername(name.toLowerCase().replace(" ", ".") + "@zapi10.com");
        u.setEnabled(true);
        return u;
    }

    private Authentication mockAuth(String username) {
        Authentication auth = mock(Authentication.class);
        lenient().when(auth.getName()).thenReturn(username);
        return auth;
    }

    private UserCreateRequest makeCreateRequest(String username, String name, String role) {
        UserCreateRequest req = new UserCreateRequest();
        req.setUsername(username);
        req.setName(name);
        req.setRole(role);
        return req;
    }

    // UUIDs fixos para consistência
    private final UUID adminId = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private final UUID clientId = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private final UUID customerId = UUID.fromString("00000000-0000-0000-0000-000000000003");
    private final UUID courierId = UUID.fromString("00000000-0000-0000-0000-000000000004");
    private final UUID organizerId = UUID.fromString("00000000-0000-0000-0000-000000000005");

    // ================================================================
    // CREATE USER
    // ================================================================

    @Nested
    @DisplayName("createUser() — Criação de usuários")
    class CreateUserTests {

        private User admin;
        private Authentication adminAuth;

        @BeforeEach
        void setUp() {
            admin = makeUser(adminId, "Admin", User.Role.ADMIN);
            adminAuth = mockAuth(admin.getUsername());
            lenient().when(userRepository.findByUsername(admin.getUsername())).thenReturn(Optional.of(admin));
        }

        @Test
        @DisplayName("ADMIN cria CLIENT com sucesso — preferência PIX criada automaticamente")
        void adminCriaClient() {
            UserCreateRequest req = makeCreateRequest("restaurante@email.com", "Restaurante X", "CLIENT");

            when(userRepository.findByUsername("restaurante@email.com")).thenReturn(Optional.empty());
            when(passwordEncoder.encode(anyString())).thenReturn("encoded");
            when(userRepository.save(any(User.class))).thenAnswer(inv -> {
                User u = inv.getArgument(0);
                u.setId(clientId);
                return u;
            });

            User result = userService.createUser(req, adminAuth);

            assertThat(result.getRole()).isEqualTo(User.Role.CLIENT);
            assertThat(result.getName()).isEqualTo("Restaurante X");
            verify(customerPaymentPreferenceRepository).save(any(CustomerPaymentPreference.class));
        }

        @Test
        @DisplayName("ADMIN cria COURIER com sucesso — sem preferência PIX")
        void adminCriaCourier() {
            UserCreateRequest req = makeCreateRequest("motoboy@email.com", "Pedro Moto", "COURIER");

            when(userRepository.findByUsername("motoboy@email.com")).thenReturn(Optional.empty());
            when(passwordEncoder.encode(anyString())).thenReturn("encoded");
            when(userRepository.save(any(User.class))).thenAnswer(inv -> {
                User u = inv.getArgument(0);
                u.setId(courierId);
                return u;
            });

            User result = userService.createUser(req, adminAuth);

            assertThat(result.getRole()).isEqualTo(User.Role.COURIER);
            verify(customerPaymentPreferenceRepository, never()).save(any());
        }

        @Test
        @DisplayName("ADMIN cria CUSTOMER com sucesso")
        void adminCriaCustomer() {
            UserCreateRequest req = makeCreateRequest("joao@email.com", "João Silva", "CUSTOMER");

            when(userRepository.findByUsername("joao@email.com")).thenReturn(Optional.empty());
            when(passwordEncoder.encode(anyString())).thenReturn("encoded");
            when(userRepository.save(any(User.class))).thenAnswer(inv -> {
                User u = inv.getArgument(0);
                u.setId(customerId);
                return u;
            });

            User result = userService.createUser(req, adminAuth);

            assertThat(result.getRole()).isEqualTo(User.Role.CUSTOMER);
        }

        @Test
        @DisplayName("ADMIN cria ORGANIZER com sucesso")
        void adminCriaOrganizer() {
            UserCreateRequest req = makeCreateRequest("gerente@email.com", "Ana Gerente", "ORGANIZER");

            when(userRepository.findByUsername("gerente@email.com")).thenReturn(Optional.empty());
            when(passwordEncoder.encode(anyString())).thenReturn("encoded");
            when(userRepository.save(any(User.class))).thenAnswer(inv -> {
                User u = inv.getArgument(0);
                u.setId(organizerId);
                return u;
            });

            User result = userService.createUser(req, adminAuth);

            assertThat(result.getRole()).isEqualTo(User.Role.ORGANIZER);
        }

        @Test
        @DisplayName("Não-ADMIN não pode criar usuários")
        void naoAdminNaoPodeCriar() {
            User courier = makeUser(courierId, "Pedro Moto", User.Role.COURIER);
            Authentication courierAuth = mockAuth(courier.getUsername());
            when(userRepository.findByUsername(courier.getUsername())).thenReturn(Optional.of(courier));

            UserCreateRequest req = makeCreateRequest("novo@email.com", "Novo", "CLIENT");

            assertThatThrownBy(() -> userService.createUser(req, courierAuth))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Apenas administradores podem criar usuários");
        }

        @Test
        @DisplayName("Email duplicado é rejeitado")
        void emailDuplicado() {
            UserCreateRequest req = makeCreateRequest("existente@email.com", "Novo", "CLIENT");
            when(userRepository.findByUsername("existente@email.com")).thenReturn(Optional.of(new User()));

            assertThatThrownBy(() -> userService.createUser(req, adminAuth))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Email já cadastrado");
        }

        @Test
        @DisplayName("Senha padrão é usada quando não fornecida")
        void senhaPadrao() {
            UserCreateRequest req = makeCreateRequest("novo@email.com", "Novo", "CLIENT");

            when(userRepository.findByUsername("novo@email.com")).thenReturn(Optional.empty());
            when(passwordEncoder.encode("senha123")).thenReturn("encoded-default");
            when(userRepository.save(any(User.class))).thenAnswer(inv -> {
                User u = inv.getArgument(0);
                u.setId(clientId);
                return u;
            });

            userService.createUser(req, adminAuth);

            verify(passwordEncoder).encode("senha123");
        }

        @Test
        @DisplayName("Senha customizada é aceita")
        void senhaCustom() {
            UserCreateRequest req = makeCreateRequest("novo@email.com", "Novo", "CLIENT");
            req.setPassword("minhasenha");

            when(userRepository.findByUsername("novo@email.com")).thenReturn(Optional.empty());
            when(passwordEncoder.encode("minhasenha")).thenReturn("encoded-custom");
            when(userRepository.save(any(User.class))).thenAnswer(inv -> {
                User u = inv.getArgument(0);
                u.setId(clientId);
                return u;
            });

            userService.createUser(req, adminAuth);

            verify(passwordEncoder).encode("minhasenha");
        }

        @Test
        @DisplayName("Data de nascimento ISO é parseada corretamente")
        void dateOfBirthIso() {
            UserCreateRequest req = makeCreateRequest("novo@email.com", "Novo", "CLIENT");
            req.setDateOfBirth("1990-05-15T00:00:00.000Z");

            when(userRepository.findByUsername("novo@email.com")).thenReturn(Optional.empty());
            when(passwordEncoder.encode(anyString())).thenReturn("encoded");
            when(userRepository.save(any(User.class))).thenAnswer(inv -> {
                User u = inv.getArgument(0);
                u.setId(clientId);
                return u;
            });

            User result = userService.createUser(req, adminAuth);

            assertThat(result.getDateOfBirth()).isEqualTo(LocalDate.of(1990, 5, 15));
        }

        @Test
        @DisplayName("Data de nascimento simples é parseada corretamente")
        void dateOfBirthSimple() {
            UserCreateRequest req = makeCreateRequest("novo@email.com", "Novo", "COURIER");
            req.setDateOfBirth("1985-12-25");

            when(userRepository.findByUsername("novo@email.com")).thenReturn(Optional.empty());
            when(passwordEncoder.encode(anyString())).thenReturn("encoded");
            when(userRepository.save(any(User.class))).thenAnswer(inv -> {
                User u = inv.getArgument(0);
                u.setId(courierId);
                return u;
            });

            User result = userService.createUser(req, adminAuth);

            assertThat(result.getDateOfBirth()).isEqualTo(LocalDate.of(1985, 12, 25));
        }

        @Test
        @DisplayName("Gênero válido é aceito")
        void generoValido() {
            UserCreateRequest req = makeCreateRequest("novo@email.com", "Novo", "CUSTOMER");
            req.setGender("FEMALE");

            when(userRepository.findByUsername("novo@email.com")).thenReturn(Optional.empty());
            when(passwordEncoder.encode(anyString())).thenReturn("encoded");
            when(userRepository.save(any(User.class))).thenAnswer(inv -> {
                User u = inv.getArgument(0);
                u.setId(customerId);
                return u;
            });

            User result = userService.createUser(req, adminAuth);

            assertThat(result.getGender()).isEqualTo(User.Gender.FEMALE);
        }

        @Test
        @DisplayName("Gênero inválido lança exceção")
        void generoInvalido() {
            UserCreateRequest req = makeCreateRequest("novo@email.com", "Novo", "CLIENT");
            req.setGender("INVALID");

            when(userRepository.findByUsername("novo@email.com")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.createUser(req, adminAuth))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Gênero inválido");
        }
    }

    // ================================================================
    // ROLE VALIDATION
    // ================================================================

    @Nested
    @DisplayName("createUser() — Validação de role")
    class RoleValidationTests {

        private User admin;
        private Authentication adminAuth;

        @BeforeEach
        void setUp() {
            admin = makeUser(adminId, "Admin", User.Role.ADMIN);
            adminAuth = mockAuth(admin.getUsername());
            lenient().when(userRepository.findByUsername(admin.getUsername())).thenReturn(Optional.of(admin));
        }

        @Test
        @DisplayName("Role inválida lança exceção")
        void roleInvalida() {
            UserCreateRequest req = makeCreateRequest("novo@email.com", "Novo", "INVALID_ROLE");
            when(userRepository.findByUsername("novo@email.com")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.createUser(req, adminAuth))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Role inválida");
        }

        @Test
        @DisplayName("Role case-insensitive (client → CLIENT)")
        void roleCaseInsensitive() {
            UserCreateRequest req = makeCreateRequest("novo@email.com", "Novo", "client");
            when(userRepository.findByUsername("novo@email.com")).thenReturn(Optional.empty());
            when(passwordEncoder.encode(anyString())).thenReturn("encoded");
            when(userRepository.save(any(User.class))).thenAnswer(inv -> {
                User u = inv.getArgument(0);
                u.setId(clientId);
                return u;
            });

            User result = userService.createUser(req, adminAuth);

            assertThat(result.getRole()).isEqualTo(User.Role.CLIENT);
        }
    }

    // ================================================================
    // SOFT DELETE (softDeleteMyAccount)
    // ================================================================

    @Nested
    @DisplayName("softDeleteMyAccount() — Exclusão suave da própria conta")
    class SoftDeleteTests {

        @Test
        @DisplayName("Anonimiza dados pessoais e bloqueia login")
        void anonimizaDados() {
            User user = makeUser(customerId, "João Silva", User.Role.CUSTOMER);
            user.setDocumentNumber("12345678901");
            user.setPhoneDdd("85");
            user.setPhoneNumber("999887766");
            Authentication auth = mockAuth(user.getUsername());

            when(userRepository.findByUsername(user.getUsername())).thenReturn(Optional.of(user));

            userService.softDeleteMyAccount(auth);

            assertThat(user.getUsername()).startsWith("deleted_");
            assertThat(user.getUsername()).endsWith("@removed.com");
            assertThat(user.getName()).isEqualTo("Usuário Removido");
            assertThat(user.getPhoneDdd()).isNull();
            assertThat(user.getPhoneNumber()).isNull();
            assertThat(user.isBlocked()).isTrue();
            assertThat(user.isEnabled()).isFalse();
            assertThat(user.getDeletedAt()).isNotNull();
            verify(userRepository).save(user);
        }

        @Test
        @DisplayName("Gera CPF válido para conta deletada")
        void geraCpfDeletado() {
            User user = makeUser(customerId, "João Silva", User.Role.CUSTOMER);
            Authentication auth = mockAuth(user.getUsername());

            when(userRepository.findByUsername(user.getUsername())).thenReturn(Optional.of(user));

            userService.softDeleteMyAccount(auth);

            // O documentNumber deve ter 11 dígitos (CPF)
            assertThat(user.getDocumentNumber()).hasSize(11);
            assertThat(user.getDocumentNumber()).matches("\\d{11}");
        }

        @Test
        @DisplayName("Conta demo não pode ser excluída")
        void contaDemoProtegida() {
            User demoUser = new User();
            demoUser.setId(UUID.randomUUID());
            demoUser.setUsername("demo.customer@zapi10.com");
            demoUser.setName("Demo Customer");
            demoUser.setRole(User.Role.CUSTOMER);
            Authentication auth = mockAuth(demoUser.getUsername());

            when(userRepository.findByUsername(demoUser.getUsername())).thenReturn(Optional.of(demoUser));

            assertThatThrownBy(() -> userService.softDeleteMyAccount(auth))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Contas de demonstração não podem ser excluídas");
        }

        @Test
        @DisplayName("Usuário não encontrado lança exceção")
        void usuarioNaoEncontrado() {
            Authentication auth = mockAuth("inexistente@email.com");
            when(userRepository.findByUsername("inexistente@email.com")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.softDeleteMyAccount(auth))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Usuário não encontrado");
        }
    }

    // ================================================================
    // FIND BY ID / FIND BY USERNAME
    // ================================================================

    @Nested
    @DisplayName("findById() / findByUsername() — Busca de usuários")
    class FindTests {

        @Test
        @DisplayName("findById encontra usuário existente e inicializa contracts")
        void findByIdEncontra() {
            User user = makeUser(clientId, "Restaurante X", User.Role.CLIENT);
            user.setEmploymentContracts(new HashSet<>());
            user.setClientContracts(new HashSet<>());

            when(userRepository.findByIdWithAddresses(clientId)).thenReturn(Optional.of(user));

            User result = userService.findById(clientId);

            assertThat(result.getId()).isEqualTo(clientId);
            assertThat(result.getName()).isEqualTo("Restaurante X");
        }

        @Test
        @DisplayName("findById lança exceção se não encontra")
        void findByIdNaoEncontra() {
            when(userRepository.findByIdWithAddresses(clientId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.findById(clientId))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Usuário não encontrado");
        }

        @Test
        @DisplayName("findByUsername encontra usuário existente")
        void findByUsernameEncontra() {
            User user = makeUser(clientId, "Restaurante X", User.Role.CLIENT);
            when(userRepository.findByUsername("restaurante.x@zapi10.com")).thenReturn(Optional.of(user));

            User result = userService.findByUsername("restaurante.x@zapi10.com");

            assertThat(result.getName()).isEqualTo("Restaurante X");
        }

        @Test
        @DisplayName("findByUsername lança exceção se não encontra")
        void findByUsernameNaoEncontra() {
            when(userRepository.findByUsername("inexistente@email.com")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.findByUsername("inexistente@email.com"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Usuário não encontrado");
        }
    }

    // ================================================================
    // UPDATE USER
    // ================================================================

    @Nested
    @DisplayName("updateUser() — Atualização de dados do usuário")
    class UpdateUserTests {

        private User admin;
        private Authentication adminAuth;

        @BeforeEach
        void setUp() {
            admin = makeUser(adminId, "Admin", User.Role.ADMIN);
            adminAuth = mockAuth(admin.getUsername());
            lenient().when(userRepository.findByUsername(admin.getUsername())).thenReturn(Optional.of(admin));
        }

        @Test
        @DisplayName("ADMIN atualiza nome de qualquer usuário")
        void adminAtualizaNome() {
            User user = makeUser(clientId, "Restaurante X", User.Role.CLIENT);
            user.setEmploymentContracts(new HashSet<>());
            user.setClientContracts(new HashSet<>());

            UserUpdateRequest req = new UserUpdateRequest();
            req.setName("Restaurante Y");

            when(userRepository.findByIdWithAddresses(clientId)).thenReturn(Optional.of(user));
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            User result = userService.updateUser(clientId, req, adminAuth);

            assertThat(result.getName()).isEqualTo("Restaurante Y");
        }

        @Test
        @DisplayName("Próprio usuário atualiza seus dados")
        void proprioUsuarioAtualiza() {
            User user = makeUser(clientId, "Restaurante X", User.Role.CLIENT);
            user.setEmploymentContracts(new HashSet<>());
            user.setClientContracts(new HashSet<>());
            Authentication userAuth = mockAuth(user.getUsername());

            UserUpdateRequest req = new UserUpdateRequest();
            req.setPhoneDdd("85");
            req.setPhoneNumber("999887766");

            when(userRepository.findByUsername(user.getUsername())).thenReturn(Optional.of(user));
            when(userRepository.findByIdWithAddresses(clientId)).thenReturn(Optional.of(user));
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            User result = userService.updateUser(clientId, req, userAuth);

            assertThat(result.getPhoneDdd()).isEqualTo("85");
            assertThat(result.getPhoneNumber()).isEqualTo("999887766");
        }

        @Test
        @DisplayName("Usuário não-admin não pode atualizar outro usuário")
        void naoAdminNaoPodeAtualizarOutro() {
            User courier = makeUser(courierId, "Pedro Moto", User.Role.COURIER);
            Authentication courierAuth = mockAuth(courier.getUsername());

            when(userRepository.findByUsername(courier.getUsername())).thenReturn(Optional.of(courier));

            UserUpdateRequest req = new UserUpdateRequest();
            req.setName("Hackeado");

            assertThatThrownBy(() -> userService.updateUser(clientId, req, courierAuth))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Não autorizado a atualizar este usuário");
        }

        @Test
        @DisplayName("Email duplicado na atualização é rejeitado")
        void emailDuplicadoNaAtualizacao() {
            User user = makeUser(clientId, "Restaurante X", User.Role.CLIENT);
            user.setEmploymentContracts(new HashSet<>());
            user.setClientContracts(new HashSet<>());
            User outroUser = makeUser(UUID.randomUUID(), "Outro", User.Role.CLIENT);

            UserUpdateRequest req = new UserUpdateRequest();
            req.setUsername("outro@email.com");

            when(userRepository.findByIdWithAddresses(clientId)).thenReturn(Optional.of(user));
            when(userRepository.findByUsername("outro@email.com")).thenReturn(Optional.of(outroUser));

            assertThatThrownBy(() -> userService.updateUser(clientId, req, adminAuth))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Email já cadastrado no sistema");
        }

        @Test
        @DisplayName("Gênero via alias 'M' mapeia para MALE")
        void generoAlias() {
            User user = makeUser(clientId, "Restaurante X", User.Role.CLIENT);
            user.setEmploymentContracts(new HashSet<>());
            user.setClientContracts(new HashSet<>());

            UserUpdateRequest req = new UserUpdateRequest();
            req.setGender("M");

            when(userRepository.findByIdWithAddresses(clientId)).thenReturn(Optional.of(user));
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            User result = userService.updateUser(clientId, req, adminAuth);

            assertThat(result.getGender()).isEqualTo(User.Gender.MALE);
        }

        @Test
        @DisplayName("Gênero via alias 'F' mapeia para FEMALE")
        void generoAliasF() {
            User user = makeUser(clientId, "Restaurante X", User.Role.CLIENT);
            user.setEmploymentContracts(new HashSet<>());
            user.setClientContracts(new HashSet<>());

            UserUpdateRequest req = new UserUpdateRequest();
            req.setGender("F");

            when(userRepository.findByIdWithAddresses(clientId)).thenReturn(Optional.of(user));
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            User result = userService.updateUser(clientId, req, adminAuth);

            assertThat(result.getGender()).isEqualTo(User.Gender.FEMALE);
        }

        @Test
        @DisplayName("ServiceType é atualizado para COURIER")
        void serviceTypeAtualizado() {
            User user = makeUser(courierId, "Pedro Moto", User.Role.COURIER);
            user.setEmploymentContracts(new HashSet<>());
            user.setClientContracts(new HashSet<>());

            UserUpdateRequest req = new UserUpdateRequest();
            req.setServiceType("DELIVERY");

            when(userRepository.findByIdWithAddresses(courierId)).thenReturn(Optional.of(user));
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            User result = userService.updateUser(courierId, req, adminAuth);

            assertThat(result.getServiceType()).isEqualTo(ServiceType.DELIVERY);
        }

        @Test
        @DisplayName("ServiceType inválido lança exceção")
        void serviceTypeInvalido() {
            User user = makeUser(courierId, "Pedro Moto", User.Role.COURIER);
            user.setEmploymentContracts(new HashSet<>());
            user.setClientContracts(new HashSet<>());

            UserUpdateRequest req = new UserUpdateRequest();
            req.setServiceType("INVALID");

            when(userRepository.findByIdWithAddresses(courierId)).thenReturn(Optional.of(user));

            assertThatThrownBy(() -> userService.updateUser(courierId, req, adminAuth))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("serviceType inválido");
        }

        @Test
        @DisplayName("Coordenadas GPS são atualizadas")
        void gpsAtualizado() {
            User user = makeUser(courierId, "Pedro Moto", User.Role.COURIER);
            user.setEmploymentContracts(new HashSet<>());
            user.setClientContracts(new HashSet<>());

            UserUpdateRequest req = new UserUpdateRequest();
            req.setGpsLatitude(-3.7172);
            req.setGpsLongitude(-38.5433);

            when(userRepository.findByIdWithAddresses(courierId)).thenReturn(Optional.of(user));
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            User result = userService.updateUser(courierId, req, adminAuth);

            assertThat(result.getGpsLatitude()).isEqualTo(-3.7172);
            assertThat(result.getGpsLongitude()).isEqualTo(-38.5433);
        }
    }

    // ================================================================
    // UPDATE LOCATION
    // ================================================================

    @Nested
    @DisplayName("updateUserLocation() — Atualização de localização GPS")
    class UpdateLocationTests {

        @Test
        @DisplayName("Courier atualiza própria localização com sucesso")
        void courierAtualizaLocalizacao() {
            User courier = makeUser(courierId, "Pedro Moto", User.Role.COURIER);
            Authentication auth = mockAuth(courier.getUsername());

            when(userRepository.findById(courierId)).thenReturn(Optional.of(courier));
            when(userRepository.findByUsername(courier.getUsername())).thenReturn(Optional.of(courier));
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            User result = userService.updateUserLocation(courierId, -3.69, -40.35, null, auth);

            assertThat(result.getGpsLatitude()).isEqualTo(-3.69);
            assertThat(result.getGpsLongitude()).isEqualTo(-40.35);
            assertThat(result.getUpdatedAt()).isNotNull();
        }

        @Test
        @DisplayName("ADMIN atualiza localização de qualquer usuário")
        void adminAtualizaLocalizacao() {
            User admin = makeUser(adminId, "Admin", User.Role.ADMIN);
            User courier = makeUser(courierId, "Pedro Moto", User.Role.COURIER);
            Authentication adminAuth = mockAuth(admin.getUsername());

            when(userRepository.findById(courierId)).thenReturn(Optional.of(courier));
            when(userRepository.findByUsername(admin.getUsername())).thenReturn(Optional.of(admin));
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            User result = userService.updateUserLocation(courierId, -3.70, -40.36, null, adminAuth);

            assertThat(result.getGpsLatitude()).isEqualTo(-3.70);
            assertThat(result.getGpsLongitude()).isEqualTo(-40.36);
        }

        @Test
        @DisplayName("Usuário não pode atualizar localização de outro")
        void naoAtualizaLocalizacaoDeOutro() {
            User courier1 = makeUser(courierId, "Pedro Moto", User.Role.COURIER);
            User courier2 = makeUser(UUID.randomUUID(), "Carlos Moto", User.Role.COURIER);
            Authentication auth1 = mockAuth(courier1.getUsername());

            when(userRepository.findById(courier2.getId())).thenReturn(Optional.of(courier2));
            when(userRepository.findByUsername(courier1.getUsername())).thenReturn(Optional.of(courier1));

            assertThatThrownBy(() -> userService.updateUserLocation(courier2.getId(), -3.69, -40.35, null, auth1))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Você só pode atualizar sua própria localização");
        }

        @Test
        @DisplayName("Timestamp ISO com Z é parseado corretamente")
        void timestampIsoComZ() {
            User courier = makeUser(courierId, "Pedro Moto", User.Role.COURIER);
            Authentication auth = mockAuth(courier.getUsername());

            when(userRepository.findById(courierId)).thenReturn(Optional.of(courier));
            when(userRepository.findByUsername(courier.getUsername())).thenReturn(Optional.of(courier));
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            User result = userService.updateUserLocation(courierId, -3.69, -40.35,
                    "2025-10-31T15:30:45Z", auth);

            assertThat(result.getGpsLatitude()).isEqualTo(-3.69);
            assertThat(result.getUpdatedAt()).isNotNull();
        }

        @Test
        @DisplayName("Timestamp sem T usa fallback com replace")
        void timestampSemT() {
            User courier = makeUser(courierId, "Pedro Moto", User.Role.COURIER);
            Authentication auth = mockAuth(courier.getUsername());

            when(userRepository.findById(courierId)).thenReturn(Optional.of(courier));
            when(userRepository.findByUsername(courier.getUsername())).thenReturn(Optional.of(courier));
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            User result = userService.updateUserLocation(courierId, -3.69, -40.35,
                    "2025-10-31 15:30:45", auth);

            assertThat(result.getUpdatedAt()).isNotNull();
        }

        @Test
        @DisplayName("Timestamp inválido usa timestamp atual (fallback)")
        void timestampInvalido() {
            User courier = makeUser(courierId, "Pedro Moto", User.Role.COURIER);
            Authentication auth = mockAuth(courier.getUsername());

            when(userRepository.findById(courierId)).thenReturn(Optional.of(courier));
            when(userRepository.findByUsername(courier.getUsername())).thenReturn(Optional.of(courier));
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            User result = userService.updateUserLocation(courierId, -3.69, -40.35,
                    "not-a-valid-date", auth);

            assertThat(result.getUpdatedAt()).isNotNull();
        }

        @Test
        @DisplayName("Usuário não encontrado lança exceção")
        void usuarioNaoEncontrado() {
            Authentication auth = mockAuth("admin@zapi10.com");
            when(userRepository.findById(courierId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.updateUserLocation(courierId, -3.69, -40.35, null, auth))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Usuário não encontrado");
        }
    }

    // ================================================================
    // ACTIVATION / DEACTIVATION
    // ================================================================

    @Nested
    @DisplayName("updateUser(enabled) — Ativação e desativação")
    class ActivationTests {

        private User admin;
        private Authentication adminAuth;

        @BeforeEach
        void setUp() {
            admin = makeUser(adminId, "Admin", User.Role.ADMIN);
            adminAuth = mockAuth(admin.getUsername());
            lenient().when(userRepository.findByUsername(admin.getUsername())).thenReturn(Optional.of(admin));
        }

        @Test
        @DisplayName("Ativar COURIER sem conta bancária lança exceção")
        void ativarCourierSemBanco() {
            User courier = makeUser(courierId, "Pedro Moto", User.Role.COURIER);
            courier.setEnabled(false);
            courier.setEmploymentContracts(new HashSet<>());
            courier.setClientContracts(new HashSet<>());

            UserUpdateRequest req = new UserUpdateRequest();
            req.setEnabled(true);

            when(userRepository.findByIdWithAddresses(courierId)).thenReturn(Optional.of(courier));
            when(bankAccountRepository.existsByUserId(courierId)).thenReturn(false);

            assertThatThrownBy(() -> userService.updateUser(courierId, req, adminAuth))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Courier precisa ter conta bancária cadastrada");
        }

        @Test
        @DisplayName("Ativar COURIER sem veículo lança exceção")
        void ativarCourierSemVeiculo() {
            User courier = makeUser(courierId, "Pedro Moto", User.Role.COURIER);
            courier.setEnabled(false);
            courier.setPagarmeRecipientId("rec_123");
            courier.setPagarmeStatus("active");
            courier.setEmploymentContracts(new HashSet<>());
            courier.setClientContracts(new HashSet<>());

            UserUpdateRequest req = new UserUpdateRequest();
            req.setEnabled(true);

            when(userRepository.findByIdWithAddresses(courierId)).thenReturn(Optional.of(courier));
            when(bankAccountRepository.existsByUserId(courierId)).thenReturn(true);
            when(vehicleRepository.findByOwnerId(courierId)).thenReturn(List.of());

            assertThatThrownBy(() -> userService.updateUser(courierId, req, adminAuth))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Courier precisa ter veículo cadastrado");
        }

        @Test
        @DisplayName("Ativar COURIER sem serviceType lança exceção")
        void ativarCourierSemServiceType() {
            User courier = makeUser(courierId, "Pedro Moto", User.Role.COURIER);
            courier.setEnabled(false);
            courier.setPagarmeRecipientId("rec_123");
            courier.setPagarmeStatus("active");
            courier.setServiceType(null);
            courier.setEmploymentContracts(new HashSet<>());
            courier.setClientContracts(new HashSet<>());

            UserUpdateRequest req = new UserUpdateRequest();
            req.setEnabled(true);

            when(userRepository.findByIdWithAddresses(courierId)).thenReturn(Optional.of(courier));
            when(bankAccountRepository.existsByUserId(courierId)).thenReturn(true);
            when(vehicleRepository.findByOwnerId(courierId)).thenReturn(List.of(new Vehicle()));

            assertThatThrownBy(() -> userService.updateUser(courierId, req, adminAuth))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Courier precisa ter tipo de serviço definido");
        }

        @Test
        @DisplayName("Ativar COURIER com todos requisitos — sucesso")
        void ativarCourierCompleto() {
            User courier = makeUser(courierId, "Pedro Moto", User.Role.COURIER);
            courier.setEnabled(false);
            courier.setPagarmeRecipientId("rec_123");
            courier.setPagarmeStatus("active");
            courier.setServiceType(ServiceType.DELIVERY);
            courier.setEmploymentContracts(new HashSet<>());
            courier.setClientContracts(new HashSet<>());

            UserUpdateRequest req = new UserUpdateRequest();
            req.setEnabled(true);

            when(userRepository.findByIdWithAddresses(courierId)).thenReturn(Optional.of(courier));
            when(bankAccountRepository.existsByUserId(courierId)).thenReturn(true);
            when(vehicleRepository.findByOwnerId(courierId)).thenReturn(List.of(new Vehicle()));
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            User result = userService.updateUser(courierId, req, adminAuth);

            assertThat(result.getEnabled()).isTrue();
        }

        @Test
        @DisplayName("Ativar CUSTOMER sem meio de pagamento lança exceção")
        void ativarCustomerSemPagamento() {
            User customer = makeUser(customerId, "João Silva", User.Role.CUSTOMER);
            customer.setEnabled(false);
            customer.setEmploymentContracts(new HashSet<>());
            customer.setClientContracts(new HashSet<>());

            UserUpdateRequest req = new UserUpdateRequest();
            req.setEnabled(true);

            when(userRepository.findByIdWithAddresses(customerId)).thenReturn(Optional.of(customer));
            when(customerCardRepository.countActiveCardsByCustomerId(customerId)).thenReturn(0L);
            when(customerPaymentPreferenceRepository.findByUserId(customerId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.updateUser(customerId, req, adminAuth))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Cliente precisa ter cartão ativo ou preferência PIX");
        }

        @Test
        @DisplayName("Ativar CUSTOMER com PIX — sucesso")
        void ativarCustomerComPix() {
            User customer = makeUser(customerId, "João Silva", User.Role.CUSTOMER);
            customer.setEnabled(false);
            customer.setEmploymentContracts(new HashSet<>());
            customer.setClientContracts(new HashSet<>());

            CustomerPaymentPreference pref = CustomerPaymentPreference.builder()
                    .preferredPaymentType(CustomerPaymentPreference.PreferredPaymentType.PIX)
                    .build();

            UserUpdateRequest req = new UserUpdateRequest();
            req.setEnabled(true);

            when(userRepository.findByIdWithAddresses(customerId)).thenReturn(Optional.of(customer));
            when(customerCardRepository.countActiveCardsByCustomerId(customerId)).thenReturn(0L);
            when(customerPaymentPreferenceRepository.findByUserId(customerId)).thenReturn(Optional.of(pref));
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            User result = userService.updateUser(customerId, req, adminAuth);

            assertThat(result.getEnabled()).isTrue();
        }

        @Test
        @DisplayName("Ativar CUSTOMER com cartão ativo — sucesso")
        void ativarCustomerComCartao() {
            User customer = makeUser(customerId, "João Silva", User.Role.CUSTOMER);
            customer.setEnabled(false);
            customer.setEmploymentContracts(new HashSet<>());
            customer.setClientContracts(new HashSet<>());

            UserUpdateRequest req = new UserUpdateRequest();
            req.setEnabled(true);

            when(userRepository.findByIdWithAddresses(customerId)).thenReturn(Optional.of(customer));
            when(customerCardRepository.countActiveCardsByCustomerId(customerId)).thenReturn(1L);
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            User result = userService.updateUser(customerId, req, adminAuth);

            assertThat(result.getEnabled()).isTrue();
        }

        @Test
        @DisplayName("Ativar ORGANIZER sem conta bancária lança exceção")
        void ativarOrganizerSemBanco() {
            User organizer = makeUser(organizerId, "Ana Gerente", User.Role.ORGANIZER);
            organizer.setEnabled(false);
            organizer.setEmploymentContracts(new HashSet<>());
            organizer.setClientContracts(new HashSet<>());

            UserUpdateRequest req = new UserUpdateRequest();
            req.setEnabled(true);

            when(userRepository.findByIdWithAddresses(organizerId)).thenReturn(Optional.of(organizer));
            when(bankAccountRepository.existsByUserId(organizerId)).thenReturn(false);

            assertThatThrownBy(() -> userService.updateUser(organizerId, req, adminAuth))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Organizer precisa ter conta bancária cadastrada");
        }

        @Test
        @DisplayName("Ativar ORGANIZER sem Pagar.me lança exceção")
        void ativarOrganizerSemPagarme() {
            User organizer = makeUser(organizerId, "Ana Gerente", User.Role.ORGANIZER);
            organizer.setEnabled(false);
            organizer.setPagarmeRecipientId(null);
            organizer.setEmploymentContracts(new HashSet<>());
            organizer.setClientContracts(new HashSet<>());

            UserUpdateRequest req = new UserUpdateRequest();
            req.setEnabled(true);

            when(userRepository.findByIdWithAddresses(organizerId)).thenReturn(Optional.of(organizer));
            when(bankAccountRepository.existsByUserId(organizerId)).thenReturn(true);

            assertThatThrownBy(() -> userService.updateUser(organizerId, req, adminAuth))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Organizer precisa ter dados de saque configurados");
        }

        @Test
        @DisplayName("Desativar qualquer role — sucesso (sem validações)")
        void desativarSemValidacao() {
            User courier = makeUser(courierId, "Pedro Moto", User.Role.COURIER);
            courier.setEnabled(true);
            courier.setEmploymentContracts(new HashSet<>());
            courier.setClientContracts(new HashSet<>());

            UserUpdateRequest req = new UserUpdateRequest();
            req.setEnabled(false);

            when(userRepository.findByIdWithAddresses(courierId)).thenReturn(Optional.of(courier));
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            User result = userService.updateUser(courierId, req, adminAuth);

            assertThat(result.getEnabled()).isFalse();
        }

        @Test
        @DisplayName("Bloquear usuário via campo blocked")
        void bloquearUsuario() {
            User user = makeUser(clientId, "Restaurante X", User.Role.CLIENT);
            user.setEmploymentContracts(new HashSet<>());
            user.setClientContracts(new HashSet<>());

            UserUpdateRequest req = new UserUpdateRequest();
            req.setBlocked(true);

            when(userRepository.findByIdWithAddresses(clientId)).thenReturn(Optional.of(user));
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            User result = userService.updateUser(clientId, req, adminAuth);

            assertThat(result.isBlocked()).isTrue();
        }
    }

    // ================================================================
    // DELETE USER (hard delete by admin)
    // ================================================================

    @Nested
    @DisplayName("deleteUser() — Exclusão por admin")
    class DeleteUserTests {

        @Test
        @DisplayName("ADMIN pode deletar qualquer usuário")
        void adminDeleta() {
            User admin = makeUser(adminId, "Admin", User.Role.ADMIN);
            User target = makeUser(clientId, "Restaurante X", User.Role.CLIENT);
            target.setEmploymentContracts(new HashSet<>());
            target.setClientContracts(new HashSet<>());
            Authentication adminAuth = mockAuth(admin.getUsername());

            when(userRepository.findByUsername(admin.getUsername())).thenReturn(Optional.of(admin));
            when(userRepository.findByIdWithAddresses(clientId)).thenReturn(Optional.of(target));

            userService.deleteUser(clientId, adminAuth);

            verify(userRepository).delete(target);
        }

        @Test
        @DisplayName("Não-ADMIN não pode deletar")
        void naoAdminNaoDeleta() {
            User courier = makeUser(courierId, "Pedro Moto", User.Role.COURIER);
            Authentication courierAuth = mockAuth(courier.getUsername());

            when(userRepository.findByUsername(courier.getUsername())).thenReturn(Optional.of(courier));

            assertThatThrownBy(() -> userService.deleteUser(clientId, courierAuth))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Apenas administradores podem deletar usuários");
        }
    }
}
