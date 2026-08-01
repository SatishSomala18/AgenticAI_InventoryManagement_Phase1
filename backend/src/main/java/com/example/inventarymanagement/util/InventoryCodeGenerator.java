package com.example.inventarymanagement.util;

import com.example.inventarymanagement.enums.Category;

import java.time.LocalDate;
import java.util.Map;

public final class InventoryCodeGenerator {

    private static final Map<Category, String> CATEGORY_PREFIX = Map.of(
            Category.GROCERY, "GRO",
            Category.ELECTRONICS, "ELC",
            Category.CLOTHING, "CLO",
            Category.HOUSEHOLD, "HHD",
            Category.PERSONAL_CARE, "PRC");

    private InventoryCodeGenerator() {
    }

    public static String generateSku(Category category, long existingCount) {
        String prefix = CATEGORY_PREFIX.getOrDefault(category, "GEN");
        return String.format("SKU-%s-%04d", prefix, existingCount + 1);
    }

    public static String generatePoNumber(long existingCount) {
        int year = LocalDate.now().getYear();
        return String.format("PO-%d-%04d", year, existingCount + 1);
    }
}
