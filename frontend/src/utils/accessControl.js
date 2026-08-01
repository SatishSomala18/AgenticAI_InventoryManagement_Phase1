import { ROLES } from './constants';

export const ROUTE_ACCESS = {
    dashboard: [ROLES.STORE_MANAGER],
    products: [ROLES.STORE_MANAGER, ROLES.INVENTORY_ANALYST, ROLES.PROCUREMENT_OFFICER, ROLES.WAREHOUSE_STAFF],
    suppliers: [ROLES.STORE_MANAGER, ROLES.PROCUREMENT_OFFICER, ROLES.INVENTORY_ANALYST],
    orders: [ROLES.STORE_MANAGER, ROLES.PROCUREMENT_OFFICER, ROLES.INVENTORY_ANALYST, ROLES.WAREHOUSE_STAFF],
    stock: [ROLES.STORE_MANAGER, ROLES.INVENTORY_ANALYST, ROLES.WAREHOUSE_STAFF],
    alerts: [ROLES.STORE_MANAGER, ROLES.INVENTORY_ANALYST],
};

export const ACTION_ACCESS = {
    createProduct: [ROLES.STORE_MANAGER, ROLES.INVENTORY_ANALYST],
    editProduct: [ROLES.STORE_MANAGER, ROLES.INVENTORY_ANALYST],
    deleteProduct: [ROLES.STORE_MANAGER, ROLES.INVENTORY_ANALYST],
    updateProductStock: [ROLES.STORE_MANAGER, ROLES.WAREHOUSE_STAFF],

    createSupplier: [ROLES.STORE_MANAGER, ROLES.PROCUREMENT_OFFICER],
    editSupplier: [ROLES.STORE_MANAGER, ROLES.PROCUREMENT_OFFICER],
    deleteSupplier: [ROLES.STORE_MANAGER, ROLES.PROCUREMENT_OFFICER],

    createOrder: [ROLES.STORE_MANAGER, ROLES.PROCUREMENT_OFFICER],
    submitOrder: [ROLES.STORE_MANAGER],
    acknowledgeOrder: [ROLES.STORE_MANAGER],
    cancelOrder: [ROLES.STORE_MANAGER],
    receiveOrder: [ROLES.STORE_MANAGER, ROLES.WAREHOUSE_STAFF],

    resolveAlert: [ROLES.STORE_MANAGER],
};

export function canAccess(role, allowedRoles) {
    if (!role || !allowedRoles?.length) return false;
    return allowedRoles.includes(role);
}

export function getHomeRouteByRole(role) {
    switch (role) {
        case ROLES.STORE_MANAGER:
            return '/dashboard';
        case ROLES.INVENTORY_ANALYST:
        case ROLES.PROCUREMENT_OFFICER:
        case ROLES.WAREHOUSE_STAFF:
            return '/products';
        default:
            return '/login';
    }
}
