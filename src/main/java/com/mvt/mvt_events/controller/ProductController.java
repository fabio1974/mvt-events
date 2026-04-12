package com.mvt.mvt_events.controller;

import com.mvt.mvt_events.jpa.Product;
import com.mvt.mvt_events.jpa.User;
import com.mvt.mvt_events.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/products")
@Tag(name = "Products", description = "Produtos do cardápio — Zapi-Food")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping("/client/{clientId}")
    @Operation(summary = "Listar produtos disponíveis de um restaurante", description = "Público — usado pela vitrine")
    public ResponseEntity<List<Product>> listByClient(@PathVariable UUID clientId) {
        return ResponseEntity.ok(productService.findAvailableByClient(clientId));
    }

    @GetMapping("/category/{categoryId}")
    @Operation(summary = "Listar produtos de uma categoria")
    public ResponseEntity<List<Product>> listByCategory(@PathVariable Long categoryId) {
        return ResponseEntity.ok(productService.findAvailableByCategory(categoryId));
    }

    @GetMapping("/me")
    @Operation(summary = "Meus produtos", description = "CLIENT: lista todos (disponíveis e indisponíveis)")
    public ResponseEntity<List<Product>> listMine(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        return ResponseEntity.ok(productService.findByClient(user.getId()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalhe do produto")
    public ResponseEntity<Product> getById(@PathVariable Long id) {
        return ResponseEntity.ok(productService.findById(id));
    }

    @PostMapping
    @Operation(summary = "Criar produto")
    public ResponseEntity<Product> create(Authentication authentication, @RequestBody Product product) {
        User user = (User) authentication.getPrincipal();
        return ResponseEntity.status(HttpStatus.CREATED).body(productService.create(user.getId(), product));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar produto")
    public ResponseEntity<Product> update(Authentication authentication, @PathVariable Long id, @RequestBody Product updates) {
        User user = (User) authentication.getPrincipal();
        return ResponseEntity.ok(productService.update(id, user.getId(), updates));
    }

    @PatchMapping("/{id}/toggle")
    @Operation(summary = "Pausar/ativar produto")
    public ResponseEntity<Map<String, Object>> toggle(Authentication authentication, @PathVariable Long id) {
        User user = (User) authentication.getPrincipal();
        Product product = productService.toggleAvailability(id, user.getId());
        return ResponseEntity.ok(Map.of(
                "available", product.getAvailable(),
                "message", product.getAvailable() ? "Produto ativado" : "Produto pausado"
        ));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remover produto")
    public ResponseEntity<Void> delete(Authentication authentication, @PathVariable Long id) {
        User user = (User) authentication.getPrincipal();
        productService.delete(id, user.getId());
        return ResponseEntity.noContent().build();
    }
}
