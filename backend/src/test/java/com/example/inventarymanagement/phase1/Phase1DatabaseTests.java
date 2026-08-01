package com.example.inventarymanagement.phase1;

import com.example.inventarymanagement.entity.POItem;
import com.example.inventarymanagement.entity.Product;
import com.example.inventarymanagement.entity.PurchaseOrder;
import com.example.inventarymanagement.entity.StockLevel;
import com.example.inventarymanagement.entity.StockMovement;
import com.example.inventarymanagement.entity.Supplier;
import com.example.inventarymanagement.enums.Category;
import com.example.inventarymanagement.enums.MovementType;
import com.example.inventarymanagement.enums.POStatus;
import com.example.inventarymanagement.repository.ProductRepository;
import com.example.inventarymanagement.repository.PurchaseOrderRepository;
import com.example.inventarymanagement.repository.StockLevelRepository;
import com.example.inventarymanagement.repository.StockMovementRepository;
import com.example.inventarymanagement.repository.SupplierRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class Phase1DatabaseTests {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private SupplierRepository supplierRepository;

    @Autowired
    private PurchaseOrderRepository purchaseOrderRepository;

    @Autowired
    private StockMovementRepository stockMovementRepository;

    @Autowired
    private StockLevelRepository stockLevelRepository;

    @Test
    void tc07p1db01_skuUniqueConstraint() {
        Product p1 = buildProduct("SKU-GRO-9999", "Test Product 1");
        productRepository.saveAndFlush(p1);

        Product p2 = buildProduct("SKU-GRO-9999", "Test Product 2");
        assertThatThrownBy(() -> productRepository.saveAndFlush(p2))
                .isInstanceOfAny(DataIntegrityViolationException.class, RuntimeException.class);
    }

    @Test
    void tc07p1db02_stockMovementLinkedToProduct() {
        Product product = productRepository.saveAndFlush(buildProduct("SKU-GRO-1111", "Movement Product"));

        StockMovement movement = new StockMovement();
        movement.setProduct(product);
        movement.setMovementType(MovementType.RECEIPT);
        movement.setQuantity(50);
        movement.setRecordedBy("Kiran");

        StockMovement saved = stockMovementRepository.saveAndFlush(movement);
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getProduct().getId()).isEqualTo(product.getId());
    }

    @Test
    void tc07p1db03_poNumberUnique() {
        Supplier supplier = supplierRepository.saveAndFlush(buildSupplier("SUP-DB-PO"));

        PurchaseOrder po1 = new PurchaseOrder();
        po1.setPoNumber("PO-2026-9999");
        po1.setSupplier(supplier);
        po1.setStatus(POStatus.DRAFT);
        po1.setTotalAmount(BigDecimal.ZERO);
        po1.setOrderDate(LocalDate.now());
        purchaseOrderRepository.saveAndFlush(po1);

        PurchaseOrder po2 = new PurchaseOrder();
        po2.setPoNumber("PO-2026-9999");
        po2.setSupplier(supplier);
        po2.setStatus(POStatus.DRAFT);
        po2.setTotalAmount(BigDecimal.ZERO);
        po2.setOrderDate(LocalDate.now());

        assertThatThrownBy(() -> purchaseOrderRepository.saveAndFlush(po2))
                .isInstanceOfAny(DataIntegrityViolationException.class, RuntimeException.class);
    }

    @Test
    void tc07p1db04_stockLevelOneToOneWithProduct() {
        Product product = productRepository.saveAndFlush(buildProduct("SKU-GRO-2222", "OneToOne Product"));

        StockLevel s1 = new StockLevel();
        s1.setProduct(product);
        s1.setQuantityOnHand(100);
        s1.setQuantityReserved(0);
        s1.recalculateAvailable();
        stockLevelRepository.saveAndFlush(s1);

        StockLevel s2 = new StockLevel();
        s2.setProduct(product);
        s2.setQuantityOnHand(50);
        s2.setQuantityReserved(0);
        s2.recalculateAvailable();

        assertThatThrownBy(() -> stockLevelRepository.saveAndFlush(s2))
                .isInstanceOfAny(DataIntegrityViolationException.class, RuntimeException.class);
    }

    private Product buildProduct(String sku, String name) {
        Product product = new Product();
        product.setSku(sku);
        product.setName(name);
        product.setCategory(Category.GROCERY);
        product.setUnitPrice(BigDecimal.valueOf(100));
        product.setCostPrice(BigDecimal.valueOf(80));
        product.setUnitOfMeasure("pieces");
        product.setReorderPoint(10);
        product.setReorderQuantity(50);
        return product;
    }

    private Supplier buildSupplier(String supplierCode) {
        Supplier supplier = new Supplier();
        supplier.setName("Supplier " + supplierCode);
        supplier.setSupplierCode(supplierCode);
        supplier.setContactEmail(supplierCode.toLowerCase() + "@example.com");
        supplier.setPaymentTermsDays(30);
        supplier.setLeadTimeDays(7);
        supplier.setActive(true);
        return supplier;
    }
}
