package com.example.inventarymanagement.service;

import com.example.inventarymanagement.dto.AlertResponse;
import com.example.inventarymanagement.dto.DashboardResponse;
import com.example.inventarymanagement.dto.ProductCreateRequest;
import com.example.inventarymanagement.dto.ProductDetailsResponse;
import com.example.inventarymanagement.dto.ProductResponse;
import com.example.inventarymanagement.dto.ProductUpdateRequest;
import com.example.inventarymanagement.dto.StockMovementRequest;
import com.example.inventarymanagement.dto.StockMovementResponse;
import com.example.inventarymanagement.enums.Category;

import java.util.List;

public interface ProductService {

    List<ProductResponse> getProducts(Category category, Boolean lowStock);

    ProductResponse createProduct(ProductCreateRequest request);

    ProductResponse updateProduct(Long productId, ProductUpdateRequest request);

    void deleteProduct(Long productId);

    ProductResponse updateStock(Long productId, StockMovementRequest request);

    ProductDetailsResponse getProductWithMovements(Long productId);

    List<AlertResponse> getLowStockAlerts();

    List<StockMovementResponse> getProductMovements(Long productId);

    DashboardResponse getDashboard();
}
