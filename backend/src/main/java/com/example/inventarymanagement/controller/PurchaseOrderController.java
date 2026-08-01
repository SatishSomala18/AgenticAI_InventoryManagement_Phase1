package com.example.inventarymanagement.controller;

import com.example.inventarymanagement.config.CommonApiErrorResponses;
import com.example.inventarymanagement.dto.PurchaseOrderCreateRequest;
import com.example.inventarymanagement.dto.PurchaseOrderResponse;
import com.example.inventarymanagement.enums.POStatus;
import com.example.inventarymanagement.service.PurchaseOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/orders")
@Tag(name = "Purchase Orders", description = "PO lifecycle operations")
@CommonApiErrorResponses
public class PurchaseOrderController {

    private final PurchaseOrderService purchaseOrderService;

    public PurchaseOrderController(PurchaseOrderService purchaseOrderService) {
        this.purchaseOrderService = purchaseOrderService;
    }

    @GetMapping
    @Operation(summary = "List purchase orders", description = "Optional filters: status and supplierId")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Purchase orders returned")
    })
    @PreAuthorize("hasAnyRole('STORE_MANAGER','PROCUREMENT_OFFICER','INVENTORY_ANALYST','WAREHOUSE_STAFF')")
    public ResponseEntity<List<PurchaseOrderResponse>> getOrders(
            @RequestParam(value = "status", required = false) POStatus status,
            @RequestParam(value = "supplier", required = false) Long supplier,
            @RequestParam(value = "supplierId", required = false) Long supplierId) {
        Long effectiveSupplierId = supplier != null ? supplier : supplierId;
        return ResponseEntity.ok(purchaseOrderService.getPurchaseOrders(status, effectiveSupplierId));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get purchase order by id")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Purchase order returned")
    })
    @PreAuthorize("hasAnyRole('STORE_MANAGER','PROCUREMENT_OFFICER','INVENTORY_ANALYST','WAREHOUSE_STAFF')")
    public ResponseEntity<PurchaseOrderResponse> getOrderById(@PathVariable("id") Long id) {
        return ResponseEntity.ok(purchaseOrderService.getPurchaseOrderById(id));
    }

    @PostMapping
    @Operation(summary = "Create purchase order", description = "PO number is auto-generated in format PO-{YEAR}-{NNNN}.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Purchase order created")
    })
    @PreAuthorize("hasAnyRole('STORE_MANAGER','PROCUREMENT_OFFICER')")
    public ResponseEntity<PurchaseOrderResponse> createOrder(@Valid @RequestBody PurchaseOrderCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(purchaseOrderService.createPurchaseOrder(request));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Update purchase order status", description = "Allowed transitions: DRAFT->SUBMITTED, SUBMITTED->ACKNOWLEDGED, ACKNOWLEDGED->RECEIVED, and cancellation from DRAFT/SUBMITTED/ACKNOWLEDGED.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Purchase order status updated")
    })
    @PreAuthorize("hasRole('STORE_MANAGER')")
    public ResponseEntity<PurchaseOrderResponse> updateStatus(@PathVariable("id") Long id,
            @RequestParam("status") POStatus status) {
        return ResponseEntity.ok(purchaseOrderService.updateStatus(id, status));
    }

    @PatchMapping("/{id}/receive")
    @Operation(summary = "Receive purchase order and update stock", description = "Marks PO received, records RECEIPT stock movement for each PO item, updates stock levels, and re-evaluates stock alerts.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Purchase order received and stock updated")
    })
    @PreAuthorize("hasAnyRole('STORE_MANAGER','WAREHOUSE_STAFF')")
    public ResponseEntity<PurchaseOrderResponse> receiveOrder(@PathVariable("id") Long id) {
        return ResponseEntity.ok(purchaseOrderService.receivePurchaseOrder(id));
    }
}
