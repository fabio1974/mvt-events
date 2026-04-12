package com.mvt.mvt_events.controller;

import com.mvt.mvt_events.jpa.StoreProfile;
import com.mvt.mvt_events.jpa.User;
import com.mvt.mvt_events.service.StoreProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/store-profile")
@Tag(name = "Store Profile", description = "Perfil da loja — Zapi-Food")
public class StoreProfileController {

    private final StoreProfileService storeProfileService;

    public StoreProfileController(StoreProfileService storeProfileService) {
        this.storeProfileService = storeProfileService;
    }

    @GetMapping("/me")
    @Operation(summary = "Meu perfil de loja", description = "Retorna ou cria o perfil da loja do CLIENT logado")
    public ResponseEntity<StoreProfile> getMyProfile(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        return ResponseEntity.ok(storeProfileService.getOrCreate(user.getId()));
    }

    @PutMapping("/me")
    @Operation(summary = "Atualizar perfil de loja")
    public ResponseEntity<StoreProfile> updateMyProfile(Authentication authentication, @RequestBody StoreProfile updates) {
        User user = (User) authentication.getPrincipal();
        return ResponseEntity.ok(storeProfileService.update(user.getId(), updates));
    }

    @PatchMapping("/me/toggle")
    @Operation(summary = "Abrir/fechar loja")
    public ResponseEntity<Map<String, Object>> toggleOpen(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        StoreProfile profile = storeProfileService.toggleOpen(user.getId());
        return ResponseEntity.ok(Map.of(
                "isOpen", profile.getIsOpen(),
                "message", profile.getIsOpen() ? "Loja aberta" : "Loja fechada"
        ));
    }

    @PatchMapping("/me/open")
    @Operation(summary = "Definir status da loja (aberta/fechada)")
    public ResponseEntity<Map<String, Object>> setOpen(Authentication authentication, @RequestBody Map<String, Boolean> body) {
        User user = (User) authentication.getPrincipal();
        boolean open = body.getOrDefault("open", false);
        StoreProfile profile = storeProfileService.setOpen(user.getId(), open);
        return ResponseEntity.ok(Map.of(
                "isOpen", profile.getIsOpen(),
                "message", profile.getIsOpen() ? "Loja aberta" : "Loja fechada"
        ));
    }
}
