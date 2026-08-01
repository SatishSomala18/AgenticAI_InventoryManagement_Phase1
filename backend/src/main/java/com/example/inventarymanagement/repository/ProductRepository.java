package com.example.inventarymanagement.repository;

import com.example.inventarymanagement.entity.Product;
import com.example.inventarymanagement.enums.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    @Query("select count(p) from Product p where p.sku like concat(:pattern, '%')")
    long countBySkuPrefix(String pattern);

    Optional<Product> findBySku(String sku);

    List<Product> findByCategory(Category category);

    List<Product> findBySupplierId(Long supplierId);

    boolean existsBySupplierId(Long supplierId);

    @Query("select p from Product p join p.stockLevel s where (s.quantityOnHand - s.quantityReserved) <= p.reorderPoint order by (s.quantityOnHand - s.quantityReserved) asc")
    List<Product> findLowStockProducts();
}
