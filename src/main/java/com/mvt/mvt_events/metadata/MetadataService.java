package com.mvt.mvt_events.metadata;

import com.mvt.mvt_events.jpa.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class MetadataService {

    @Autowired
    private JpaMetadataExtractor jpaExtractor;

    private static final Map<String, EntityConfig> ENTITIES = new HashMap<>();

    static {
        // ==================== Sistema Base ====================
        ENTITIES.put("organization", new EntityConfig(Organization.class, "Grupos", "/api/organizations"));
        ENTITIES.put("user", new EntityConfig(User.class, "Usuários", "/api/users"));

        // TODO: Recriar Payment para deliveries
        // ENTITIES.put("payment", new EntityConfig(Payment.class, "Pagamentos",
        // "/api/payments"));

        // ==================== Zapi10 (Sistema de Entregas) ====================
        ENTITIES.put("delivery", new EntityConfig(Delivery.class, "Entregas", "/api/deliveries"));
        ENTITIES.put("courierProfile",
                new EntityConfig(CourierProfile.class, "Perfis de Motoboy", "/api/courier-profiles"));
        ENTITIES.put("evaluation", new EntityConfig(Evaluation.class, "Avaliações", "/api/evaluations"));
    }

    public Map<String, EntityMetadata> getAllEntitiesMetadata() {
        Map<String, EntityMetadata> all = new HashMap<>();
        for (Map.Entry<String, EntityConfig> entry : ENTITIES.entrySet()) {
            all.put(entry.getKey(), getEntityMetadata(entry.getKey()));
        }
        return all;
    }

    public EntityMetadata getEntityMetadata(String entityName) {
        EntityConfig config = ENTITIES.get(entityName);
        if (config == null)
            throw new IllegalArgumentException("Entity not found: " + entityName);

        EntityMetadata metadata = new EntityMetadata(entityName, config.label, config.endpoint);
        List<FieldMetadata> fields = jpaExtractor.extractFields(config.entityClass);

        // Criar tableFields e aplicar visibilidade baseada em @Visible(table)
        List<FieldMetadata> tableFields = fields.stream()
                .filter(f -> !isSystemField(f.getName()))
                .map(f -> {
                    FieldMetadata copy = copyField(f);
                    // Se @Visible(table = false), marca como não visível
                    if (f.getHiddenFromTable() != null && f.getHiddenFromTable()) {
                        copy.setVisible(false);
                        copy.setSortable(false);
                    }
                    return copy;
                })
                .toList();

        // Criar formFields e aplicar visibilidade baseada em @Visible(form)
        List<FieldMetadata> formFields = fields.stream()
                .filter(f -> !isSystemField(f.getName()))
                .map(f -> {
                    FieldMetadata copy = copyField(f);
                    // ⚠️ IMPORTANTE: Campos com @DisplayLabel devem SEMPRE estar visíveis no form
                    // porque são usados como label em relacionamentos
                    boolean isDisplayLabel = isDisplayLabelField(config.entityClass, f.getName());

                    if (isDisplayLabel) {
                        // Campos @DisplayLabel sempre visíveis
                        copy.setVisible(true);
                    } else if (f.getHiddenFromForm() != null && f.getHiddenFromForm()) {
                        // Se @Visible(form = false), marca como não visível
                        copy.setVisible(false);
                    } else if (f.getHiddenFromForm() != null && !f.getHiddenFromForm()) {
                        // Se @Visible(form = true), marca como visível
                        copy.setVisible(true);
                    }
                    return copy;
                })
                .toList();

        metadata.setTableFields(tableFields);
        metadata.setFormFields(formFields);

        // Detectar e definir o labelField (campo com @DisplayLabel)
        String labelField = findDisplayLabelField(config.entityClass);
        if (labelField != null) {
            metadata.setLabelField(labelField);
        }

        // Extrair filtros e remover apenas os marcados com @Visible(filter = false)
        List<FilterMetadata> filters = jpaExtractor.extractFilters(config.entityClass).stream()
                .filter(f -> !isFieldHiddenFromFilter(fields, f.getName()))
                .toList();
        metadata.setFilters(filters);

        metadata.setPagination(null);

        return metadata;
    }

    /**
     * Cria uma cópia do FieldMetadata para poder modificar a propriedade visible
     * sem afetar outras listas
     */
    private FieldMetadata copyField(FieldMetadata source) {
        FieldMetadata copy = new FieldMetadata();
        copy.setName(source.getName());
        copy.setLabel(source.getLabel());
        copy.setType(source.getType());
        copy.setRequired(source.getRequired());
        copy.setReadonly(source.isReadonly());
        copy.setSortable(source.isSortable());
        copy.setSearchable(source.isSearchable());
        copy.setVisible(source.isVisible());
        copy.setWidth(source.getWidth());
        copy.setOptions(source.getOptions());
        copy.setRelationship(source.getRelationship());
        copy.setMaxLength(source.getMaxLength());
        copy.setMinLength(source.getMinLength());
        copy.setMax(source.getMax());
        copy.setMin(source.getMin());
        copy.setDefaultValue(source.getDefaultValue());
        copy.setHiddenFromTable(source.getHiddenFromTable());
        copy.setHiddenFromForm(source.getHiddenFromForm());
        copy.setHiddenFromFilter(source.getHiddenFromFilter());
        copy.setComputed(source.getComputed());
        copy.setComputedDependencies(source.getComputedDependencies());
        return copy;
    }

    private boolean isSystemField(String fieldName) {
        return "id".equals(fieldName) || "createdAt".equals(fieldName) ||
                "updatedAt".equals(fieldName) || "tenantId".equals(fieldName);
    }

    /**
     * Verifica se um filtro corresponde a um campo que está oculto de filtros
     * via @Visible(filter = false)
     */
    private boolean isFieldHiddenFromFilter(List<FieldMetadata> fields, String filterName) {
        return fields.stream()
                .filter(f -> f.getName().equals(filterName))
                .anyMatch(f -> f.getHiddenFromFilter() != null && f.getHiddenFromFilter());
    }

    /**
     * Verifica se um campo tem a anotação @DisplayLabel.
     * Campos com @DisplayLabel devem sempre estar visíveis no formFields
     * porque são usados como label em relacionamentos.
     */
    private boolean isDisplayLabelField(Class<?> entityClass, String fieldName) {
        try {
            java.lang.reflect.Field field = entityClass.getDeclaredField(fieldName);
            return field.isAnnotationPresent(DisplayLabel.class);
        } catch (NoSuchFieldException e) {
            // Campo pode estar em superclasse
            Class<?> superclass = entityClass.getSuperclass();
            if (superclass != null) {
                try {
                    java.lang.reflect.Field field = superclass.getDeclaredField(fieldName);
                    return field.isAnnotationPresent(DisplayLabel.class);
                } catch (NoSuchFieldException ex) {
                    return false;
                }
            }
            return false;
        }
    }

    /**
     * Encontra o nome do campo anotado com @DisplayLabel.
     * Retorna o nome do campo, ou null se não encontrado.
     */
    private String findDisplayLabelField(Class<?> entityClass) {
        try {
            // Procura nos campos da classe
            for (java.lang.reflect.Field field : entityClass.getDeclaredFields()) {
                if (field.isAnnotationPresent(DisplayLabel.class)) {
                    return field.getName();
                }
            }

            // Procura nos campos da superclasse
            Class<?> superclass = entityClass.getSuperclass();
            if (superclass != null) {
                for (java.lang.reflect.Field field : superclass.getDeclaredFields()) {
                    if (field.isAnnotationPresent(DisplayLabel.class)) {
                        return field.getName();
                    }
                }
            }
        } catch (Exception e) {
            System.err.println(
                    "Error finding display label field for: " + entityClass.getName() + " - " + e.getMessage());
        }
        return null;
    }

    private static class EntityConfig {
        final Class<?> entityClass;
        final String label;
        final String endpoint;

        EntityConfig(Class<?> entityClass, String label, String endpoint) {
            this.entityClass = entityClass;
            this.label = label;
            this.endpoint = endpoint;
        }
    }
}
