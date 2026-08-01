package com.example.inventarymanagement.repository;

import com.example.inventarymanagement.entity.POItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface POItemRepository extends JpaRepository<POItem, Long> {

    boolean existsByProductId(Long productId);
}
