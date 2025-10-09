package com.mvt.mvt_events.metadata;

import com.mvt.mvt_events.metadata.FilterMetadata.FilterOption;
import jakarta.persistence.*;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Extrator genérico de metadata a partir de entidades JPA.
 * Lê anotações @Entity, @Column, @Enumerated, @OneToMany, etc.
 * e gera FieldMetadata e RelationshipMetadata automaticamente.
 */
@Component
public class JpaMetadataExtractor {

    // Mapa de traduções de campos comuns (inglês → português)
    private static final Map<String, String> FIELD_TRANSLATIONS = new HashMap<>();

    static {
        // ==================== CAMPOS BÁSICOS ====================
        FIELD_TRANSLATIONS.put("name", "Nome");
        FIELD_TRANSLATIONS.put("description", "Descrição");
        FIELD_TRANSLATIONS.put("email", "E-mail");
        FIELD_TRANSLATIONS.put("phone", "Telefone");
        FIELD_TRANSLATIONS.put("address", "Endereço");
        FIELD_TRANSLATIONS.put("city", "Cidade");
        FIELD_TRANSLATIONS.put("state", "Estado");
        FIELD_TRANSLATIONS.put("country", "País");
        FIELD_TRANSLATIONS.put("zipCode", "CEP");
        FIELD_TRANSLATIONS.put("postalCode", "CEP");
        FIELD_TRANSLATIONS.put("slug", "URL Amigável");
        FIELD_TRANSLATIONS.put("website", "Website");
        FIELD_TRANSLATIONS.put("notes", "Observações");

        // ==================== DATAS ====================
        FIELD_TRANSLATIONS.put("date", "Data");
        FIELD_TRANSLATIONS.put("eventDate", "Data do Evento");
        FIELD_TRANSLATIONS.put("registrationDate", "Data de Inscrição");
        FIELD_TRANSLATIONS.put("startDate", "Data de Início");
        FIELD_TRANSLATIONS.put("endDate", "Data de Término");
        FIELD_TRANSLATIONS.put("registrationStartDate", "Início das Inscrições");
        FIELD_TRANSLATIONS.put("registrationEndDate", "Fim das Inscrições");
        FIELD_TRANSLATIONS.put("eventTime", "Horário");
        FIELD_TRANSLATIONS.put("startsAt", "Inicia em");
        FIELD_TRANSLATIONS.put("dateOfBirth", "Data de Nascimento");
        FIELD_TRANSLATIONS.put("processedAt", "Processado em");
        FIELD_TRANSLATIONS.put("refundedAt", "Reembolsado em");

        // ==================== EVENT ====================
        FIELD_TRANSLATIONS.put("eventType", "Tipo de Evento");
        FIELD_TRANSLATIONS.put("location", "Local");
        FIELD_TRANSLATIONS.put("maxParticipants", "Máximo de Participantes");
        FIELD_TRANSLATIONS.put("currentParticipants", "Participantes Atuais");
        FIELD_TRANSLATIONS.put("registrationOpen", "Inscrições Abertas");
        FIELD_TRANSLATIONS.put("categories", "Categorias");
        FIELD_TRANSLATIONS.put("currency", "Moeda");
        FIELD_TRANSLATIONS.put("termsAndConditions", "Termos e Condições");
        FIELD_TRANSLATIONS.put("bannerUrl", "URL do Banner");
        FIELD_TRANSLATIONS.put("platformFeePercentage", "Taxa da Plataforma (%)");
        FIELD_TRANSLATIONS.put("transferFrequency", "Frequência de Transferência");

        // ==================== EVENT CATEGORY ====================
        FIELD_TRANSLATIONS.put("gender", "Gênero");
        FIELD_TRANSLATIONS.put("minAge", "Idade Mínima");
        FIELD_TRANSLATIONS.put("maxAge", "Idade Máxima");
        FIELD_TRANSLATIONS.put("distance", "Distância");
        FIELD_TRANSLATIONS.put("distanceUnit", "Unidade de Distância");
        FIELD_TRANSLATIONS.put("observations", "Observações");
        FIELD_TRANSLATIONS.put("isActive", "Ativa");

        // ==================== FINANCEIRO/PAYMENT ====================
        FIELD_TRANSLATIONS.put("price", "Preço");
        FIELD_TRANSLATIONS.put("amount", "Valor");
        FIELD_TRANSLATIONS.put("paymentMethod", "Método de Pagamento");
        FIELD_TRANSLATIONS.put("gatewayProvider", "Gateway de Pagamento");
        FIELD_TRANSLATIONS.put("gatewayPaymentId", "ID do Pagamento");
        FIELD_TRANSLATIONS.put("gatewayFee", "Taxa do Gateway");
        FIELD_TRANSLATIONS.put("gatewayResponse", "Resposta do Gateway");
        FIELD_TRANSLATIONS.put("transactionId", "ID da Transação");
        FIELD_TRANSLATIONS.put("refundAmount", "Valor do Reembolso");
        FIELD_TRANSLATIONS.put("refundReason", "Motivo do Reembolso");

        // ==================== STATUS/FLAGS ====================
        FIELD_TRANSLATIONS.put("status", "Status");
        FIELD_TRANSLATIONS.put("enabled", "Ativo");
        FIELD_TRANSLATIONS.put("active", "Ativo");

        // ==================== USER ====================
        FIELD_TRANSLATIONS.put("username", "E-mail");
        FIELD_TRANSLATIONS.put("password", "Senha");
        FIELD_TRANSLATIONS.put("role", "Perfil");
        FIELD_TRANSLATIONS.put("cpf", "CPF");
        FIELD_TRANSLATIONS.put("emergencyContact", "Contato de Emergência");

        // ==================== ORGANIZATION ====================
        FIELD_TRANSLATIONS.put("organization", "Organização");
        FIELD_TRANSLATIONS.put("contactEmail", "E-mail de Contato");
        FIELD_TRANSLATIONS.put("contactPhone", "Telefone de Contato");
        FIELD_TRANSLATIONS.put("logoUrl", "URL do Logo");

        // ==================== RELACIONAMENTOS ====================
        FIELD_TRANSLATIONS.put("event", "Evento");
        FIELD_TRANSLATIONS.put("user", "Usuário");
        FIELD_TRANSLATIONS.put("registration", "Inscrição");
        FIELD_TRANSLATIONS.put("payments", "Pagamentos");
        FIELD_TRANSLATIONS.put("category", "Categoria");
    }

