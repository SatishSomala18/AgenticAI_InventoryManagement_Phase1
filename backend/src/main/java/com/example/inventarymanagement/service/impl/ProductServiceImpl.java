package com.example.inventarymanagement.service.impl;

import com.example.inventarymanagement.dto.AlertResponse;
import com.example.inventarymanagement.dto.DashboardResponse;
import com.example.inventarymanagement.dto.ProductCreateRequest;
import com.example.inventarymanagement.dto.ProductDetailsResponse;
import com.example.inventarymanagement.dto.ProductResponse;
import com.example.inventarymanagement.dto.ProductUpdateRequest;
import com.example.inventarymanagement.dto.StockMovementRequest;
import com.example.inventarymanagement.dto.StockMovementResponse;
import com.example.inventarymanagement.entity.Product;
import com.example.inventarymanagement.entity.StockLevel;
import com.example.inventarymanagement.entity.StockMovement;
import com.example.inventarymanagement.entity.Supplier;
import com.example.inventarymanagement.enums.Category;
import com.example.inventarymanagement.enums.MovementType;
import com.example.inventarymanagement.enums.POStatus;
import com.example.inventarymanagement.exception.BusinessValidationException;
import com.example.inventarymanagement.exception.BusinessRuleException;
import com.example.inventarymanagement.exception.ResourceNotFoundException;
import com.example.inventarymanagement.repository.POItemRepository;
import com.example.inventarymanagement.repository.ProductRepository;
import com.example.inventarymanagement.repository.PurchaseOrderRepository;
import com.example.inventarymanagement.repository.StockLevelRepository;
import com.example.inventarymanagement.repository.StockMovementRepository;
import com.example.inventarymanagement.repository.SupplierRepository;
import com.example.inventarymanagement.repository.UserRepository;
import com.example.inventarymanagement.service.AlertService;
import com.example.inventarymanagement.service.ProductService;
import com.example.inventarymanagement.util.InventoryCodeGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@Transactional
public class ProductServiceImpl implements ProductService {

    private static final Logger logger = LoggerFactory.getLogger(ProductServiceImpl.class);

    private final ProductRepository productRepository;
    private final POItemRepository poItemRepository;
    private final SupplierRepository supplierRepository;
    private final StockLevelRepository stockLevelRepository;
    private final StockMovementRepository stockMovementRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final UserRepository userRepository;
    private final AlertService alertService;

    @Value("${app.poc-id:POC-07}")
    private String pocId;

