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
     */
    @Query("SELECT c FROM CustomerCard c WHERE c.customer.id = :customerId AND c.deletedAt IS NULL ORDER BY c.isDefault DESC, c.lastUsedAt DESC NULLS LAST, c.createdAt DESC")
    List<CustomerCard> findActiveCardsByCustomerId(@Param("customerId") UUID customerId);

    /**
     * Busca o cartão padrão de um cliente.
     */
    @Query("SELECT c FROM CustomerCard c WHERE c.customer.id = :customerId AND c.isDefault = TRUE AND c.deletedAt IS NULL")
    Optional<CustomerCard> findDefaultCardByCustomerId(@Param("customerId") UUID customerId);

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
     * Remove flag is_default de todos os cartões do cliente.
     * Usado antes de marcar um novo cartão como padrão.
     */
    @Modifying
    @Query("UPDATE CustomerCard c SET c.isDefault = FALSE WHERE c.customer.id = :customerId")
    void clearDefaultFlag(@Param("customerId") UUID customerId);

    /**
     * Conta quantos cartões ativos um cliente possui.
     */
    @Query("SELECT COUNT(c) FROM CustomerCard c WHERE c.customer.id = :customerId AND c.deletedAt IS NULL AND c.isActive = TRUE")
    long countActiveCardsByCustomerId(@Param("customerId") UUID customerId);

    /**
     * Verifica se existe um cartão padrão para o cliente.
     */
    @Query("SELECT COUNT(c) > 0 FROM CustomerCard c WHERE c.customer.id = :customerId AND c.isDefault = TRUE AND c.deletedAt IS NULL")
    boolean hasDefaultCard(@Param("customerId") UUID customerId);
}
