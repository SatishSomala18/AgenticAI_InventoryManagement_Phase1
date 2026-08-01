package com.example.inventarymanagement.service.impl;

import com.example.inventarymanagement.dto.StockLevelResponse;
import com.example.inventarymanagement.entity.StockLevel;
import com.example.inventarymanagement.exception.ResourceNotFoundException;
import com.example.inventarymanagement.repository.StockLevelRepository;
import com.example.inventarymanagement.service.StockLevelService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class StockLevelServiceImpl implements StockLevelService {

    private final StockLevelRepository stockLevelRepository;

    public StockLevelServiceImpl(StockLevelRepository stockLevelRepository) {
        this.stockLevelRepository = stockLevelRepository;
    }

    @Override
    public List<StockLevelResponse> getStockLevels() {
        return stockLevelRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Override
    public StockLevelResponse getStockLevelByProductId(Long productId) {
        StockLevel stockLevel = stockLevelRepository.findByProductId(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Stock level not found for product id=" + productId));
        return toResponse(stockLevel);
    }

    private StockLevelResponse toResponse(StockLevel stockLevel) {
        StockLevelResponse response = new StockLevelResponse();
        response.setId(stockLevel.getId());
        response.setProductId(stockLevel.getProduct().getId());
        response.setProductSku(stockLevel.getProduct().getSku());
        response.setProductName(stockLevel.getProduct().getName());
        response.setWarehouse(stockLevel.getWarehouse());
        response.setQuantityOnHand(stockLevel.getQuantityOnHand());
        response.setQuantityReserved(stockLevel.getQuantityReserved());
        response.setQuantityAvailable(stockLevel.getQuantityAvailable());
        response.setLastUpdated(stockLevel.getLastUpdated());
        return response;
    }
}