    // Mapa de traduções para valores de ENUMS
    private static final Map<String, String> ENUM_TRANSLATIONS = new HashMap<>();

    static {
        // ==================== EVENT TYPE ====================
        ENUM_TRANSLATIONS.put("RUNNING", "Corrida");
        ENUM_TRANSLATIONS.put("CYCLING", "Ciclismo");
        ENUM_TRANSLATIONS.put("TRIATHLON", "Triatlo");
        ENUM_TRANSLATIONS.put("SWIMMING", "Natação");
        ENUM_TRANSLATIONS.put("WALKING", "Caminhada");
        ENUM_TRANSLATIONS.put("TRAIL_RUNNING", "Trail Running");
        ENUM_TRANSLATIONS.put("MOUNTAIN_BIKING", "Mountain Bike");
        ENUM_TRANSLATIONS.put("ROAD_CYCLING", "Ciclismo de Estrada");
        ENUM_TRANSLATIONS.put("MARATHON", "Maratona");
        ENUM_TRANSLATIONS.put("HALF_MARATHON", "Meia Maratona");
        ENUM_TRANSLATIONS.put("ULTRA_MARATHON", "Ultra Maratona");
        ENUM_TRANSLATIONS.put("OBSTACLE_RACE", "Corrida de Obstáculos");
        ENUM_TRANSLATIONS.put("DUATHLON", "Duatlo");
        ENUM_TRANSLATIONS.put("HIKING", "Caminhada");
        ENUM_TRANSLATIONS.put("ADVENTURE_RACE", "Corrida de Aventura");

        // ==================== STATUS (Event, Payment, Registration)
        // ====================
        ENUM_TRANSLATIONS.put("DRAFT", "Rascunho");
        ENUM_TRANSLATIONS.put("PUBLISHED", "Publicado");
        ENUM_TRANSLATIONS.put("CANCELLED", "Cancelado");
        ENUM_TRANSLATIONS.put("COMPLETED", "Concluído");
        ENUM_TRANSLATIONS.put("PENDING", "Pendente");
        ENUM_TRANSLATIONS.put("ACTIVE", "Ativa");
        ENUM_TRANSLATIONS.put("PROCESSING", "Processando");
        ENUM_TRANSLATIONS.put("FAILED", "Falhou");
        ENUM_TRANSLATIONS.put("REFUNDED", "Reembolsado");

        // ==================== GENDER ====================
        ENUM_TRANSLATIONS.put("MALE", "Masculino");
        ENUM_TRANSLATIONS.put("FEMALE", "Feminino");
        ENUM_TRANSLATIONS.put("MIXED", "Misto");
        ENUM_TRANSLATIONS.put("OTHER", "Outro");

        // ==================== DISTANCE UNIT ====================
        ENUM_TRANSLATIONS.put("KM", "Quilômetros (km)");
        ENUM_TRANSLATIONS.put("MILES", "Milhas (mi)");
        ENUM_TRANSLATIONS.put("METERS", "Metros (m)");

        // ==================== TRANSFER FREQUENCY ====================
        ENUM_TRANSLATIONS.put("IMMEDIATE", "Imediato");
        ENUM_TRANSLATIONS.put("DAILY", "Diário");
        ENUM_TRANSLATIONS.put("WEEKLY", "Semanal");
        ENUM_TRANSLATIONS.put("MONTHLY", "Mensal");
        ENUM_TRANSLATIONS.put("ON_DEMAND", "Sob Demanda");

        // ==================== PAYMENT METHOD ====================
        ENUM_TRANSLATIONS.put("CREDIT_CARD", "Cartão de Crédito");
        ENUM_TRANSLATIONS.put("DEBIT_CARD", "Cartão de Débito");
        ENUM_TRANSLATIONS.put("PIX", "PIX");
        ENUM_TRANSLATIONS.put("BANK_TRANSFER", "Transferência Bancária");
        ENUM_TRANSLATIONS.put("PAYPAL_ACCOUNT", "Conta PayPal");
        ENUM_TRANSLATIONS.put("CASH", "Dinheiro");

        // ==================== ROLE ====================
        ENUM_TRANSLATIONS.put("USER", "Usuário");
        ENUM_TRANSLATIONS.put("ORGANIZER", "Organizador");
        ENUM_TRANSLATIONS.put("ADMIN", "Administrador");

        // ==================== GENERIC ====================
        ENUM_TRANSLATIONS.put("OTHER", "Outro");
    }

