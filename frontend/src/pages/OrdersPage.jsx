import { useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import toast from 'react-hot-toast';
import { FaPlus, FaArrowRotateRight, FaTruckRampBox, FaCircleCheck, FaBan, FaEye, FaArrowRotateLeft } from 'react-icons/fa6';
import DataTable from '../components/DataTable';
import FilterPanel from '../components/FilterPanel';
import LoadingSpinner from '../components/LoadingSpinner';
import PageHeader from '../components/PageHeader';
import ActionButton from '../components/ActionButton';
import { getOrders, receiveOrder, updateOrderStatus } from '../services/purchaseOrderService';
import { getSuppliers } from '../services/supplierService';
import { useAuthorization } from '../hooks/useAuthorization';
import { PO_STATUSES } from '../utils/constants';
import { formatCurrency, formatDate, humanizeEnum } from '../utils/formatters';
import { parseApiError } from '../utils/apiError';

export default function OrdersPage() {
  const { canAction, canRoute } = useAuthorization();
  const [loading, setLoading] = useState(true);
  const [orders, setOrders] = useState([]);
  const [suppliers, setSuppliers] = useState([]);
  const [status, setStatus] = useState('');
  const [supplierId, setSupplierId] = useState('');

  const draftCount = useMemo(() => orders.filter((item) => item.status === 'DRAFT').length, [orders]);
  const inProgressCount = useMemo(
    () => orders.filter((item) => ['SUBMITTED', 'ACKNOWLEDGED'].includes(item.status)).length,
    [orders]
  );
  const receivedCount = useMemo(() => orders.filter((item) => item.status === 'RECEIVED').length, [orders]);
  const cancelledCount = useMemo(() => orders.filter((item) => item.status === 'CANCELLED').length, [orders]);

  const load = async () => {
    try {
      setLoading(true);
      const [orderData, supplierData] = await Promise.all([
        getOrders({ status: status || undefined, supplier: supplierId || undefined }),
        canRoute('suppliers') ? getSuppliers() : Promise.resolve([]),
      ]);
      setOrders(orderData);
      setSuppliers(supplierData);
    } catch (error) {
      toast.error(parseApiError(error));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    load();
  }, [status, supplierId]);

  const onSubmitOrder = async (row) => {
    try {
      await updateOrderStatus(row.id, 'SUBMITTED');
      toast.success(`Order ${row.poNumber} submitted`);
      load();
    } catch (error) {
      toast.error(parseApiError(error));
    }
  };

  const onCancelOrder = async (row) => {
    try {
      await updateOrderStatus(row.id, 'CANCELLED');
      toast.success(`Order ${row.poNumber} cancelled`);
      load();
    } catch (error) {
      toast.error(parseApiError(error));
    }
  };

  const onReceiveOrder = async (row) => {
    try {
      await receiveOrder(row.id);
      toast.success(`Order ${row.poNumber} received`);
      load();
    } catch (error) {
      toast.error(parseApiError(error));
    }
  };

  const supplierOptions = useMemo(() => suppliers.map((item) => ({ value: item.id, label: item.name })), [suppliers]);

  const clearFilters = () => {
    setStatus('');
    setSupplierId('');
  };

  if (loading) return <LoadingSpinner message="Loading purchase orders..." />;

  return (
    <div className="d-flex flex-column gap-3 products-clean-shell">
      <PageHeader
        eyebrow="Procurement"
        title="Purchase Orders"
        description="Track requisitions, approvals, and receiving from a cleaner operations view."
        actions={[
          canAction('createOrder') ? (
            <ActionButton key="create" as={Link} to="/orders/new" icon={FaPlus}>
              Create Purchase Order
            </ActionButton>
          ) : null,
          <ActionButton key="refresh" icon={FaArrowRotateRight} variant="secondary" onClick={load}>
            Refresh
          </ActionButton>,
        ].filter(Boolean)}
      />

      <FilterPanel>
        <div className="row g-3 align-items-center">
          <div className="col-lg-7 col-md-8">
            <div className="d-flex flex-wrap gap-2">
              <span className="metric-chip">{orders.length} orders</span>
              <span className="metric-chip">{draftCount} draft</span>
              <span className="metric-chip">{inProgressCount} in progress</span>
              <span className="metric-chip">{receivedCount} received</span>
              <span className="metric-chip">{cancelledCount} cancelled</span>
            </div>
          </div>
          <div className="col-lg-5 col-md-4 d-flex justify-content-md-end">
            <button type="button" className="btn btn-modern btn-modern-secondary" onClick={clearFilters}><FaArrowRotateLeft className="me-1" />Clear Filters</button>
          </div>
        </div>
      </FilterPanel>

      <FilterPanel>
        <div className="row g-3 align-items-end">
          <div className="col-md-6">
            <label className="form-label text-muted small mb-1">Status</label>
            <select className="form-select" value={status} onChange={(e) => setStatus(e.target.value)}>
              <option value="">All Statuses</option>
              {PO_STATUSES.map((item) => <option key={item} value={item}>{humanizeEnum(item)}</option>)}
            </select>
          </div>
          <div className="col-md-6">
            <label className="form-label text-muted small mb-1">Supplier</label>
            <select className="form-select" value={supplierId} onChange={(e) => setSupplierId(e.target.value)}>
              <option value="">All Suppliers</option>
              {supplierOptions.map((item) => <option key={item.value} value={item.value}>{item.label}</option>)}
            </select>
          </div>
        </div>
      </FilterPanel>

      <DataTable
        data={orders}
        columns={[
          { key: 'poNumber', title: 'PO Number' },
          { key: 'supplierName', title: 'Supplier' },
          {
            key: 'status',
            title: 'Status',
            render: (row) => {
              const tones = { DRAFT: 'secondary', SUBMITTED: 'primary', ACKNOWLEDGED: 'warning', RECEIVED: 'success', CANCELLED: 'danger' };
              return <span className={`badge rounded-pill bg-${tones[row.status] || 'secondary'} text-white`}>{humanizeEnum(row.status)}</span>;
            },
          },
          { key: 'totalAmount', title: 'Total', render: (row) => formatCurrency(row.totalAmount) },
          { key: 'orderDate', title: 'Order Date', render: (row) => formatDate(row.orderDate) },
          { key: 'expectedDelivery', title: 'Expected Delivery', render: (row) => formatDate(row.expectedDelivery) },
        ]}
        rowActions={(row) => (
          <div className="row-action-icons" role="group" aria-label={`Actions for ${row.poNumber}`}>
            <Link to={`/orders/${row.id}`} className="btn btn-sm btn-modern btn-modern-secondary icon-btn" title="View order" aria-label={`View ${row.poNumber}`}><FaEye /></Link>
            {canAction('submitOrder') ? <button className="btn btn-sm btn-modern btn-modern-success icon-btn" onClick={() => onSubmitOrder(row)} disabled={row.status !== 'DRAFT'} title="Submit order" aria-label={`Submit ${row.poNumber}`}><FaCircleCheck /></button> : null}
            {canAction('receiveOrder') ? <button className="btn btn-sm btn-modern btn-modern-warning icon-btn" onClick={() => onReceiveOrder(row)} disabled={!['SUBMITTED', 'ACKNOWLEDGED'].includes(row.status)} title="Receive order" aria-label={`Receive ${row.poNumber}`}><FaTruckRampBox /></button> : null}
            {canAction('cancelOrder') ? <button className="btn btn-sm btn-modern btn-modern-danger icon-btn" onClick={() => onCancelOrder(row)} disabled={!['DRAFT', 'SUBMITTED', 'ACKNOWLEDGED'].includes(row.status)} title="Cancel order" aria-label={`Cancel ${row.poNumber}`}><FaBan /></button> : null}
          </div>
        )}
      />
    </div>
  );
}
