package com.mvt.mvt_events.service;

import com.mvt.mvt_events.jpa.CustomerPaymentPreference;
import com.mvt.mvt_events.jpa.User;
import com.mvt.mvt_events.repository.BankAccountRepository;
import com.mvt.mvt_events.repository.CustomerCardRepository;
import com.mvt.mvt_events.repository.CustomerPaymentPreferenceRepository;
import com.mvt.mvt_events.repository.UserRepository;
import com.mvt.mvt_events.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Recalcula o flag {@code User.enabled} a partir dos pré-requisitos do role.
 *
 * <p>Substitui a abordagem antiga em que o flag era atualizado apenas:
 * <ul>
 *   <li>No startup pelo {@code UserIntegrityCheck} (latência alta, perdia eventos entre restarts)</li>
 *   <li>Pelo endpoint admin {@code PUT /users/{id}} setando enabled=true (manual)</li>
 * </ul>
 *
 * <p>Os pontos de save dos artefatos de onboarding (BankAccount, Vehicle, pagarme_status,
 * serviceType, CustomerCard, CustomerPaymentPreference) chamam {@link #recalculate(UUID)}
 * para manter o flag em sincronia com a fonte da verdade.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserActivationService {

    private final UserRepository userRepository;
    private final BankAccountRepository bankAccountRepository;
    private final VehicleRepository vehicleRepository;
    private final CustomerCardRepository customerCardRepository;
    private final CustomerPaymentPreferenceRepository customerPaymentPreferenceRepository;

    /**
     * Recalcula {@code User.enabled} para o usuário informado conforme os pré-requisitos do role.
     * Usuários com {@code blocked=true} ou {@code deletedAt!=null} nunca são reativados.
     *
     * @param userId id do usuário
     * @return novo valor de {@code enabled} após o recálculo
     */
    @Transactional
    public boolean recalculate(UUID userId) {
        if (userId == null) return false;

        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            log.warn("[ACTIVATION] recalculate chamado para userId inexistente: {}", userId);
            return false;
        }

        // Conta deletada ou bloqueada nunca pode ficar enabled.
        if (user.getDeletedAt() != null || user.isBlocked()) {
            if (user.getEnabled()) {
                user.setEnabled(false);
                userRepository.save(user);
                log.info("[ACTIVATION] {} desativado (blocked ou deleted)", user.getUsername());
            }
            return false;
        }

        boolean shouldBeEnabled = meetsPrerequisites(user);

        if (shouldBeEnabled != user.getEnabled()) {
            user.setEnabled(shouldBeEnabled);
            userRepository.save(user);
            log.info("[ACTIVATION] {} {} (role={})",
                    user.getUsername(),
                    shouldBeEnabled ? "ativado" : "desativado",
                    user.getRole());
        }

        return shouldBeEnabled;
    }

    /**
     * Verifica se o usuário cumpre os pré-requisitos do seu role.
     * Espelha a lógica de {@code UserService.getActivationStatus} e {@code UserIntegrityCheck},
     * que devem permanecer consistentes com este método.
     */
    private boolean meetsPrerequisites(User user) {
        switch (user.getRole()) {
            case ORGANIZER:
                return bankAccountRepository.existsByUserId(user.getId())
                        && hasActivePagarmeRecipient(user);

            case COURIER:
                return bankAccountRepository.existsByUserId(user.getId())
                        && hasActivePagarmeRecipient(user)
                        && !vehicleRepository.findByOwnerId(user.getId()).isEmpty()
                        && user.getServiceType() != null;

            case CUSTOMER:
                return hasPaymentMethod(user);

            case CLIENT:
                return bankAccountRepository.existsByUserId(user.getId())
                        && hasPaymentMethod(user);

            // ADMIN, WAITER, USER: sem pré-requisitos transacionais — manter como está.
            default:
                return user.getEnabled();
        }
    }

    private boolean hasActivePagarmeRecipient(User user) {
        return user.getPagarmeRecipientId() != null
                && "active".equals(user.getPagarmeStatus());
    }

    private boolean hasPaymentMethod(User user) {
        boolean hasActiveCard = customerCardRepository.countActiveCardsByCustomerId(user.getId()) > 0;
        boolean hasPixPreference = customerPaymentPreferenceRepository.findByUserId(user.getId())
                .map(pref -> pref.getPreferredPaymentType() == CustomerPaymentPreference.PreferredPaymentType.PIX)
                .orElse(false);
        return hasActiveCard || hasPixPreference;
    }
}
