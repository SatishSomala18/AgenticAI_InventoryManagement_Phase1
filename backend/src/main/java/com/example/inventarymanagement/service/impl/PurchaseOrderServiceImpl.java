package com.example.inventarymanagement.service.impl;

import com.example.inventarymanagement.dto.PurchaseOrderCreateRequest;
import com.example.inventarymanagement.dto.PurchaseOrderItemRequest;
import com.example.inventarymanagement.dto.PurchaseOrderItemResponse;
import com.example.inventarymanagement.dto.PurchaseOrderResponse;
import com.example.inventarymanagement.entity.POItem;
import com.example.inventarymanagement.entity.Product;
import com.example.inventarymanagement.entity.PurchaseOrder;
import com.example.inventarymanagement.entity.StockLevel;
import com.example.inventarymanagement.entity.StockMovement;
import com.example.inventarymanagement.entity.Supplier;
import com.example.inventarymanagement.enums.MovementType;
import com.example.inventarymanagement.enums.POStatus;
import com.example.inventarymanagement.exception.BusinessValidationException;
import com.example.inventarymanagement.exception.InvalidStateTransitionException;
import com.example.inventarymanagement.exception.ResourceNotFoundException;
import com.example.inventarymanagement.repository.ProductRepository;
import com.example.inventarymanagement.repository.PurchaseOrderRepository;
import com.example.inventarymanagement.repository.StockLevelRepository;
import com.example.inventarymanagement.repository.StockMovementRepository;
import com.example.inventarymanagement.repository.SupplierRepository;
import com.example.inventarymanagement.repository.UserRepository;
import com.example.inventarymanagement.service.AlertService;
import com.example.inventarymanagement.service.PurchaseOrderService;
import com.example.inventarymanagement.util.InventoryCodeGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class PurchaseOrderServiceImpl implements PurchaseOrderService {

    private static final Logger logger = LoggerFactory.getLogger(PurchaseOrderServiceImpl.class);

    private final PurchaseOrderRepository purchaseOrderRepository;
    private final SupplierRepository supplierRepository;
    private final ProductRepository productRepository;
    private final StockLevelRepository stockLevelRepository;
    private final StockMovementRepository stockMovementRepository;
    private final UserRepository userRepository;
    private final AlertService alertService;

    @Value("${app.poc-id:POC-07}")
    private String pocId;

    public PurchaseOrderServiceImpl(PurchaseOrderRepository purchaseOrderRepository,
            SupplierRepository supplierRepository,
            ProductRepository productRepository,
            StockLevelRepository stockLevelRepository,
            StockMovementRepository stockMovementRepository,
            UserRepository userRepository,
            AlertService alertService) {
        this.purchaseOrderRepository = purchaseOrderRepository;
        this.supplierRepository = supplierRepository;
        this.productRepository = productRepository;
        this.stockLevelRepository = stockLevelRepository;
        this.stockMovementRepository = stockMovementRepository;
        this.userRepository = userRepository;
        this.alertService = alertService;
    }

    @Override
    @Transactional(readOnly = true)
    public List<PurchaseOrderResponse> getPurchaseOrders(POStatus status, Long supplierId) {
        List<PurchaseOrder> orders;

        if (status != null && supplierId != null) {
            orders = purchaseOrderRepository.findByStatusAndSupplierId(status, supplierId);
        } else if (status != null) {
            orders = purchaseOrderRepository.findByStatus(status);
        } else if (supplierId != null) {
            orders = purchaseOrderRepository.findBySupplierId(supplierId);
        } else {
            orders = purchaseOrderRepository.findAll();
        }

        return orders.stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PurchaseOrderResponse getPurchaseOrderById(Long poId) {
        PurchaseOrder purchaseOrder = purchaseOrderRepository.findById(poId)
                .orElseThrow(() -> new ResourceNotFoundException("Purchase order not found for id=" + poId));
        return toResponse(purchaseOrder);
    }

    @Override
    public PurchaseOrderResponse createPurchaseOrder(PurchaseOrderCreateRequest request) {
        Supplier supplier = supplierRepository.findById(request.getSupplierId())
                .orElseThrow(
                        () -> new ResourceNotFoundException("Supplier not found for id=" + request.getSupplierId()));

        String poPrefix = "PO-" + LocalDate.now().getYear() + "-";
        long existingCount = purchaseOrderRepository.countByPoNumberPrefix(poPrefix);

        PurchaseOrder purchaseOrder = new PurchaseOrder();
        purchaseOrder.setPoNumber(nextAvailablePoNumber(existingCount));
        purchaseOrder.setSupplier(supplier);
        purchaseOrder.setStatus(POStatus.DRAFT);
        purchaseOrder.setOrderDate(request.getOrderDate());
        purchaseOrder.setExpectedDelivery(request.getExpectedDelivery());

        List<POItem> items = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (PurchaseOrderItemRequest itemRequest : request.getItems()) {
            Product product = productRepository.findById(itemRequest.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Product not found for id=" + itemRequest.getProductId()));
            validateSupplierProductMatch(supplier.getId(), product, itemRequest);

            POItem item = new POItem();
            item.setPurchaseOrder(purchaseOrder);
            item.setProduct(product);
            item.setQuantityOrdered(itemRequest.getQuantityOrdered());
            item.setQuantityReceived(itemRequest.getQuantityReceived());
            item.setUnitCost(itemRequest.getUnitCost());
            items.add(item);

            totalAmount = totalAmount.add(itemRequest.getUnitCost()
                    .multiply(BigDecimal.valueOf(itemRequest.getQuantityOrdered())));
        }

        purchaseOrder.setItems(items);
        purchaseOrder.setTotalAmount(totalAmount);
        purchaseOrder.setCreatedBy(resolveCurrentActorName());

        PurchaseOrder saved = purchaseOrderRepository.save(purchaseOrder);

        logger.info("event=po_created poc_id={} phase=P1 po_number={} supplier_id={} total_amount={}",
                pocId, saved.getPoNumber(), saved.getSupplier().getId(), saved.getTotalAmount());

        return toResponse(saved);
    }

    @Override
    public PurchaseOrderResponse updateStatus(Long poId, POStatus status) {
        PurchaseOrder purchaseOrder = purchaseOrderRepository.findById(poId)
                .orElseThrow(() -> new ResourceNotFoundException("Purchase order not found for id=" + poId));

        validateStatusTransition(purchaseOrder.getStatus(), status);
        purchaseOrder.setStatus(status);
        return toResponse(purchaseOrderRepository.save(purchaseOrder));
    }

    @Override
    public PurchaseOrderResponse receivePurchaseOrder(Long poId) {
        PurchaseOrder purchaseOrder = purchaseOrderRepository.findById(poId)
                .orElseThrow(() -> new ResourceNotFoundException("Purchase order not found for id=" + poId));

        if (purchaseOrder.getStatus() != POStatus.SUBMITTED && purchaseOrder.getStatus() != POStatus.ACKNOWLEDGED) {
            throw new InvalidStateTransitionException("Only SUBMITTED or ACKNOWLEDGED purchase orders can be received");
        }

        purchaseOrder.setStatus(POStatus.RECEIVED);
        purchaseOrder.setReceivedDate(LocalDate.now());
        purchaseOrder.setReceivedBy(resolveCurrentActorName());

        for (POItem item : purchaseOrder.getItems()) {
            int qty = item.getQuantityReceived() != null ? item.getQuantityReceived() : item.getQuantityOrdered();
            item.setQuantityReceived(qty);

            StockLevel stockLevel = stockLevelRepository.findByProductId(item.getProduct().getId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Stock level not found for product id=" + item.getProduct().getId()));
            stockLevel.setQuantityOnHand(stockLevel.getQuantityOnHand() + qty);
            stockLevel.recalculateAvailable();
            stockLevel.touchUpdatedAt();
            stockLevelRepository.save(stockLevel);

            StockMovement movement = new StockMovement();
            movement.setProduct(item.getProduct());
            movement.setMovementType(MovementType.RECEIPT);
            movement.setQuantity(qty);
            movement.setReferenceNumber(purchaseOrder.getPoNumber());
            movement.setNotes("Received from PO " + purchaseOrder.getPoNumber());
            movement.setRecordedBy(resolveCurrentActorName());
            stockMovementRepository.save(movement);

            alertService.resolveActiveAlertsForProduct(item.getProduct().getId());
            alertService.evaluateAlerts(item.getProduct(), stockLevel);
        }

        PurchaseOrder saved = purchaseOrderRepository.save(purchaseOrder);
        return toResponse(saved);
    }

    private void validateStatusTransition(POStatus current, POStatus target) {
        if (current == target) {
            return;
        }

        boolean allowed = switch (current) {
            case DRAFT -> target == POStatus.SUBMITTED || target == POStatus.CANCELLED;
            case SUBMITTED -> target == POStatus.ACKNOWLEDGED || target == POStatus.CANCELLED;
            case ACKNOWLEDGED -> target == POStatus.RECEIVED || target == POStatus.CANCELLED;
            case RECEIVED, CANCELLED -> false;
        };

        if (!allowed) {
            throw new InvalidStateTransitionException(
                    "Invalid PO status transition from " + current + " to " + target);
        }
    }

    private PurchaseOrderResponse toResponse(PurchaseOrder purchaseOrder) {
        PurchaseOrderResponse response = new PurchaseOrderResponse();
        response.setId(purchaseOrder.getId());
        response.setPoNumber(purchaseOrder.getPoNumber());
        response.setSupplierId(purchaseOrder.getSupplier().getId());
        response.setSupplierName(purchaseOrder.getSupplier().getName());
        response.setStatus(purchaseOrder.getStatus());
        response.setTotalAmount(purchaseOrder.getTotalAmount());
        response.setOrderDate(purchaseOrder.getOrderDate());
        response.setExpectedDelivery(purchaseOrder.getExpectedDelivery());
        response.setReceivedDate(purchaseOrder.getReceivedDate());
        response.setCreatedBy(purchaseOrder.getCreatedBy());
        response.setReceivedBy(purchaseOrder.getReceivedBy());
        response.setItems(purchaseOrder.getItems().stream().map(this::toItemResponse).toList());
        return response;
    }

    private PurchaseOrderItemResponse toItemResponse(POItem item) {
        PurchaseOrderItemResponse response = new PurchaseOrderItemResponse();
        response.setId(item.getId());
        response.setProductId(item.getProduct().getId());
        response.setProductSku(item.getProduct().getSku());
        response.setProductName(item.getProduct().getName());
        response.setQuantityOrdered(item.getQuantityOrdered());
        response.setQuantityReceived(item.getQuantityReceived());
        response.setUnitCost(item.getUnitCost());
        return response;
    }

    private String nextAvailablePoNumber(long startCount) {
        long candidateCount = Math.max(0, startCount);
        String poNumber = InventoryCodeGenerator.generatePoNumber(candidateCount);

        while (purchaseOrderRepository.existsByPoNumber(poNumber)) {
            candidateCount++;
            poNumber = InventoryCodeGenerator.generatePoNumber(candidateCount);
        }

        return poNumber;
    }

    private void validateSupplierProductMatch(Long supplierId, Product product, PurchaseOrderItemRequest itemRequest) {
        if (product.getSupplier() == null || product.getSupplier().getId() == null) {
            throw new BusinessValidationException(
                    "Product " + itemRequest.getProductId() + " is not mapped to any supplier");
        }
        if (!supplierId.equals(product.getSupplier().getId())) {
            throw new BusinessValidationException(
                    "Product " + itemRequest.getProductId() + " does not belong to supplier " + supplierId);
        }
    }

    private String resolveCurrentActorName() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return "system";
        }

        String username = authentication.getName();
        if (username == null || username.isBlank() || "anonymousUser".equalsIgnoreCase(username)) {
            return "system";
        }

        String roleAbbrev = authentication.getAuthorities().stream()
                .findFirst()
                .map(a -> roleToAbbrev(a.getAuthority()))
                .orElse("");

        return userRepository.findByEmail(username)
                .map(user -> {
                    String fullName = user.getFullName();
                    String displayName = (fullName == null || fullName.isBlank()) ? username : fullName;
                    return roleAbbrev.isEmpty() ? displayName : displayName + " (" + roleAbbrev + ")";
                })
                .orElse(roleAbbrev.isEmpty() ? username : username + " (" + roleAbbrev + ")");
    }

    private static String roleToAbbrev(String authority) {
        return switch (authority.replace("ROLE_", "").toUpperCase()) {
            case "STORE_MANAGER" -> "SM";
            case "INVENTORY_ANALYST" -> "IA";
            case "PROCUREMENT_OFFICER" -> "PO";
            case "WAREHOUSE_STAFF" -> "WS";
            default -> "";
        };
    }
}
