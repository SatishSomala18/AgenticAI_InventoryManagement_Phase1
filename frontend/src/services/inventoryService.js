import { apiClient } from '../api/client';
import { API_PATHS } from '../utils/constants';

export function getDashboard() {
    return apiClient.get(API_PATHS.DASHBOARD).then((res) => res.data);
}

export function getLowStockAlerts() {
    return apiClient.get(API_PATHS.LOW_ALERTS).then((res) => res.data);
}

export function getAlerts() {
    return apiClient.get(API_PATHS.ALERTS).then((res) => res.data);
}

export function getAlertById(id) {
    return apiClient.get(`${API_PATHS.ALERTS}/${id}`).then((res) => res.data);
}

export function resolveAlert(id) {
    return apiClient.patch(`${API_PATHS.ALERTS}/${id}/resolve`).then((res) => res.data);
}

export function getStockLevels() {
    return apiClient.get(API_PATHS.STOCK_LEVELS).then((res) => res.data);
}

export function getStockMovements() {
    return apiClient.get(API_PATHS.STOCK_MOVEMENTS).then((res) => res.data);
}
