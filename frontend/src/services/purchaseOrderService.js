import { apiClient } from '../api/client';
import { API_PATHS } from '../utils/constants';

export function getOrders(params = {}) {
    return apiClient.get(API_PATHS.ORDERS, { params }).then((res) => res.data);
}

export function getOrderById(id) {
    return apiClient.get(`${API_PATHS.ORDERS}/${id}`).then((res) => res.data);
}

export function createOrder(payload) {
    return apiClient.post(API_PATHS.ORDERS, payload).then((res) => res.data);
}

export function updateOrderStatus(id, status) {
    return apiClient.patch(`${API_PATHS.ORDERS}/${id}/status`, null, { params: { status } }).then((res) => res.data);
}

export function receiveOrder(id) {
    return apiClient.patch(`${API_PATHS.ORDERS}/${id}/receive`).then((res) => res.data);
}
