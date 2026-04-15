package com.mvt.mvt_events.repository;

import com.mvt.mvt_events.jpa.ClientWaiter;
import com.mvt.mvt_events.jpa.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ClientWaiterRepository extends JpaRepository<ClientWaiter, Long> {

    Optional<ClientWaiter> findByClientAndWaiter(User client, User waiter);

    @Query("SELECT cw FROM ClientWaiter cw WHERE cw.client.id = :clientId AND cw.active = true")
    List<ClientWaiter> findActiveByClientId(@Param("clientId") UUID clientId);

    @Query("SELECT cw FROM ClientWaiter cw JOIN FETCH cw.client WHERE cw.waiter.id = :waiterId AND cw.active = true")
    List<ClientWaiter> findActiveByWaiterId(@Param("waiterId") UUID waiterId);

    @Query("SELECT COUNT(cw) > 0 FROM ClientWaiter cw WHERE cw.client.id = :clientId AND cw.waiter.id = :waiterId AND cw.active = true")
    boolean hasActiveLink(@Param("clientId") UUID clientId, @Param("waiterId") UUID waiterId);
}
