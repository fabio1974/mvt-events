package com.mvt.mvt_events.metadata;

import java.util.List;

public class FilterMetadata {
    private String name;
    private String label;
    private String type; // text, select, date, dateRange, number, numberRange, entity
    private String field; // campo da entidade que este filtro afeta
    private List<FilterOption> options; // para filtros tipo select
    private String placeholder;
    private EntityFilterConfig entityConfig; // para filtros tipo entity

    public FilterMetadata() {
    }

    public FilterMetadata(String name, String label, String type, String field) {
        this.name = name;
        this.label = label;
        this.type = type;
        this.field = field;
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

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getField() {
        return field;
    }

    public void setField(String field) {
        this.field = field;
    }

    public List<FilterOption> getOptions() {
        return options;
    }

    public void setOptions(List<FilterOption> options) {
        this.options = options;
    }

    public String getPlaceholder() {
        return placeholder;
    }

    public void setPlaceholder(String placeholder) {
        this.placeholder = placeholder;
    }

    public EntityFilterConfig getEntityConfig() {
        return entityConfig;
    }

    public void setEntityConfig(EntityFilterConfig entityConfig) {
        this.entityConfig = entityConfig;
    }

    public static class FilterOption {
        private String label;
        private String value;

        public FilterOption() {
        }

        public FilterOption(String label, String value) {
            this.label = label;
            this.value = value;
        }

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }
}
