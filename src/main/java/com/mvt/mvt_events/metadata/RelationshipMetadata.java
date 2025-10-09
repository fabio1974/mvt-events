package com.mvt.mvt_events.metadata;

import java.util.List;

/**
 * Metadata para relacionamentos entre entidades.
 * Usado especialmente para relacionamentos ONE_TO_MANY que precisam
 * ser renderizados como arrays de formulários aninhados no frontend.
 */
public class RelationshipMetadata {

    /**
     * Tipo de relacionamento: ONE_TO_ONE, ONE_TO_MANY, MANY_TO_ONE, MANY_TO_MANY
     */
    private String type;

    /**
     * Nome da entidade relacionada (ex: "eventCategory")
     */
    private String targetEntity;

    /**
     * Endpoint para buscar/salvar dados da entidade relacionada
     */
    private String targetEndpoint;

    /**
     * Se true, salva/atualiza/deleta em cascata
     */
    private Boolean cascade;

    /**
     * Se true, remove órfãos ao remover da coleção
     */
    private Boolean orphanRemoval;

    /**
     * Campos da entidade relacionada (para formulários aninhados)
     * Estes campos definem o schema do formulário para cada item do array
     */
    private List<FieldMetadata> fields;

    public RelationshipMetadata() {
    }

    public RelationshipMetadata(String type, String targetEntity, String targetEndpoint) {
        this.type = type;
        this.targetEntity = targetEntity;
        this.targetEndpoint = targetEndpoint;
        this.cascade = false;
        this.orphanRemoval = false;
    }

    // Getters and Setters
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getTargetEntity() {
        return targetEntity;
    }

    public void setTargetEntity(String targetEntity) {
        this.targetEntity = targetEntity;
    }

    public String getTargetEndpoint() {
        return targetEndpoint;
    }

    public void setTargetEndpoint(String targetEndpoint) {
        this.targetEndpoint = targetEndpoint;
    }

    public Boolean getCascade() {
        return cascade;
    }

    public void setCascade(Boolean cascade) {
        this.cascade = cascade;
    }

    public Boolean getOrphanRemoval() {
        return orphanRemoval;
    }

    public void setOrphanRemoval(Boolean orphanRemoval) {
        this.orphanRemoval = orphanRemoval;
    }

    public List<FieldMetadata> getFields() {
        return fields;
    }

    public void setFields(List<FieldMetadata> fields) {
        this.fields = fields;
    }
}
