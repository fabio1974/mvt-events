package com.mvt.mvt_events.controller;

import com.mvt.mvt_events.jpa.Address;
import com.mvt.mvt_events.repository.AddressRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/addresses")
@RequiredArgsConstructor
@Slf4j
public class AddressController {

    private final AddressRepository addressRepository;

    /**
     * Busca endereço por User ID (UUID)
     * 
     * Endpoint: GET /api/addresses/{userId}
     * 
     * @param userId UUID do usuário
     * @return Address do usuário ou 404
     */
    @GetMapping("/{userId}")
    public ResponseEntity<Address> getAddressByUserId(@PathVariable UUID userId) {
        log.debug("📍 Buscando endereço do usuário: {}", userId);
        
        return addressRepository.findByUserId(userId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
