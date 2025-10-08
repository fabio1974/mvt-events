package com.mvt.mvt_events.metadata;

import java.util.List;

public class EntityMetadata {
    private String name;
    private String label;
    private String endpoint;
    private List<FieldMetadata> fields;
    private List<FilterMetadata> filters;
    private PaginationConfig pagination;

    public EntityMetadata() {
    }

    public EntityMetadata(String name, String label, String endpoint) {
        this.name = name;
        this.label = label;
        this.endpoint = endpoint;
    }

    // Getters and Setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public List<FieldMetadata> getFields() {
        return fields;
    }

    public void setFields(List<FieldMetadata> fields) {
        this.fields = fields;
    }

    public List<FilterMetadata> getFilters() {
        return filters;
    }

    public void setFilters(List<FilterMetadata> filters) {
        this.filters = filters;
    }

    public PaginationConfig getPagination() {
        return pagination;
    }

    public void setPagination(PaginationConfig pagination) {
        this.pagination = pagination;
    }
}
