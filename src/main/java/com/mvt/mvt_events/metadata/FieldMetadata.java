package com.mvt.mvt_events.metadata;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.mvt.mvt_events.metadata.FilterMetadata.FilterOption;
import java.util.List;

public class FieldMetadata {
    private String name;
    private String label;
    private String type; // string, number, date, boolean, enum, nested
    private boolean sortable;
    private boolean searchable;
    private boolean visible;
    private String format; // para datas, números, etc
    private Integer width; // largura da coluna
    private String align; // left, center, right

    // Form-specific properties
    private Boolean required;
    private boolean readonly = false; // Campo é visível mas não editável (primitivo para sempre serializar)
    private String placeholder;
    private Integer minLength;
    private Integer maxLength;
    private Double min;
    private Double max;
    private String pattern;

    // Enum/Select options (para type="enum" ou type="select")
    private List<FilterOption> options;

    // Relationship metadata (para type="nested")
    private RelationshipMetadata relationship;

    // Flags para controlar onde o campo deve ser oculto (@HideFromMetadata)
    private Boolean hiddenFromTable;
    private Boolean hiddenFromForm;
    private Boolean hiddenFromFilter;

    // Default value
    private Object defaultValue;

    // Computed field properties (@Computed)
    private String computed; // Nome da função de cálculo (ex: "categoryName")
    private List<String> computedDependencies; // Campos que disparam recálculo

    public FieldMetadata() {
    }

    public FieldMetadata(String name, String label, String type) {
        this.name = name;
        this.label = label;
        this.type = type;
        this.sortable = true;
        this.searchable = true;
        this.visible = true;
        this.align = "left";
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

    public boolean isSortable() {
        return sortable;
    }

    public void setSortable(boolean sortable) {
        this.sortable = sortable;
    }

    public boolean isSearchable() {
        return searchable;
    }

    public void setSearchable(boolean searchable) {
        this.searchable = searchable;
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public Integer getWidth() {
        return width;
    }

    public void setWidth(Integer width) {
        this.width = width;
    }

    public String getAlign() {
        return align;
    }

    public void setAlign(String align) {
        this.align = align;
    }

    public Boolean getRequired() {
        return required;
    }

    public void setRequired(Boolean required) {
        this.required = required;
    }

    public boolean isReadonly() {
        return readonly;
    }

    public void setReadonly(boolean readonly) {
        this.readonly = readonly;
    }

    public String getPlaceholder() {
        return placeholder;
    }

    public void setPlaceholder(String placeholder) {
        this.placeholder = placeholder;
    }

    public Integer getMinLength() {
        return minLength;
    }

    public void setMinLength(Integer minLength) {
        this.minLength = minLength;
    }

    public Integer getMaxLength() {
        return maxLength;
    }

    public void setMaxLength(Integer maxLength) {
        this.maxLength = maxLength;
    }

    public Double getMin() {
        return min;
    }

    public void setMin(Double min) {
        this.min = min;
    }

    public Double getMax() {
        return max;
    }

    public void setMax(Double max) {
        this.max = max;
    }

    public String getPattern() {
        return pattern;
    }

    public void setPattern(String pattern) {
        this.pattern = pattern;
    }

    public List<FilterOption> getOptions() {
        return options;
    }

    public void setOptions(List<FilterOption> options) {
        this.options = options;
    }

    public RelationshipMetadata getRelationship() {
        return relationship;
    }

    public void setRelationship(RelationshipMetadata relationship) {
        this.relationship = relationship;
    }

    @JsonIgnore
    public Boolean getHiddenFromTable() {
        return hiddenFromTable;
    }

    public void setHiddenFromTable(Boolean hiddenFromTable) {
        this.hiddenFromTable = hiddenFromTable;
    }

    @JsonIgnore
    public Boolean getHiddenFromForm() {
        return hiddenFromForm;
    }

    public void setHiddenFromForm(Boolean hiddenFromForm) {
        this.hiddenFromForm = hiddenFromForm;
    }

    @JsonIgnore
    public Boolean getHiddenFromFilter() {
        return hiddenFromFilter;
    }

    public void setHiddenFromFilter(Boolean hiddenFromFilter) {
        this.hiddenFromFilter = hiddenFromFilter;
    }

    public Object getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(Object defaultValue) {
        this.defaultValue = defaultValue;
    }

    public String getComputed() {
        return computed;
    }

    public void setComputed(String computed) {
        this.computed = computed;
    }

    public List<String> getComputedDependencies() {
        return computedDependencies;
    }

    public void setComputedDependencies(List<String> computedDependencies) {
        this.computedDependencies = computedDependencies;
    }
}
