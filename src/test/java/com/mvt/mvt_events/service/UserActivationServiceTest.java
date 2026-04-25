package com.mvt.mvt_events.service;

import com.mvt.mvt_events.jpa.CustomerPaymentPreference;
import com.mvt.mvt_events.jpa.ServiceType;
import com.mvt.mvt_events.jpa.User;
import com.mvt.mvt_events.jpa.Vehicle;
import com.mvt.mvt_events.repository.BankAccountRepository;
import com.mvt.mvt_events.repository.CustomerCardRepository;
import com.mvt.mvt_events.repository.CustomerPaymentPreferenceRepository;
import com.mvt.mvt_events.repository.UserRepository;
import com.mvt.mvt_events.repository.VehicleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Cobre a lógica de recálculo do flag {@code User.enabled} a partir dos pré-requisitos por role.
 * Espelha as condições de {@code UserIntegrityCheck} e {@code UserService.getActivationStatus}.
 */
@ExtendWith(MockitoExtension.class)
class UserActivationServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private BankAccountRepository bankAccountRepository;
    @Mock private VehicleRepository vehicleRepository;
    @Mock private CustomerCardRepository customerCardRepository;
    @Mock private CustomerPaymentPreferenceRepository customerPaymentPreferenceRepository;

    @InjectMocks
    private UserActivationService service;

    private final UUID userId = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

    private User makeUser(User.Role role, boolean initialEnabled) {
        User u = new User();
        u.setId(userId);
        u.setRole(role);
        u.setUsername("user@test.com");
        u.setEnabled(initialEnabled);
        return u;
    }

    private void mockUser(User user) {
        lenient().when(userRepository.findById(userId)).thenReturn(Optional.of(user));
    }

    private void mockHasBank(boolean has) {
        lenient().when(bankAccountRepository.existsByUserId(userId)).thenReturn(has);
    }

    private void mockHasVehicle(boolean has) {
        lenient().when(vehicleRepository.findByOwnerId(userId))
                .thenReturn(has ? List.of(new Vehicle()) : Collections.emptyList());
    }

    private void mockPagarmeOk(User user, boolean ok) {
        if (ok) {
            user.setPagarmeRecipientId("rec_123");
            user.setPagarmeStatus("active");
        } else {
            user.setPagarmeRecipientId(null);
            user.setPagarmeStatus(null);
        }
    }

    private void mockHasActiveCard(boolean has) {
        lenient().when(customerCardRepository.countActiveCardsByCustomerId(userId))
                .thenReturn(has ? 1L : 0L);
    }

    private void mockPixPreference(boolean has) {
        if (has) {
            CustomerPaymentPreference pref = CustomerPaymentPreference.builder()
                    .preferredPaymentType(CustomerPaymentPreference.PreferredPaymentType.PIX)
                    .build();
            lenient().when(customerPaymentPreferenceRepository.findByUserId(userId))
                    .thenReturn(Optional.of(pref));
        } else {
            lenient().when(customerPaymentPreferenceRepository.findByUserId(userId))
                    .thenReturn(Optional.empty());
        }
    }

    // ================================================================
    // GUARDS — usuário inválido / blocked / deleted
    // ================================================================

    @Nested
    @DisplayName("Guards — usuário ausente / blocked / deleted")
    class GuardsTests {

        @Test
        @DisplayName("userId null retorna false sem consultar repos")
        void userIdNull() {
            assertThat(service.recalculate(null)).isFalse();
            verify(userRepository, never()).findById(any());
        }

        @Test
        @DisplayName("userId inexistente retorna false")
        void userIdInexistente() {
            when(userRepository.findById(userId)).thenReturn(Optional.empty());
            assertThat(service.recalculate(userId)).isFalse();
        }

        @Test
        @DisplayName("Usuário blocked nunca é reativado, mesmo cumprindo prereqs")
        void blockedNuncaReativa() {
            User u = makeUser(User.Role.COURIER, true);
            u.setBlocked(true);
            mockUser(u);

            // Mesmo se cumprisse prereqs, blocked força enabled=false
            mockHasBank(true);
            mockHasVehicle(true);
            mockPagarmeOk(u, true);
            u.setServiceType(ServiceType.DELIVERY);

            assertThat(service.recalculate(userId)).isFalse();
            verify(userRepository).save(u);
            assertThat(u.getEnabled()).isFalse();
        }

        @Test
        @DisplayName("Usuário deletado (deletedAt != null) nunca é reativado")
        void deletedNuncaReativa() {
            User u = makeUser(User.Role.COURIER, true);
            u.setDeletedAt(LocalDateTime.now());
            mockUser(u);

            assertThat(service.recalculate(userId)).isFalse();
            assertThat(u.getEnabled()).isFalse();
        }

        @Test
        @DisplayName("Usuário blocked já com enabled=false não é re-salvo (idempotência)")
        void blockedJaDesativadoNaoSalva() {
            User u = makeUser(User.Role.COURIER, false);
            u.setBlocked(true);
            mockUser(u);

            assertThat(service.recalculate(userId)).isFalse();
            verify(userRepository, never()).save(any());
        }
    }

    // ================================================================
    // COURIER — banco + pagarme + veículo + serviceType
    // ================================================================

    @Nested
    @DisplayName("COURIER — todos os 4 prereqs")
    class CourierTests {

        @Test
        @DisplayName("Sem nenhum prereq → enabled=false")
        void semPrereqs() {
            User u = makeUser(User.Role.COURIER, true);
            mockUser(u);
            mockHasBank(false);
            mockHasVehicle(false);

            assertThat(service.recalculate(userId)).isFalse();
            assertThat(u.getEnabled()).isFalse();
        }

        @Test
        @DisplayName("Só banco → enabled=false (faltam 3)")
        void soBanco() {
            User u = makeUser(User.Role.COURIER, true);
            mockUser(u);
            mockHasBank(true);
            mockHasVehicle(false);
            mockPagarmeOk(u, false);

            assertThat(service.recalculate(userId)).isFalse();
        }

        @Test
        @DisplayName("Banco + pagarme + veículo, sem serviceType → enabled=false")
        void faltaServiceType() {
            User u = makeUser(User.Role.COURIER, false);
            mockUser(u);
            mockHasBank(true);
            mockHasVehicle(true);
            mockPagarmeOk(u, true);
            u.setServiceType(null);

            assertThat(service.recalculate(userId)).isFalse();
        }

        @Test
        @DisplayName("Pagar.me com status diferente de 'active' → enabled=false")
        void pagarmeNaoActive() {
            User u = makeUser(User.Role.COURIER, true);
            mockUser(u);
            mockHasBank(true);
            mockHasVehicle(true);
            u.setPagarmeRecipientId("rec_123");
            u.setPagarmeStatus("registration"); // não é 'active'
            u.setServiceType(ServiceType.DELIVERY);

            assertThat(service.recalculate(userId)).isFalse();
        }

        @Test
        @DisplayName("Todos os 4 prereqs → enabled=true (transição false→true persiste)")
        void todosPrereqsAtiva() {
            User u = makeUser(User.Role.COURIER, false);
            mockUser(u);
            mockHasBank(true);
            mockHasVehicle(true);
            mockPagarmeOk(u, true);
            u.setServiceType(ServiceType.BOTH);

            assertThat(service.recalculate(userId)).isTrue();
            verify(userRepository).save(u);
            assertThat(u.getEnabled()).isTrue();
        }

        @Test
        @DisplayName("Já enabled=true e prereqs OK → não re-salva (idempotência)")
        void jaAtivadoIdempotente() {
            User u = makeUser(User.Role.COURIER, true);
            mockUser(u);
            mockHasBank(true);
            mockHasVehicle(true);
            mockPagarmeOk(u, true);
            u.setServiceType(ServiceType.DELIVERY);

            assertThat(service.recalculate(userId)).isTrue();
            verify(userRepository, never()).save(any());
        }
    }

    // ================================================================
    // ORGANIZER — banco + pagarme
    // ================================================================

    @Nested
    @DisplayName("ORGANIZER — banco + pagarme")
    class OrganizerTests {

        @Test
        @DisplayName("Sem prereqs → false")
        void semPrereqs() {
            User u = makeUser(User.Role.ORGANIZER, true);
            mockUser(u);
            mockHasBank(false);

            assertThat(service.recalculate(userId)).isFalse();
        }

        @Test
        @DisplayName("Banco + pagarme.active → true")
        void completo() {
            User u = makeUser(User.Role.ORGANIZER, false);
            mockUser(u);
            mockHasBank(true);
            mockPagarmeOk(u, true);

            assertThat(service.recalculate(userId)).isTrue();
            verify(userRepository).save(u);
        }
    }

    // ================================================================
    // CUSTOMER — cartão ativo OU PIX
    // ================================================================

    @Nested
    @DisplayName("CUSTOMER — cartão ativo OU preferência PIX")
    class CustomerTests {

        @Test
        @DisplayName("Sem cartão e sem PIX → false")
        void semNada() {
            User u = makeUser(User.Role.CUSTOMER, true);
            mockUser(u);
            mockHasActiveCard(false);
            mockPixPreference(false);

            assertThat(service.recalculate(userId)).isFalse();
        }

        @Test
        @DisplayName("Apenas cartão ativo → true")
        void soCartao() {
            User u = makeUser(User.Role.CUSTOMER, false);
            mockUser(u);
            mockHasActiveCard(true);
            mockPixPreference(false);

            assertThat(service.recalculate(userId)).isTrue();
        }

        @Test
        @DisplayName("Apenas preferência PIX → true")
        void soPix() {
            User u = makeUser(User.Role.CUSTOMER, false);
            mockUser(u);
            mockHasActiveCard(false);
            mockPixPreference(true);

            assertThat(service.recalculate(userId)).isTrue();
        }
    }

    // ================================================================
    // CLIENT — banco + (cartão ou PIX)
    // ================================================================

    @Nested
    @DisplayName("CLIENT — banco + (cartão ou PIX)")
    class ClientTests {

        @Test
        @DisplayName("Sem banco → false (mesmo com PIX)")
        void semBanco() {
            User u = makeUser(User.Role.CLIENT, true);
            mockUser(u);
            mockHasBank(false);
            mockPixPreference(true);

            assertThat(service.recalculate(userId)).isFalse();
        }

        @Test
        @DisplayName("Banco + PIX → true")
        void bancoMaisPix() {
            User u = makeUser(User.Role.CLIENT, false);
            mockUser(u);
            mockHasBank(true);
            mockHasActiveCard(false);
            mockPixPreference(true);

            assertThat(service.recalculate(userId)).isTrue();
        }

        @Test
        @DisplayName("Banco + cartão → true")
        void bancoMaisCartao() {
            User u = makeUser(User.Role.CLIENT, false);
            mockUser(u);
            mockHasBank(true);
            mockHasActiveCard(true);
            mockPixPreference(false);

            assertThat(service.recalculate(userId)).isTrue();
        }
    }

    // ================================================================
    // OUTROS ROLES — sem mudança
    // ================================================================

    @Nested
    @DisplayName("Roles sem prereqs transacionais (ADMIN, WAITER) — mantêm estado")
    class OthersTests {

        @Test
        @DisplayName("ADMIN com enabled=true permanece true sem chamar repos de prereq")
        void adminMantem() {
            User u = makeUser(User.Role.ADMIN, true);
            mockUser(u);

            assertThat(service.recalculate(userId)).isTrue();
            verify(userRepository, never()).save(any());
            verify(bankAccountRepository, never()).existsByUserId(any());
        }

        @Test
        @DisplayName("WAITER com enabled=false permanece false (não é reativado por daemon)")
        void waiterMantem() {
            User u = makeUser(User.Role.WAITER, false);
            mockUser(u);

            assertThat(service.recalculate(userId)).isFalse();
            verify(userRepository, never()).save(any());
        }
    }
}
