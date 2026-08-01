import { apiClient } from '../api/client';
import { API_PATHS } from '../utils/constants';

export function getSuppliers() {
    return apiClient.get(API_PATHS.SUPPLIERS).then((res) => res.data);
}

export function getSupplierById(id) {
    return apiClient.get(`${API_PATHS.SUPPLIERS}/${id}`).then((res) => res.data);
}

export function createSupplier(payload) {
    return apiClient.post(API_PATHS.SUPPLIERS, payload).then((res) => res.data);
}

export function updateSupplier(id, payload) {
    return apiClient.put(`${API_PATHS.SUPPLIERS}/${id}`, payload).then((res) => res.data);
}

export function deleteSupplier(id) {
    return apiClient.delete(`${API_PATHS.SUPPLIERS}/${id}`);
}

export function getSupplierCatalog(id) {
    return apiClient.get(`${API_PATHS.SUPPLIERS}/${id}/catalog`).then((res) => res.data);
}
