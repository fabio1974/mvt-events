package com.mvt.mvt_events.service;

import com.mvt.mvt_events.jpa.CustomerCard;
import com.mvt.mvt_events.jpa.CustomerPaymentPreference;
import com.mvt.mvt_events.jpa.User;
import com.mvt.mvt_events.payment.service.PagarMeService;
import com.mvt.mvt_events.repository.CustomerCardRepository;
import com.mvt.mvt_events.repository.CustomerPaymentPreferenceRepository;
import com.mvt.mvt_events.repository.DeliveryRepository;
import com.mvt.mvt_events.repository.PaymentRepository;
import com.mvt.mvt_events.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Testes unitarios do CustomerCardService -- cobre addCard (ambas sobrecargas),
 * listCustomerCards, getDefaultCard, setDefaultCard, deleteCard, markCardAsUsed,
 * hasActiveCards, findByPagarmeCardId e mapBrand.
 */
@ExtendWith(MockitoExtension.class)
class CustomerCardServiceTest {

    @Mock private CustomerCardRepository cardRepository;
    @Mock private CustomerPaymentPreferenceRepository preferenceRepository;
    @Mock private UserRepository userRepository;
    @Mock private PagarMeService pagarMeService;
    @Mock private DeliveryRepository deliveryRepository;
    @Mock private PaymentRepository paymentRepository;
    @Mock private SiteConfigurationService siteConfigurationService;
    @Mock private TransactionTemplate transactionTemplate;

    @InjectMocks
    private CustomerCardService customerCardService;

    // ========== Helpers ==========

    private final UUID customerId = UUID.fromString("00000000-0000-0000-0000-000000000001");

    private User makeUser() {
        User u = new User();
        u.setId(customerId);
        u.setName("Joao");
        u.setUsername("joao@zapi10.com");
        u.setRole(User.Role.CUSTOMER);
        u.setPagarmeCustomerId("cus_abc123");
        return u;
    }

    private CustomerCard makeCard(Long id, boolean active) {
        CustomerCard card = new CustomerCard();
        card.setId(id);
        User user = makeUser();
        card.setCustomer(user);
        card.setPagarmeCardId("card_" + id);
        card.setLastFourDigits("4242");
        card.setBrand(CustomerCard.CardBrand.VISA);
        card.setHolderName("JOAO DA SILVA");
        card.setExpMonth(12);
        card.setExpYear(2030);
        card.setIsActive(active);
        card.setIsVerified(true);
        return card;
    }

    private Authentication makeAuth(String username) {
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn(username);
        return auth;
    }

    // ================================================================
    // addCard (token-based)
    // ================================================================

    @Nested
    @DisplayName("addCard(token) -- Adiciona cartao via token do Pagar.me")
    class AddCardTokenTests {

        @Test
        @DisplayName("Adiciona cartao com sucesso quando customer ja tem pagarmeCustomerId")
        void adicionaComSucesso() {
            User user = makeUser();
            when(userRepository.findById(customerId)).thenReturn(Optional.of(user));

            Map<String, Object> cardData = Map.of(
                    "id", "card_new123",
                    "last_four_digits", "1234",
                    "brand", "VISA",
                    "holder_name", "JOAO",
                    "exp_month", 6,
                    "exp_year", 2028
            );
            when(pagarMeService.createCard(eq("cus_abc123"), eq("tok_xyz"), any()))
                    .thenReturn(cardData);
            when(cardRepository.findByPagarmeCardId("card_new123")).thenReturn(Optional.empty());
            when(cardRepository.save(any())).thenAnswer(inv -> {
                CustomerCard c = inv.getArgument(0);
                c.setId(1L);
                return c;
            });
            when(cardRepository.countActiveCardsByCustomerId(customerId)).thenReturn(1L);
            when(preferenceRepository.findByUserId(customerId)).thenReturn(Optional.empty());
            when(preferenceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(deliveryRepository.findByClientIdAndStatusesWithJoins(eq(customerId), anyList()))
                    .thenReturn(List.of());

            CustomerCard result = customerCardService.addCard(customerId, "tok_xyz", false, null);

            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getIsActive()).isTrue();
            assertThat(result.getIsVerified()).isTrue();
            verify(pagarMeService).createCard(eq("cus_abc123"), eq("tok_xyz"), any());
        }

