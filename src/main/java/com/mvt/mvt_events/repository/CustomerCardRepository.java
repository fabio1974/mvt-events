package com.mvt.mvt_events.repository;

import com.mvt.mvt_events.jpa.CustomerCard;
import com.mvt.mvt_events.jpa.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CustomerCardRepository extends JpaRepository<CustomerCard, Long> {

    /**
     * Busca todos os cartões ativos de um cliente (não deletados).
     * Ordenação: mais recentemente usado primeiro, depois por data de criação.
     */
    @Query("SELECT c FROM CustomerCard c WHERE c.customer.id = :customerId AND c.deletedAt IS NULL ORDER BY c.lastUsedAt DESC NULLS LAST, c.createdAt DESC")
    List<CustomerCard> findActiveCardsByCustomerId(@Param("customerId") UUID customerId);

    /**
     * Busca o cartão padrão de um cliente.
     * DEPRECATED: Use CustomerPaymentPreferenceService.getDefaultCard() que consulta customer_payment_preferences.default_card_id
     */
    @Deprecated
    default Optional<CustomerCard> findDefaultCardByCustomerId(UUID customerId) {
        return Optional.empty(); // Removido - usar CustomerPaymentPreference
    }

    /**
     * Busca cartão por ID do Pagar.me (único identificador externo).
     */
    Optional<CustomerCard> findByPagarmeCardId(String pagarmeCardId);

    /**
     * Busca cartão por ID, apenas se pertencer ao cliente e não estiver deletado.
     */
    @Query("SELECT c FROM CustomerCard c WHERE c.id = :cardId AND c.customer.id = :customerId AND c.deletedAt IS NULL")
    Optional<CustomerCard> findByIdAndCustomerId(@Param("cardId") Long cardId, @Param("customerId") UUID customerId);

    /**
     * REMOVIDO: clearDefaultFlag e setDefaultCardAtomic não são mais necessários.
     * Agora usamos customer_payment_preferences.default_card_id para identificar o cartão padrão.
     */

    /**
     * Conta quantos cartões ativos um cliente possui.
     */
    @Query("SELECT COUNT(c) FROM CustomerCard c WHERE c.customer.id = :customerId AND c.deletedAt IS NULL AND c.isActive = TRUE")
    long countActiveCardsByCustomerId(@Param("customerId") UUID customerId);

    /**
     * REMOVIDO: hasDefaultCard não é mais necessário.
     * Use CustomerPaymentPreferenceService para verificar se existe cartão padrão.
     */
}