    /**
     * Extrai todos os campos de uma entidade JPA
     */
    public List<FieldMetadata> extractFields(Class<?> entityClass) {
        List<FieldMetadata> fields = new ArrayList<>();

        // Processa todos os campos da classe e superclasses
        getAllFields(entityClass).forEach(field -> {
            // Ignora campos anotados com @Transient ou @JsonIgnore
            if (field.isAnnotationPresent(Transient.class) ||
                    field.isAnnotationPresent(com.fasterxml.jackson.annotation.JsonIgnore.class)) {
                return;
            }

            FieldMetadata metadata = createFieldMetadata(field, entityClass);
            if (metadata != null) {
                fields.add(metadata);
            }
        });

        return fields;
    }

    /**
     * Cria FieldMetadata a partir de um Field da entidade
     */
    private FieldMetadata createFieldMetadata(Field field, Class<?> entityClass) {
        String fieldName = field.getName();

        // ✅ OCULTA campos de sistema
        if (isSystemField(fieldName)) {
            return null;
        }

        String label = extractLabel(field);
        String type = determineFieldType(field);

        // Ignora relacionamentos ManyToOne/OneToOne aqui (serão tratados separadamente)
        if (field.isAnnotationPresent(ManyToOne.class) || field.isAnnotationPresent(OneToOne.class)) {
            return null;
        }

        // Se for OneToMany, cria metadata de relacionamento
        if (field.isAnnotationPresent(OneToMany.class)) {
            return createRelationshipField(field);
        }

        FieldMetadata metadata = new FieldMetadata(fieldName, label, type);

        // Extrai validações da anotação @Column
        if (field.isAnnotationPresent(Column.class)) {
            Column column = field.getAnnotation(Column.class);
            metadata.setRequired(!column.nullable());

            if (column.length() > 0 && type.equals("string")) {
                metadata.setMaxLength(column.length());
            }

            if (field.getType().equals(BigDecimal.class) && column.precision() > 0) {
                // Define max baseado na precisão
                metadata.setMax(Math.pow(10, column.precision() - column.scale()) - 1);
            }
        }

        // Se for ENUM, extrai as opções
        if (field.isAnnotationPresent(Enumerated.class) && field.getType().isEnum()) {
            metadata.setType("select"); // Enum sempre é select
            metadata.setOptions(extractEnumOptions(field.getType()));
        }

        return metadata;
    }

