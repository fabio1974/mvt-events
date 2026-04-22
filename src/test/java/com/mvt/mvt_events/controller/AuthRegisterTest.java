package com.mvt.mvt_events.controller;

import com.mvt.mvt_events.jpa.Organization;
import com.mvt.mvt_events.jpa.User;
import com.mvt.mvt_events.jpa.CustomerPaymentPreference;
import com.mvt.mvt_events.repository.CustomerPaymentPreferenceRepository;
import com.mvt.mvt_events.repository.OrganizationRepository;
import com.mvt.mvt_events.repository.UserRepository;
import com.mvt.mvt_events.service.EmailService;
import com.mvt.mvt_events.service.RestaurantTableService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Testes do AuthController.register() — verifica que registros criam
 * entidades dependentes automaticamente (Organization para ORGANIZER,
 * PaymentPreference para CLIENT) dentro da mesma transação.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthController.register() — criação transacional de entidades")
class AuthRegisterTest {

    @Mock private UserRepository userRepository;
    @Mock private OrganizationRepository organizationRepository;
    @Mock private CustomerPaymentPreferenceRepository customerPaymentPreferenceRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private EmailService emailService;
    @Mock private RestaurantTableService restaurantTableService;

    @InjectMocks private AuthController authController;

    private AuthController.RegisterRequest makeRequest(String email, String name, String role) {
        AuthController.RegisterRequest req = new AuthController.RegisterRequest();
        req.setUsername(email);
        req.setPassword("Senha@123");
        req.setName(name);
        req.setRole(role);
        return req;
    }

