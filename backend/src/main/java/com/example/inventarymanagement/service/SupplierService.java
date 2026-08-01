package com.example.inventarymanagement.service;

import com.example.inventarymanagement.dto.SupplierCatalogItemResponse;
import com.example.inventarymanagement.dto.SupplierCreateRequest;
import com.example.inventarymanagement.dto.SupplierResponse;
import com.example.inventarymanagement.dto.SupplierUpdateRequest;

import java.util.List;

public interface SupplierService {

    SupplierResponse createSupplier(SupplierCreateRequest request);

    List<SupplierResponse> getSuppliers();

    SupplierResponse getSupplier(Long supplierId);

    SupplierResponse updateSupplier(Long supplierId, SupplierUpdateRequest request);

    void deleteSupplier(Long supplierId);

    List<SupplierCatalogItemResponse> getSupplierCatalog(Long supplierId);
}
