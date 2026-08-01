export const ROLES = {
    STORE_MANAGER: 'STORE_MANAGER',
    INVENTORY_ANALYST: 'INVENTORY_ANALYST',
    PROCUREMENT_OFFICER: 'PROCUREMENT_OFFICER',
    WAREHOUSE_STAFF: 'WAREHOUSE_STAFF',
};

export const CATEGORIES = [
    'GROCERY',
    'ELECTRONICS',
    'CLOTHING',
    'HOUSEHOLD',
    'PERSONAL_CARE',
];

export const MOVEMENT_TYPES = ['RECEIPT', 'SALE', 'ADJUSTMENT', 'TRANSFER', 'RETURN'];

export const PO_STATUSES = ['DRAFT', 'SUBMITTED', 'ACKNOWLEDGED', 'RECEIVED', 'CANCELLED'];

export const ALERT_TYPES = ['LOW_STOCK', 'OUT_OF_STOCK', 'OVERSTOCK', 'REORDER_SUGGESTED'];

export const API_PATHS = {
    AUTH_LOGIN: '/auth/login',
    AUTH_REGISTER: '/auth/register',
    PRODUCTS: '/products',
    SUPPLIERS: '/suppliers',
    ORDERS: '/orders',
    DASHBOARD: '/dashboard',
    ALERTS: '/alerts',
    LOW_ALERTS: '/stock/low-alerts',
    STOCK_LEVELS: '/stock-levels',
    STOCK_MOVEMENTS: '/stock-movements',
};

export const TOKEN_STORAGE_KEY = 'inventory.auth';
