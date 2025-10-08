package com.mvt.mvt_events.metadata;

public class PaginationConfig {
    private int defaultPageSize;
    private int[] pageSizeOptions;
    private boolean showSizeSelector;

    public PaginationConfig() {
        this.defaultPageSize = 10;
        this.pageSizeOptions = new int[] { 5, 10, 20, 50, 100 };
        this.showSizeSelector = true;
    }

    public PaginationConfig(int defaultPageSize, int[] pageSizeOptions) {
        this.defaultPageSize = defaultPageSize;
        this.pageSizeOptions = pageSizeOptions;
        this.showSizeSelector = true;
    }

    // Getters and Setters
    public int getDefaultPageSize() {
        return defaultPageSize;
    }

    public void setDefaultPageSize(int defaultPageSize) {
        this.defaultPageSize = defaultPageSize;
    }

    public int[] getPageSizeOptions() {
        return pageSizeOptions;
    }

    public void setPageSizeOptions(int[] pageSizeOptions) {
        this.pageSizeOptions = pageSizeOptions;
    }

    public boolean isShowSizeSelector() {
        return showSizeSelector;
    }

    public void setShowSizeSelector(boolean showSizeSelector) {
        this.showSizeSelector = showSizeSelector;
    }
}
