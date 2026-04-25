package com.mvt.mvt_events.service;

import com.mvt.mvt_events.jpa.CustomerCard;
import com.mvt.mvt_events.jpa.CustomerPaymentPreference;
import com.mvt.mvt_events.jpa.CustomerPaymentPreference.PreferredPaymentType;
import com.mvt.mvt_events.jpa.User;
import com.mvt.mvt_events.repository.CustomerCardRepository;
import com.mvt.mvt_events.repository.CustomerPaymentPreferenceRepository;
import com.mvt.mvt_events.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Testes unitarios do CustomerPaymentPreferenceService -- cobre getPreference,
 * savePreference, setPixAsPreferred, setCreditCardAsPreferred, prefersPix e prefersCreditCard.
 */
@ExtendWith(MockitoExtension.class)
class CustomerPaymentPreferenceServiceTest {

    @Mock private CustomerPaymentPreferenceRepository preferenceRepository;
    @Mock private CustomerCardRepository cardRepository;
    @Mock private UserRepository userRepository;
    @Mock private UserActivationService userActivationService;

    @InjectMocks
    private CustomerPaymentPreferenceService service;

    // ========== Helpers ==========

    private final UUID userId = UUID.fromString("00000000-0000-0000-0000-000000000001");

    private User makeUser() {
        User u = new User();
        u.setId(userId);
        u.setName("Joao");
        u.setUsername("joao@zapi10.com");
        u.setRole(User.Role.CUSTOMER);
        return u;
    }

    private CustomerCard makeCard(Long id, boolean active) {
        CustomerCard card = new CustomerCard();
        card.setId(id);
        card.setCustomer(makeUser());
        card.setLastFourDigits("4242");
        card.setBrand(CustomerCard.CardBrand.VISA);
        card.setIsActive(active);
        card.setExpMonth(12);
        card.setExpYear(2030);
        return card;
    }

    private CustomerPaymentPreference makePref(PreferredPaymentType type, CustomerCard card) {
        return CustomerPaymentPreference.builder()
                .user(makeUser())
                .preferredPaymentType(type)
                .defaultCard(card)
                .build();
    }

    // ================================================================
    // getPreference
    // ================================================================

    @Nested
    @DisplayName("getPreference() -- Busca preferencia de pagamento")
    class GetPreferenceTests {

        @Test
        @DisplayName("Retorna null quando nao existe preferencia")
        void retornaNullQuandoNaoExiste() {
            when(preferenceRepository.findByUserId(userId)).thenReturn(Optional.empty());

            CustomerPaymentPreference result = service.getPreference(userId);

            assertThat(result).isNull();
        }

        @Test
        @DisplayName("Retorna preferencia existente com cartao inicializado")
        void retornaPreferenciaComCartao() {
            CustomerCard card = makeCard(1L, true);
            CustomerPaymentPreference pref = makePref(PreferredPaymentType.CREDIT_CARD, card);
            when(preferenceRepository.findByUserId(userId)).thenReturn(Optional.of(pref));

            CustomerPaymentPreference result = service.getPreference(userId);

            assertThat(result).isNotNull();
            assertThat(result.getPreferredPaymentType()).isEqualTo(PreferredPaymentType.CREDIT_CARD);
            assertThat(result.getDefaultCard()).isNotNull();
        }

        @Test
        @DisplayName("Retorna preferencia PIX sem cartao")
        void retornaPreferenciaPixSemCartao() {
            CustomerPaymentPreference pref = makePref(PreferredPaymentType.PIX, null);
            when(preferenceRepository.findByUserId(userId)).thenReturn(Optional.of(pref));

            CustomerPaymentPreference result = service.getPreference(userId);

            assertThat(result).isNotNull();
            assertThat(result.getPreferredPaymentType()).isEqualTo(PreferredPaymentType.PIX);
            assertThat(result.getDefaultCard()).isNull();
        }
    }

    // ================================================================
    // savePreference
    // ================================================================

