package com.mvt.mvt_events.controller;

import com.mvt.mvt_events.jpa.Product;
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
import org.springframework.security.core.Authentication;
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
    @Operation(summary = "Listar restaurantes", description = "Lista restaurantes próximos com perfil de loja. Filtros: serviceType, open, lat/lng/radius")
    public ResponseEntity<List<Map<String, Object>>> listStores(
            @RequestParam(required = false) String serviceType,
            @RequestParam(required = false, defaultValue = "true") boolean openOnly,
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lng,
            @RequestParam(required = false, defaultValue = "5") double radiusKm,
            Authentication authentication) {

        // Busca CLIENTs com store_profile (JOIN FETCH user)
        List<StoreProfile> profiles = openOnly
                ? storeProfileRepository.findByIsOpenTrue()
                : storeProfileRepository.findAllWithUser();

        // Se não recebeu lat/lng, tenta pegar do usuário logado
        Double userLat = lat;
        Double userLng = lng;
        if (userLat == null || userLng == null) {
            if (authentication != null && authentication.getPrincipal() instanceof User) {
                User currentUser = (User) authentication.getPrincipal();
                userLat = currentUser.getGpsLatitude();
                userLng = currentUser.getGpsLongitude();
            }
        }

        final Double finalLat = userLat;
        final Double finalLng = userLng;

        List<Map<String, Object>> stores = profiles.stream()
                .filter(p -> p.getUser() != null && p.getUser().getRole() == User.Role.CLIENT)
                .filter(p -> serviceType == null || (p.getUser().getServiceType() != null && serviceType.equalsIgnoreCase(p.getUser().getServiceType().name())))
                // Filtro por distância (Haversine)
                .filter(p -> {
                    if (finalLat == null || finalLng == null) return true; // sem coordenadas → mostra tudo
                    Double storeLat = p.getUser().getGpsLatitude();
                    Double storeLng = p.getUser().getGpsLongitude();
                    if (storeLat == null || storeLng == null) return true; // sem coords do store → mostra
                    return haversineKm(finalLat, finalLng, storeLat, storeLng) <= radiusKm;
                })
                .map(p -> {
                    Map<String, Object> store = mapStoreToResponse(p);
                    // Adiciona distância se possível
                    if (finalLat != null && finalLng != null && p.getUser().getGpsLatitude() != null && p.getUser().getGpsLongitude() != null) {
                        double dist = haversineKm(finalLat, finalLng, p.getUser().getGpsLatitude(), p.getUser().getGpsLongitude());
                        store.put("distanceKm", Math.round(dist * 10.0) / 10.0);
                    }
                    return store;
                })
                .sorted((a, b) -> {
                    Double da = (Double) a.getOrDefault("distanceKm", 999.0);
                    Double db = (Double) b.getOrDefault("distanceKm", 999.0);
                    return Double.compare(da, db);
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(stores);
    }

    private double haversineKm(double lat1, double lon1, double lat2, double lon2) {
        double R = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
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
            var catProducts = productService.findAvailableByCategory(cat.getId()).stream()
                    .filter(p -> p.getSalesChannel() == Product.SalesChannel.DELIVERY || p.getSalesChannel() == Product.SalesChannel.ALL)
                    .collect(Collectors.toList());
            catMap.put("products", catProducts);
            return catMap;
        }).collect(Collectors.toList());

        // Produtos sem categoria
        var uncategorized = productService.findAvailableByClient(clientId).stream()
                .filter(p -> p.getCategory() == null)
                .filter(p -> p.getSalesChannel() == Product.SalesChannel.DELIVERY || p.getSalesChannel() == Product.SalesChannel.ALL)
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
