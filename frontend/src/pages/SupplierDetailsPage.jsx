import { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import toast from 'react-hot-toast';
import { FaArrowLeft, FaTruckField } from 'react-icons/fa6';
import DataTable from '../components/DataTable';
import LoadingSpinner from '../components/LoadingSpinner';
import PageHeader from '../components/PageHeader';
import { getSupplierById, getSupplierCatalog } from '../services/supplierService';
import { formatCurrency } from '../utils/formatters';
import { parseApiError } from '../utils/apiError';

export default function SupplierDetailsPage() {
  const { id } = useParams();
  const [loading, setLoading] = useState(true);
  const [supplier, setSupplier] = useState(null);
  const [catalog, setCatalog] = useState([]);

  useEffect(() => {
    const load = async () => {
      try {
        setLoading(true);
        const [supplierData, catalogData] = await Promise.all([getSupplierById(id), getSupplierCatalog(id)]);
        setSupplier(supplierData);
        setCatalog(catalogData);
      } catch (error) {
        toast.error(parseApiError(error));
      } finally {
        setLoading(false);
      }
    };
    load();
  }, [id]);

  if (loading) return <LoadingSpinner message="Loading supplier details..." />;

  return (
    <div className="d-flex flex-column gap-3 products-clean-shell">
      <PageHeader
        eyebrow="Vendor detail"
        title="Supplier details"
        description="Review supplier contact information and catalog coverage."
        actions={<Link to="/suppliers" className="btn btn-modern btn-modern-secondary"><FaArrowLeft className="me-2" />Back</Link>}
      />

      <section className="details-section">
        <div className="section-body">
          <h5 className="section-title mb-3">Supplier Overview</h5>
          <div className="row g-2 g-md-3">
            <div className="col-sm-6 col-lg-4"><div className="details-item"><div className="label">Supplier Name</div><div className="value">{supplier?.name || '-'}</div></div></div>
            <div className="col-sm-6 col-lg-4"><div className="details-item"><div className="label">Code</div><div className="value">{supplier?.supplierCode || '-'}</div></div></div>
            <div className="col-sm-12 col-lg-4"><div className="details-item"><div className="label">Email</div><div className="value">{supplier?.contactEmail || '-'}</div></div></div>
            <div className="col-sm-6 col-lg-6"><div className="details-item"><div className="label">Payment Terms</div><div className="value">{supplier?.paymentTermsDays ?? '-'} days</div></div></div>
            <div className="col-sm-6 col-lg-6"><div className="details-item"><div className="label">Lead Time</div><div className="value">{supplier?.leadTimeDays ?? '-'} days</div></div></div>
          </div>
        </div>
      </section>

      <div className="d-flex flex-column gap-2">
        <h5 className="d-flex align-items-center gap-2"><FaTruckField />Supplier Catalog</h5>
        <DataTable
          data={catalog}
          columns={[
            { key: 'sku', title: 'SKU' },
            { key: 'name', title: 'Product Name' },
            { key: 'unitCost', title: 'Unit Cost', render: (row) => <span className="badge rounded-pill text-bg-light border">{formatCurrency(row.unitCost)}</span> },
          ]}
        />
      </div>
    </div>
  );
}
