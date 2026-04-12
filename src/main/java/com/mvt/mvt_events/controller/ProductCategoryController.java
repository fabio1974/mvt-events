package com.mvt.mvt_events.controller;

import com.mvt.mvt_events.jpa.ProductCategory;
import com.mvt.mvt_events.jpa.User;
import com.mvt.mvt_events.service.ProductCategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/categories")
@Tag(name = "Product Categories", description = "Categorias do cardápio — Zapi-Food")
public class ProductCategoryController {

    private final ProductCategoryService categoryService;

    public ProductCategoryController(ProductCategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping("/client/{clientId}")
    @Operation(summary = "Listar categorias de um restaurante", description = "Público — usado pela vitrine")
    public ResponseEntity<List<ProductCategory>> listByClient(@PathVariable UUID clientId) {
        return ResponseEntity.ok(categoryService.findActiveByClient(clientId));
    }

    @GetMapping("/me")
    @Operation(summary = "Minhas categorias", description = "CLIENT: lista todas (ativas e inativas)")
    public ResponseEntity<List<ProductCategory>> listMine(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        return ResponseEntity.ok(categoryService.findByClient(user.getId()));
    }

    @PostMapping
    @Operation(summary = "Criar categoria")
    public ResponseEntity<ProductCategory> create(Authentication authentication, @RequestBody ProductCategory category) {
        User user = (User) authentication.getPrincipal();
        return ResponseEntity.status(HttpStatus.CREATED).body(categoryService.create(user.getId(), category));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar categoria")
    public ResponseEntity<ProductCategory> update(Authentication authentication, @PathVariable Long id, @RequestBody ProductCategory updates) {
        User user = (User) authentication.getPrincipal();
        return ResponseEntity.ok(categoryService.update(id, user.getId(), updates));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remover categoria")
    public ResponseEntity<Void> delete(Authentication authentication, @PathVariable Long id) {
        User user = (User) authentication.getPrincipal();
        categoryService.delete(id, user.getId());
        return ResponseEntity.noContent().build();
    }
}