    /**
     * Cria FieldMetadata para relacionamento OneToMany
     */
    private FieldMetadata createRelationshipField(Field field) {
        OneToMany oneToMany = field.getAnnotation(OneToMany.class);
        String fieldName = field.getName();
        String label = extractLabel(field);

        // Determina a entidade alvo do relacionamento
        Class<?> targetEntity = getTargetEntity(field, oneToMany);
        if (targetEntity == null) {
            return null;
        }

        String targetEntityName = toEntityName(targetEntity.getSimpleName());
        String targetEndpoint = "/api/" + toKebabCase(field.getName());

        RelationshipMetadata relationship = new RelationshipMetadata(
                "ONE_TO_MANY",
                targetEntityName,
                targetEndpoint);

        // Extrai cascade e orphanRemoval
        relationship.setCascade(oneToMany.cascade().length > 0);
        relationship.setOrphanRemoval(oneToMany.orphanRemoval());

        // Extrai campos da entidade alvo recursivamente
        relationship.setFields(extractFields(targetEntity));

        FieldMetadata metadata = new FieldMetadata(fieldName, label, "nested");
        metadata.setVisible(false); // Relacionamentos não aparecem na tabela
        metadata.setSortable(false);
        metadata.setSearchable(false);
        metadata.setRelationship(relationship);

        return metadata;
    }

    /**
     * Determina o tipo do campo baseado no tipo Java
     */
    private String determineFieldType(Field field) {
        Class<?> type = field.getType();

        if (type.equals(String.class)) {
            // Verifica se é TEXT (textarea)
            if (field.isAnnotationPresent(Column.class)) {
                Column col = field.getAnnotation(Column.class);
                if (col.columnDefinition().toUpperCase().contains("TEXT")) {
                    return "textarea";
                }
            }
            return "string";
        }

        if (type.equals(Integer.class) || type.equals(int.class) ||
                type.equals(Long.class) || type.equals(long.class)) {
            return "number";
        }

        if (type.equals(BigDecimal.class)) {
            // Verifica se é moeda (campo com "price" ou "valor" no nome)
            String fieldName = field.getName().toLowerCase();
            if (fieldName.contains("price") || fieldName.contains("valor")) {
                return "currency";
            }
            return "number";
        }

        if (type.equals(Boolean.class) || type.equals(boolean.class)) {
            return "boolean";
        }

        if (type.equals(LocalDate.class)) {
            return "date";
        }

        if (type.equals(LocalDateTime.class)) {
            return "datetime";
        }

        if (type.isEnum()) {
            return "select"; // Enums são sempre select
        }

        return "string"; // Fallback
    }

