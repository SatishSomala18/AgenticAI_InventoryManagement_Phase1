package com.example.inventarymanagement.repository;

import com.example.inventarymanagement.entity.StockMovement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StockMovementRepository extends JpaRepository<StockMovement, Long> {

    List<StockMovement> findTop20ByProductIdOrderByRecordedAtDesc(Long productId);

    List<StockMovement> findByProductIdOrderByRecordedAtDesc(Long productId);

    List<StockMovement> findAllByOrderByRecordedAtDesc();
}
