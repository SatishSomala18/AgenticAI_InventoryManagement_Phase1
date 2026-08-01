package com.example.inventarymanagement.repository;

import com.example.inventarymanagement.entity.Alert;
import com.example.inventarymanagement.enums.AlertType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AlertRepository extends JpaRepository<Alert, Long> {

    Optional<Alert> findFirstByProductIdAndAlertTypeAndIsResolvedFalse(Long productId, AlertType alertType);

    List<Alert> findByProductIdAndIsResolvedFalse(Long productId);

    List<Alert> findByIsResolvedFalseOrderByTriggeredAtDesc();

    List<Alert> findByAlertTypeInAndIsResolvedFalseOrderByTriggeredAtDesc(List<AlertType> alertTypes);
}