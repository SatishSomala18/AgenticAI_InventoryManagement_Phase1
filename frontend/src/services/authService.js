import { apiClient } from '../api/client';
import { API_PATHS } from '../utils/constants';

export function login(payload) {
    return apiClient.post(API_PATHS.AUTH_LOGIN, payload).then((res) => res.data);
}

export function register(payload) {
    return apiClient.post(API_PATHS.AUTH_REGISTER, payload).then((res) => res.data);
}
