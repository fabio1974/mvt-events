package com.mvt.mvt_events.controller;

import com.mvt.mvt_events.jpa.ProductCategory;
import com.mvt.mvt_events.jpa.StoreProfile;
import com.mvt.mvt_events.jpa.User;
import com.mvt.mvt_events.repository.StoreProfileRepository;
import com.mvt.mvt_events.repository.UserRepository;
import com.mvt.mvt_events.service.ProductCategoryService;
import com.mvt.mvt_events.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Vitrine pública de restaurantes — Zapi-Food.
 * Endpoints acessíveis por CUSTOMERs para navegar e escolher restaurantes.
 */
@RestController
@RequestMapping("/api/stores")
@Tag(name = "Stores", description = "Vitrine de restaurantes — Zapi-Food")
public class StoreController {

    private final StoreProfileRepository storeProfileRepository;
    private final UserRepository userRepository;
    private final ProductCategoryService categoryService;
    private final ProductService productService;

    public StoreController(StoreProfileRepository storeProfileRepository, UserRepository userRepository,
                           ProductCategoryService categoryService, ProductService productService) {
        this.storeProfileRepository = storeProfileRepository;
        this.userRepository = userRepository;
        this.categoryService = categoryService;
        this.productService = productService;
    }

    @GetMapping
    @Operation(summary = "Listar restaurantes", description = "Lista restaurantes abertos com perfil de loja. Filtros: serviceType, open")
    public ResponseEntity<List<Map<String, Object>>> listStores(
            @RequestParam(required = false) String serviceType,
            @RequestParam(required = false, defaultValue = "true") boolean openOnly) {

        // Busca CLIENTs com store_profile
        List<StoreProfile> profiles = openOnly
                ? storeProfileRepository.findByIsOpenTrue()
                : storeProfileRepository.findAll();

        List<Map<String, Object>> stores = profiles.stream()
                .filter(p -> p.getUser() != null && p.getUser().getRole() == User.Role.CLIENT)
                .filter(p -> serviceType == null || (p.getUser().getServiceType() != null && serviceType.equalsIgnoreCase(p.getUser().getServiceType().name())))
                .map(this::mapStoreToResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(stores);
    }

    @GetMapping("/{clientId}")
    @Operation(summary = "Detalhe do restaurante", description = "Dados da loja + informações do CLIENT")
    public ResponseEntity<Map<String, Object>> getStore(@PathVariable UUID clientId) {
        StoreProfile profile = storeProfileRepository.findByUserId(clientId)
                .orElseThrow(() -> new RuntimeException("Restaurante não encontrado"));
        return ResponseEntity.ok(mapStoreToResponse(profile));
    }

    @GetMapping("/{clientId}/menu")
    @Operation(summary = "Cardápio completo", description = "Categorias + produtos disponíveis do restaurante")
    public ResponseEntity<Map<String, Object>> getMenu(@PathVariable UUID clientId) {
        StoreProfile profile = storeProfileRepository.findByUserId(clientId)
                .orElseThrow(() -> new RuntimeException("Restaurante não encontrado"));

        List<ProductCategory> categories = categoryService.findActiveByClient(clientId);

        List<Map<String, Object>> menuCategories = categories.stream().map(cat -> {
            Map<String, Object> catMap = new LinkedHashMap<>();
            catMap.put("id", cat.getId());
            catMap.put("name", cat.getName());
            catMap.put("description", cat.getDescription());
            catMap.put("imageUrl", cat.getImageUrl());
            catMap.put("products", productService.findAvailableByCategory(cat.getId()));
            return catMap;
        }).collect(Collectors.toList());

        // Produtos sem categoria
        var uncategorized = productService.findAvailableByClient(clientId).stream()
                .filter(p -> p.getCategory() == null)
                .collect(Collectors.toList());

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("store", mapStoreToResponse(profile));
        response.put("categories", menuCategories);
        if (!uncategorized.isEmpty()) {
            response.put("uncategorized", uncategorized);
        }

        return ResponseEntity.ok(response);
    }

    private Map<String, Object> mapStoreToResponse(StoreProfile profile) {
        User user = profile.getUser();
        Map<String, Object> store = new LinkedHashMap<>();
        store.put("id", user.getId());
        store.put("name", user.getName());
        store.put("serviceType", user.getServiceType());
        store.put("isOpen", profile.getIsOpen());
        store.put("description", profile.getDescription());
        store.put("logoUrl", profile.getLogoUrl());
        store.put("coverUrl", profile.getCoverUrl());
        store.put("minOrder", profile.getMinOrder());
        store.put("avgPreparationMinutes", profile.getAvgPreparationMinutes());
        store.put("openingHours", profile.getOpeningHours());
        // Localização do restaurante (para calcular distância)
        store.put("latitude", user.getGpsLatitude());
        store.put("longitude", user.getGpsLongitude());
        return store;
    }
}
