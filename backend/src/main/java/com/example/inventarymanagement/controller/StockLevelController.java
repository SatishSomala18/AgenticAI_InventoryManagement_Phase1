package com.example.inventarymanagement.controller;

import com.example.inventarymanagement.config.CommonApiErrorResponses;
import com.example.inventarymanagement.dto.StockLevelResponse;
import com.example.inventarymanagement.service.StockLevelService;
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
@RequestMapping("/api/v1/stock-levels")
@Tag(name = "Stock Levels", description = "Warehouse stock balance and availability")
@CommonApiErrorResponses
public class StockLevelController {

    private final StockLevelService stockLevelService;

    public StockLevelController(StockLevelService stockLevelService) {
        this.stockLevelService = stockLevelService;
    }

    @GetMapping
    @Operation(summary = "List stock levels")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Stock levels returned")
    })
    @PreAuthorize("hasAnyRole('STORE_MANAGER','INVENTORY_ANALYST','WAREHOUSE_STAFF')")
    public ResponseEntity<List<StockLevelResponse>> getStockLevels() {
        return ResponseEntity.ok(stockLevelService.getStockLevels());
    }

    @GetMapping("/{productId}")
    @Operation(summary = "Get stock level by product id")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Stock level returned")
    })
    @PreAuthorize("hasAnyRole('STORE_MANAGER','INVENTORY_ANALYST','WAREHOUSE_STAFF')")
    public ResponseEntity<StockLevelResponse> getStockLevel(@PathVariable("productId") Long productId) {
        return ResponseEntity.ok(stockLevelService.getStockLevelByProductId(productId));
    }
}