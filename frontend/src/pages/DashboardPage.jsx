import { useEffect, useMemo, useState } from 'react';
import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, PieChart, Pie, Cell } from 'recharts';
import { FaBoxesStacked, FaTriangleExclamation, FaClipboardList, FaDollarSign, FaLayerGroup, FaWarehouse, FaChartPie } from 'react-icons/fa6';
import DashboardCard from '../components/DashboardCard';
import LoadingSpinner from '../components/LoadingSpinner';
import EmptyState from '../components/EmptyState';
import PageHeader from '../components/PageHeader';
import { getDashboard, getLowStockAlerts } from '../services/inventoryService';
import { getProducts } from '../services/productService';
import { formatCurrency, humanizeEnum } from '../utils/formatters';
import { parseApiError } from '../utils/apiError';
import toast from 'react-hot-toast';

const chartColors = ['#0d6efd', '#fd7e14', '#20c997', '#6f42c1', '#dc3545'];

export default function DashboardPage() {
  const [loading, setLoading] = useState(true);
  const [dashboard, setDashboard] = useState(null);
  const [products, setProducts] = useState([]);
  const [alerts, setAlerts] = useState([]);

  useEffect(() => {
    const load = async () => {
      try {
        setLoading(true);
        const [dashboardData, productsData, alertsData] = await Promise.all([
          getDashboard(),
          getProducts(),
          getLowStockAlerts(),
        ]);
        setDashboard(dashboardData);
        setProducts(productsData);
        setAlerts(alertsData);
      } catch (error) {
        toast.error(parseApiError(error));
      } finally {
        setLoading(false);
      }
    };
    load();
  }, []);

  const categoryChartData = useMemo(() => {
    const map = new Map();
    products.forEach((product) => {
      const current = map.get(product.category) || 0;
      const qty = Number(product.quantityOnHand ?? product.quantityAvailable ?? 0);
      map.set(product.category, current + qty);
    });

    const rows = Array.from(map.entries()).map(([name, value]) => ({ name: humanizeEnum(name), value }));
    const allZero = rows.length > 0 && rows.every((item) => Number(item.value) === 0);
    if (allZero) {
      const fallback = new Map();
      products.forEach((product) => {
        const current = fallback.get(product.category) || 0;
        fallback.set(product.category, current + 1);
      });
      return Array.from(fallback.entries()).map(([name, value]) => ({ name: humanizeEnum(name), value }));
    }

    return rows;
  }, [products]);

  const topProducts = useMemo(() => {
    return [...products]
      .sort((left, right) => Number(left.quantityAvailable || 0) - Number(right.quantityAvailable || 0))
      .slice(0, 5);
  }, [products]);

  if (loading) return <LoadingSpinner message="Loading dashboard..." />;
  if (!dashboard) return <EmptyState title="Dashboard not available" />;

  return (
    <div className="d-flex flex-column gap-4 products-clean-shell">
      <PageHeader
        eyebrow="Executive view"
        title="Inventory dashboard"
        description="Track product health, category mix, purchase order load, and stock alerts with a fast operational overview."
      />

      <div className="modern-panel">
        <div className="card-body d-flex flex-wrap gap-2">
          <span className="metric-chip">{products.length} products</span>
          <span className="metric-chip">{alerts.length} alerts</span>
          <span className="metric-chip">{dashboard.openPoCount} open orders</span>
        </div>
      </div>

      <div className="row g-3">
        <DashboardCard title="Total Products" value={dashboard.totalProducts} icon={FaBoxesStacked} tone="primary" />
        <DashboardCard title="Low / Out of Stock" value={dashboard.lowStockCount + dashboard.outOfStockCount} icon={FaTriangleExclamation} tone="warning" />
        <DashboardCard title="Open Purchase Orders" value={dashboard.openPoCount} icon={FaClipboardList} tone="info" />
        <DashboardCard title="Total Stock Value" value={formatCurrency(dashboard.totalStockValue)} icon={FaDollarSign} tone="success" />
      </div>

      <div className="row g-3">
        <div className="col-12 col-xl-7">
          <div className="card border-0 surface-card h-100">
            <div className="card-header bg-transparent border-0 fw-semibold px-4 pt-4">Stock Value by Category (On Hand Units)</div>
            <div className="card-body" style={{ height: 320 }}>
              <ResponsiveContainer width="100%" height="100%">
                <BarChart data={categoryChartData}>
                  <CartesianGrid strokeDasharray="3 3" />
                  <XAxis dataKey="name" />
                  <YAxis />
                  <Tooltip />
                  <Bar dataKey="value" fill="#0d6efd" radius={[6, 6, 0, 0]} />
                </BarChart>
              </ResponsiveContainer>
            </div>
          </div>
        </div>

        <div className="col-12 col-xl-5">
          <div className="card border-0 surface-card h-100">
            <div className="card-header bg-transparent border-0 fw-semibold px-4 pt-4">Inventory Category Mix</div>
            <div className="card-body" style={{ height: 320 }}>
              <ResponsiveContainer width="100%" height="100%">
                <PieChart>
                  <Pie data={categoryChartData} dataKey="value" nameKey="name" outerRadius={110} label>
                    {categoryChartData.map((entry, index) => (
                      <Cell key={entry.name} fill={chartColors[index % chartColors.length]} />
                    ))}
                  </Pie>
                  <Tooltip />
                </PieChart>
              </ResponsiveContainer>
            </div>
          </div>
        </div>
      </div>

      <div className="row g-3">
        <div className="col-12 col-xl-4">
          <div className="card h-100 border-0 surface-card">
            <div className="card-body">
              <div className="d-flex align-items-center justify-content-between mb-3">
                <div>
                  <div className="text-muted small">Operational mix</div>
                  <h5 className="mb-0">Top inventory signals</h5>
                </div>
                <span className="badge badge-soft rounded-pill"><FaWarehouse className="me-1" />Live</span>
              </div>
              <div className="d-flex flex-column gap-3">
                <div className="d-flex justify-content-between"><span>Low stock</span><strong>{dashboard.lowStockCount}</strong></div>
                <div className="d-flex justify-content-between"><span>Out of stock</span><strong>{dashboard.outOfStockCount}</strong></div>
                <div className="d-flex justify-content-between"><span>Open POs</span><strong>{dashboard.openPoCount}</strong></div>
              </div>
            </div>
          </div>
        </div>

        <div className="col-12 col-xl-8">
          <div className="card border-0 surface-card h-100">
            <div className="card-body">
              <div className="d-flex align-items-center justify-content-between mb-3">
                <div>
                  <div className="text-muted small">At risk products</div>
                  <h5 className="mb-0">Lowest stock items</h5>
                </div>
                <FaLayerGroup className="text-primary" />
              </div>
              <div className="table-responsive">
                <table className="table mb-0 align-middle">
                  <thead>
                    <tr>
                      <th>Product</th>
                      <th>Category</th>
                      <th>Available</th>
                      <th>Reorder</th>
                    </tr>
                  </thead>
                  <tbody>
                    {topProducts.length ? topProducts.map((product) => (
                      <tr key={product.id}>
                        <td>{product.name}</td>
                        <td>{humanizeEnum(product.category)}</td>
                        <td><span className="badge badge-soft">{product.quantityAvailable}</span></td>
                        <td>{product.reorderPoint}</td>
                      </tr>
                    )) : (
                      <tr><td colSpan="4" className="text-center text-muted py-4">No product data yet.</td></tr>
                    )}
                  </tbody>
                </table>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div className="card border-0 surface-card">
        <div className="card-header bg-transparent border-0 fw-semibold d-flex align-items-center gap-2 px-4 pt-4"><FaChartPie />Recent Low Stock Alerts</div>
        <div className="table-responsive">
          <table className="table mb-0 align-middle">
            <thead className="table-light">
              <tr>
                <th>Product</th>
                <th>Type</th>
                <th>Message</th>
                <th>Status</th>
              </tr>
            </thead>
            <tbody>
              {alerts.length ? alerts.slice(0, 8).map((alert) => (
                <tr key={alert.id}>
                  <td>{alert.productName}</td>
                  <td><span className="badge rounded-pill text-bg-light border">{humanizeEnum(alert.alertType)}</span></td>
                  <td>{alert.message}</td>
                  <td>{alert.resolved ? <span className="badge rounded-pill text-bg-success-subtle text-success-emphasis">Resolved</span> : <span className="badge rounded-pill text-bg-warning-subtle text-warning-emphasis">Open</span>}</td>
                </tr>
              )) : (
                <tr>
                  <td colSpan="4" className="text-center text-muted py-4">No active low-stock alerts.</td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}
