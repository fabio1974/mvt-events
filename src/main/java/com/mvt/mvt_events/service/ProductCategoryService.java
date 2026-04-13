package com.mvt.mvt_events.service;

import com.mvt.mvt_events.jpa.ProductCategory;
import com.mvt.mvt_events.jpa.User;
import com.mvt.mvt_events.repository.ProductCategoryRepository;
import com.mvt.mvt_events.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class ProductCategoryService {

    private final ProductCategoryRepository categoryRepository;
    private final UserRepository userRepository;

    public ProductCategoryService(ProductCategoryRepository categoryRepository, UserRepository userRepository) {
        this.categoryRepository = categoryRepository;
        this.userRepository = userRepository;
    }

    public List<ProductCategory> findByClient(UUID clientId) {
        return categoryRepository.findByClientIdOrderByDisplayOrderAsc(clientId);
    }

    public List<ProductCategory> findActiveByClient(UUID clientId) {
        return categoryRepository.findByClientIdAndActiveTrueOrderByDisplayOrderAsc(clientId);
    }

    public ProductCategory findById(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Categoria não encontrada"));
    }

    public ProductCategory create(UUID clientId, ProductCategory category) {
        User client = userRepository.findById(clientId)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));
        if (client.getRole() != User.Role.CLIENT) {
            throw new RuntimeException("Apenas CLIENTs podem criar categorias");
        }
        // Verificar duplicidade (case-insensitive)
        boolean exists = categoryRepository.findByClientIdOrderByDisplayOrderAsc(clientId).stream()
                .anyMatch(c -> c.getName().equalsIgnoreCase(category.getName().trim()));
        if (exists) {
            throw new RuntimeException("Já existe uma categoria com o nome '" + category.getName().trim() + "'");
        }
        category.setClient(client);
        return categoryRepository.save(category);
    }

    public ProductCategory update(Long id, UUID clientId, ProductCategory updates) {
        ProductCategory category = findById(id);
        validateOwner(category, clientId);

        if (updates.getName() != null) category.setName(updates.getName());
        if (updates.getDescription() != null) category.setDescription(updates.getDescription());
        if (updates.getDisplayOrder() != null) category.setDisplayOrder(updates.getDisplayOrder());
        if (updates.getActive() != null) category.setActive(updates.getActive());
        if (updates.getImageUrl() != null) category.setImageUrl(updates.getImageUrl());

        return categoryRepository.save(category);
    }

    public void delete(Long id, UUID clientId) {
        ProductCategory category = findById(id);
        validateOwner(category, clientId);
        categoryRepository.delete(category);
    }

    private void validateOwner(ProductCategory category, UUID clientId) {
        if (!category.getClient().getId().equals(clientId)) {
            throw new RuntimeException("Você não tem permissão para alterar esta categoria");
        }
    }
}
