import { apiClient } from '../api/client';
import { API_PATHS } from '../utils/constants';

function normalizeProduct(product = {}) {
    const quantityOnHand = product.quantityOnHand ?? product.quantity_on_hand;
    const quantityReserved = product.quantityReserved ?? product.quantity_reserved;
    const quantityAvailable = product.quantityAvailable ?? product.quantity_available;

    return {
        ...product,
        unitOfMeasure: product.unitOfMeasure ?? product.unit_of_measure ?? 'pieces',
        reorderPoint: product.reorderPoint ?? product.reorder_point,
        reorderQuantity: product.reorderQuantity ?? product.reorder_quantity,
        supplierId: product.supplierId ?? product.supplier_id,
        unitPrice: product.unitPrice ?? product.unit_price,
        costPrice: product.costPrice ?? product.cost_price,
        quantityOnHand: quantityOnHand ?? 0,
        quantityReserved: quantityReserved ?? 0,
        quantityAvailable: quantityAvailable ?? Math.max(0, Number(quantityOnHand ?? 0) - Number(quantityReserved ?? 0)),
    };
}

function withProductAliases(payload = {}) {
    return {
        ...payload,
        unit_of_measure: payload.unitOfMeasure,
        reorder_point: payload.reorderPoint,
        reorder_quantity: payload.reorderQuantity,
        initial_quantity_on_hand: payload.initialQuantityOnHand,
        supplier_id: payload.supplierId,
        unit_price: payload.unitPrice,
        cost_price: payload.costPrice,
    };
}

export function getProducts(params = {}) {
    return apiClient.get(API_PATHS.PRODUCTS, { params }).then((res) => (Array.isArray(res.data) ? res.data.map(normalizeProduct) : []));
}

export function createProduct(payload) {
    return apiClient.post(API_PATHS.PRODUCTS, withProductAliases(payload)).then((res) => normalizeProduct(res.data));
}

export function updateProduct(id, payload) {
    return apiClient.put(`${API_PATHS.PRODUCTS}/${id}`, withProductAliases(payload)).then((res) => normalizeProduct(res.data));
}

export function deleteProduct(id) {
    return apiClient.delete(`${API_PATHS.PRODUCTS}/${id}`);
}

export function getProductById(id) {
    return apiClient.get(`${API_PATHS.PRODUCTS}/${id}`).then((res) => ({
        ...res.data,
        product: normalizeProduct(res.data?.product || {}),
    }));
}

export function getProductMovements(id) {
    return apiClient.get(`${API_PATHS.PRODUCTS}/${id}/movements`).then((res) => res.data);
}

export function updateProductStock(id, payload) {
    return apiClient.patch(`${API_PATHS.PRODUCTS}/${id}/stock`, payload).then((res) => res.data);
}
