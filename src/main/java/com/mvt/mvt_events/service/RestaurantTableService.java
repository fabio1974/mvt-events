package com.mvt.mvt_events.service;

import com.mvt.mvt_events.jpa.RestaurantTable;
import com.mvt.mvt_events.jpa.User;
import com.mvt.mvt_events.repository.RestaurantTableRepository;
import com.mvt.mvt_events.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class RestaurantTableService {

    private final RestaurantTableRepository tableRepository;
    private final UserRepository userRepository;

    public RestaurantTableService(RestaurantTableRepository tableRepository, UserRepository userRepository) {
        this.tableRepository = tableRepository;
        this.userRepository = userRepository;
    }

    public List<RestaurantTable> findByClient(UUID clientId) {
        return tableRepository.findByClientIdOrderByNumber(clientId);
    }

    public List<RestaurantTable> findActiveByClient(UUID clientId) {
        return tableRepository.findByClientIdAndActiveOrderByNumber(clientId, true);
    }

    public RestaurantTable findById(Long id) {
        return tableRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Mesa não encontrada"));
    }

    public RestaurantTable create(UUID clientId, Integer number, String label, Integer seats) {
        User client = userRepository.findById(clientId)
                .orElseThrow(() -> new RuntimeException("Estabelecimento não encontrado"));
        if (client.getRole() != User.Role.CLIENT) {
            throw new RuntimeException("Apenas estabelecimentos podem ter mesas");
        }
        if (tableRepository.existsByClientIdAndNumber(clientId, number)) {
            throw new RuntimeException("Mesa #" + number + " já existe neste estabelecimento");
        }

        RestaurantTable table = RestaurantTable.builder()
                .client(client)
                .number(number)
                .label(label)
                .seats(seats)
                .active(true)
                .build();
        return tableRepository.save(table);
    }

    public List<RestaurantTable> createBatch(UUID clientId, int from, int to, Integer seats) {
        User client = userRepository.findById(clientId)
                .orElseThrow(() -> new RuntimeException("Estabelecimento não encontrado"));
        if (client.getRole() != User.Role.CLIENT) {
            throw new RuntimeException("Apenas estabelecimentos podem ter mesas");
        }

        List<RestaurantTable> tables = new java.util.ArrayList<>();
        for (int i = from; i <= to; i++) {
            if (!tableRepository.existsByClientIdAndNumber(clientId, i)) {
                RestaurantTable table = RestaurantTable.builder()
                        .client(client)
                        .number(i)
                        .seats(seats)
                        .active(true)
                        .build();
                tables.add(tableRepository.save(table));
            }
        }
        return tables;
    }

    public RestaurantTable update(Long id, UUID clientId, String label, Integer seats, Boolean active) {
        RestaurantTable table = findById(id);
        if (!table.getClient().getId().equals(clientId)) {
            throw new RuntimeException("Mesa não pertence a este estabelecimento");
        }
        if (label != null) table.setLabel(label);
        if (seats != null) table.setSeats(seats);
        if (active != null) table.setActive(active);
        return tableRepository.save(table);
    }

    public void delete(Long id, UUID clientId) {
        RestaurantTable table = findById(id);
        if (!table.getClient().getId().equals(clientId)) {
            throw new RuntimeException("Mesa não pertence a este estabelecimento");
        }
        tableRepository.delete(table);
    }
}
