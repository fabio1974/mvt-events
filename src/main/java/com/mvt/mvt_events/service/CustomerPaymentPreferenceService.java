package com.mvt.mvt_events.service;

import com.mvt.mvt_events.jpa.CustomerCard;
import com.mvt.mvt_events.jpa.CustomerPaymentPreference;
import com.mvt.mvt_events.jpa.CustomerPaymentPreference.PreferredPaymentType;
import com.mvt.mvt_events.jpa.User;
import com.mvt.mvt_events.repository.CustomerCardRepository;
import com.mvt.mvt_events.repository.CustomerPaymentPreferenceRepository;
import com.mvt.mvt_events.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

/**
 * Service para gerenciar preferências de pagamento dos clientes.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CustomerPaymentPreferenceService {

    private final CustomerPaymentPreferenceRepository preferenceRepository;
    private final CustomerCardRepository cardRepository;
    private final UserRepository userRepository;

    /**
     * Busca a preferência de pagamento do cliente.
     * Se não existir, retorna uma preferência padrão (PIX).
     * 
     * IMPORTANTE: Inicializa o defaultCard para evitar LazyInitializationException.
     */
    @Transactional(readOnly = true)
    public CustomerPaymentPreference getPreference(UUID userId) {
        Optional<CustomerPaymentPreference> prefOpt = preferenceRepository.findByUserId(userId);
        
        if (prefOpt.isEmpty()) {
            return createDefaultPreference(userId);
        }
        
        CustomerPaymentPreference preference = prefOpt.get();
        
        // Força inicialização do defaultCard para evitar LazyInitializationException
        if (preference.getDefaultCard() != null) {
            // Acessa propriedades para forçar o Hibernate a carregar
            preference.getDefaultCard().getLastFourDigits();
            preference.getDefaultCard().getBrand();
        }
        
        return preference;
    }

    /**
     * Cria uma preferência padrão (PIX) para o cliente.
     * Não salva no banco - apenas retorna um objeto para exibição.
     */
    private CustomerPaymentPreference createDefaultPreference(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));
        
        return CustomerPaymentPreference.builder()
                .user(user)
                .preferredPaymentType(PreferredPaymentType.PIX)
                .defaultCard(null)
                .build();
    }

    /**
     * Salva ou atualiza a preferência de pagamento do cliente.
     */
    @Transactional
    public CustomerPaymentPreference savePreference(UUID userId, PreferredPaymentType paymentType, Long cardId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        // Busca preferência existente ou cria nova
        CustomerPaymentPreference preference = preferenceRepository.findByUserId(userId)
                .orElse(CustomerPaymentPreference.builder()
                        .user(user)
                        .build());

        preference.setPreferredPaymentType(paymentType);

        // Se for cartão de crédito, valida e define o cartão
        if (paymentType == PreferredPaymentType.CREDIT_CARD) {
            if (cardId == null) {
                throw new IllegalArgumentException("Cartão é obrigatório quando o tipo de pagamento é CREDIT_CARD");
            }

            CustomerCard card = cardRepository.findById(cardId)
                    .orElseThrow(() -> new RuntimeException("Cartão não encontrado"));

            // Valida se o cartão pertence ao usuário
            if (!card.getCustomer().getId().equals(userId)) {
                throw new RuntimeException("Cartão não pertence ao usuário");
            }

            // Valida se o cartão está ativo
            if (!card.getIsActive()) {
                throw new RuntimeException("Cartão está inativo");
            }

            preference.setDefaultCard(card);
            log.info("Preferência atualizada para CREDIT_CARD com cartão ID {} para usuário {}", cardId, userId);
        } else {
            // PIX não precisa de cartão
            preference.setDefaultCard(null);
            log.info("Preferência atualizada para PIX para usuário {}", userId);
        }

        return preferenceRepository.save(preference);
    }

    /**
     * Define o tipo de pagamento como PIX.
     */
    @Transactional
    public CustomerPaymentPreference setPixAsPreferred(UUID userId) {
        return savePreference(userId, PreferredPaymentType.PIX, null);
    }

    /**
     * Define o tipo de pagamento como cartão de crédito.
     */
    @Transactional
    public CustomerPaymentPreference setCreditCardAsPreferred(UUID userId, Long cardId) {
        return savePreference(userId, PreferredPaymentType.CREDIT_CARD, cardId);
    }

    /**
     * Verifica se o cliente prefere PIX.
     */
    @Transactional(readOnly = true)
    public boolean prefersPix(UUID userId) {
        return preferenceRepository.findByUserId(userId)
                .map(CustomerPaymentPreference::prefersPix)
                .orElse(true); // Default é PIX
    }

    /**
     * Verifica se o cliente prefere cartão de crédito.
     */
    @Transactional(readOnly = true)
    public boolean prefersCreditCard(UUID userId) {
        return preferenceRepository.findByUserId(userId)
                .map(CustomerPaymentPreference::prefersCreditCard)
                .orElse(false);
    }
}
