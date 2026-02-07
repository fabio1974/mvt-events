package com.mvt.mvt_events.repository;

import com.mvt.mvt_events.jpa.CustomerPaymentPreference;
import com.mvt.mvt_events.jpa.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CustomerPaymentPreferenceRepository extends JpaRepository<CustomerPaymentPreference, Long> {

    /**
     * Busca a preferência de pagamento pelo usuário.
     */
    Optional<CustomerPaymentPreference> findByUser(User user);

    /**
     * Busca a preferência de pagamento pelo ID do usuário.
     */
    Optional<CustomerPaymentPreference> findByUserId(UUID userId);

    /**
     * Verifica se o usuário já possui uma preferência cadastrada.
     */
    boolean existsByUserId(UUID userId);

    /**
     * Deleta a preferência de pagamento do usuário.
     */
    void deleteByUserId(UUID userId);
}
