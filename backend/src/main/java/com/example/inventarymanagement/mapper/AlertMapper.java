package com.example.inventarymanagement.mapper;

import com.example.inventarymanagement.dto.AlertResponse;
import com.example.inventarymanagement.entity.Alert;
import org.springframework.stereotype.Component;

@Component
public class AlertMapper {

    public AlertResponse toResponse(Alert alert) {
        AlertResponse response = new AlertResponse();
        response.setId(alert.getId());
        response.setAlertType(alert.getAlertType());
        response.setMessage(alert.getMessage());
        response.setTriggeredAt(alert.getTriggeredAt());
        response.setResolved(alert.getIsResolved());
        response.setProductId(alert.getProduct().getId());
        response.setProductSku(alert.getProduct().getSku());
        response.setProductName(alert.getProduct().getName());
        return response;
    }
}