    /**
     * Extrai o label do campo (traduzido para português)
     */
    private String extractLabel(Field field) {
        String fieldName = field.getName();

        // ✅ Usa tradução se disponível
        if (FIELD_TRANSLATIONS.containsKey(fieldName)) {
            return FIELD_TRANSLATIONS.get(fieldName);
        }

        // Fallback: converte camelCase para "Título Capitalizado"
        return toTitleCase(fieldName);
    }

    /**
     * Extrai opções de um Enum com seus displayNames traduzidos
     */
    private List<FilterOption> extractEnumOptions(Class<?> enumClass) {
        List<FilterOption> options = new ArrayList<>();

        for (Object enumConstant : enumClass.getEnumConstants()) {
            String value = ((Enum<?>) enumConstant).name();
            String label;

            // 1️⃣ Primeiro verifica o mapa de traduções (prioridade máxima)
            if (ENUM_TRANSLATIONS.containsKey(value)) {
                label = ENUM_TRANSLATIONS.get(value);
            } else {
                // 2️⃣ Se não tiver tradução, tenta buscar método getDisplayName() no enum
                try {
                    Method getDisplayName = enumClass.getMethod("getDisplayName");
                    label = (String) getDisplayName.invoke(enumConstant);
                } catch (Exception e) {
                    // 3️⃣ Fallback: converte nome para title case
                    label = toTitleCase(value);
                }
            }

            // ✅ CORRIGIDO: FilterOption(label, value) - label PRIMEIRO!
            options.add(new FilterOption(label, value));
        }

        return options;
    }

    /**
     * Obtém a entidade alvo de um relacionamento
     */
    private Class<?> getTargetEntity(Field field, OneToMany annotation) {
        // Primeiro tenta pegar do targetEntity explícito
        if (!annotation.targetEntity().equals(void.class)) {
            return annotation.targetEntity();
        }

        // Tenta extrair do tipo genérico (List<EventCategory>)
        try {
            java.lang.reflect.ParameterizedType paramType = (java.lang.reflect.ParameterizedType) field
                    .getGenericType();
            return (Class<?>) paramType.getActualTypeArguments()[0];
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Obtém todos os campos incluindo de superclasses
     */
    private List<Field> getAllFields(Class<?> clazz) {
        List<Field> fields = new ArrayList<>();
        Class<?> current = clazz;

        while (current != null && current != Object.class) {
            fields.addAll(Arrays.asList(current.getDeclaredFields()));
            current = current.getSuperclass();
        }

        return fields;
    }

    /**
     * Converte "EventCategory" para "eventCategory"
     */
    private String toEntityName(String className) {
        if (className == null || className.isEmpty()) {
            return className;
        }
        return Character.toLowerCase(className.charAt(0)) + className.substring(1);
    }

    /**
     * Converte "categories" para "categories" (kebab-case)
     */
    private String toKebabCase(String camelCase) {
        return camelCase.replaceAll("([a-z])([A-Z])", "$1-$2").toLowerCase();
    }

    /**
     * Converte "eventType" para "Event Type" ou "USER" para "User"
     */
    private String toTitleCase(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }

        // Se está todo em MAIÚSCULAS (PENDING, USER, etc), converte para Title Case
        if (input.equals(input.toUpperCase())) {
            return input.charAt(0) + input.substring(1).toLowerCase();
        }

        // Se é camelCase (eventType), adiciona espaços antes de maiúsculas
        String withSpaces = input.replaceAll("([A-Z])", " $1");
        return withSpaces.substring(0, 1).toUpperCase() + withSpaces.substring(1);
    }

    /**
     * Verifica se é um campo de sistema que não deve aparecer em formulários
     */
    private boolean isSystemField(String fieldName) {
        return fieldName.equals("id")
                || fieldName.equals("createdAt")
                || fieldName.equals("updatedAt")
                || fieldName.equals("createdDate")
                || fieldName.equals("lastModifiedDate")
                || fieldName.equals("tenantId");
    }
}
