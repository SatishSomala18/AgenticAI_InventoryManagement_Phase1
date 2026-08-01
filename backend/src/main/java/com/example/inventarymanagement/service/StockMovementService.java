package com.example.inventarymanagement.service;

import com.example.inventarymanagement.dto.StockMovementResponse;

import java.util.List;

public interface StockMovementService {

    List<StockMovementResponse> getStockMovements();

    StockMovementResponse getStockMovementById(Long movementId);

    List<StockMovementResponse> getStockMovementsByProductId(Long productId);
}