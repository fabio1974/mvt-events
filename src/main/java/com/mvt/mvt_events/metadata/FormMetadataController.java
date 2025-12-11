package com.mvt.mvt_events.metadata;

import com.mvt.mvt_events.jpa.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * Controller para metadata de FORMULÁRIOS.
 * Retorna estrutura completa com validações e relacionamentos nested.
 */
@RestController
@RequestMapping("/api/metadata/forms")
public class FormMetadataController {

    @Autowired
    private JpaMetadataExtractor jpaExtractor;

    // Mapa de configuração das entidades
    private static final Map<String, EntityConfig> ENTITIES = new HashMap<>();

    static {
        // ==================== Sistema Base ====================
        ENTITIES.put("organization", new EntityConfig(Organization.class, "Organizações", "/api/organizations"));
        ENTITIES.put("user", new EntityConfig(User.class, "Usuários", "/api/users"));
        ENTITIES.put("siteConfiguration", new EntityConfig(SiteConfiguration.class, "Configurações do Sistema", "/api/site-configuration"));
        ENTITIES.put("specialZone", new EntityConfig(SpecialZone.class, "Zonas Especiais", "/api/special-zones"));
        ENTITIES.put("bankAccount", new EntityConfig(BankAccount.class, "Contas Bancárias", "/api/bank-accounts"));

        // ==================== Pagamentos ====================
        ENTITIES.put("payment", new EntityConfig(Payment.class, "Pagamentos", "/api/payments"));

        // ==================== Zapi10 (Sistema de Entregas) ====================
        ENTITIES.put("delivery", new EntityConfig(Delivery.class, "Entregas", "/api/deliveries"));
        ENTITIES.put("courierProfile", new EntityConfig(CourierProfile.class, "Perfis de Motoboy", "/api/courier-profiles"));
        ENTITIES.put("evaluation", new EntityConfig(Evaluation.class, "Avaliações", "/api/evaluations"));
        ENTITIES.put("clientContract", new EntityConfig(ClientContract.class, "Contrato de Serviço", "/api/client-contracts"));
        ENTITIES.put("employmentContract", new EntityConfig(EmploymentContract.class, "Contrato de Motoboy", "/api/employment-contracts"));
    }

    /**
     * GET /api/metadata/forms
     * Retorna metadata de formulário para TODAS as entidades
     */
    @GetMapping
    public Map<String, EntityMetadata> getAllFormMetadata() {
        Map<String, EntityMetadata> metadata = new HashMap<>();
        ENTITIES.forEach((name, config) -> {
            metadata.put(name, getFormMetadata(name));
        });
        return metadata;
    }

    /**
     * GET /api/metadata/forms/{entityName}
     * Retorna metadata de formulário para uma entidade específica
     */
    @GetMapping("/{entityName}")
    public EntityMetadata getFormMetadata(@PathVariable String entityName) {
        EntityConfig config = ENTITIES.get(entityName);
        if (config == null) {
            throw new IllegalArgumentException("Entity not found: " + entityName);
        }

        EntityMetadata metadata = new EntityMetadata(entityName, config.label, config.endpoint);

        // ✅ EXTRAI CAMPOS AUTOMATICAMENTE VIA JPA
        // Inclui: validações, enums com options, relacionamentos nested
        List<FieldMetadata> fields = jpaExtractor.extractFields(config.entityClass);

        // Customiza placeholders e outras propriedades de UI
        customizeFormFields(entityName, fields);

        metadata.setFields(fields);

        return metadata;
    }

    /**
     * Customiza campos de formulário (placeholders, etc)
     */
    private void customizeFormFields(String entityName, List<FieldMetadata> fields) {
        fields.forEach(field -> {
            // Adiciona placeholders genéricos se não tiver
            if (field.getType().equals("string") && field.getPlaceholder() == null) {
                field.setPlaceholder("Digite " + field.getLabel().toLowerCase());
            }

            // Customizações específicas por entidade
            if (entityName.equals("event")) {
                switch (field.getName()) {
                    case "name":
                        field.setPlaceholder("Digite o nome do evento");
                        break;
                    case "location":
                        field.setPlaceholder("Digite o local do evento");
                        break;
                    case "description":
                        field.setPlaceholder("Descreva o evento");
                        break;
                    case "eventType":
                        field.setPlaceholder("Selecione o esporte");
                        break;
                    case "status":
                        field.setPlaceholder("Selecione o status");
                        break;
                }
            }
        });
    }

    /**
     * Classe interna para configuração de entidades
     */
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
