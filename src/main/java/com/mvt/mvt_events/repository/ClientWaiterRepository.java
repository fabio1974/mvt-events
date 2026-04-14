package com.mvt.mvt_events.repository;

import com.mvt.mvt_events.jpa.ClientWaiter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ClientWaiterRepository extends JpaRepository<ClientWaiter, Long> {

    List<ClientWaiter> findByClientId(UUID clientId);

    List<ClientWaiter> findByClientIdAndActive(UUID clientId, Boolean active);

    List<ClientWaiter> findByWaiterId(UUID waiterId);

    List<ClientWaiter> findByWaiterIdAndActive(UUID waiterId, Boolean active);

    Optional<ClientWaiter> findByClientIdAndWaiterId(UUID clientId, UUID waiterId);

    Optional<ClientWaiter> findByClientIdAndPin(UUID clientId, String pin);

    boolean existsByClientIdAndWaiterId(UUID clientId, UUID waiterId);

    @Query("SELECT cw FROM ClientWaiter cw JOIN FETCH cw.client WHERE cw.waiter.id = :waiterId AND cw.active = true")
    List<ClientWaiter> findActiveEstablishmentsByWaiter(@Param("waiterId") UUID waiterId);
}
