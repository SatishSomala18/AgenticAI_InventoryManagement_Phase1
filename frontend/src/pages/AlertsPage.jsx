import { useEffect, useState } from 'react';
import toast from 'react-hot-toast';
import { FaCircleCheck, FaArrowRotateRight } from 'react-icons/fa6';
import DataTable from '../components/DataTable';
import LoadingSpinner from '../components/LoadingSpinner';
import PageHeader from '../components/PageHeader';
import FilterPanel from '../components/FilterPanel';
import ActionButton from '../components/ActionButton';
import { useAuthorization } from '../hooks/useAuthorization';
import { getAlerts, resolveAlert } from '../services/inventoryService';
import { formatDateTime, humanizeEnum } from '../utils/formatters';
import { parseApiError } from '../utils/apiError';

export default function AlertsPage() {
  const { canAction } = useAuthorization();
  const [loading, setLoading] = useState(true);
  const [alerts, setAlerts] = useState([]);

  const load = async () => {
    try {
      setLoading(true);
      setAlerts(await getAlerts());
    } catch (error) {
      toast.error(parseApiError(error));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    load();
  }, []);

  const onResolve = async (id) => {
    try {
      await resolveAlert(id);
      toast.success('Alert resolved');
      load();
    } catch (error) {
      toast.error(parseApiError(error));
    }
  };

  if (loading) {
    return <LoadingSpinner message="Loading alerts..." />;
  }

  const canResolveAlerts = canAction('resolveAlert');

  return (
    <div className="d-flex flex-column gap-3 products-clean-shell">
      <PageHeader
        eyebrow="Risk monitoring"
        title="Inventory Alerts"
        description="Act on low-stock signals quickly and keep inventory exceptions visible."
        actions={
          <ActionButton
            icon={FaArrowRotateRight}
            variant="secondary"
            onClick={load}
          >
            Refresh
          </ActionButton>
        }
      />

      <FilterPanel>
        <div className="d-flex flex-wrap gap-2">
          <span className="metric-chip">{alerts.length} total alerts</span>
          <span className="metric-chip">
            {alerts.filter((item) => !item.resolved).length} open
          </span>
          <span className="metric-chip">
            {alerts.filter((item) => item.resolved).length} resolved
          </span>
        </div>
      </FilterPanel>

      <DataTable
        data={alerts}
        columns={[
          { key: 'productSku', title: 'SKU' },
          { key: 'productName', title: 'Product' },
          {
            key: 'alertType',
            title: 'Type',
            render: (row) => (
              <span className="badge rounded-pill text-bg-light border">
                {humanizeEnum(row.alertType)}
              </span>
            ),
          },
          { key: 'message', title: 'Message' },
          {
            key: 'triggeredAt',
            title: 'Triggered',
            render: (row) => formatDateTime(row.triggeredAt),
          },
          {
            key: 'resolved',
            title: 'Status',
            render: (row) =>
              row.resolved ? (
                <span className="badge rounded-pill text-bg-success-subtle text-success-emphasis status-badge">
                  Resolved
                </span>
              ) : (
                <span className="badge rounded-pill text-bg-warning-subtle text-warning-emphasis status-badge">
                  Open
                </span>
              ),
          },
        ]}
        {...(canResolveAlerts && {
          rowActions: (row) => (
            <button
              className="btn btn-sm btn-modern btn-modern-success icon-btn"
              onClick={() => onResolve(row.id)}
              disabled={row.resolved}
              title="Resolve alert"
              aria-label={`Resolve alert for ${row.productName}`}
            >
              <FaCircleCheck />
            </button>
          ),
        })}
      />
    </div>
  );
}