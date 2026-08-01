package com.example.inventarymanagement.dto;

import java.util.List;

public class ProductDetailsResponse {

    private ProductResponse product;
    private List<StockMovementResponse> recentMovements;

    public ProductResponse getProduct() {
        return product;
    }

    public void setProduct(ProductResponse product) {
        this.product = product;
    }

    public List<StockMovementResponse> getRecentMovements() {
        return recentMovements;
    }

    public void setRecentMovements(List<StockMovementResponse> recentMovements) {
        this.recentMovements = recentMovements;
    }
}
