package com.example.inventarymanagement.controller;

import com.example.inventarymanagement.config.CommonApiErrorResponses;
import com.example.inventarymanagement.dto.SupplierCatalogItemResponse;
import com.example.inventarymanagement.dto.SupplierCreateRequest;
import com.example.inventarymanagement.dto.SupplierResponse;
import com.example.inventarymanagement.dto.SupplierUpdateRequest;
import com.example.inventarymanagement.service.SupplierService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/suppliers")
@Tag(name = "Suppliers", description = "Supplier management and supplier catalogs")
@CommonApiErrorResponses
public class SupplierController {

    private final SupplierService supplierService;

    public SupplierController(SupplierService supplierService) {
        this.supplierService = supplierService;
    }

    @PostMapping
    @Operation(summary = "Create supplier")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Supplier created")
    })
    @PreAuthorize("hasAnyRole('STORE_MANAGER','PROCUREMENT_OFFICER')")
    public ResponseEntity<SupplierResponse> createSupplier(@Valid @RequestBody SupplierCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(supplierService.createSupplier(request));
    }

    @GetMapping
    @Operation(summary = "List suppliers")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Suppliers returned")
    })
    @PreAuthorize("hasAnyRole('STORE_MANAGER','PROCUREMENT_OFFICER','INVENTORY_ANALYST')")
    public ResponseEntity<List<SupplierResponse>> listSuppliers() {
        return ResponseEntity.ok(supplierService.getSuppliers());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get supplier by id")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Supplier returned")
    })
    @PreAuthorize("hasAnyRole('STORE_MANAGER','PROCUREMENT_OFFICER','INVENTORY_ANALYST')")
    public ResponseEntity<SupplierResponse> getSupplier(@PathVariable("id") Long supplierId) {
        return ResponseEntity.ok(supplierService.getSupplier(supplierId));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update supplier")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Supplier updated")
    })
    @PreAuthorize("hasAnyRole('STORE_MANAGER','PROCUREMENT_OFFICER')")
    public ResponseEntity<SupplierResponse> updateSupplier(@PathVariable("id") Long supplierId,
            @Valid @RequestBody SupplierUpdateRequest request) {
        return ResponseEntity.ok(supplierService.updateSupplier(supplierId, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete supplier")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Supplier deleted")
    })
    @PreAuthorize("hasAnyRole('STORE_MANAGER','PROCUREMENT_OFFICER')")
    public ResponseEntity<Void> deleteSupplier(@PathVariable("id") Long supplierId) {
        supplierService.deleteSupplier(supplierId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/catalog")
    @Operation(summary = "Get supplier catalog with unit cost")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Supplier catalog returned")
    })
    @PreAuthorize("hasAnyRole('STORE_MANAGER','PROCUREMENT_OFFICER','INVENTORY_ANALYST')")
    public ResponseEntity<List<SupplierCatalogItemResponse>> getCatalog(@PathVariable("id") Long supplierId) {
        return ResponseEntity.ok(supplierService.getSupplierCatalog(supplierId));
    }
}
