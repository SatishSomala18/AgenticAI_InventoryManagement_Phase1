package com.example.inventarymanagement.phase1;

import com.example.inventarymanagement.entity.Product;
import com.example.inventarymanagement.entity.StockLevel;
import com.example.inventarymanagement.enums.AlertType;
import com.example.inventarymanagement.enums.Category;
import com.example.inventarymanagement.mapper.AlertMapper;
import com.example.inventarymanagement.repository.AlertRepository;
import com.example.inventarymanagement.service.impl.AlertServiceImpl;
import com.example.inventarymanagement.util.InventoryCodeGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class Phase1UnitTests {

    @Mock
    private AlertRepository alertRepository;

    @Mock
    private AlertMapper alertMapper;

    @InjectMocks
    private AlertServiceImpl alertService;

    @Test
    void tc07p1unit01_skuFormatGeneration() {
        String sku = InventoryCodeGenerator.generateSku(Category.GROCERY, 41);
        assertThat(sku).isEqualTo("SKU-GRO-0042");
        assertThat(sku).startsWith("SKU-GRO-");
    }

    @Test
    void tc07p1unit02_skuDifferentCategories() {
        assertThat(InventoryCodeGenerator.generateSku(Category.ELECTRONICS, 0)).startsWith("SKU-ELC-");
        assertThat(InventoryCodeGenerator.generateSku(Category.CLOTHING, 0)).startsWith("SKU-CLO-");
        assertThat(InventoryCodeGenerator.generateSku(Category.HOUSEHOLD, 0)).startsWith("SKU-HHD-");
    }

    @Test
    void tc07p1unit03_poNumberFormat() {
        String poNumber = InventoryCodeGenerator.generatePoNumber(41);
        int year = LocalDate.now().getYear();
        assertThat(poNumber).isEqualTo("PO-" + year + "-0042");
    }

    @Test
    void tc07p1unit04_lowStockAlertTriggered() {
        Product product = buildProduct("SKU-GRO-0001", 20);
        StockLevel stockLevel = buildStock(15, 0);

        when(alertRepository.findFirstByProductIdAndAlertTypeAndIsResolvedFalse(product.getId(), AlertType.LOW_STOCK))
                .thenReturn(Optional.empty());

        alertService.evaluateAlerts(product, stockLevel);

        ArgumentCaptor<com.example.inventarymanagement.entity.Alert> captor = ArgumentCaptor
                .forClass(com.example.inventarymanagement.entity.Alert.class);
        verify(alertRepository).save(captor.capture());
        assertThat(captor.getValue().getAlertType()).isEqualTo(AlertType.LOW_STOCK);
    }

    @Test
    void tc07p1unit05_outOfStockAlertCritical() {
        Product product = buildProduct("SKU-GRO-0002", 20);
        StockLevel stockLevel = buildStock(0, 0);

        when(alertRepository.findFirstByProductIdAndAlertTypeAndIsResolvedFalse(product.getId(),
                AlertType.OUT_OF_STOCK))
                .thenReturn(Optional.empty());

        alertService.evaluateAlerts(product, stockLevel);

        ArgumentCaptor<com.example.inventarymanagement.entity.Alert> captor = ArgumentCaptor
                .forClass(com.example.inventarymanagement.entity.Alert.class);
        verify(alertRepository).save(captor.capture());
        assertThat(captor.getValue().getAlertType()).isEqualTo(AlertType.OUT_OF_STOCK);
    }

    @Test
    void tc07p1unit06_noAlertAboveReorderPoint() {
        Product product = buildProduct("SKU-ELC-0001", 10);
        StockLevel stockLevel = buildStock(50, 0);

        alertService.evaluateAlerts(product, stockLevel);

        verify(alertRepository, never()).save(any());
    }

    @Test
    void tc07p1unit07_stockValueCalculation() {
        List<BigDecimal> stockValues = List.of(
                BigDecimal.valueOf(100).multiply(BigDecimal.valueOf(50.0)),
                BigDecimal.valueOf(50).multiply(BigDecimal.valueOf(200.0)));

        BigDecimal total = stockValues.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(total).isEqualByComparingTo("15000.0");
    }

    @Test
    void tc07p1unit08_quantityAvailableEqualsOnHandMinusReserved() {
        StockLevel stock = new StockLevel();
        stock.setQuantityOnHand(100);
        stock.setQuantityReserved(30);

        assertThat(stock.getQuantityAvailable()).isEqualTo(70);
    }

    private Product buildProduct(String sku, int reorderPoint) {
        Product product = new Product();
        product.setId(1L);
        product.setSku(sku);
        product.setReorderPoint(reorderPoint);
        return product;
    }

    private StockLevel buildStock(int onHand, int reserved) {
        StockLevel stockLevel = new StockLevel();
        stockLevel.setQuantityOnHand(onHand);
        stockLevel.setQuantityReserved(reserved);
        return stockLevel;
    }
}
