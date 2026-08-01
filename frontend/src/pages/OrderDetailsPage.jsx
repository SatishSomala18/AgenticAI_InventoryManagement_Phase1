import { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import toast from 'react-hot-toast';
import { FaArrowLeft, FaCircleCheck, FaTruckRampBox, FaBan } from 'react-icons/fa6';
import DataTable from '../components/DataTable';
import LoadingSpinner from '../components/LoadingSpinner';
import PageHeader from '../components/PageHeader';
import { getOrderById, receiveOrder, updateOrderStatus } from '../services/purchaseOrderService';
import { useAuthorization } from '../hooks/useAuthorization';
import { formatCurrency, formatDate, humanizeEnum } from '../utils/formatters';
import { parseApiError } from '../utils/apiError';

export default function OrderDetailsPage() {
  const { canAction } = useAuthorization();
  const { id } = useParams();
  const [loading, setLoading] = useState(true);
  const [order, setOrder] = useState(null);

  const load = async () => {
    try {
      setLoading(true);
      setOrder(await getOrderById(id));
    } catch (error) {
      toast.error(parseApiError(error));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    load();
  }, [id]);

  const updateStatus = async (status) => {
    try {
      await updateOrderStatus(id, status);
      toast.success(`PO moved to ${status}`);
      load();
    } catch (error) {
      toast.error(parseApiError(error));
    }
  };

  const receive = async () => {
    try {
      await receiveOrder(id);
      toast.success('PO received');
      load();
    } catch (error) {
      toast.error(parseApiError(error));
    }
  };

  if (loading) return <LoadingSpinner message="Loading purchase order details..." />;
  if (!order) return <div className="alert alert-warning">Purchase order not found.</div>;

  return (
    <div className="d-flex flex-column gap-3 products-clean-shell">
      <PageHeader
        eyebrow="Order detail"
        title={`Purchase Order ${order.poNumber}`}
        description="Review order metadata and manage lifecycle actions from one focused screen."
        actions={<Link to="/orders" className="btn btn-modern btn-modern-secondary"><FaArrowLeft className="me-2" />Back</Link>}
      />

      <section className="details-section">
        <div className="section-body">
          <h5 className="section-title mb-3">Order Overview</h5>
          <div className="row g-2 g-md-3">
            <div className="col-sm-6 col-lg-4"><div className="details-item"><div className="label">Supplier</div><div className="value">{order.supplierName}</div></div></div>
            <div className="col-sm-6 col-lg-4"><div className="details-item"><div className="label">Status</div><div className="value">{humanizeEnum(order.status)}</div></div></div>
            <div className="col-sm-6 col-lg-4"><div className="details-item"><div className="label">Order Date</div><div className="value">{formatDate(order.orderDate)}</div></div></div>
            <div className="col-sm-6 col-lg-4"><div className="details-item"><div className="label">Expected Delivery</div><div className="value">{formatDate(order.expectedDelivery)}</div></div></div>
            <div className="col-sm-6 col-lg-4"><div className="details-item"><div className="label">Received Date</div><div className="value">{formatDate(order.receivedDate)}</div></div></div>
            <div className="col-sm-6 col-lg-4"><div className="details-item"><div className="label">Total Amount</div><div className="value">{formatCurrency(order.totalAmount)}</div></div></div>
            <div className="col-sm-6 col-lg-4"><div className="details-item"><div className="label">Created By</div><div className="value">{order.createdBy || '-'}</div></div></div>
            {order.receivedBy && (
              <div className="col-sm-6 col-lg-4"><div className="details-item"><div className="label">Received By</div><div className="value">{order.receivedBy}</div></div></div>
            )}
          </div>
          <div className="d-flex flex-wrap gap-2 align-items-end mt-3">
            {canAction('submitOrder') ? <button className="btn btn-modern btn-modern-success" onClick={() => updateStatus('SUBMITTED')} disabled={order.status !== 'DRAFT'}><FaCircleCheck className="me-1" />Submit</button> : null}
            {canAction('acknowledgeOrder') ? <button className="btn btn-modern btn-modern-primary" onClick={() => updateStatus('ACKNOWLEDGED')} disabled={order.status !== 'SUBMITTED'}>Acknowledge</button> : null}
            {canAction('receiveOrder') ? <button className="btn btn-modern btn-modern-warning" onClick={receive} disabled={!['SUBMITTED', 'ACKNOWLEDGED'].includes(order.status)}><FaTruckRampBox className="me-1" />Receive</button> : null}
            {canAction('cancelOrder') ? <button className="btn btn-modern btn-modern-danger" onClick={() => updateStatus('CANCELLED')} disabled={!['DRAFT', 'SUBMITTED', 'ACKNOWLEDGED'].includes(order.status)}><FaBan className="me-1" />Cancel</button> : null}
          </div>
        </div>
      </section>

      <div className="d-flex flex-column gap-2">
        <h5>Order Items</h5>
        <DataTable
          data={order.items || []}
          columns={[
            { key: 'productSku', title: 'SKU' },
            { key: 'productName', title: 'Product' },
            { key: 'quantityOrdered', title: 'Ordered' },
            { key: 'quantityReceived', title: 'Received', render: (row) => <span className="badge rounded-pill text-bg-light border">{row.quantityReceived}</span> },
            { key: 'unitCost', title: 'Unit Cost', render: (row) => formatCurrency(row.unitCost) },
          ]}
        />
      </div>
    </div>
  );
}
