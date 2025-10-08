package com.mvt.mvt_events.metadata;

public class FieldMetadata {
    private String name;
    private String label;
    private String type; // string, number, date, boolean, enum
    private boolean sortable;
    private boolean searchable;
    private boolean visible;
    private String format; // para datas, números, etc
    private Integer width; // largura da coluna
    private String align; // left, center, right

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
}
