package com.mvt.mvt_events.service;

import com.mvt.mvt_events.jpa.CustomerCard;
import com.mvt.mvt_events.jpa.User;
import com.mvt.mvt_events.payment.service.PagarMeService;
import com.mvt.mvt_events.repository.CustomerCardRepository;
import com.mvt.mvt_events.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Service para gerenciar cartões de crédito tokenizados dos clientes.
 * 
 * SEGURANÇA (PCI Compliance):
 * - NUNCA processa número completo do cartão no backend
 * - Espera token do Pagar.me já criado no frontend
 * - Armazena apenas: token + últimos 4 dígitos + bandeira
 * 
 * Funcionalidades:
 * - Adicionar cartão (recebe token do frontend)
 * - Listar cartões do cliente
 * - Definir cartão padrão
 * - Deletar cartão (soft delete)
 * - Verificar expiração automática
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CustomerCardService {

    private final CustomerCardRepository cardRepository;
    private final UserRepository userRepository;
    private final PagarMeService pagarMeService;

    /**
     * Adiciona um novo cartão para o cliente.
     * 
     * @param customerId ID do cliente
     * @param cardToken Token do cartão (gerado no frontend com Pagar.me JS)
     * @param setAsDefault Se deve marcar como cartão padrão
     * @return Cartão salvo
     */
    @Transactional
    public CustomerCard addCard(UUID customerId, String cardToken, Boolean setAsDefault) {
        log.info("Adicionando cartão para customer: {} | Token: {}", customerId, cardToken);

        // Validar cliente
        User customer = userRepository.findById(customerId)
                .orElseThrow(() -> new RuntimeException("Cliente não encontrado"));

        // Garantir que cliente tem pagarmeCustomerId
        String pagarmeCustomerId = customer.getPagarmeCustomerId();
        if (pagarmeCustomerId == null) {
            // Criar customer no Pagar.me
            pagarmeCustomerId = pagarMeService.createCustomer(customer);
            customer.setPagarmeCustomerId(pagarmeCustomerId);
            userRepository.save(customer);
            log.info("   └─ Customer Pagar.me criado: {}", pagarmeCustomerId);
        }

        // Criar cartão no Pagar.me
        Map<String, Object> cardData = pagarMeService.createCard(pagarmeCustomerId, cardToken);
        
        // Extrair dados do cartão
        String pagarmeCardId = (String) cardData.get("id");
        String lastFourDigits = (String) cardData.get("last_four_digits");
        String brandStr = (String) cardData.get("brand");
        String holderName = (String) cardData.get("holder_name");
        Integer expMonth = (Integer) cardData.get("exp_month");
        Integer expYear = (Integer) cardData.get("exp_year");

        // Verificar se cartão já não foi cadastrado
        if (cardRepository.findByPagarmeCardId(pagarmeCardId).isPresent()) {
            throw new RuntimeException("Este cartão já está cadastrado");
        }

        // Criar entidade
        CustomerCard card = new CustomerCard();
        card.setCustomer(customer);
        card.setPagarmeCardId(pagarmeCardId);
        card.setLastFourDigits(lastFourDigits);
        card.setBrand(mapBrand(brandStr));
        card.setHolderName(holderName);
        card.setExpMonth(expMonth);
        card.setExpYear(expYear);
        card.setIsActive(true);
        card.setIsVerified(true); // Já verificado pelo Pagar.me

        // Se não tem cartões, este é o padrão automaticamente
        boolean hasCards = cardRepository.countActiveCardsByCustomerId(customerId) > 0;
        if (!hasCards || Boolean.TRUE.equals(setAsDefault)) {
            cardRepository.clearDefaultFlag(customerId);
            card.setIsDefault(true);
        } else {
            card.setIsDefault(false);
        }

        CustomerCard saved = cardRepository.save(card);
        log.info("Cartão adicionado com sucesso: {} | Default: {}", saved.getId(), saved.getIsDefault());
        
        return saved;
    }

    /**
     * Sobrecarga para manter compatibilidade com assinatura antiga.
     */
    @Transactional
    public CustomerCard addCard(
            UUID customerId,
            String pagarmeCardId,
            String lastFourDigits,
            CustomerCard.CardBrand brand,
            String holderName,
            Integer expMonth,
            Integer expYear,
            Boolean setAsDefault) {
        
        // Esta versão é para quando já temos os dados extraídos
        // Usado quando o cartão já foi criado no Pagar.me antes
        log.info("Adicionando cartão (dados pré-extraídos) para customer: {}", customerId);

        User customer = userRepository.findById(customerId)
                .orElseThrow(() -> new RuntimeException("Cliente não encontrado"));

        if (cardRepository.findByPagarmeCardId(pagarmeCardId).isPresent()) {
            throw new RuntimeException("Este cartão já está cadastrado");
        }

        CustomerCard card = new CustomerCard();
        card.setCustomer(customer);
        card.setPagarmeCardId(pagarmeCardId);
        card.setLastFourDigits(lastFourDigits);
        card.setBrand(brand);
        card.setHolderName(holderName);
        card.setExpMonth(expMonth);
        card.setExpYear(expYear);
        card.setIsActive(true);
        card.setIsVerified(false);

        boolean hasCards = cardRepository.countActiveCardsByCustomerId(customerId) > 0;
        if (!hasCards || Boolean.TRUE.equals(setAsDefault)) {
            cardRepository.clearDefaultFlag(customerId);
            card.setIsDefault(true);
        } else {
            card.setIsDefault(false);
        }

        CustomerCard saved = cardRepository.save(card);
        log.info("Cartão adicionado com sucesso: {} | Default: {}", saved.getId(), saved.getIsDefault());
        
        return saved;
    }

    /**
     * Lista todos os cartões ativos do cliente.
     * Ordena por: padrão primeiro, depois por último uso.
     */
    @Transactional(readOnly = true)
    public List<CustomerCard> listCustomerCards(UUID customerId) {
        return cardRepository.findActiveCardsByCustomerId(customerId);
    }

    /**
     * Busca o cartão padrão do cliente.
     */
    @Transactional(readOnly = true)
    public CustomerCard getDefaultCard(UUID customerId) {
        return cardRepository.findDefaultCardByCustomerId(customerId)
                .orElseThrow(() -> new RuntimeException("Cliente não possui cartão padrão cadastrado"));
    }

    /**
     * Define um cartão como padrão.
     * Remove flag de todos os outros cartões do cliente.
     */
    @Transactional
    public CustomerCard setDefaultCard(UUID customerId, Long cardId, Authentication authentication) {
        // Verificar autorização
        validateCustomerAccess(customerId, authentication);

        // Buscar cartão
        CustomerCard card = cardRepository.findByIdAndCustomerId(cardId, customerId)
                .orElseThrow(() -> new RuntimeException("Cartão não encontrado"));

        // Verificar se está ativo
        if (!card.getIsActive()) {
            throw new RuntimeException("Cartão inativo não pode ser definido como padrão");
        }

        // Verificar se está expirado
        if (card.isExpired()) {
            throw new RuntimeException("Cartão expirado não pode ser definido como padrão");
        }

        // Remover flag de outros cartões
        cardRepository.clearDefaultFlag(customerId);

        // Definir como padrão
        card.setIsDefault(true);
        CustomerCard updated = cardRepository.save(card);

        log.info("Cartão {} definido como padrão para customer {}", cardId, customerId);
        return updated;
    }

    /**
     * Deleta um cartão (soft delete).
     * Mantém no banco para auditoria e histórico de transações.
     */
    @Transactional
    public void deleteCard(UUID customerId, Long cardId, Authentication authentication) {
        // Verificar autorização
        validateCustomerAccess(customerId, authentication);

        // Buscar cartão
        CustomerCard card = cardRepository.findByIdAndCustomerId(cardId, customerId)
                .orElseThrow(() -> new RuntimeException("Cartão não encontrado"));

        // Soft delete
        card.softDelete();
        cardRepository.save(card);

        log.info("Cartão {} deletado (soft) para customer {}", cardId, customerId);

        // Se era o padrão, definir outro como padrão
        if (Boolean.TRUE.equals(card.getIsDefault())) {
            List<CustomerCard> activeCards = cardRepository.findActiveCardsByCustomerId(customerId);
            if (!activeCards.isEmpty()) {
                CustomerCard newDefault = activeCards.get(0);
                newDefault.setIsDefault(true);
                cardRepository.save(newDefault);
                log.info("Novo cartão padrão: {} para customer {}", newDefault.getId(), customerId);
            }
        }
    }

    /**
     * Atualiza timestamp de último uso do cartão.
     * Chamado após pagamento bem-sucedido.
     */
    @Transactional
    public void markCardAsUsed(String pagarmeCardId) {
        cardRepository.findByPagarmeCardId(pagarmeCardId).ifPresent(card -> {
            card.setLastUsedAt(LocalDateTime.now());
            cardRepository.save(card);
            log.debug("Cartão {} marcado como usado", pagarmeCardId);
        });
    }

    /**
     * Verifica se cliente possui cartões ativos.
     */
    @Transactional(readOnly = true)
    public boolean hasActiveCards(UUID customerId) {
        return cardRepository.countActiveCardsByCustomerId(customerId) > 0;
    }

    /**
     * Busca cartão por ID do Pagar.me.
     */
    @Transactional(readOnly = true)
    public CustomerCard findByPagarmeCardId(String pagarmeCardId) {
        return cardRepository.findByPagarmeCardId(pagarmeCardId)
                .orElseThrow(() -> new RuntimeException("Cartão não encontrado: " + pagarmeCardId));
    }

    // ============================================================================
    // MÉTODOS PRIVADOS
    // ============================================================================

    /**
     * Valida se usuário logado tem acesso ao customer.
     * Apenas o próprio customer ou admin pode gerenciar cartões.
     */
    private void validateCustomerAccess(UUID customerId, Authentication authentication) {
        String username = authentication.getName();
        User loggedUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Usuário logado não encontrado"));

        if (!loggedUser.getId().equals(customerId) && !loggedUser.isAdmin()) {
            throw new RuntimeException("Não autorizado a gerenciar cartões deste cliente");
        }
    }

    /**
     * Mapeia string de bandeira para enum.
     */
    public static CustomerCard.CardBrand mapBrand(String brandStr) {
        if (brandStr == null) return CustomerCard.CardBrand.OTHER;
        
        return switch (brandStr.toUpperCase()) {
            case "VISA" -> CustomerCard.CardBrand.VISA;
            case "MASTERCARD", "MASTER" -> CustomerCard.CardBrand.MASTERCARD;
            case "AMEX", "AMERICAN EXPRESS" -> CustomerCard.CardBrand.AMEX;
            case "ELO" -> CustomerCard.CardBrand.ELO;
            case "HIPERCARD" -> CustomerCard.CardBrand.HIPERCARD;
            case "DINERS", "DINERS CLUB" -> CustomerCard.CardBrand.DINERS;
            case "DISCOVER" -> CustomerCard.CardBrand.DISCOVER;
            case "JCB" -> CustomerCard.CardBrand.JCB;
            default -> CustomerCard.CardBrand.OTHER;
        };
    }
}
