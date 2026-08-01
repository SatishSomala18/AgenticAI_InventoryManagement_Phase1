import { lazy, Suspense } from 'react';
import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth';
import AppLayout from '../layouts/AppLayout';
import ProtectedRoute from './ProtectedRoute';
import LoadingSpinner from '../components/LoadingSpinner';
import { ROUTE_ACCESS } from '../utils/accessControl';

const LoginPage = lazy(() => import('../pages/LoginPage'));
const RegisterPage = lazy(() => import('../pages/RegisterPage'));
const DashboardPage = lazy(() => import('../pages/DashboardPage'));
const ProductsPage = lazy(() => import('../pages/ProductsPage'));
const ProductDetailsPage = lazy(() => import('../pages/ProductDetailsPage'));
const SuppliersPage = lazy(() => import('../pages/SuppliersPage'));
const SupplierDetailsPage = lazy(() => import('../pages/SupplierDetailsPage'));
const OrdersPage = lazy(() => import('../pages/OrdersPage'));
const OrderCreatePage = lazy(() => import('../pages/OrderCreatePage'));
const OrderDetailsPage = lazy(() => import('../pages/OrderDetailsPage'));
const StockPage = lazy(() => import('../pages/StockPage'));
const AlertsPage = lazy(() => import('../pages/AlertsPage'));
const NotFoundPage = lazy(() => import('../pages/NotFoundPage'));

function HomeRedirect() {
  const { homeRoute } = useAuth();
  return <Navigate to={homeRoute} replace />;
}

export default function AppRoutes() {
  return (
    <BrowserRouter>
      <Suspense fallback={<LoadingSpinner message="Loading page..." />}>
        <Routes>
          <Route path="/login" element={<LoginPage />} />
          <Route path="/register" element={<RegisterPage />} />

          <Route element={<ProtectedRoute />}>
            <Route element={<AppLayout />}>
              <Route path="/" element={<HomeRedirect />} />

              <Route element={<ProtectedRoute allowedRoles={ROUTE_ACCESS.dashboard} />}>
                <Route path="/dashboard" element={<DashboardPage />} />
              </Route>

              <Route element={<ProtectedRoute allowedRoles={ROUTE_ACCESS.products} />}>
                <Route path="/products" element={<ProductsPage />} />
                <Route path="/products/:id" element={<ProductDetailsPage />} />
              </Route>

              <Route element={<ProtectedRoute allowedRoles={ROUTE_ACCESS.suppliers} />}>
                <Route path="/suppliers" element={<SuppliersPage />} />
                <Route path="/suppliers/:id" element={<SupplierDetailsPage />} />
              </Route>

              <Route element={<ProtectedRoute allowedRoles={ROUTE_ACCESS.orders} />}>
                <Route path="/orders" element={<OrdersPage />} />
                <Route path="/orders/:id" element={<OrderDetailsPage />} />
              </Route>

              <Route element={<ProtectedRoute allowedRoles={["STORE_MANAGER", "PROCUREMENT_OFFICER"]} />}>
                <Route path="/orders/new" element={<OrderCreatePage />} />
              </Route>

              <Route element={<ProtectedRoute allowedRoles={ROUTE_ACCESS.stock} />}>
                <Route path="/stock" element={<StockPage />} />
              </Route>

              <Route element={<ProtectedRoute allowedRoles={ROUTE_ACCESS.alerts} />}>
                <Route path="/alerts" element={<AlertsPage />} />
              </Route>
            </Route>
          </Route>

          <Route path="*" element={<NotFoundPage />} />
        </Routes>
      </Suspense>
    </BrowserRouter>
  );
}
