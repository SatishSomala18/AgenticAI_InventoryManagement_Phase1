import { useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import toast from 'react-hot-toast';
import { FaPlus, FaPenToSquare, FaTrash, FaArrowRotateRight, FaArrowRotateLeft, FaEye, FaBoxesStacked } from 'react-icons/fa6';
import DataTable from '../components/DataTable';
import FilterPanel from '../components/FilterPanel';
import SearchBar from '../components/SearchBar';
import LoadingSpinner from '../components/LoadingSpinner';
import PageHeader from '../components/PageHeader';
import ActionButton from '../components/ActionButton';
import FloatingPanel from '../components/FloatingPanel';
import { createProduct, deleteProduct, getProductById, getProducts, updateProduct, updateProductStock } from '../services/productService';
import { getSuppliers } from '../services/supplierService';
import { CATEGORIES, MOVEMENT_TYPES } from '../utils/constants';
import { formatCurrency, humanizeEnum } from '../utils/formatters';
import { parseApiError } from '../utils/apiError';
import { useDebouncedValue } from '../hooks/useDebouncedValue';
import { useAuthorization } from '../hooks/useAuthorization';
import { validateProduct } from '../utils/validation';

const initialCreate = {
  name: '',
  category: 'GROCERY',
  unitPrice: '',
  costPrice: '',
  unitOfMeasure: 'pieces',
  reorderPoint: 10,
  reorderQuantity: 50,
  initialQuantityOnHand: 0,
  supplierId: '',
};

export default function ProductsPage() {
  const { canAction, canRoute } = useAuthorization();
  const [loading, setLoading] = useState(true);
  const [items, setItems] = useState([]);
  const [suppliers, setSuppliers] = useState([]);
  const [search, setSearch] = useState('');
  const [category, setCategory] = useState('');
  const [lowStockOnly, setLowStockOnly] = useState(false);
  const [createOpen, setCreateOpen] = useState(false);
  const [createValues, setCreateValues] = useState(initialCreate);
  const [stockValues, setStockValues] = useState({});
  const [activeStockProductId, setActiveStockProductId] = useState(null);
  const [editId, setEditId] = useState(null);
  const [editOpen, setEditOpen] = useState(false);
  const [editValues, setEditValues] = useState(initialCreate);
  const [createErrors, setCreateErrors] = useState({});
  const [editErrors, setEditErrors] = useState({});
  const [pendingDelete, setPendingDelete] = useState(null);
  const debouncedSearch = useDebouncedValue(search, 250);

  const supplierById = useMemo(
    () => new Map(suppliers.map((supplier) => [supplier.id, supplier.name])),
    [suppliers]
  );

  const effectiveAvailable = (item) => {
    const onHand = Number(item.quantityOnHand ?? 0);
    const reserved = Number(item.quantityReserved ?? 0);
    const derived = Math.max(0, onHand - reserved);
    const raw = Number(item.quantityAvailable ?? derived);
    if (Number.isNaN(raw)) return derived;
    return raw !== derived ? derived : raw;
  };

  const lowStockCount = useMemo(
    () => items.filter((item) => effectiveAvailable(item) <= Number(item.reorderPoint || 0)).length,
    [items]
  );

  const load = async () => {
    try {
      setLoading(true);
      const [productsData, supplierData] = await Promise.all([
        getProducts({ category: category || undefined, low_stock: lowStockOnly || undefined }),
        canRoute('suppliers') ? getSuppliers() : Promise.resolve([]),
      ]);
      setItems(productsData);
      setSuppliers(supplierData);
    } catch (error) {
      toast.error(parseApiError(error));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    load();
  }, [category, lowStockOnly]);

  const filtered = useMemo(() => {
    const term = debouncedSearch.trim().toLowerCase();
    if (!term) return items;
    return items.filter((item) =>
      [item.name, item.sku, item.category]
        .filter(Boolean)
        .some((value) => value.toLowerCase().includes(term))
    );
  }, [items, debouncedSearch]);

  const sortedProducts = useMemo(() => {
    const categoryRank = Object.fromEntries(CATEGORIES.map((item, index) => [item, index]));
    return [...filtered].sort((left, right) => {
      const categoryDiff = (categoryRank[left.category] ?? 99) - (categoryRank[right.category] ?? 99);
      if (categoryDiff !== 0) return categoryDiff;
      const stockDiff = effectiveAvailable(left) - effectiveAvailable(right);
      if (stockDiff !== 0) return stockDiff;
      return String(left.name || '').localeCompare(String(right.name || ''));
    });
  }, [filtered]);

  const onCreate = async (event) => {
    event.preventDefault();
    const nextCreateErrors = validateProduct(createValues);
    if (Object.keys(nextCreateErrors).length) {
      setCreateErrors(nextCreateErrors);
      return;
    }
    setCreateErrors({});
    try {
      const openingQuantity = Number(createValues.initialQuantityOnHand || 0);
      const created = await createProduct({
        ...createValues,
        unitPrice: Number(createValues.unitPrice),
        costPrice: Number(createValues.costPrice),
        reorderPoint: Number(createValues.reorderPoint),
        reorderQuantity: Number(createValues.reorderQuantity),
        initialQuantityOnHand: openingQuantity,
        supplierId: createValues.supplierId ? Number(createValues.supplierId) : null,
      });

      if (openingQuantity > 0 && created?.id) {
        const latest = await getProductById(created.id).catch(() => null);
        const createdProduct = latest?.product || created;
        const onHand = Number(createdProduct?.quantityOnHand ?? 0);
        const reserved = Number(createdProduct?.quantityReserved ?? 0);
        const available = Math.max(0, onHand - reserved);

        if (onHand === 0 && available === 0) {
          await updateProductStock(created.id, {
            movementType: 'RECEIPT',
            quantity: openingQuantity,
            referenceNumber: `OPEN-${created.id}`,
            notes: 'Auto-applied opening quantity fallback',
          });
        }
      }

      toast.success('Product created');
      setCreateValues(initialCreate);
      setCreateOpen(false);
      load();
    } catch (error) {
      toast.error(parseApiError(error));
    }
  };

  const onStockUpdate = async (productId) => {
    const values = stockValues[productId];
    if (!values?.movementType || !values?.quantity) {
      toast.error('Movement type and quantity are required');
      return;
    }

    try {
      await updateProductStock(productId, {
        ...values,
        quantity: Number(values.quantity),
      });
      toast.success('Stock updated');
      setStockValues((prev) => ({ ...prev, [productId]: {} }));
      setActiveStockProductId(null);
      load();
    } catch (error) {
      toast.error(parseApiError(error));
    }
  };

  const startEdit = (row) => {
    setEditId(row.id);
    setEditValues({
      name: row.name,
      category: row.category,
      unitPrice: row.unitPrice,
      costPrice: row.costPrice,
      unitOfMeasure: row.unitOfMeasure || 'pieces',
      reorderPoint: row.reorderPoint,
      reorderQuantity: row.reorderQuantity,
      supplierId: row.supplierId || '',
    });
    setEditOpen(true);
  };

  const onEditSave = async (event) => {
    event.preventDefault();
    if (!editId) return;
    const nextEditErrors = validateProduct(editValues);
    if (Object.keys(nextEditErrors).length) {
      setEditErrors(nextEditErrors);
      return;
    }
    setEditErrors({});
    try {
      await updateProduct(editId, {
        ...editValues,
        unitPrice: Number(editValues.unitPrice),
        costPrice: Number(editValues.costPrice),
        reorderPoint: Number(editValues.reorderPoint),
        reorderQuantity: Number(editValues.reorderQuantity),
        supplierId: editValues.supplierId ? Number(editValues.supplierId) : null,
      });
      toast.success('Product updated');
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
      await deleteProduct(pendingDelete.id);
      toast.success('Product deleted');
      setPendingDelete(null);
      load();
    } catch (error) {
      toast.error(parseApiError(error));
      setPendingDelete(null);
    }
  };

  const resetFilters = () => {
    setSearch('');
    setCategory('');
    setLowStockOnly(false);
  };

  const openStockEditor = (productId) => {
    setActiveStockProductId(productId);
    if (!stockValues[productId]) {
      setStockValues((prev) => ({ ...prev, [productId]: { movementType: '', quantity: '' } }));
    }
  };

  const activeStockProduct = items.find((item) => item.id === activeStockProductId) || null;

  if (loading) return <LoadingSpinner message="Loading products..." />;

  return (
    <div className="d-flex flex-column gap-3 products-clean-shell">
      <PageHeader
        eyebrow="Inventory"
        title="Products"
        description="Clean operational view of product catalog, stock health, and supplier coverage."
        actions={[
          canAction('createProduct') ? (
            <ActionButton key="create" icon={FaPlus} onClick={() => setCreateOpen(true)}>
              New Product
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
            <SearchBar value={search} onChange={setSearch} placeholder="Search by name or SKU..." />
          </div>
          <div className="col-lg-5 col-md-4 d-flex justify-content-md-end">
            <span className="metric-chip">{items.length} products</span>
            <span className="metric-chip ms-2">{lowStockCount} low stock</span>
          </div>
        </div>
      </FilterPanel>

      <FloatingPanel open={createOpen && canAction('createProduct')} title="Create Product" onClose={() => { setCreateOpen(false); setCreateErrors({}); }}>
        <form className="row g-3 align-items-start" onSubmit={onCreate}>
          <div className="col-md-4">
            <label className="form-label text-muted small mb-1">Name</label>
            <input className={`form-control${createErrors.name ? ' is-invalid' : ''}`} placeholder="Name" value={createValues.name} onChange={(e) => setCreateValues((p) => ({ ...p, name: e.target.value }))} />
            {createErrors.name && <div className="invalid-feedback d-block">{createErrors.name}</div>}
          </div>
          <div className="col-md-4"><label className="form-label text-muted small mb-1">Category</label><select className="form-select" value={createValues.category} onChange={(e) => setCreateValues((p) => ({ ...p, category: e.target.value }))}>{CATEGORIES.map((c) => <option key={c}>{humanizeEnum(c)}</option>)}</select></div>
          <div className="col-md-4"><label className="form-label text-muted small mb-1">Supplier</label><select className="form-select" value={createValues.supplierId} onChange={(e) => setCreateValues((p) => ({ ...p, supplierId: e.target.value }))}><option value="">No Supplier</option>{suppliers.map((s) => <option key={s.id} value={s.id}>{s.name}</option>)}</select></div>
          <div className="col-md-3">
            <label className="form-label text-muted small mb-1">Unit Price</label>
            <input min="0.01" step="0.01" type="number" className={`form-control${createErrors.unitPrice ? ' is-invalid' : ''}`} value={createValues.unitPrice} onChange={(e) => setCreateValues((p) => ({ ...p, unitPrice: e.target.value }))} />
            {createErrors.unitPrice && <div className="invalid-feedback d-block">{createErrors.unitPrice}</div>}
          </div>
          <div className="col-md-3">
            <label className="form-label text-muted small mb-1">Cost Price</label>
            <input min="0.01" step="0.01" type="number" className={`form-control${createErrors.costPrice ? ' is-invalid' : ''}`} value={createValues.costPrice} onChange={(e) => setCreateValues((p) => ({ ...p, costPrice: e.target.value }))} />
            {createErrors.costPrice && <div className="invalid-feedback d-block">{createErrors.costPrice}</div>}
          </div>
          <div className="col-md-3">
            <label className="form-label text-muted small mb-1">Unit of Measure</label>
            <input className={`form-control${createErrors.unitOfMeasure ? ' is-invalid' : ''}`} value={createValues.unitOfMeasure} onChange={(e) => setCreateValues((p) => ({ ...p, unitOfMeasure: e.target.value }))} placeholder="pieces / kg / litre / box" />
            {createErrors.unitOfMeasure && <div className="invalid-feedback d-block">{createErrors.unitOfMeasure}</div>}
          </div>
          <div className="col-md-3"><label className="form-label text-muted small mb-1">Initial Quantity</label><input min="0" type="number" className="form-control" value={createValues.initialQuantityOnHand} onChange={(e) => setCreateValues((p) => ({ ...p, initialQuantityOnHand: e.target.value }))} /></div>
          <div className="col-md-6">
            <label className="form-label text-muted small mb-1">Reorder Point</label>
            <input min="0" type="number" className={`form-control${createErrors.reorderPoint ? ' is-invalid' : ''}`} value={createValues.reorderPoint} onChange={(e) => setCreateValues((p) => ({ ...p, reorderPoint: e.target.value }))} />
            {createErrors.reorderPoint && <div className="invalid-feedback d-block">{createErrors.reorderPoint}</div>}
          </div>
          <div className="col-md-6">
            <label className="form-label text-muted small mb-1">Reorder Quantity</label>
            <input min="1" type="number" className={`form-control${createErrors.reorderQuantity ? ' is-invalid' : ''}`} value={createValues.reorderQuantity} onChange={(e) => setCreateValues((p) => ({ ...p, reorderQuantity: e.target.value }))} />
            {createErrors.reorderQuantity && <div className="invalid-feedback d-block">{createErrors.reorderQuantity}</div>}
          </div>
          <div className="col-12 d-flex gap-2 justify-content-end pt-2">
            <button className="btn btn-modern btn-modern-secondary" type="button" onClick={() => { setCreateOpen(false); setCreateErrors({}); }}>Cancel</button>
            <button className="btn btn-modern btn-modern-success" type="submit">Save Product</button>
          </div>
        </form>
      </FloatingPanel>

      <FloatingPanel open={editOpen && canAction('editProduct') && Boolean(editId)} title="Update Product" onClose={() => { setEditOpen(false); setEditId(null); setEditErrors({}); }}>
        <form className="row g-3 align-items-start" onSubmit={onEditSave}>
          <div className="col-md-4">
            <label className="form-label text-muted small mb-1">Name</label>
            <input className={`form-control${editErrors.name ? ' is-invalid' : ''}`} placeholder="Name" value={editValues.name} onChange={(e) => setEditValues((p) => ({ ...p, name: e.target.value }))} />
            {editErrors.name && <div className="invalid-feedback d-block">{editErrors.name}</div>}
          </div>
          <div className="col-md-4"><label className="form-label text-muted small mb-1">Category</label><select className="form-select" value={editValues.category} onChange={(e) => setEditValues((p) => ({ ...p, category: e.target.value }))}>{CATEGORIES.map((c) => <option key={c}>{humanizeEnum(c)}</option>)}</select></div>
          <div className="col-md-4"><label className="form-label text-muted small mb-1">Supplier</label><select className="form-select" value={editValues.supplierId} onChange={(e) => setEditValues((p) => ({ ...p, supplierId: e.target.value }))}><option value="">No Supplier</option>{suppliers.map((s) => <option key={s.id} value={s.id}>{s.name}</option>)}</select></div>
          <div className="col-md-3">
            <label className="form-label text-muted small mb-1">Unit Price</label>
            <input min="0.01" step="0.01" type="number" className={`form-control${editErrors.unitPrice ? ' is-invalid' : ''}`} value={editValues.unitPrice} onChange={(e) => setEditValues((p) => ({ ...p, unitPrice: e.target.value }))} />
            {editErrors.unitPrice && <div className="invalid-feedback d-block">{editErrors.unitPrice}</div>}
          </div>
          <div className="col-md-3">
            <label className="form-label text-muted small mb-1">Cost Price</label>
            <input min="0.01" step="0.01" type="number" className={`form-control${editErrors.costPrice ? ' is-invalid' : ''}`} value={editValues.costPrice} onChange={(e) => setEditValues((p) => ({ ...p, costPrice: e.target.value }))} />
            {editErrors.costPrice && <div className="invalid-feedback d-block">{editErrors.costPrice}</div>}
          </div>
          <div className="col-md-3">
            <label className="form-label text-muted small mb-1">Unit of Measure</label>
            <input className={`form-control${editErrors.unitOfMeasure ? ' is-invalid' : ''}`} value={editValues.unitOfMeasure} onChange={(e) => setEditValues((p) => ({ ...p, unitOfMeasure: e.target.value }))} />
            {editErrors.unitOfMeasure && <div className="invalid-feedback d-block">{editErrors.unitOfMeasure}</div>}
          </div>
          <div className="col-md-3">
            <label className="form-label text-muted small mb-1">Reorder Point</label>
            <input min="0" type="number" className={`form-control${editErrors.reorderPoint ? ' is-invalid' : ''}`} value={editValues.reorderPoint} onChange={(e) => setEditValues((p) => ({ ...p, reorderPoint: e.target.value }))} />
            {editErrors.reorderPoint && <div className="invalid-feedback d-block">{editErrors.reorderPoint}</div>}
          </div>
          <div className="col-md-12">
            <label className="form-label text-muted small mb-1">Reorder Quantity</label>
            <input min="1" type="number" className={`form-control${editErrors.reorderQuantity ? ' is-invalid' : ''}`} value={editValues.reorderQuantity} onChange={(e) => setEditValues((p) => ({ ...p, reorderQuantity: e.target.value }))} />
            {editErrors.reorderQuantity && <div className="invalid-feedback d-block">{editErrors.reorderQuantity}</div>}
          </div>
          <div className="col-12 d-flex gap-2 justify-content-end pt-2">
            <button className="btn btn-modern btn-modern-secondary" type="button" onClick={() => { setEditOpen(false); setEditId(null); setEditErrors({}); }}>Cancel</button>
            <button className="btn btn-modern btn-modern-success" type="submit">Update Product</button>
          </div>
        </form>
      </FloatingPanel>

      <FilterPanel>
        <div className="row g-3 align-items-end">
          <div className="col-md-2">
            <div className="filters-label">Filters</div>
          </div>
          <div className="col-md-4">
            <label className="form-label text-muted small mb-1">Category</label>
            <select className="form-select" value={category} onChange={(e) => setCategory(e.target.value)}><option value="">All</option>{CATEGORIES.map((c) => <option key={c} value={c}>{humanizeEnum(c)}</option>)}</select>
          </div>
          <div className="col-md-4">
            <label className="form-label text-muted small mb-1">Stock Level</label>
            <div className="form-check pt-2"><input className="form-check-input" type="checkbox" checked={lowStockOnly} id="lowStockOnly" onChange={(e) => setLowStockOnly(e.target.checked)} /><label className="form-check-label" htmlFor="lowStockOnly">Low stock only</label></div>
          </div>
          <div className="col-md-2 d-flex justify-content-md-end">
            <button type="button" className="btn btn-modern btn-modern-secondary" onClick={resetFilters}><FaArrowRotateLeft className="me-1" />Clear</button>
          </div>
        </div>
      </FilterPanel>

      <FloatingPanel
        open={Boolean(activeStockProduct) && canAction('updateProductStock')}
        title={activeStockProduct ? `Update Stock - ${activeStockProduct.name}` : 'Update Stock'}
        onClose={() => setActiveStockProductId(null)}
        width="760px"
      >
        {activeStockProduct ? (
          <div className="row g-2 align-items-end">
            <div className="col-md-4">
              <label className="form-label text-muted small mb-1">Selected Product</label>
              <div className="form-control bg-light">{activeStockProduct.name} ({activeStockProduct.sku})</div>
            </div>
            <div className="col-md-4">
              <label className="form-label text-muted small mb-1">Movement Type</label>
              <select className="form-select" value={stockValues[activeStockProduct.id]?.movementType || ''} onChange={(e) => setStockValues((prev) => ({ ...prev, [activeStockProduct.id]: { ...prev[activeStockProduct.id], movementType: e.target.value } }))}>
                <option value="">Select type</option>
                {MOVEMENT_TYPES.map((type) => <option key={type} value={type}>{humanizeEnum(type)}</option>)}
              </select>
            </div>
            <div className="col-md-4">
              <label className="form-label text-muted small mb-1">Quantity</label>
              <input className="form-control" type="number" min="1" placeholder="Qty" value={stockValues[activeStockProduct.id]?.quantity || ''} onChange={(e) => setStockValues((prev) => ({ ...prev, [activeStockProduct.id]: { ...prev[activeStockProduct.id], quantity: e.target.value } }))} />
            </div>
            <div className="col-12 d-flex gap-2 justify-content-end pt-2">
              <button className="btn btn-modern btn-modern-secondary" type="button" onClick={() => setActiveStockProductId(null)}>Cancel</button>
              <button className="btn btn-modern btn-modern-primary" type="button" onClick={() => onStockUpdate(activeStockProduct.id)}>Update Stock</button>
            </div>
          </div>
        ) : null}
      </FloatingPanel>

      <DataTable
        data={sortedProducts}
        columns={[
          { key: 'sku', title: 'SKU' },
          { key: 'name', title: 'Name' },
          {
            key: 'category',
            title: 'Category',
            render: (row) => <span className="badge rounded-pill text-bg-light border">{humanizeEnum(row.category)}</span>,
          },
          { key: 'unitOfMeasure', title: 'UOM', render: (row) => row.unitOfMeasure || 'pieces' },
          { key: 'unitPrice', title: 'M.R.P', render: (row) => formatCurrency(row.unitPrice) },
          {
            key: 'quantityAvailable',
            title: 'Available',
            render: (row) => (
              <span className={effectiveAvailable(row) <= Number(row.reorderPoint || 0) ? 'stock-low fw-semibold' : ''}>
                {effectiveAvailable(row)}
              </span>
            ),
          },
          { key: 'reorderPoint', title: 'RP' },
          { key: 'reorderQuantity', title: 'RQ' },
          {
            key: 'supplierName',
            title: 'Supplier',
            render: (row) => row.supplierName || '-',
          },
        ]}
        rowActions={(row) => (
          <div className="row-action-icons" role="group" aria-label={`Actions for ${row.name}`}>
            <Link className="btn btn-sm btn-modern btn-modern-secondary icon-btn" to={`/products/${row.id}`} title="View details" aria-label={`View details for ${row.name}`}><FaEye /></Link>
            {canAction('updateProductStock') ? <button className="btn btn-sm btn-modern btn-modern-secondary icon-btn" title="Adjust stock" aria-label={`Adjust stock for ${row.name}`} onClick={() => openStockEditor(row.id)}><FaBoxesStacked /></button> : null}
            {canAction('editProduct') ? <button className="btn btn-sm btn-modern btn-modern-secondary icon-btn" title="Edit product" aria-label={`Edit ${row.name}`} onClick={() => startEdit(row)}><FaPenToSquare /></button> : null}
            {canAction('deleteProduct') ? <button className="btn btn-sm btn-modern btn-modern-danger icon-btn" title="Delete product" aria-label={`Delete ${row.name}`} onClick={() => setPendingDelete(row)}><FaTrash /></button> : null}
          </div>
        )}
      />
      {pendingDelete && (
        <div className="floating-overlay" style={{ zIndex: 2000 }}>
          <div className="floating-panel" style={{ maxWidth: '420px' }}>
            <div className="floating-panel-header">
              <span className="fw-semibold">Delete Product</span>
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
