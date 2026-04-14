package com.mvt.mvt_events.service;

import com.mvt.mvt_events.jpa.ClientWaiter;
import com.mvt.mvt_events.jpa.User;
import com.mvt.mvt_events.repository.ClientWaiterRepository;
import com.mvt.mvt_events.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class ClientWaiterService {

    private final ClientWaiterRepository clientWaiterRepository;
    private final UserRepository userRepository;

    public ClientWaiterService(ClientWaiterRepository clientWaiterRepository, UserRepository userRepository) {
        this.clientWaiterRepository = clientWaiterRepository;
        this.userRepository = userRepository;
    }

    public List<ClientWaiter> findByClient(UUID clientId) {
        return clientWaiterRepository.findByClientId(clientId);
    }

    public List<ClientWaiter> findActiveByClient(UUID clientId) {
        return clientWaiterRepository.findByClientIdAndActive(clientId, true);
    }

    /** Estabelecimentos onde o garçom está ativo */
    public List<ClientWaiter> findEstablishmentsByWaiter(UUID waiterId) {
        return clientWaiterRepository.findActiveEstablishmentsByWaiter(waiterId);
    }

    public ClientWaiter link(UUID clientId, UUID waiterId, String pin) {
        User client = userRepository.findById(clientId)
                .orElseThrow(() -> new RuntimeException("Estabelecimento não encontrado"));
        if (client.getRole() != User.Role.CLIENT) {
            throw new RuntimeException("Apenas estabelecimentos podem vincular garçons");
        }

        User waiter = userRepository.findById(waiterId)
                .orElseThrow(() -> new RuntimeException("Garçom não encontrado"));
        if (waiter.getRole() != User.Role.WAITER) {
            throw new RuntimeException("Usuário não é um garçom (role atual: " + waiter.getRole() + ")");
        }

        if (clientWaiterRepository.existsByClientIdAndWaiterId(clientId, waiterId)) {
            throw new RuntimeException("Garçom já vinculado a este estabelecimento");
        }

        ClientWaiter cw = ClientWaiter.builder()
                .client(client)
                .waiter(waiter)
                .pin(pin)
                .active(true)
                .build();
        return clientWaiterRepository.save(cw);
    }

    public ClientWaiter linkByEmail(UUID clientId, String waiterEmail, String pin) {
        User waiter = userRepository.findByUsername(waiterEmail)
                .orElseThrow(() -> new RuntimeException("Garçom não encontrado com email: " + waiterEmail));
        return link(clientId, waiter.getId(), pin);
    }

    public ClientWaiter updatePin(UUID clientId, UUID waiterId, String newPin) {
        ClientWaiter cw = clientWaiterRepository.findByClientIdAndWaiterId(clientId, waiterId)
                .orElseThrow(() -> new RuntimeException("Vínculo não encontrado"));
        cw.setPin(newPin);
        return clientWaiterRepository.save(cw);
    }

    public ClientWaiter toggleActive(UUID clientId, UUID waiterId, boolean active) {
        ClientWaiter cw = clientWaiterRepository.findByClientIdAndWaiterId(clientId, waiterId)
                .orElseThrow(() -> new RuntimeException("Vínculo não encontrado"));
        cw.setActive(active);
        return clientWaiterRepository.save(cw);
    }

    public void unlink(UUID clientId, UUID waiterId) {
        ClientWaiter cw = clientWaiterRepository.findByClientIdAndWaiterId(clientId, waiterId)
                .orElseThrow(() -> new RuntimeException("Vínculo não encontrado"));
        clientWaiterRepository.delete(cw);
    }

    /** Valida PIN do garçom em um estabelecimento (login rápido) */
    public ClientWaiter validatePin(UUID clientId, String pin) {
        return clientWaiterRepository.findByClientIdAndPin(clientId, pin)
                .filter(ClientWaiter::getActive)
                .orElseThrow(() -> new RuntimeException("PIN inválido ou garçom inativo"));
    }
}
