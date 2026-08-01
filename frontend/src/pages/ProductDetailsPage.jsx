import { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import toast from 'react-hot-toast';
import { FaArrowLeft, FaBoxesStacked } from 'react-icons/fa6';
import LoadingSpinner from '../components/LoadingSpinner';
import DataTable from '../components/DataTable';
import PageHeader from '../components/PageHeader';
import { getProductById } from '../services/productService';
import { formatCurrency, formatDateTime, humanizeEnum } from '../utils/formatters';
import { parseApiError } from '../utils/apiError';

export default function ProductDetailsPage() {
  const { id } = useParams();
  const [loading, setLoading] = useState(true);
  const [data, setData] = useState(null);

  useEffect(() => {
    const load = async () => {
      try {
        setLoading(true);
        setData(await getProductById(id));
      } catch (error) {
        toast.error(parseApiError(error));
      } finally {
        setLoading(false);
      }
    };
    load();
  }, [id]);

  if (loading) return <LoadingSpinner message="Loading product details..." />;
  if (!data?.product) return <div className="alert alert-warning">Product not found.</div>;

  const { product, recentMovements } = data;
  const onHand = Number(product.quantityOnHand ?? 0);
  const reserved = Number(product.quantityReserved ?? 0);
  const available = Math.max(0, onHand - reserved);

  return (
    <div className="d-flex flex-column gap-3 products-clean-shell">
      <PageHeader
        eyebrow="Catalog detail"
        title="Product details"
        description="Inspect inventory, pricing, and recent movements for a single product."
        actions={<Link to="/products" className="btn btn-modern btn-modern-secondary"><FaArrowLeft className="me-2" />Back</Link>}
      />

      <section className="details-section">
        <div className="section-body">
          <h5 className="section-title mb-3">Product Overview</h5>
          <div className="row g-2 g-md-3">
            <div className="col-sm-6 col-lg-3"><div className="details-item"><div className="label">SKU</div><div className="value">{product.sku}</div></div></div>
            <div className="col-sm-6 col-lg-3"><div className="details-item"><div className="label">Name</div><div className="value">{product.name}</div></div></div>
            <div className="col-sm-6 col-lg-3"><div className="details-item"><div className="label">Category</div><div className="value">{humanizeEnum(product.category)}</div></div></div>
            <div className="col-sm-6 col-lg-3"><div className="details-item"><div className="label">Supplier</div><div className="value">{product.supplierName || '-'}</div></div></div>
            <div className="col-sm-6 col-lg-3"><div className="details-item"><div className="label">Unit Price</div><div className="value">{formatCurrency(product.unitPrice)}</div></div></div>
            <div className="col-sm-6 col-lg-3"><div className="details-item"><div className="label">Cost Price</div><div className="value">{formatCurrency(product.costPrice)}</div></div></div>
            <div className="col-sm-6 col-lg-3"><div className="details-item"><div className="label">Unit Of Measure</div><div className="value">{product.unitOfMeasure || '-'}</div></div></div>
            <div className="col-sm-6 col-lg-3"><div className="details-item"><div className="label">On Hand</div><div className="value">{onHand}</div></div></div>
            <div className="col-sm-6 col-lg-3"><div className="details-item"><div className="label">Reserved</div><div className="value">{reserved}</div></div></div>
            <div className="col-sm-6 col-lg-3"><div className="details-item"><div className="label">Available</div><div className="value">{available}</div></div></div>
            <div className="col-sm-6 col-lg-3"><div className="details-item"><div className="label">Reorder Point</div><div className="value">{product.reorderPoint}</div></div></div>
            <div className="col-sm-6 col-lg-3"><div className="details-item"><div className="label">Reorder Quantity</div><div className="value">{product.reorderQuantity}</div></div></div>
          </div>
        </div>
      </section>

      <div className="d-flex flex-column gap-2">
        <h5 className="d-flex align-items-center gap-2"><FaBoxesStacked />Recent Stock Movements</h5>
        <DataTable
          data={recentMovements || []}
          columns={[
            { key: 'recordedAt', title: 'Recorded At', render: (row) => formatDateTime(row.recordedAt) },
            { key: 'movementType', title: 'Type', render: (row) => <span className="badge rounded-pill text-bg-light border">{humanizeEnum(row.movementType)}</span> },
            { key: 'quantity', title: 'Quantity' },
            { key: 'recordedBy', title: 'Recorded By' },
            { key: 'notes', title: 'Notes' },
          ]}
        />
      </div>
    </div>
  );
}
