package com.mvt.mvt_events.repository;

import com.mvt.mvt_events.jpa.ClientSubscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ClientSubscriptionRepository extends JpaRepository<ClientSubscription, Long> {

    /**
     * Busca subscription ativa de um client para um serviço específico.
     */
    Optional<ClientSubscription> findByClientIdAndServiceCodeAndActiveTrue(UUID clientId, String serviceCode);

    /**
     * Busca todas as subscriptions ativas de um client.
     */
    List<ClientSubscription> findByClientIdAndActiveTrue(UUID clientId);

    /**
     * Busca todas as subscriptions ativas (para o scheduler).
     */
    List<ClientSubscription> findByActiveTrue();

    /**
     * Busca subscriptions ativas com um billing_due_day específico.
     */
    List<ClientSubscription> findByActiveTrueAndBillingDueDay(Integer billingDueDay);

    /**
     * Verifica se já existe fatura gerada para uma subscription num mês.
     */
    @Query("SELECT COUNT(p) > 0 FROM Payment p WHERE p.subscription.id = :subscriptionId AND p.referenceMonth = :referenceMonth AND p.status <> 'CANCELLED'")
    boolean existsInvoiceForMonth(@Param("subscriptionId") Long subscriptionId, @Param("referenceMonth") String referenceMonth);
}
