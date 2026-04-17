package com.mvt.mvt_events.jpa;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Produto/item do cardápio — módulo Zapi-Food.
 * Ex: "Pizza Margherita", R$ 29,90
 */
@Entity
@Table(name = "products")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private User client;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    @Getter(lombok.AccessLevel.NONE)
    @Setter(lombok.AccessLevel.NONE)
    private ProductCategory category;

    // Getter/setter manuais (Lombok excluído para controle de Jackson)
    @com.fasterxml.jackson.annotation.JsonIgnore
    public ProductCategory getCategory() { return category; }
    public void setCategory(ProductCategory category) { this.category = category; }

    @NotBlank
    @Size(max = 200)
    @Column(nullable = false, length = 200)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @NotNull
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Builder.Default
    @Column(nullable = false)
    private Boolean available = true;

    @Column(name = "preparation_time_minutes")
    private Integer preparationTimeMinutes;

    @Builder.Default
    @Column(name = "display_order", nullable = false)
    private Integer displayOrder = 0;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "sales_channel", nullable = false, length = 20)
    private SalesChannel salesChannel = SalesChannel.ALL;

    public enum SalesChannel {
        DELIVERY, TABLE, ALL
    }

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    // Output: retorna categoryId e categoryName como campos planos no JSON
    @com.fasterxml.jackson.annotation.JsonGetter("categoryId")
    public Long serializeCategoryId() {
        return category != null ? category.getId() : null;
    }

    @com.fasterxml.jackson.annotation.JsonGetter("categoryName")
    public String serializeCategoryName() {
        try {
            return category != null ? category.getName() : null;
        } catch (Exception e) {
            return null;
        }
    }

    // Input: aceita { "category": { "id": N } } no request JSON
    @com.fasterxml.jackson.annotation.JsonSetter("category")
    public void setCategoryFromJson(java.util.Map<String, Object> catMap) {
        if (catMap != null && catMap.get("id") != null) {
            ProductCategory cat = new ProductCategory();
            cat.setId(((Number) catMap.get("id")).longValue());
            this.category = cat;
        }
    }

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
        updatedAt = OffsetDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}