        @Test
        @DisplayName("Cria customer no Pagar.me quando nao existe pagarmeCustomerId")
        void criaCustomerNoPagarMe() {
            User user = makeUser();
            user.setPagarmeCustomerId(null);
            when(userRepository.findById(customerId)).thenReturn(Optional.of(user));
            when(pagarMeService.createCustomer(user)).thenReturn("cus_new999");

            Map<String, Object> cardData = Map.of(
                    "id", "card_new",
                    "last_four_digits", "5678",
                    "brand", "MASTERCARD",
                    "holder_name", "JOAO",
                    "exp_month", 3,
                    "exp_year", 2029
            );
            when(pagarMeService.createCard(eq("cus_new999"), any(), any())).thenReturn(cardData);
            when(cardRepository.findByPagarmeCardId("card_new")).thenReturn(Optional.empty());
            when(cardRepository.save(any())).thenAnswer(inv -> {
                CustomerCard c = inv.getArgument(0);
                c.setId(1L);
                return c;
            });
            when(cardRepository.countActiveCardsByCustomerId(customerId)).thenReturn(1L);
            when(preferenceRepository.findByUserId(customerId)).thenReturn(Optional.empty());
            when(preferenceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(deliveryRepository.findByClientIdAndStatusesWithJoins(eq(customerId), anyList()))
                    .thenReturn(List.of());

            customerCardService.addCard(customerId, "tok_abc", false, null);

            verify(pagarMeService).createCustomer(user);
            verify(userRepository).save(user);
            assertThat(user.getPagarmeCustomerId()).isEqualTo("cus_new999");
        }

        @Test
        @DisplayName("Lanca excecao quando cartao ja esta cadastrado")
        void lancaExcecaoCartaoJaCadastrado() {
            User user = makeUser();
            when(userRepository.findById(customerId)).thenReturn(Optional.of(user));

            Map<String, Object> cardData = Map.of(
                    "id", "card_dup",
                    "last_four_digits", "4242",
                    "brand", "VISA",
                    "holder_name", "JOAO",
                    "exp_month", 12,
                    "exp_year", 2030
            );
            when(pagarMeService.createCard(any(), any(), any())).thenReturn(cardData);
            when(cardRepository.findByPagarmeCardId("card_dup")).thenReturn(Optional.of(makeCard(1L, true)));

            assertThatThrownBy(() -> customerCardService.addCard(customerId, "tok_xyz", false, null))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("já está cadastrado");
        }

        @Test
        @DisplayName("Lanca excecao quando cliente nao encontrado")
        void lancaExcecaoClienteNaoEncontrado() {
            when(userRepository.findById(customerId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> customerCardService.addCard(customerId, "tok_xyz", false, null))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("não encontrado");
        }
    }

    // ================================================================
    // addCard (pre-extracted data)
    // ================================================================

    @Nested
    @DisplayName("addCard(dados pre-extraidos) -- Adiciona cartao com dados ja extraidos")
    class AddCardPreExtractedTests {

        @Test
        @DisplayName("Adiciona cartao pre-extraido com sucesso")
        void adicionaComSucesso() {
            User user = makeUser();
            when(userRepository.findById(customerId)).thenReturn(Optional.of(user));
            when(cardRepository.findByPagarmeCardId("card_pre")).thenReturn(Optional.empty());
            when(cardRepository.save(any())).thenAnswer(inv -> {
                CustomerCard c = inv.getArgument(0);
                c.setId(1L);
                return c;
            });
            when(preferenceRepository.findByUserId(customerId)).thenReturn(Optional.empty());
            when(preferenceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            CustomerCard result = customerCardService.addCard(
                    customerId, "card_pre", "9999", CustomerCard.CardBrand.ELO,
                    "MARIA", 6, 2028, true);

            assertThat(result.getLastFourDigits()).isEqualTo("9999");
            assertThat(result.getBrand()).isEqualTo(CustomerCard.CardBrand.ELO);
            assertThat(result.getIsVerified()).isFalse(); // pre-extracted is not verified
        }

        @Test
        @DisplayName("Lanca excecao quando cartao duplicado")
        void lancaExcecaoCartaoDuplicado() {
            User user = makeUser();
            when(userRepository.findById(customerId)).thenReturn(Optional.of(user));
            when(cardRepository.findByPagarmeCardId("card_dup")).thenReturn(Optional.of(makeCard(1L, true)));

            assertThatThrownBy(() -> customerCardService.addCard(
                    customerId, "card_dup", "4242", CustomerCard.CardBrand.VISA,
                    "JOAO", 12, 2030, false))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("já está cadastrado");
        }
    }

    // ================================================================
    // listCustomerCards
    // ================================================================

    @Nested
    @DisplayName("listCustomerCards() -- Lista cartoes ativos")
    class ListCustomerCardsTests {

        @Test
        @DisplayName("Retorna cartoes com isDefault populado")
        void retornaCartoesComIsDefault() {
            CustomerCard card1 = makeCard(1L, true);
            CustomerCard card2 = makeCard(2L, true);
            when(cardRepository.findActiveCardsByCustomerId(customerId)).thenReturn(List.of(card1, card2));

            CustomerPaymentPreference pref = CustomerPaymentPreference.builder()
                    .defaultCard(card1)
                    .build();
            when(preferenceRepository.findByUserId(customerId)).thenReturn(Optional.of(pref));

            List<CustomerCard> result = customerCardService.listCustomerCards(customerId);

            assertThat(result).hasSize(2);
            assertThat(result.get(0).getIsDefault()).isTrue();
            assertThat(result.get(1).getIsDefault()).isFalse();
        }

        @Test
        @DisplayName("Retorna lista vazia quando nao ha cartoes")
        void retornaListaVazia() {
            when(cardRepository.findActiveCardsByCustomerId(customerId)).thenReturn(List.of());

            List<CustomerCard> result = customerCardService.listCustomerCards(customerId);

            assertThat(result).isEmpty();
        }
    }

    // ================================================================
    // getDefaultCard
    // ================================================================

    @Nested
    @DisplayName("getDefaultCard() -- Busca cartao padrao")
    class GetDefaultCardTests {

        @Test
        @DisplayName("Retorna cartao padrao quando existe")
        void retornaCartaoPadrao() {
            CustomerCard card = makeCard(1L, true);
            CustomerPaymentPreference pref = CustomerPaymentPreference.builder()
                    .defaultCard(card)
                    .build();
            when(preferenceRepository.findByUserId(customerId)).thenReturn(Optional.of(pref));

            CustomerCard result = customerCardService.getDefaultCard(customerId);

            assertThat(result).isSameAs(card);
        }

        @Test
        @DisplayName("Lanca excecao quando nao ha preferencia")
        void lancaExcecaoSemPreferencia() {
            when(preferenceRepository.findByUserId(customerId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> customerCardService.getDefaultCard(customerId))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("preferência de pagamento");
        }

        @Test
        @DisplayName("Lanca excecao quando preferencia nao tem cartao padrao")
        void lancaExcecaoSemCartaoPadrao() {
            CustomerPaymentPreference pref = CustomerPaymentPreference.builder()
                    .defaultCard(null)
                    .build();
            when(preferenceRepository.findByUserId(customerId)).thenReturn(Optional.of(pref));

            assertThatThrownBy(() -> customerCardService.getDefaultCard(customerId))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("cartão padrão");
        }
    }

    // ================================================================
    // setDefaultCard
    // ================================================================

    @Nested
    @DisplayName("setDefaultCard() -- Define cartao padrao")
    class SetDefaultCardTests {

        @Test
        @DisplayName("Define cartao ativo como padrao com sucesso")
        void defineComSucesso() {
            User user = makeUser();
            CustomerCard card = makeCard(1L, true);
            Authentication auth = makeAuth("joao@zapi10.com");

            when(userRepository.findByUsername("joao@zapi10.com")).thenReturn(Optional.of(user));
            when(cardRepository.findByIdAndCustomerId(1L, customerId)).thenReturn(Optional.of(card));
            when(userRepository.findById(customerId)).thenReturn(Optional.of(user));
            when(preferenceRepository.findByUserId(customerId)).thenReturn(Optional.empty());
            when(preferenceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            CustomerCard result = customerCardService.setDefaultCard(customerId, 1L, auth);

            assertThat(result).isSameAs(card);
            verify(preferenceRepository).save(any());
        }

        @Test
        @DisplayName("Lanca excecao quando cartao nao encontrado")
        void lancaExcecaoCartaoNaoEncontrado() {
            User user = makeUser();
            Authentication auth = makeAuth("joao@zapi10.com");
            when(userRepository.findByUsername("joao@zapi10.com")).thenReturn(Optional.of(user));
            when(cardRepository.findByIdAndCustomerId(999L, customerId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> customerCardService.setDefaultCard(customerId, 999L, auth))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("não encontrado");
        }

        @Test
        @DisplayName("Lanca excecao quando cartao inativo")
        void lancaExcecaoCartaoInativo() {
            User user = makeUser();
            CustomerCard card = makeCard(1L, false);
            Authentication auth = makeAuth("joao@zapi10.com");

            when(userRepository.findByUsername("joao@zapi10.com")).thenReturn(Optional.of(user));
            when(cardRepository.findByIdAndCustomerId(1L, customerId)).thenReturn(Optional.of(card));

            assertThatThrownBy(() -> customerCardService.setDefaultCard(customerId, 1L, auth))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("inativo");
        }

        @Test
        @DisplayName("Lanca excecao quando usuario nao autorizado")
        void lancaExcecaoNaoAutorizado() {
            UUID outroId = UUID.randomUUID();
            User outroUser = new User();
            outroUser.setId(outroId);
            outroUser.setRole(User.Role.CUSTOMER);
            Authentication auth = makeAuth("outro@zapi10.com");

            when(userRepository.findByUsername("outro@zapi10.com")).thenReturn(Optional.of(outroUser));

            assertThatThrownBy(() -> customerCardService.setDefaultCard(customerId, 1L, auth))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Não autorizado");
        }
    }

    // ================================================================
    // deleteCard
    // ================================================================

    @Nested
    @DisplayName("deleteCard() -- Deleta cartao (soft delete)")
    class DeleteCardTests {

        @Test
        @DisplayName("Soft delete com sucesso e promove proximo cartao")
        void softDeleteComPromocao() {
            User user = makeUser();
            CustomerCard card = makeCard(1L, true);
            CustomerCard nextCard = makeCard(2L, true);
            Authentication auth = makeAuth("joao@zapi10.com");

            when(userRepository.findByUsername("joao@zapi10.com")).thenReturn(Optional.of(user));
            when(cardRepository.findByIdAndCustomerId(1L, customerId)).thenReturn(Optional.of(card));
            when(userRepository.findById(customerId)).thenReturn(Optional.of(user));
            when(cardRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            CustomerPaymentPreference pref = CustomerPaymentPreference.builder()
                    .user(user)
                    .defaultCard(card)
                    .preferredPaymentType(CustomerPaymentPreference.PreferredPaymentType.CREDIT_CARD)
                    .build();
            when(preferenceRepository.findByUserId(customerId)).thenReturn(Optional.of(pref));
            when(cardRepository.findActiveCardsByCustomerId(customerId)).thenReturn(List.of(nextCard));

            customerCardService.deleteCard(customerId, 1L, auth);

            assertThat(card.getIsActive()).isFalse();
            assertThat(card.getDeletedAt()).isNotNull();
            verify(pagarMeService).deleteCard("cus_abc123", "card_1");
            verify(preferenceRepository, times(1)).save(pref);
            assertThat(pref.getDefaultCard()).isSameAs(nextCard);
        }

        @Test
        @DisplayName("Troca para PIX quando ultimo cartao e deletado")
        void trocaParaPixQuandoUltimoCartao() {
            User user = makeUser();
            CustomerCard card = makeCard(1L, true);
            Authentication auth = makeAuth("joao@zapi10.com");

            when(userRepository.findByUsername("joao@zapi10.com")).thenReturn(Optional.of(user));
            when(cardRepository.findByIdAndCustomerId(1L, customerId)).thenReturn(Optional.of(card));
            when(userRepository.findById(customerId)).thenReturn(Optional.of(user));
            when(cardRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            CustomerPaymentPreference pref = CustomerPaymentPreference.builder()
                    .user(user)
                    .defaultCard(card)
                    .preferredPaymentType(CustomerPaymentPreference.PreferredPaymentType.CREDIT_CARD)
                    .build();
            when(preferenceRepository.findByUserId(customerId)).thenReturn(Optional.of(pref));
            when(cardRepository.findActiveCardsByCustomerId(customerId)).thenReturn(List.of());

            customerCardService.deleteCard(customerId, 1L, auth);

            assertThat(pref.getPreferredPaymentType())
                    .isEqualTo(CustomerPaymentPreference.PreferredPaymentType.PIX);
            assertThat(pref.getDefaultCard()).isNull();
        }

        @Test
        @DisplayName("Aborta exclusao local se Pagar.me falhar")
        void abortaSepagarMeFalhar() {
            User user = makeUser();
            CustomerCard card = makeCard(1L, true);
            Authentication auth = makeAuth("joao@zapi10.com");

            when(userRepository.findByUsername("joao@zapi10.com")).thenReturn(Optional.of(user));
            when(cardRepository.findByIdAndCustomerId(1L, customerId)).thenReturn(Optional.of(card));
            when(userRepository.findById(customerId)).thenReturn(Optional.of(user));
            doThrow(new RuntimeException("Pagar.me error"))
                    .when(pagarMeService).deleteCard(any(), any());

            assertThatThrownBy(() -> customerCardService.deleteCard(customerId, 1L, auth))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Pagar.me");

            // Cartao nao deve ter sido soft-deleted
            assertThat(card.getIsActive()).isTrue();
        }
    }

    // ================================================================
    // markCardAsUsed
    // ================================================================

    @Nested
    @DisplayName("markCardAsUsed() -- Marca cartao como usado")
    class MarkCardAsUsedTests {

        @Test
        @DisplayName("Atualiza lastUsedAt quando cartao encontrado")
        void atualizaLastUsedAt() {
            CustomerCard card = makeCard(1L, true);
            when(cardRepository.findByPagarmeCardId("card_1")).thenReturn(Optional.of(card));
            when(cardRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            customerCardService.markCardAsUsed("card_1");

            assertThat(card.getLastUsedAt()).isNotNull();
            verify(cardRepository).save(card);
        }

        @Test
        @DisplayName("Nao faz nada quando cartao nao encontrado")
        void naoFazNadaQuandoNaoEncontrado() {
            when(cardRepository.findByPagarmeCardId("card_xxx")).thenReturn(Optional.empty());

            customerCardService.markCardAsUsed("card_xxx");

            verify(cardRepository, never()).save(any());
        }
    }

    // ================================================================
    // hasActiveCards
    // ================================================================

    @Nested
    @DisplayName("hasActiveCards() -- Verifica se ha cartoes ativos")
    class HasActiveCardsTests {

        @Test
        @DisplayName("Retorna true quando ha cartoes ativos")
        void retornaTrueQuandoHaCartoes() {
            when(cardRepository.countActiveCardsByCustomerId(customerId)).thenReturn(2L);

            assertThat(customerCardService.hasActiveCards(customerId)).isTrue();
        }

        @Test
        @DisplayName("Retorna false quando nao ha cartoes ativos")
        void retornaFalseQuandoNaoHaCartoes() {
            when(cardRepository.countActiveCardsByCustomerId(customerId)).thenReturn(0L);

            assertThat(customerCardService.hasActiveCards(customerId)).isFalse();
        }
    }

    // ================================================================
    // findByPagarmeCardId
    // ================================================================

    @Nested
    @DisplayName("findByPagarmeCardId() -- Busca por ID do Pagar.me")
    class FindByPagarmeCardIdTests {

        @Test
        @DisplayName("Retorna cartao quando encontrado")
        void retornaQuandoEncontrado() {
            CustomerCard card = makeCard(1L, true);
            when(cardRepository.findByPagarmeCardId("card_1")).thenReturn(Optional.of(card));

            CustomerCard result = customerCardService.findByPagarmeCardId("card_1");

            assertThat(result).isSameAs(card);
        }

        @Test
        @DisplayName("Lanca excecao quando nao encontrado")
        void lancaExcecaoQuandoNaoEncontrado() {
            when(cardRepository.findByPagarmeCardId("card_xxx")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> customerCardService.findByPagarmeCardId("card_xxx"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("não encontrado");
        }
    }

    // ================================================================
    // mapBrand (static)
    // ================================================================

    @Nested
    @DisplayName("mapBrand() -- Mapeia string de bandeira para enum")
    class MapBrandTests {

        @Test
        @DisplayName("Mapeia bandeiras conhecidas corretamente")
        void mapeiaBandeirasConhecidas() {
            assertThat(CustomerCardService.mapBrand("VISA")).isEqualTo(CustomerCard.CardBrand.VISA);
            assertThat(CustomerCardService.mapBrand("visa")).isEqualTo(CustomerCard.CardBrand.VISA);
            assertThat(CustomerCardService.mapBrand("MASTERCARD")).isEqualTo(CustomerCard.CardBrand.MASTERCARD);
            assertThat(CustomerCardService.mapBrand("MASTER")).isEqualTo(CustomerCard.CardBrand.MASTERCARD);
            assertThat(CustomerCardService.mapBrand("AMEX")).isEqualTo(CustomerCard.CardBrand.AMEX);
            assertThat(CustomerCardService.mapBrand("AMERICAN EXPRESS")).isEqualTo(CustomerCard.CardBrand.AMEX);
            assertThat(CustomerCardService.mapBrand("ELO")).isEqualTo(CustomerCard.CardBrand.ELO);
            assertThat(CustomerCardService.mapBrand("HIPERCARD")).isEqualTo(CustomerCard.CardBrand.HIPERCARD);
            assertThat(CustomerCardService.mapBrand("DINERS")).isEqualTo(CustomerCard.CardBrand.DINERS);
            assertThat(CustomerCardService.mapBrand("DINERS CLUB")).isEqualTo(CustomerCard.CardBrand.DINERS);
            assertThat(CustomerCardService.mapBrand("DISCOVER")).isEqualTo(CustomerCard.CardBrand.DISCOVER);
            assertThat(CustomerCardService.mapBrand("JCB")).isEqualTo(CustomerCard.CardBrand.JCB);
        }

        @Test
        @DisplayName("Retorna OTHER para bandeira desconhecida")
        void retornaOtherParaDesconhecida() {
            assertThat(CustomerCardService.mapBrand("XPTO")).isEqualTo(CustomerCard.CardBrand.OTHER);
        }

        @Test
        @DisplayName("Retorna OTHER para null")
        void retornaOtherParaNull() {
            assertThat(CustomerCardService.mapBrand(null)).isEqualTo(CustomerCard.CardBrand.OTHER);
        }
    }
}
