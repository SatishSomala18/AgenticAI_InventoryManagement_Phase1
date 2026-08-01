package com.example.inventarymanagement.controller;

import com.example.inventarymanagement.config.CommonApiErrorResponses;
import com.example.inventarymanagement.dto.ProductCreateRequest;
import com.example.inventarymanagement.dto.ProductDetailsResponse;
import com.example.inventarymanagement.dto.ProductResponse;
import com.example.inventarymanagement.dto.ProductUpdateRequest;
import com.example.inventarymanagement.dto.StockMovementRequest;
import com.example.inventarymanagement.dto.StockMovementResponse;
import com.example.inventarymanagement.enums.Category;
import com.example.inventarymanagement.service.ProductService;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/products")
@Tag(name = "Products", description = "Product inventory operations")
@CommonApiErrorResponses
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    @Operation(summary = "List products", description = "Optional filters: category and low_stock=true")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Products returned")
    })
    @PreAuthorize("hasAnyRole('STORE_MANAGER','INVENTORY_ANALYST','PROCUREMENT_OFFICER','WAREHOUSE_STAFF')")
    public ResponseEntity<List<ProductResponse>> getProducts(
            @RequestParam(value = "category", required = false) Category category,
            @RequestParam(value = "low_stock", required = false) Boolean lowStock,
            @RequestParam(value = "lowStock", required = false) Boolean lowStockCompat) {
        Boolean effectiveLowStock = lowStock != null ? lowStock : lowStockCompat;
        return ResponseEntity.ok(productService.getProducts(category, effectiveLowStock));
    }

    @PostMapping
    @Operation(summary = "Create product", description = "SKU is auto-generated in format SKU-{CATEGORY_PREFIX}-{NNNN}.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Product created")
    })
    @PreAuthorize("hasAnyRole('STORE_MANAGER','INVENTORY_ANALYST')")
    public ResponseEntity<ProductResponse> createProduct(@Valid @RequestBody ProductCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(productService.createProduct(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update product")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Product updated")
    })
    @PreAuthorize("hasAnyRole('STORE_MANAGER','INVENTORY_ANALYST')")
    public ResponseEntity<ProductResponse> updateProduct(@PathVariable("id") Long id,
            @Valid @RequestBody ProductUpdateRequest request) {
        return ResponseEntity.ok(productService.updateProduct(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete product")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Product deleted")
    })
    @PreAuthorize("hasAnyRole('STORE_MANAGER','INVENTORY_ANALYST')")
    public ResponseEntity<Void> deleteProduct(@PathVariable("id") Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/stock")
    @Operation(summary = "Record stock movement and update stock level", description = "Creates StockMovement and updates quantity_on_hand, quantity_reserved, quantity_available, and last_updated.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Stock updated and movement recorded")
    })
    @PreAuthorize("hasAnyRole('STORE_MANAGER','WAREHOUSE_STAFF')")
    public ResponseEntity<ProductResponse> updateStock(@PathVariable("id") Long id,
            @Valid @RequestBody StockMovementRequest request) {
        return ResponseEntity.ok(productService.updateStock(id, request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get product details with recent stock movements")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Product details returned")
    })
    @PreAuthorize("hasAnyRole('STORE_MANAGER','INVENTORY_ANALYST','PROCUREMENT_OFFICER','WAREHOUSE_STAFF')")
    public ResponseEntity<ProductDetailsResponse> getProduct(@PathVariable("id") Long id) {
        return ResponseEntity.ok(productService.getProductWithMovements(id));
    }

    @GetMapping("/{id}/movements")
    @Operation(summary = "Get stock movement history for a product")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Product movement history returned")
    })
    @PreAuthorize("hasAnyRole('STORE_MANAGER','INVENTORY_ANALYST','WAREHOUSE_STAFF')")
    public ResponseEntity<List<StockMovementResponse>> getProductMovements(@PathVariable("id") Long id) {
        return ResponseEntity.ok(productService.getProductMovements(id));
    }
}
