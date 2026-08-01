package com.example.inventarymanagement.service.impl;

import com.example.inventarymanagement.dto.SupplierCatalogItemResponse;
import com.example.inventarymanagement.dto.SupplierCreateRequest;
import com.example.inventarymanagement.dto.SupplierResponse;
import com.example.inventarymanagement.dto.SupplierUpdateRequest;
import com.example.inventarymanagement.entity.Product;
import com.example.inventarymanagement.entity.Supplier;
import com.example.inventarymanagement.exception.BusinessRuleException;
import com.example.inventarymanagement.exception.DuplicateResourceException;
import com.example.inventarymanagement.exception.ResourceNotFoundException;
import com.example.inventarymanagement.repository.ProductRepository;
import com.example.inventarymanagement.repository.PurchaseOrderRepository;
import com.example.inventarymanagement.repository.SupplierRepository;
import com.example.inventarymanagement.service.SupplierService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class SupplierServiceImpl implements SupplierService {

    private final SupplierRepository supplierRepository;
    private final ProductRepository productRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;

    public SupplierServiceImpl(SupplierRepository supplierRepository,
            ProductRepository productRepository,
            PurchaseOrderRepository purchaseOrderRepository) {
        this.supplierRepository = supplierRepository;
        this.productRepository = productRepository;
        this.purchaseOrderRepository = purchaseOrderRepository;
    }

    @Override
    public SupplierResponse createSupplier(SupplierCreateRequest request) {
        if (supplierRepository.findBySupplierCode(request.getSupplierCode()).isPresent()) {
            throw new DuplicateResourceException(
                    "Supplier code '" + request.getSupplierCode() + "' is already in use");
        }
        Supplier supplier = new Supplier();
        supplier.setName(request.getName());
        supplier.setSupplierCode(request.getSupplierCode());
        supplier.setContactEmail(request.getContactEmail());
        supplier.setPaymentTermsDays(request.getPaymentTermsDays());
        supplier.setLeadTimeDays(request.getLeadTimeDays());
        supplier.setActive(true);

        Supplier saved = supplierRepository.save(supplier);
        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SupplierResponse> getSuppliers() {
        return supplierRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public SupplierResponse getSupplier(Long supplierId) {
        Supplier supplier = supplierRepository.findById(supplierId)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier not found for id=" + supplierId));
        return toResponse(supplier);
    }

    @Override
    public SupplierResponse updateSupplier(Long supplierId, SupplierUpdateRequest request) {
        Supplier supplier = supplierRepository.findById(supplierId)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier not found for id=" + supplierId));

        supplierRepository.findBySupplierCode(request.getSupplierCode())
                .ifPresent(existing -> {
                    if (!existing.getId().equals(supplierId)) {
                        throw new DuplicateResourceException(
                                "Supplier code '" + request.getSupplierCode() + "' is already in use");
                    }
                });

        supplier.setName(request.getName());
        supplier.setSupplierCode(request.getSupplierCode());
        supplier.setContactEmail(request.getContactEmail());
        supplier.setPaymentTermsDays(request.getPaymentTermsDays());
        supplier.setLeadTimeDays(request.getLeadTimeDays());
        supplier.setActive(request.getActive());

        Supplier saved = supplierRepository.save(supplier);
        return toResponse(saved);
    }

    @Override
    public void deleteSupplier(Long supplierId) {
        Supplier supplier = supplierRepository.findById(supplierId)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier not found for id=" + supplierId));

        if (productRepository.existsBySupplierId(supplierId)) {
            throw new BusinessRuleException("Cannot delete supplier with linked products");
        }
        if (purchaseOrderRepository.existsBySupplierId(supplierId)) {
            throw new BusinessRuleException("Cannot delete supplier with purchase order history");
        }

        supplierRepository.delete(supplier);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SupplierCatalogItemResponse> getSupplierCatalog(Long supplierId) {
        supplierRepository.findById(supplierId)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier not found for id=" + supplierId));

        return productRepository.findBySupplierId(supplierId).stream().map(this::toCatalogItem).toList();
    }

    private SupplierCatalogItemResponse toCatalogItem(Product product) {
        SupplierCatalogItemResponse response = new SupplierCatalogItemResponse();
        response.setProductId(product.getId());
        response.setSku(product.getSku());
        response.setName(product.getName());
        response.setUnitCost(product.getCostPrice());
        return response;
    }

    private SupplierResponse toResponse(Supplier supplier) {
        SupplierResponse response = new SupplierResponse();
        response.setId(supplier.getId());
        response.setName(supplier.getName());
        response.setSupplierCode(supplier.getSupplierCode());
        response.setContactEmail(supplier.getContactEmail());
        response.setPaymentTermsDays(supplier.getPaymentTermsDays());
        response.setLeadTimeDays(supplier.getLeadTimeDays());
        response.setActive(supplier.getActive());
        return response;
    }
}
