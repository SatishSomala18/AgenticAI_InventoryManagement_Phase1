package com.example.inventarymanagement.controller;

import com.example.inventarymanagement.config.CommonApiErrorResponses;
import com.example.inventarymanagement.dto.AlertResponse;
import com.example.inventarymanagement.dto.DashboardResponse;
import com.example.inventarymanagement.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Inventory Insights", description = "Low-stock monitoring and dashboard endpoints")
@CommonApiErrorResponses
public class InventoryController {

    private final ProductService productService;

    public InventoryController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping("/stock/low-alerts")
    @PreAuthorize("hasRole('STORE_MANAGER')")
    @Operation(summary = "Get low-stock and out-of-stock alerts (store manager only)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Low-stock and out-of-stock alerts returned")
    })
    public ResponseEntity<List<AlertResponse>> lowStockAlerts() {
        return ResponseEntity.ok(productService.getLowStockAlerts());
    }

    @GetMapping("/dashboard")
    @PreAuthorize("hasRole('STORE_MANAGER')")
    @Operation(summary = "Get inventory dashboard metrics (store manager only)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Dashboard metrics returned")
    })
    public ResponseEntity<DashboardResponse> dashboard() {
        return ResponseEntity.ok(productService.getDashboard());
    }
}
