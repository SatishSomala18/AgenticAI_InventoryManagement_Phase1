package com.example.inventarymanagement.service;

import com.example.inventarymanagement.dto.StockLevelResponse;

import java.util.List;

public interface StockLevelService {

    List<StockLevelResponse> getStockLevels();

    StockLevelResponse getStockLevelByProductId(Long productId);
}