package com.example.inventarymanagement.controller;

import com.example.inventarymanagement.config.CommonApiErrorResponses;
import com.example.inventarymanagement.dto.StockMovementResponse;
import com.example.inventarymanagement.service.StockMovementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/stock-movements")
@Tag(name = "Stock Movements", description = "Inventory movement audit logs")
@CommonApiErrorResponses
public class StockMovementController {

    private final StockMovementService stockMovementService;

    public StockMovementController(StockMovementService stockMovementService) {
        this.stockMovementService = stockMovementService;
    }

    @GetMapping
    @Operation(summary = "List stock movements")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Stock movements returned")
    })
    @PreAuthorize("hasAnyRole('STORE_MANAGER','INVENTORY_ANALYST','WAREHOUSE_STAFF')")
    public ResponseEntity<List<StockMovementResponse>> getStockMovements() {
        return ResponseEntity.ok(stockMovementService.getStockMovements());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get stock movement by id")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Stock movement returned")
    })
    @PreAuthorize("hasAnyRole('STORE_MANAGER','INVENTORY_ANALYST','WAREHOUSE_STAFF')")
    public ResponseEntity<StockMovementResponse> getStockMovement(@PathVariable("id") Long id) {
        return ResponseEntity.ok(stockMovementService.getStockMovementById(id));
    }
}