    public ProductServiceImpl(ProductRepository productRepository,
            POItemRepository poItemRepository,
            SupplierRepository supplierRepository,
            StockLevelRepository stockLevelRepository,
            StockMovementRepository stockMovementRepository,
            PurchaseOrderRepository purchaseOrderRepository,
            UserRepository userRepository,
            AlertService alertService) {
        this.productRepository = productRepository;
        this.poItemRepository = poItemRepository;
        this.supplierRepository = supplierRepository;
        this.stockLevelRepository = stockLevelRepository;
        this.stockMovementRepository = stockMovementRepository;
        this.purchaseOrderRepository = purchaseOrderRepository;
        this.userRepository = userRepository;
        this.alertService = alertService;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductResponse> getProducts(Category category, Boolean lowStock) {
        List<Product> products;

        if (Boolean.TRUE.equals(lowStock)) {
            products = productRepository.findLowStockProducts();
            if (category != null) {
                products = products.stream().filter(p -> p.getCategory() == category).toList();
            }
        } else if (category != null) {
            products = productRepository.findByCategory(category);
        } else {
            products = productRepository.findAll();
        }

        return products.stream().map(product -> toProductResponse(product, product.getStockLevel())).toList();
    }

    @Override
    public ProductResponse createProduct(ProductCreateRequest request) {
        Product product = new Product();
        product.setName(request.getName());
        product.setCategory(request.getCategory());
        product.setUnitPrice(request.getUnitPrice());
        product.setCostPrice(request.getCostPrice());
        product.setUnitOfMeasure(request.getUnitOfMeasure());
        product.setReorderPoint(request.getReorderPoint());
        product.setReorderQuantity(request.getReorderQuantity());

        if (request.getSupplierId() != null) {
            Supplier supplier = supplierRepository.findById(request.getSupplierId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Supplier not found for id=" + request.getSupplierId()));
            product.setSupplier(supplier);
        }

        long existingCount = productRepository.countBySkuPrefix("SKU-" + categoryPrefix(request.getCategory()) + "-");
        product.setSku(nextAvailableSku(request.getCategory(), existingCount));

        Product saved = productRepository.save(product);

        StockLevel stockLevel = new StockLevel();
        stockLevel.setProduct(saved);
        int openingQuantity = Math.max(0,
                request.getInitialQuantityOnHand() == null ? 0 : request.getInitialQuantityOnHand());
        stockLevel.setQuantityOnHand(openingQuantity);
        stockLevel.setQuantityReserved(0);
        stockLevel.setQuantityAvailable(openingQuantity);
        stockLevel.touchUpdatedAt();
        stockLevelRepository.save(stockLevel);

        if (openingQuantity > 0) {
            StockMovement openingMovement = new StockMovement();
            openingMovement.setProduct(saved);
            openingMovement.setMovementType(MovementType.RECEIPT);
            openingMovement.setQuantity(openingQuantity);
            openingMovement.setReferenceNumber("OPEN-" + saved.getId());
            openingMovement.setNotes("Opening stock for new product");
            openingMovement.setRecordedBy(resolveCurrentActorName());
            stockMovementRepository.save(openingMovement);
        }

        alertService.resolveActiveAlertsForProduct(saved.getId());
        alertService.evaluateAlerts(saved, stockLevel);

        saved.setStockLevel(stockLevel);
        return toProductResponse(saved, stockLevel);
    }

    @Override
    public ProductResponse updateProduct(Long productId, ProductUpdateRequest request) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found for id=" + productId));

        product.setName(request.getName());
        product.setCategory(request.getCategory());
        product.setUnitPrice(request.getUnitPrice());
        product.setCostPrice(request.getCostPrice());
        product.setUnitOfMeasure(request.getUnitOfMeasure());
        product.setReorderPoint(request.getReorderPoint());
        product.setReorderQuantity(request.getReorderQuantity());

        if (request.getSupplierId() == null) {
            product.setSupplier(null);
        } else {
            Supplier supplier = supplierRepository.findById(request.getSupplierId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Supplier not found for id=" + request.getSupplierId()));
            product.setSupplier(supplier);
        }

        Product saved = productRepository.save(product);
        return toProductResponse(saved, saved.getStockLevel());
    }

    @Override
    public void deleteProduct(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found for id=" + productId));

        if (poItemRepository.existsByProductId(productId)) {
            throw new BusinessRuleException("Cannot delete product with purchase order history");
        }

        productRepository.delete(product);
    }

    @Override
    public ProductResponse updateStock(Long productId, StockMovementRequest request) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found for id=" + productId));

        StockLevel stockLevel = stockLevelRepository.findByProductId(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Stock level not found for product id=" + productId));

        int delta = normalizeDelta(request.getMovementType(), request.getQuantity());
        int newOnHand = stockLevel.getQuantityOnHand() + delta;
        if (newOnHand < 0) {
            throw new BusinessValidationException("Insufficient stock for movement");
        }

        stockLevel.setQuantityOnHand(newOnHand);
        stockLevel.recalculateAvailable();
        stockLevel.touchUpdatedAt();
        stockLevelRepository.save(stockLevel);

        StockMovement movement = new StockMovement();
        movement.setProduct(product);
        movement.setMovementType(request.getMovementType());
        movement.setQuantity(delta);
        movement.setReferenceNumber(request.getReferenceNumber());
        movement.setNotes(request.getNotes());
        movement.setRecordedBy(resolveRecordedBy(request.getRecordedBy()));
        stockMovementRepository.save(movement);

        alertService.resolveActiveAlertsForProduct(productId);
        alertService.evaluateAlerts(product, stockLevel);

        logger.info(
                "event=stock_updated poc_id={} phase=P1 product_sku={} movement_type={} quantity={} new_quantity_on_hand={}",
                pocId, product.getSku(), request.getMovementType(), delta, stockLevel.getQuantityOnHand());

        return toProductResponse(product, stockLevel);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductDetailsResponse getProductWithMovements(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found for id=" + productId));

        StockLevel stockLevel = stockLevelRepository.findByProductId(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Stock level not found for product id=" + productId));

        ProductDetailsResponse details = new ProductDetailsResponse();
        details.setProduct(toProductResponse(product, stockLevel));
        details.setRecentMovements(stockMovementRepository.findTop20ByProductIdOrderByRecordedAtDesc(productId)
                .stream().map(this::toMovementResponse).toList());
        return details;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AlertResponse> getLowStockAlerts() {
        return alertService.getLowAndOutOfStockAlerts();
    }

    @Override
    @Transactional(readOnly = true)
    public List<StockMovementResponse> getProductMovements(Long productId) {
        if (!productRepository.existsById(productId)) {
            throw new ResourceNotFoundException("Product not found for id=" + productId);
        }
        return stockMovementRepository.findByProductIdOrderByRecordedAtDesc(productId)
                .stream().map(this::toMovementResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public DashboardResponse getDashboard() {
        List<Product> products = productRepository.findAll();

        long totalProducts = products.size();
        long lowStockCount = 0;
        long outOfStockCount = 0;
        BigDecimal totalStockValue = BigDecimal.ZERO;

        for (Product product : products) {
            StockLevel level = product.getStockLevel();
            if (level == null) {
                continue;
            }
            int available = computeAvailable(level);
            if (available == 0) {
                outOfStockCount++;
            } else if (available <= product.getReorderPoint()) {
                lowStockCount++;
            }
            totalStockValue = totalStockValue.add(product.getCostPrice()
                    .multiply(BigDecimal.valueOf(level.getQuantityOnHand())));
        }

        long openPoCount = purchaseOrderRepository
                .countByStatusIn(List.of(POStatus.DRAFT, POStatus.SUBMITTED, POStatus.ACKNOWLEDGED));

        DashboardResponse response = new DashboardResponse();
        response.setTotalProducts(totalProducts);
        response.setLowStockCount(lowStockCount);
        response.setOutOfStockCount(outOfStockCount);
        response.setOpenPoCount(openPoCount);
        response.setTotalStockValue(totalStockValue);
        return response;
    }

    private int normalizeDelta(MovementType movementType, int quantity) {
        if (movementType == MovementType.SALE || movementType == MovementType.TRANSFER) {
            return -quantity;
        }
        return quantity;
    }

    private ProductResponse toProductResponse(Product product, StockLevel stockLevel) {
        ProductResponse response = new ProductResponse();
        response.setId(product.getId());
        response.setSku(product.getSku());
        response.setName(product.getName());
        response.setCategory(product.getCategory());
        response.setUnitPrice(product.getUnitPrice());
        response.setCostPrice(product.getCostPrice());
        response.setUnitOfMeasure(product.getUnitOfMeasure());
        response.setReorderPoint(product.getReorderPoint());
        response.setReorderQuantity(product.getReorderQuantity());
        if (stockLevel != null) {
            int onHand = stockLevel.getQuantityOnHand() == null ? 0 : stockLevel.getQuantityOnHand();
            int reserved = stockLevel.getQuantityReserved() == null ? 0 : stockLevel.getQuantityReserved();
            int available = Math.max(0, onHand - reserved);
            response.setQuantityOnHand(onHand);
            response.setQuantityReserved(reserved);
            response.setQuantityAvailable(available);
        }
        if (product.getSupplier() != null) {
            response.setSupplierId(product.getSupplier().getId());
            response.setSupplierName(product.getSupplier().getName());
        }
        return response;
    }

    private String resolveRecordedBy(String requestedRecordedBy) {
        if (requestedRecordedBy != null) {
            String normalized = requestedRecordedBy.trim();
            if (!normalized.isEmpty() && !"system".equalsIgnoreCase(normalized)) {
                return normalized;
            }
        }
        return resolveCurrentActorName();
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

    private StockMovementResponse toMovementResponse(StockMovement movement) {
        StockMovementResponse response = new StockMovementResponse();
        response.setId(movement.getId());
        response.setProductId(movement.getProduct().getId());
        response.setProductSku(movement.getProduct().getSku());
        response.setProductName(movement.getProduct().getName());
        response.setMovementType(movement.getMovementType());
        response.setQuantity(movement.getQuantity());
        response.setReferenceNumber(movement.getReferenceNumber());
        response.setNotes(movement.getNotes());
        response.setRecordedBy(movement.getRecordedBy());
        response.setRecordedAt(movement.getRecordedAt());
        return response;
    }

    private String categoryPrefix(Category category) {
        return switch (category) {
            case GROCERY -> "GRO";
            case ELECTRONICS -> "ELC";
            case CLOTHING -> "CLO";
            case HOUSEHOLD -> "HHD";
            case PERSONAL_CARE -> "PRC";
        };
    }

    private int computeAvailable(StockLevel stockLevel) {
        int onHand = stockLevel.getQuantityOnHand() == null ? 0 : stockLevel.getQuantityOnHand();
        int reserved = stockLevel.getQuantityReserved() == null ? 0 : stockLevel.getQuantityReserved();
        return Math.max(0, onHand - reserved);
    }

    private String nextAvailableSku(Category category, long startCount) {
        long candidateCount = Math.max(0, startCount);
        String sku = InventoryCodeGenerator.generateSku(category, candidateCount);

        while (productRepository.findBySku(sku).isPresent()) {
            candidateCount++;
            sku = InventoryCodeGenerator.generateSku(category, candidateCount);
        }

        return sku;
    }
}
