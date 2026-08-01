import axios from 'axios';
import { TOKEN_STORAGE_KEY } from '../utils/constants';

const baseURL = import.meta.env.VITE_API_BASE_URL || '/api/v1';

export const apiClient = axios.create({
    baseURL,
    timeout: 15000,
    headers: {
        'Content-Type': 'application/json',
    },
});

function readSession() {
    const raw = localStorage.getItem(TOKEN_STORAGE_KEY);
    if (!raw) return null;
    try {
        return JSON.parse(raw);
    } catch {
        localStorage.removeItem(TOKEN_STORAGE_KEY);
        return null;
    }
}

function isSessionExpired(session) {
    if (!session?.loggedAt || !session?.expiresInMs) {
        return true;
    }
    return Date.now() >= Number(session.loggedAt) + Number(session.expiresInMs);
}

apiClient.interceptors.request.use((config) => {
    const session = readSession();
    if (session?.accessToken && !isSessionExpired(session)) {
        config.headers.Authorization = `Bearer ${session.accessToken}`;
    } else if (session && isSessionExpired(session)) {
        localStorage.removeItem(TOKEN_STORAGE_KEY);
    }
    return config;
});

apiClient.interceptors.response.use(
    (response) => response,
    (error) => {
        if (error.response?.status === 401) {
            const session = readSession();
            if (session && isSessionExpired(session)) {
                localStorage.removeItem(TOKEN_STORAGE_KEY);
                if (window.location.pathname !== '/login') {
                    window.location.href = '/login';
                }
            }
        }
        return Promise.reject(error);
    }
);
