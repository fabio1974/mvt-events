package com.mvt.mvt_events.service;

import com.mvt.mvt_events.jpa.Product;
import com.mvt.mvt_events.jpa.ProductCategory;
import com.mvt.mvt_events.jpa.User;
import com.mvt.mvt_events.repository.ProductCategoryRepository;
import com.mvt.mvt_events.repository.ProductRepository;
import com.mvt.mvt_events.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductCategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final ImageService imageService;

    public ProductService(ProductRepository productRepository, ProductCategoryRepository categoryRepository,
                          UserRepository userRepository, ImageService imageService) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.userRepository = userRepository;
        this.imageService = imageService;
    }

    public List<Product> findByClient(UUID clientId) {
        return productRepository.findByClientIdOrderByDisplayOrderAsc(clientId);
    }

    public List<Product> findAvailableByClient(UUID clientId) {
        return productRepository.findByClientIdAndAvailableTrueOrderByDisplayOrderAsc(clientId);
    }

    public List<Product> findByCategory(Long categoryId) {
        return productRepository.findByCategoryIdOrderByDisplayOrderAsc(categoryId);
    }

    public List<Product> findAvailableByCategory(Long categoryId) {
        return productRepository.findByCategoryIdAndAvailableTrueOrderByDisplayOrderAsc(categoryId);
    }

    public Product findById(Long id) {
        return productRepository.findByIdWithCategory(id)
                .orElseThrow(() -> new RuntimeException("Produto não encontrado"));
    }

    public Product create(UUID clientId, Product product) {
        User client = userRepository.findById(clientId)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));
        if (client.getRole() != User.Role.CLIENT) {
            throw new RuntimeException("Apenas CLIENTs podem criar produtos");
        }

        product.setClient(client);

        // Vincular categoria se informada
        if (product.getCategory() != null && product.getCategory().getId() != null) {
            ProductCategory category = categoryRepository.findById(product.getCategory().getId())
                    .orElseThrow(() -> new RuntimeException("Categoria não encontrada"));
            if (!category.getClient().getId().equals(clientId)) {
                throw new RuntimeException("Categoria não pertence a este restaurante");
            }
            product.setCategory(category);
        }

        Product saved = productRepository.save(product);
        return productRepository.findByIdWithCategory(saved.getId()).orElse(saved);
    }

    public Product update(Long id, UUID clientId, Product updates) {
        Product product = findById(id);
        validateOwner(product, clientId);

        if (updates.getName() != null) product.setName(updates.getName());
        if (updates.getDescription() != null) product.setDescription(updates.getDescription());
        if (updates.getPrice() != null) product.setPrice(updates.getPrice());
        if (updates.getImageUrl() != null) product.setImageUrl(updates.getImageUrl());
        if (updates.getAvailable() != null) product.setAvailable(updates.getAvailable());
        if (updates.getPreparationTimeMinutes() != null) product.setPreparationTimeMinutes(updates.getPreparationTimeMinutes());
        if (updates.getDisplayOrder() != null) product.setDisplayOrder(updates.getDisplayOrder());

        // Trocar categoria
        if (updates.getCategory() != null && updates.getCategory().getId() != null) {
            ProductCategory category = categoryRepository.findById(updates.getCategory().getId())
                    .orElseThrow(() -> new RuntimeException("Categoria não encontrada"));
            if (!category.getClient().getId().equals(clientId)) {
                throw new RuntimeException("Categoria não pertence a este restaurante");
            }
            product.setCategory(category);
        }

        Product saved = productRepository.save(product);
        return productRepository.findByIdWithCategory(saved.getId()).orElse(saved);
    }

    public Product toggleAvailability(Long id, UUID clientId) {
        Product product = findById(id);
        validateOwner(product, clientId);
        product.setAvailable(!product.getAvailable());
        Product saved = productRepository.save(product);
        return productRepository.findByIdWithCategory(saved.getId()).orElse(saved);
    }

    public void delete(Long id, UUID clientId) {
        Product product = findById(id);
        validateOwner(product, clientId);
        // Remove imagem do Cloudinary se existir
        if (product.getImageUrl() != null) {
            imageService.delete(product.getImageUrl());
        }
        productRepository.delete(product);
    }

    private void validateOwner(Product product, UUID clientId) {
        if (!product.getClient().getId().equals(clientId)) {
            throw new RuntimeException("Você não tem permissão para alterar este produto");
        }
    }
}
