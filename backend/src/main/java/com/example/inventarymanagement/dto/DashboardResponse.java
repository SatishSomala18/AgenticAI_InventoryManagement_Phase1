package com.example.inventarymanagement.dto;

import java.math.BigDecimal;

public class DashboardResponse {

    private long totalProducts;
    private long lowStockCount;
    private long outOfStockCount;
    private long openPoCount;
    private BigDecimal totalStockValue;

    public long getTotalProducts() {
        return totalProducts;
    }

    public void setTotalProducts(long totalProducts) {
        this.totalProducts = totalProducts;
    }

    public long getLowStockCount() {
        return lowStockCount;
    }

    public void setLowStockCount(long lowStockCount) {
        this.lowStockCount = lowStockCount;
    }

    public long getOutOfStockCount() {
        return outOfStockCount;
    }

    public void setOutOfStockCount(long outOfStockCount) {
        this.outOfStockCount = outOfStockCount;
    }

    public long getOpenPoCount() {
        return openPoCount;
    }

    public void setOpenPoCount(long openPoCount) {
        this.openPoCount = openPoCount;
    }

    public BigDecimal getTotalStockValue() {
        return totalStockValue;
    }

    public void setTotalStockValue(BigDecimal totalStockValue) {
        this.totalStockValue = totalStockValue;
    }
}
