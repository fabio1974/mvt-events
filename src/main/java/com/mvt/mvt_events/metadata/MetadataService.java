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
        ENTITIES.put("siteConfiguration", new EntityConfig(SiteConfiguration.class, "Configurações do Sistema", "/api/site-configuration"));
        ENTITIES.put("specialZone", new EntityConfig(SpecialZone.class, "Zonas Especiais", "/api/special-zones"));
        ENTITIES.put("bankAccount", new EntityConfig(BankAccount.class, "Contas Bancárias", "/api/bank-accounts"));
        ENTITIES.put("address", new EntityConfig(Address.class, "Endereços", "/api/addresses"));

        // ==================== Pagamentos (Pagar.me Integration) ====================
        ENTITIES.put("payment", new EntityConfig(Payment.class, "Pagamentos", "/api/payments"));

        // ==================== Zapi10 (Sistema de Entregas) ====================
        ENTITIES.put("delivery", new EntityConfig(Delivery.class, "Corridas", "/api/deliveries"));
        // CourierProfile removido - todos dados de courier estão em User com role COURIER
        ENTITIES.put("evaluation", new EntityConfig(Evaluation.class, "Avaliações", "/api/evaluations"));
        ENTITIES.put("clientContract", new EntityConfig(ClientContract.class, "Contrato de Serviço", "/api/client-contracts"));
        ENTITIES.put("employmentContract", new EntityConfig(EmploymentContract.class, "Contrato de Motoboy", "/api/employment-contracts"));

        // ==================== Zapi-Food ====================
        ENTITIES.put("foodOrder", new EntityConfig(FoodOrder.class, "Pedidos", "/api/orders"));
    }

    public Map<String, EntityMetadata> getAllEntitiesMetadata() {
        Map<String, EntityMetadata> all = new HashMap<>();
        for (Map.Entry<String, EntityConfig> entry : ENTITIES.entrySet()) {
            all.put(entry.getKey(), getEntityMetadata(entry.getKey()));
        }
        // Virtual entities (relatórios)
        all.put("mobileVersionReport", buildMobileVersionReportMetadata());
        return all;
    }

    /**
     * Metadata do relatório de versões do app mobile (virtual — não tem entidade JPA própria).
     * Endpoint: /api/users/mobile-versions (DTO MobileVersionRow no UserController).
     */
    private EntityMetadata buildMobileVersionReportMetadata() {
        EntityMetadata m = new EntityMetadata("mobileVersionReport", "Versões Mobile", "/api/users/mobile-versions");

        List<FieldMetadata> tableFields = new java.util.ArrayList<>();
        tableFields.add(reportField("name", "Nome", "string"));
        tableFields.add(reportField("username", "E-mail", "string"));
        tableFields.add(reportField("role", "Role", "string"));
        tableFields.add(reportField("mobileAppVersion", "Versão", "string"));
        tableFields.add(reportField("mobilePlatform", "Plataforma", "string"));
        tableFields.add(reportField("mobileVersionUpdatedAt", "Último login", "datetime"));
        m.setTableFields(tableFields);
        m.setFormFields(List.of()); // relatório não tem form

        List<FilterMetadata> filters = new java.util.ArrayList<>();
        FilterMetadata fPlatform = new FilterMetadata("mobilePlatform", "Plataforma", "select", "mobilePlatform");
        fPlatform.setOptions(List.of(
                new FilterMetadata.FilterOption("Android", "android"),
                new FilterMetadata.FilterOption("iOS", "ios")
        ));
        filters.add(fPlatform);

        FilterMetadata fVersion = new FilterMetadata("mobileAppVersion", "Versão", "text", "mobileAppVersion");
        fVersion.setPlaceholder("ex.: v1.0.9-99");
        filters.add(fVersion);

        FilterMetadata fRole = new FilterMetadata("role", "Role", "select", "role");
        List<FilterMetadata.FilterOption> roleOpts = new java.util.ArrayList<>();
        for (User.Role r : User.Role.values()) {
            roleOpts.add(new FilterMetadata.FilterOption(r.name(), r.name()));
        }
        fRole.setOptions(roleOpts);
        filters.add(fRole);

        FilterMetadata fDate = new FilterMetadata("mobileVersionUpdatedAt", "Último login", "daterange", "mobileVersionUpdatedAt");
        filters.add(fDate);

        m.setFilters(filters);
        m.setPagination(null);
        return m;
    }

    private static FieldMetadata reportField(String name, String label, String type) {
        FieldMetadata f = new FieldMetadata(name, label, type);
        f.setSortable(true);
        f.setVisible(true);
        f.setReadonly(true);
        return f;
    }

    public EntityMetadata getEntityMetadata(String entityName) {
        // Virtual entities (relatórios sem entidade JPA dedicada)
        if ("mobileVersionReport".equals(entityName)) {
            return buildMobileVersionReportMetadata();
        }

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
