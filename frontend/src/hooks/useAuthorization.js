import { ACTION_ACCESS, ROUTE_ACCESS, canAccess } from '../utils/accessControl';
import { useAuth } from './useAuth';

export function useAuthorization() {
    const { role } = useAuth();

    const canRoute = (routeKey) => canAccess(role, ROUTE_ACCESS[routeKey]);
    const canAction = (actionKey) => canAccess(role, ACTION_ACCESS[actionKey]);

    return {
        role,
        canRoute,
        canAction,
    };
}
