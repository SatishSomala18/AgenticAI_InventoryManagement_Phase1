import { useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import toast from 'react-hot-toast';
import { FaPlus, FaPenToSquare, FaTrash, FaArrowRotateRight, FaTruckField, FaEye, FaArrowRotateLeft } from 'react-icons/fa6';
import DataTable from '../components/DataTable';
import FilterPanel from '../components/FilterPanel';
import SearchBar from '../components/SearchBar';
import LoadingSpinner from '../components/LoadingSpinner';
import PageHeader from '../components/PageHeader';
import ActionButton from '../components/ActionButton';
import FloatingPanel from '../components/FloatingPanel';
import { createSupplier, deleteSupplier, getSuppliers, updateSupplier } from '../services/supplierService';
import { parseApiError } from '../utils/apiError';
import { useAuthorization } from '../hooks/useAuthorization';
import { validateSupplier } from '../utils/validation';

const initialValues = {
  name: '',
  supplierCode: '',
  contactEmail: '',
  paymentTermsDays: 30,
  leadTimeDays: 7,
};

export default function SuppliersPage() {
  const { canAction } = useAuthorization();
  const [loading, setLoading] = useState(true);
  const [createOpen, setCreateOpen] = useState(false);
  const [suppliers, setSuppliers] = useState([]);
  const [search, setSearch] = useState('');
  const [values, setValues] = useState(initialValues);
  const [editId, setEditId] = useState(null);
  const [editOpen, setEditOpen] = useState(false);
  const [editValues, setEditValues] = useState(initialValues);
  const [createErrors, setCreateErrors] = useState({});
  const [editErrors, setEditErrors] = useState({});
  const [pendingDelete, setPendingDelete] = useState(null);

  const activeSuppliers = useMemo(() => suppliers.filter((item) => item.active).length, [suppliers]);

  const load = async () => {
    try {
      setLoading(true);
      setSuppliers(await getSuppliers());
    } catch (error) {
      toast.error(parseApiError(error));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    load();
  }, []);

  const filtered = useMemo(() => {
    const term = search.trim().toLowerCase();
    if (!term) return suppliers;
    return suppliers.filter((item) =>
      [item.name, item.supplierCode, item.contactEmail].filter(Boolean).some((value) => value.toLowerCase().includes(term))
    );
  }, [suppliers, search]);

  const onSubmit = async (event) => {
    event.preventDefault();
    const nextErrors = validateSupplier(values);
    if (Object.keys(nextErrors).length) {
      setCreateErrors(nextErrors);
      return;
    }
    setCreateErrors({});
    try {
      await createSupplier({
        ...values,
        paymentTermsDays: Number(values.paymentTermsDays),
        leadTimeDays: Number(values.leadTimeDays),
      });
      toast.success('Supplier created');
      setValues(initialValues);
      setCreateOpen(false);
      load();
    } catch (error) {
      toast.error(parseApiError(error));
    }
  };

  const onStartEdit = (row) => {
    setEditId(row.id);
    setEditValues({
      name: row.name,
      supplierCode: row.supplierCode,
      contactEmail: row.contactEmail || '',
      paymentTermsDays: row.paymentTermsDays,
      leadTimeDays: row.leadTimeDays,
      active: row.active,
    });
    setEditOpen(true);
  };

  const onUpdate = async (event) => {
    event.preventDefault();
    if (!editId) return;
    const nextErrors = validateSupplier(editValues);
    if (Object.keys(nextErrors).length) {
      setEditErrors(nextErrors);
      return;
    }
    setEditErrors({});
    try {
      await updateSupplier(editId, {
        ...editValues,
        paymentTermsDays: Number(editValues.paymentTermsDays),
        leadTimeDays: Number(editValues.leadTimeDays),
      });
      toast.success('Supplier updated');
      setEditId(null);
      setEditOpen(false);
      load();
    } catch (error) {
      toast.error(parseApiError(error));
    }
  };

  const onDelete = async () => {
    if (!pendingDelete) return;
    try {
      await deleteSupplier(pendingDelete.id);
      toast.success('Supplier deleted');
      setPendingDelete(null);
      load();
    } catch (error) {
      toast.error(parseApiError(error));
      setPendingDelete(null);
    }
  };

  const resetSearch = () => setSearch('');

  if (loading) return <LoadingSpinner message="Loading suppliers..." />;

  return (
    <div className="d-flex flex-column gap-3 products-clean-shell">
      <PageHeader
        eyebrow="Vendor network"
        title="Suppliers"
        description="Manage approved vendors, contact details, and sourcing readiness from a single queue."
        actions={[
          canAction('createSupplier') ? (
            <ActionButton key="create" icon={FaPlus} onClick={() => setCreateOpen(true)}>
              Create Supplier
            </ActionButton>
          ) : null,
          <ActionButton key="refresh" icon={FaArrowRotateRight} variant="secondary" onClick={load}>
            Refresh
          </ActionButton>,
        ].filter(Boolean)}
      />

      <FilterPanel>
        <div className="row g-3 align-items-center">
          <div className="col-lg-7 col-md-8"><SearchBar value={search} onChange={setSearch} placeholder="Search suppliers by name, code, or email" /></div>
          <div className="col-lg-5 col-md-4 d-flex justify-content-md-end">
            <span className="metric-chip">{suppliers.length} suppliers</span>
            <span className="metric-chip ms-2">{activeSuppliers} active</span>
          </div>
        </div>
      </FilterPanel>

      <FloatingPanel open={createOpen && canAction('createSupplier')} title="Create Supplier" onClose={() => { setCreateOpen(false); setCreateErrors({}); }}>
        <form onSubmit={onSubmit} className="row g-3">
          <div className="col-md-4">
            <label className="form-label text-muted small mb-1">Supplier Name</label>
            <input className={`form-control${createErrors.name ? ' is-invalid' : ''}`} value={values.name} onChange={(e) => setValues((p) => ({ ...p, name: e.target.value }))} />
            {createErrors.name && <div className="invalid-feedback d-block">{createErrors.name}</div>}
          </div>
          <div className="col-md-3">
            <label className="form-label text-muted small mb-1">Supplier Code</label>
            <input className={`form-control${createErrors.supplierCode ? ' is-invalid' : ''}`} value={values.supplierCode} onChange={(e) => setValues((p) => ({ ...p, supplierCode: e.target.value }))} />
            {createErrors.supplierCode && <div className="invalid-feedback d-block">{createErrors.supplierCode}</div>}
          </div>
          <div className="col-md-5">
            <label className="form-label text-muted small mb-1">Contact Email</label>
            <input type="email" className={`form-control${createErrors.contactEmail ? ' is-invalid' : ''}`} value={values.contactEmail} onChange={(e) => setValues((p) => ({ ...p, contactEmail: e.target.value }))} />
            {createErrors.contactEmail && <div className="invalid-feedback d-block">{createErrors.contactEmail}</div>}
          </div>
          <div className="col-md-6">
            <label className="form-label text-muted small mb-1">Payment Terms Days</label>
            <input type="number" min="0" className={`form-control${createErrors.paymentTermsDays ? ' is-invalid' : ''}`} value={values.paymentTermsDays} onChange={(e) => setValues((p) => ({ ...p, paymentTermsDays: e.target.value }))} />
            {createErrors.paymentTermsDays && <div className="invalid-feedback d-block">{createErrors.paymentTermsDays}</div>}
          </div>
          <div className="col-md-6">
            <label className="form-label text-muted small mb-1">Lead Time Days</label>
            <input type="number" min="0" className={`form-control${createErrors.leadTimeDays ? ' is-invalid' : ''}`} value={values.leadTimeDays} onChange={(e) => setValues((p) => ({ ...p, leadTimeDays: e.target.value }))} />
            {createErrors.leadTimeDays && <div className="invalid-feedback d-block">{createErrors.leadTimeDays}</div>}
          </div>
          <div className="col-12 d-flex justify-content-end gap-2 pt-2">
            <button className="btn btn-modern btn-modern-secondary" type="button" onClick={() => { setCreateOpen(false); setCreateErrors({}); }}>Cancel</button>
            <button className="btn btn-modern btn-modern-success">Save Supplier</button>
          </div>
        </form>
      </FloatingPanel>

      <FloatingPanel open={editOpen && canAction('editSupplier') && Boolean(editId)} title="Update Supplier" onClose={() => { setEditOpen(false); setEditId(null); setEditErrors({}); }}>
        <form onSubmit={onUpdate} className="row g-3">
          <div className="col-md-4">
            <label className="form-label text-muted small mb-1">Supplier Name</label>
            <input className={`form-control${editErrors.name ? ' is-invalid' : ''}`} value={editValues.name} onChange={(e) => setEditValues((p) => ({ ...p, name: e.target.value }))} />
            {editErrors.name && <div className="invalid-feedback d-block">{editErrors.name}</div>}
          </div>
          <div className="col-md-3">
            <label className="form-label text-muted small mb-1">Supplier Code</label>
            <input className={`form-control${editErrors.supplierCode ? ' is-invalid' : ''}`} value={editValues.supplierCode} onChange={(e) => setEditValues((p) => ({ ...p, supplierCode: e.target.value }))} />
            {editErrors.supplierCode && <div className="invalid-feedback d-block">{editErrors.supplierCode}</div>}
          </div>
          <div className="col-md-5">
            <label className="form-label text-muted small mb-1">Contact Email</label>
            <input type="email" className={`form-control${editErrors.contactEmail ? ' is-invalid' : ''}`} value={editValues.contactEmail} onChange={(e) => setEditValues((p) => ({ ...p, contactEmail: e.target.value }))} />
            {editErrors.contactEmail && <div className="invalid-feedback d-block">{editErrors.contactEmail}</div>}
          </div>
          <div className="col-md-4">
            <label className="form-label text-muted small mb-1">Payment Terms Days</label>
            <input type="number" min="0" className={`form-control${editErrors.paymentTermsDays ? ' is-invalid' : ''}`} value={editValues.paymentTermsDays} onChange={(e) => setEditValues((p) => ({ ...p, paymentTermsDays: e.target.value }))} />
            {editErrors.paymentTermsDays && <div className="invalid-feedback d-block">{editErrors.paymentTermsDays}</div>}
          </div>
          <div className="col-md-4">
            <label className="form-label text-muted small mb-1">Lead Time Days</label>
            <input type="number" min="0" className={`form-control${editErrors.leadTimeDays ? ' is-invalid' : ''}`} value={editValues.leadTimeDays} onChange={(e) => setEditValues((p) => ({ ...p, leadTimeDays: e.target.value }))} />
            {editErrors.leadTimeDays && <div className="invalid-feedback d-block">{editErrors.leadTimeDays}</div>}
          </div>
          <div className="col-md-4 d-flex align-items-end"><div className="form-check"><input id="activeSupplier" className="form-check-input" type="checkbox" checked={Boolean(editValues.active)} onChange={(e) => setEditValues((p) => ({ ...p, active: e.target.checked }))} /><label className="form-check-label" htmlFor="activeSupplier">Active</label></div></div>
          <div className="col-12 d-flex justify-content-end gap-2 pt-2">
            <button className="btn btn-modern btn-modern-secondary" type="button" onClick={() => { setEditOpen(false); setEditId(null); setEditErrors({}); }}>Cancel</button>
            <button className="btn btn-modern btn-modern-success">Update Supplier</button>
          </div>
        </form>
      </FloatingPanel>

      <FilterPanel>
        <div className="row g-3 align-items-end">
          <div className="col-md-10">
            <div className="filters-label">Quick filters</div>
            <div className="small text-muted">Use search to narrow supplier results quickly.</div>
          </div>
          <div className="col-md-2 d-flex justify-content-md-end">
            <button type="button" className="btn btn-modern btn-modern-secondary" onClick={resetSearch}><FaArrowRotateLeft className="me-1" />Clear</button>
          </div>
        </div>
      </FilterPanel>

      <DataTable
        data={filtered}
        columns={[
          { key: 'name', title: 'Name' },
          { key: 'supplierCode', title: 'Code' },
          { key: 'contactEmail', title: 'Email' },
          { key: 'paymentTermsDays', title: 'Payment Terms' },
          { key: 'leadTimeDays', title: 'Lead Time' },
          { key: 'active', title: 'Active', render: (row) => (row.active ? <span className="badge rounded-pill text-bg-success-subtle text-success-emphasis status-badge">Active</span> : <span className="badge rounded-pill text-bg-secondary">Inactive</span>) },
        ]}
        rowActions={(row) => (
          <div className="row-action-icons" role="group" aria-label={`Actions for ${row.name}`}>
            <Link to={`/suppliers/${row.id}`} className="btn btn-sm btn-modern btn-modern-secondary icon-btn" title="View supplier" aria-label={`View ${row.name}`}><FaEye /></Link>
            {canAction('editSupplier') ? <button className="btn btn-sm btn-modern btn-modern-secondary icon-btn" onClick={() => onStartEdit(row)} title="Edit supplier" aria-label={`Edit ${row.name}`}><FaPenToSquare /></button> : null}
            {canAction('deleteSupplier') ? <button className="btn btn-sm btn-modern btn-modern-danger icon-btn" onClick={() => setPendingDelete(row)} title="Delete supplier" aria-label={`Delete ${row.name}`}><FaTrash /></button> : null}
          </div>
        )}
      />
      {pendingDelete && (
        <div className="floating-overlay" style={{ zIndex: 2000 }}>
          <div className="floating-panel" style={{ maxWidth: '420px' }}>
            <div className="floating-panel-header">
              <span className="fw-semibold">Delete Supplier</span>
            </div>
            <div className="floating-panel-body">
              <p className="mb-3">Are you sure you want to delete <strong>{pendingDelete.name}</strong>? This action cannot be undone.</p>
              <div className="d-flex justify-content-end gap-2">
                <button className="btn btn-modern btn-modern-secondary" onClick={() => setPendingDelete(null)}>Cancel</button>
                <button className="btn btn-modern btn-modern-danger" onClick={onDelete}>Delete</button>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