    @Nested
    @DisplayName("savePreference() -- Salva preferencia de pagamento")
    class SavePreferenceTests {

        @Test
        @DisplayName("Salva preferencia PIX com sucesso")
        void salvaPixComSucesso() {
            User user = makeUser();
            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(preferenceRepository.findByUserId(userId)).thenReturn(Optional.empty());
            when(preferenceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            CustomerPaymentPreference result = service.savePreference(userId, PreferredPaymentType.PIX, null);

            assertThat(result.getPreferredPaymentType()).isEqualTo(PreferredPaymentType.PIX);
            assertThat(result.getDefaultCard()).isNull();
        }

        @Test
        @DisplayName("Salva preferencia CREDIT_CARD com cartao valido")
        void salvaCreditCardComCartaoValido() {
            User user = makeUser();
            CustomerCard card = makeCard(1L, true);
            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(preferenceRepository.findByUserId(userId)).thenReturn(Optional.empty());
            when(cardRepository.findById(1L)).thenReturn(Optional.of(card));
            when(preferenceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            CustomerPaymentPreference result = service.savePreference(userId, PreferredPaymentType.CREDIT_CARD, 1L);

            assertThat(result.getPreferredPaymentType()).isEqualTo(PreferredPaymentType.CREDIT_CARD);
            assertThat(result.getDefaultCard()).isSameAs(card);
        }

        @Test
        @DisplayName("Lanca excecao quando usuario nao encontrado")
        void lancaExcecaoUsuarioNaoEncontrado() {
            when(userRepository.findById(userId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.savePreference(userId, PreferredPaymentType.PIX, null))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("não encontrado");
        }

        @Test
        @DisplayName("Lanca excecao quando CREDIT_CARD sem cardId")
        void lancaExcecaoSemCardId() {
            User user = makeUser();
            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(preferenceRepository.findByUserId(userId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.savePreference(userId, PreferredPaymentType.CREDIT_CARD, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("obrigatório");
        }

        @Test
        @DisplayName("Lanca excecao quando cartao nao encontrado")
        void lancaExcecaoCartaoNaoEncontrado() {
            User user = makeUser();
            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(preferenceRepository.findByUserId(userId)).thenReturn(Optional.empty());
            when(cardRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.savePreference(userId, PreferredPaymentType.CREDIT_CARD, 999L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Cartão não encontrado");
        }

        @Test
        @DisplayName("Lanca excecao quando cartao nao pertence ao usuario")
        void lancaExcecaoCartaoDeOutroUsuario() {
            User user = makeUser();
            CustomerCard card = makeCard(1L, true);
            // Cartao pertence a outro usuario
            User outroUser = new User();
            outroUser.setId(UUID.randomUUID());
            card.setCustomer(outroUser);

            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(preferenceRepository.findByUserId(userId)).thenReturn(Optional.empty());
            when(cardRepository.findById(1L)).thenReturn(Optional.of(card));

            assertThatThrownBy(() -> service.savePreference(userId, PreferredPaymentType.CREDIT_CARD, 1L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("não pertence ao usuário");
        }

        @Test
        @DisplayName("Lanca excecao quando cartao esta inativo")
        void lancaExcecaoCartaoInativo() {
            User user = makeUser();
            CustomerCard card = makeCard(1L, false); // inativo

            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(preferenceRepository.findByUserId(userId)).thenReturn(Optional.empty());
            when(cardRepository.findById(1L)).thenReturn(Optional.of(card));

            assertThatThrownBy(() -> service.savePreference(userId, PreferredPaymentType.CREDIT_CARD, 1L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("inativo");
        }

        @Test
        @DisplayName("Atualiza preferencia existente em vez de criar nova")
        void atualizaPreferenciaExistente() {
            User user = makeUser();
            CustomerPaymentPreference existing = makePref(PreferredPaymentType.CREDIT_CARD, makeCard(1L, true));
            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(preferenceRepository.findByUserId(userId)).thenReturn(Optional.of(existing));
            when(preferenceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            CustomerPaymentPreference result = service.savePreference(userId, PreferredPaymentType.PIX, null);

            assertThat(result).isSameAs(existing);
            assertThat(result.getPreferredPaymentType()).isEqualTo(PreferredPaymentType.PIX);
            assertThat(result.getDefaultCard()).isNull();
        }
    }

    // ================================================================
    // setPixAsPreferred / setCreditCardAsPreferred
    // ================================================================

    @Nested
    @DisplayName("setPixAsPreferred() / setCreditCardAsPreferred()")
    class ShortcutMethodsTests {

        @Test
        @DisplayName("setPixAsPreferred delega para savePreference com PIX")
        void setPixDelegaParaSavePreference() {
            User user = makeUser();
            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(preferenceRepository.findByUserId(userId)).thenReturn(Optional.empty());
            when(preferenceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            CustomerPaymentPreference result = service.setPixAsPreferred(userId);

            assertThat(result.getPreferredPaymentType()).isEqualTo(PreferredPaymentType.PIX);
        }

        @Test
        @DisplayName("setCreditCardAsPreferred delega para savePreference com CREDIT_CARD")
        void setCreditCardDelegaParaSavePreference() {
            User user = makeUser();
            CustomerCard card = makeCard(1L, true);
            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(preferenceRepository.findByUserId(userId)).thenReturn(Optional.empty());
            when(cardRepository.findById(1L)).thenReturn(Optional.of(card));
            when(preferenceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            CustomerPaymentPreference result = service.setCreditCardAsPreferred(userId, 1L);

            assertThat(result.getPreferredPaymentType()).isEqualTo(PreferredPaymentType.CREDIT_CARD);
            assertThat(result.getDefaultCard()).isSameAs(card);
        }
    }

    // ================================================================
    // prefersPix / prefersCreditCard
    // ================================================================

    @Nested
    @DisplayName("prefersPix() / prefersCreditCard()")
    class PreferenceCheckTests {

        @Test
        @DisplayName("prefersPix retorna true quando preferencia e PIX")
        void prefersPixTrue() {
            CustomerPaymentPreference pref = makePref(PreferredPaymentType.PIX, null);
            when(preferenceRepository.findByUserId(userId)).thenReturn(Optional.of(pref));

            assertThat(service.prefersPix(userId)).isTrue();
        }

        @Test
        @DisplayName("prefersPix retorna false quando preferencia e CREDIT_CARD")
        void prefersPixFalseQuandoCreditCard() {
            CustomerPaymentPreference pref = makePref(PreferredPaymentType.CREDIT_CARD, makeCard(1L, true));
            when(preferenceRepository.findByUserId(userId)).thenReturn(Optional.of(pref));

            assertThat(service.prefersPix(userId)).isFalse();
        }

        @Test
        @DisplayName("prefersPix retorna false quando nao ha preferencia")
        void prefersPixFalseQuandoSemPreferencia() {
            when(preferenceRepository.findByUserId(userId)).thenReturn(Optional.empty());

            assertThat(service.prefersPix(userId)).isFalse();
        }

        @Test
        @DisplayName("prefersCreditCard retorna true quando preferencia e CREDIT_CARD")
        void prefersCreditCardTrue() {
            CustomerPaymentPreference pref = makePref(PreferredPaymentType.CREDIT_CARD, makeCard(1L, true));
            when(preferenceRepository.findByUserId(userId)).thenReturn(Optional.of(pref));

            assertThat(service.prefersCreditCard(userId)).isTrue();
        }

        @Test
        @DisplayName("prefersCreditCard retorna false quando nao ha preferencia")
        void prefersCreditCardFalseQuandoSemPreferencia() {
            when(preferenceRepository.findByUserId(userId)).thenReturn(Optional.empty());

            assertThat(service.prefersCreditCard(userId)).isFalse();
        }
    }
}
