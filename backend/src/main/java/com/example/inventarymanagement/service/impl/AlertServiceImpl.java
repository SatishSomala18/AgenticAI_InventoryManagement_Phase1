package com.example.inventarymanagement.service.impl;

import com.example.inventarymanagement.dto.AlertResponse;
import com.example.inventarymanagement.entity.Alert;
import com.example.inventarymanagement.entity.Product;
import com.example.inventarymanagement.entity.StockLevel;
import com.example.inventarymanagement.enums.AlertType;
import com.example.inventarymanagement.exception.ResourceNotFoundException;
import com.example.inventarymanagement.mapper.AlertMapper;
import com.example.inventarymanagement.repository.AlertRepository;
import com.example.inventarymanagement.service.AlertService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class AlertServiceImpl implements AlertService {

    private static final Logger logger = LoggerFactory.getLogger(AlertServiceImpl.class);

    private final AlertRepository alertRepository;
    private final AlertMapper alertMapper;

    public AlertServiceImpl(AlertRepository alertRepository, AlertMapper alertMapper) {
        this.alertRepository = alertRepository;
        this.alertMapper = alertMapper;
    }

    @Override
    public List<AlertResponse> getAlerts() {
        reconcileLowAndOutOfStockAlerts();
        return alertRepository.findAll().stream().map(alertMapper::toResponse).toList();
    }

    @Override
    public List<AlertResponse> getLowAndOutOfStockAlerts() {
        reconcileLowAndOutOfStockAlerts();
        return alertRepository.findByAlertTypeInAndIsResolvedFalseOrderByTriggeredAtDesc(
                List.of(AlertType.OUT_OF_STOCK, AlertType.LOW_STOCK))
                .stream()
                .sorted((a, b) -> {
                    int pa = a.getAlertType() == AlertType.OUT_OF_STOCK ? 0 : 1;
                    int pb = b.getAlertType() == AlertType.OUT_OF_STOCK ? 0 : 1;
                    if (pa != pb) {
                        return Integer.compare(pa, pb);
                    }
                    return b.getTriggeredAt().compareTo(a.getTriggeredAt());
                })
                .map(alertMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public AlertResponse getAlert(Long alertId) {
        Alert alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new ResourceNotFoundException("Alert not found for id=" + alertId));
        return alertMapper.toResponse(alert);
    }

    @Override
    public AlertResponse resolveAlert(Long alertId) {
        Alert alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new ResourceNotFoundException("Alert not found for id=" + alertId));
        alert.setIsResolved(true);
        return alertMapper.toResponse(alertRepository.save(alert));
    }

    @Override
    public void resolveActiveAlertsForProduct(Long productId) {
        List<Alert> alerts = alertRepository.findByProductIdAndIsResolvedFalse(productId);
        for (Alert alert : alerts) {
            alert.setIsResolved(true);
        }
        alertRepository.saveAll(alerts);
    }

    @Override
    public void evaluateAlerts(Product product, StockLevel stockLevel) {
        int available = computeAvailable(stockLevel);
        if (available == 0) {
            createAlertIfNotExists(product, AlertType.OUT_OF_STOCK,
                    product.getName() + " (" + product.getSku() + ") is out of stock.");
            return;
        }

        if (available <= product.getReorderPoint()) {
            createAlertIfNotExists(product, AlertType.LOW_STOCK,
                    product.getName() + " (" + product.getSku() + "): only " + available
                            + " units left (reorder point: "
                            + product.getReorderPoint() + ").");
        }
    }

    @Override
    public void createAlertIfNotExists(Product product, AlertType alertType, String message) {
        boolean exists = alertRepository.findFirstByProductIdAndAlertTypeAndIsResolvedFalse(product.getId(), alertType)
                .isPresent();
        if (exists) {
            return;
        }

        Alert alert = new Alert();
        alert.setProduct(product);
        alert.setAlertType(alertType);
        alert.setMessage(message);
        alert.setIsResolved(false);
        alertRepository.save(alert);

        logger.info("event=stock_alert_created product_sku={} alert_type={} message={}",
                product.getSku(), alertType, message);
    }

    private int computeAvailable(StockLevel stockLevel) {
        if (stockLevel == null) {
            return 0;
        }
        int onHand = stockLevel.getQuantityOnHand() == null ? 0 : stockLevel.getQuantityOnHand();
        int reserved = stockLevel.getQuantityReserved() == null ? 0 : stockLevel.getQuantityReserved();
        return Math.max(0, onHand - reserved);
    }

    private void reconcileLowAndOutOfStockAlerts() {
        List<Alert> activeAlerts = alertRepository.findByAlertTypeInAndIsResolvedFalseOrderByTriggeredAtDesc(
                List.of(AlertType.OUT_OF_STOCK, AlertType.LOW_STOCK));
        if (activeAlerts.isEmpty()) {
            return;
        }

        Map<Long, List<Alert>> alertsByProductId = new HashMap<>();
        for (Alert alert : activeAlerts) {
            Long productId = alert.getProduct().getId();
            alertsByProductId.computeIfAbsent(productId, ignored -> new ArrayList<>()).add(alert);
        }

        List<Alert> toResolve = new ArrayList<>();
        for (List<Alert> productAlerts : alertsByProductId.values()) {
            Product product = productAlerts.get(0).getProduct();
            int available = computeAvailable(product.getStockLevel());

            boolean shouldOutOfStockBeOpen = available == 0;
            boolean shouldLowStockBeOpen = available > 0 && available <= product.getReorderPoint();

            for (Alert alert : productAlerts) {
                boolean shouldRemainOpen = switch (alert.getAlertType()) {
                    case OUT_OF_STOCK -> shouldOutOfStockBeOpen;
                    case LOW_STOCK -> shouldLowStockBeOpen;
                    default -> true;
                };

                if (!shouldRemainOpen) {
                    alert.setIsResolved(true);
                    toResolve.add(alert);
                }
            }
        }

        if (!toResolve.isEmpty()) {
            alertRepository.saveAll(toResolve);
        }
    }
}