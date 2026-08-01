package com.example.inventarymanagement.service;

import com.example.inventarymanagement.dto.PurchaseOrderCreateRequest;
import com.example.inventarymanagement.dto.PurchaseOrderResponse;
import com.example.inventarymanagement.enums.POStatus;

import java.util.List;

public interface PurchaseOrderService {

    List<PurchaseOrderResponse> getPurchaseOrders(POStatus status, Long supplierId);

    PurchaseOrderResponse getPurchaseOrderById(Long poId);

    PurchaseOrderResponse createPurchaseOrder(PurchaseOrderCreateRequest request);

    PurchaseOrderResponse updateStatus(Long poId, POStatus status);

    PurchaseOrderResponse receivePurchaseOrder(Long poId);
}
