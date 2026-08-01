package com.example.inventarymanagement.controller;

import com.example.inventarymanagement.config.CommonApiErrorResponses;
import com.example.inventarymanagement.dto.AlertResponse;
import com.example.inventarymanagement.service.AlertService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/alerts")
@Tag(name = "Alerts", description = "Inventory alert lifecycle")
@CommonApiErrorResponses
public class AlertController {

    private final AlertService alertService;

    public AlertController(AlertService alertService) {
        this.alertService = alertService;
    }

    @GetMapping
    @Operation(summary = "List alerts")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Alerts returned")
    })
    @PreAuthorize("hasAnyRole('STORE_MANAGER','INVENTORY_ANALYST')")
    public ResponseEntity<List<AlertResponse>> getAlerts() {
        return ResponseEntity.ok(alertService.getAlerts());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get alert by id")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Alert returned")
    })
    @PreAuthorize("hasAnyRole('STORE_MANAGER','INVENTORY_ANALYST')")
    public ResponseEntity<AlertResponse> getAlertById(@PathVariable("id") Long id) {
        return ResponseEntity.ok(alertService.getAlert(id));
    }

    @PatchMapping("/{id}/resolve")
    @Operation(summary = "Resolve an alert")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Alert resolved")
    })
    @PreAuthorize("hasRole('STORE_MANAGER')")
    public ResponseEntity<AlertResponse> resolveAlert(@PathVariable("id") Long id) {
        return ResponseEntity.ok(alertService.resolveAlert(id));
    }
}