package com.example.inventarymanagement.service;

import com.example.inventarymanagement.dto.AlertResponse;
import com.example.inventarymanagement.entity.Product;
import com.example.inventarymanagement.entity.StockLevel;
import com.example.inventarymanagement.enums.AlertType;

import java.util.List;

public interface AlertService {

    List<AlertResponse> getAlerts();

    List<AlertResponse> getLowAndOutOfStockAlerts();

    AlertResponse getAlert(Long alertId);

    AlertResponse resolveAlert(Long alertId);

    void resolveActiveAlertsForProduct(Long productId);

    void evaluateAlerts(Product product, StockLevel stockLevel);

    void createAlertIfNotExists(Product product, AlertType alertType, String message);
}