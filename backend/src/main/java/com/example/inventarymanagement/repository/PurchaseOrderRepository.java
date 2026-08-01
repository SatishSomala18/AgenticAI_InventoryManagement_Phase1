package com.example.inventarymanagement.repository;

import com.example.inventarymanagement.entity.PurchaseOrder;
import com.example.inventarymanagement.enums.POStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, Long> {

    @Query("select count(po) from PurchaseOrder po where po.poNumber like concat(:pattern, '%')")
    long countByPoNumberPrefix(String pattern);

    boolean existsByPoNumber(String poNumber);

    long countByStatusIn(Iterable<POStatus> statuses);

    List<PurchaseOrder> findByStatus(POStatus status);

    List<PurchaseOrder> findBySupplierId(Long supplierId);

    List<PurchaseOrder> findByStatusAndSupplierId(POStatus status, Long supplierId);

    boolean existsBySupplierId(Long supplierId);
}
