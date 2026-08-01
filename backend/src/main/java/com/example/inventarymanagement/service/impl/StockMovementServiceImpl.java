package com.example.inventarymanagement.service.impl;

import com.example.inventarymanagement.dto.StockMovementResponse;
import com.example.inventarymanagement.entity.StockMovement;
import com.example.inventarymanagement.exception.ResourceNotFoundException;
import com.example.inventarymanagement.repository.StockMovementRepository;
import com.example.inventarymanagement.service.StockMovementService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class StockMovementServiceImpl implements StockMovementService {

    private final StockMovementRepository stockMovementRepository;

    public StockMovementServiceImpl(StockMovementRepository stockMovementRepository) {
        this.stockMovementRepository = stockMovementRepository;
    }

    @Override
    public List<StockMovementResponse> getStockMovements() {
        return stockMovementRepository.findAllByOrderByRecordedAtDesc().stream().map(this::toResponse).toList();
    }

    @Override
    public StockMovementResponse getStockMovementById(Long movementId) {
        StockMovement movement = stockMovementRepository.findById(movementId)
                .orElseThrow(() -> new ResourceNotFoundException("Stock movement not found for id=" + movementId));
        return toResponse(movement);
    }

    @Override
    public List<StockMovementResponse> getStockMovementsByProductId(Long productId) {
        return stockMovementRepository.findByProductIdOrderByRecordedAtDesc(productId)
                .stream().map(this::toResponse).toList();
    }

    private StockMovementResponse toResponse(StockMovement movement) {
        StockMovementResponse response = new StockMovementResponse();
        response.setId(movement.getId());
        response.setProductId(movement.getProduct().getId());
        response.setProductSku(movement.getProduct().getSku());
        response.setProductName(movement.getProduct().getName());
        response.setMovementType(movement.getMovementType());
        response.setQuantity(movement.getQuantity());
        response.setReferenceNumber(movement.getReferenceNumber());
        response.setNotes(movement.getNotes());
        response.setRecordedAt(movement.getRecordedAt());
        response.setRecordedBy(movement.getRecordedBy());
        return response;
    }
}