    private void setupCommonMocks() {
        when(userRepository.findByUsername(anyString())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(UUID.randomUUID());
            return u;
        });
    }

    // ================================================================
    // ORGANIZER — Organization criada automaticamente
    // ================================================================

    @Nested
    @DisplayName("Registro de ORGANIZER")
    class OrganizerRegistration {

        @Test
        @DisplayName("ORGANIZER recebe Organization com owner = ele mesmo")
        void organizerCriaOrganization() {
            setupCommonMocks();
            when(organizationRepository.save(any(Organization.class))).thenAnswer(inv -> inv.getArgument(0));

            authController.register(makeRequest("davi@gmail.com", "Davi Pop", "ORGANIZER"));

            ArgumentCaptor<Organization> orgCaptor = ArgumentCaptor.forClass(Organization.class);
            verify(organizationRepository).save(orgCaptor.capture());

            Organization org = orgCaptor.getValue();
            assertThat(org.getOwner()).isNotNull();
            assertThat(org.getOwner().getUsername()).isEqualTo("davi@gmail.com");
            assertThat(org.getName()).isEqualTo("Grupo de Davi Pop");
        }

        @Test
        @DisplayName("ORGANIZER NÃO recebe preferência de pagamento PIX")
        void organizerSemPix() {
            setupCommonMocks();
            when(organizationRepository.save(any(Organization.class))).thenAnswer(inv -> inv.getArgument(0));

            authController.register(makeRequest("davi@gmail.com", "Davi Pop", "ORGANIZER"));

            verify(customerPaymentPreferenceRepository, never()).save(any());
        }

        @Test
        @DisplayName("ORGANIZER — registro completa sem erro (campo slug foi removido em V129)")
        void organizerRegistraSemSlug() {
            setupCommonMocks();
            when(organizationRepository.save(any(Organization.class))).thenAnswer(inv -> inv.getArgument(0));

            assertThatCode(() ->
                authController.register(makeRequest("fabio@gmail.com", "Fabio Gerente", "ORGANIZER"))
            ).doesNotThrowAnyException();

            verify(organizationRepository).save(any(Organization.class));
        }
    }

    // ================================================================
    // CLIENT — PaymentPreference PIX criada automaticamente
    // ================================================================

    @Nested
    @DisplayName("Registro de CLIENT")
    class ClientRegistration {

        @Test
        @DisplayName("CLIENT recebe preferência PIX automaticamente")
        void clientRecebePix() {
            setupCommonMocks();
            when(customerPaymentPreferenceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            authController.register(makeRequest("restaurante@gmail.com", "Pizza Express", "CLIENT"));

            ArgumentCaptor<CustomerPaymentPreference> prefCaptor = ArgumentCaptor.forClass(CustomerPaymentPreference.class);
            verify(customerPaymentPreferenceRepository).save(prefCaptor.capture());

            CustomerPaymentPreference pref = prefCaptor.getValue();
            assertThat(pref.getPreferredPaymentType()).isEqualTo(CustomerPaymentPreference.PreferredPaymentType.PIX);
            assertThat(pref.getUser().getUsername()).isEqualTo("restaurante@gmail.com");
        }

        @Test
        @DisplayName("CLIENT NÃO recebe Organization")
        void clientSemOrganization() {
            setupCommonMocks();
            when(customerPaymentPreferenceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            authController.register(makeRequest("restaurante@gmail.com", "Pizza Express", "CLIENT"));

            verify(organizationRepository, never()).save(any());
        }
    }

    // ================================================================
    // CUSTOMER — nenhuma entidade extra
    // ================================================================

    @Nested
    @DisplayName("Registro de CUSTOMER")
    class CustomerRegistration {

        @Test
        @DisplayName("CUSTOMER salva User com role correto, senha criptografada, sem extras")
        void customerSemExtras() {
            setupCommonMocks();

            authController.register(makeRequest("joao@gmail.com", "João Silva", "CUSTOMER"));

            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(userCaptor.capture());
            User saved = userCaptor.getValue();
            assertThat(saved.getRole()).isEqualTo(User.Role.CUSTOMER);
            assertThat(saved.getName()).isEqualTo("João Silva");
            assertThat(saved.getUsername()).isEqualTo("joao@gmail.com");
            assertThat(saved.getPassword()).isEqualTo("encoded");

            verify(organizationRepository, never()).save(any());
            verify(customerPaymentPreferenceRepository, never()).save(any());
        }
    }

    // ================================================================
    // COURIER — nenhuma entidade extra
    // ================================================================

    @Nested
    @DisplayName("Registro de COURIER")
    class CourierRegistration {

        @Test
        @DisplayName("COURIER salva User com role correto, senha criptografada, sem extras")
        void courierSemExtras() {
            setupCommonMocks();

            authController.register(makeRequest("pedro@gmail.com", "Pedro Moto", "COURIER"));

            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(userCaptor.capture());
            User saved = userCaptor.getValue();
            assertThat(saved.getRole()).isEqualTo(User.Role.COURIER);
            assertThat(saved.getName()).isEqualTo("Pedro Moto");
            assertThat(saved.getPassword()).isEqualTo("encoded");

            verify(organizationRepository, never()).save(any());
            verify(customerPaymentPreferenceRepository, never()).save(any());
        }
    }

    // ================================================================
    // WAITER — nenhuma entidade extra
    // ================================================================

    @Nested
    @DisplayName("Registro de WAITER")
    class WaiterRegistration {

        @Test
        @DisplayName("WAITER salva User com role correto, senha criptografada, sem extras")
        void waiterSemExtras() {
            setupCommonMocks();

            authController.register(makeRequest("ana.garcom@gmail.com", "Ana Garçom", "WAITER"));

            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(userCaptor.capture());
            User saved = userCaptor.getValue();
            assertThat(saved.getRole()).isEqualTo(User.Role.WAITER);
            assertThat(saved.getName()).isEqualTo("Ana Garçom");
            assertThat(saved.getPassword()).isEqualTo("encoded");

            verify(organizationRepository, never()).save(any());
            verify(customerPaymentPreferenceRepository, never()).save(any());
        }
    }

    // ================================================================
    // Duplicidade de email
    // ================================================================

    @Nested
    @DisplayName("Validações de registro")
    class RegistrationValidation {

        @Test
        @DisplayName("Email duplicado lança exceção")
        void emailDuplicado() {
            User existing = new User();
            existing.setUsername("davi@gmail.com");
            when(userRepository.findByUsername("davi@gmail.com")).thenReturn(Optional.of(existing));

            assertThatThrownBy(() -> authController.register(makeRequest("davi@gmail.com", "Davi", "ORGANIZER")))
                    .isInstanceOf(Exception.class);

            verify(organizationRepository, never()).save(any());
        }
    }
}
