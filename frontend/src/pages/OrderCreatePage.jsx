import { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import toast from 'react-hot-toast';
import { FaPlus, FaTrash, FaArrowLeft, FaFloppyDisk } from 'react-icons/fa6';
import PageHeader from '../components/PageHeader';
import FilterPanel from '../components/FilterPanel';
import ActionButton from '../components/ActionButton';
import { createOrder } from '../services/purchaseOrderService';
import { getSuppliers } from '../services/supplierService';
import { getProducts } from '../services/productService';
import { parseApiError } from '../utils/apiError';
import { validateOrder } from '../utils/validation';

const emptyItem = { productId: '', quantityOrdered: 1, unitCost: '' };

export default function OrderCreatePage() {
  const navigate = useNavigate();
  const [loading, setLoading] = useState(false);
  const [suppliers, setSuppliers] = useState([]);
  const [products, setProducts] = useState([]);
  const [errors, setErrors] = useState({});
  const [values, setValues] = useState({
    supplierId: '',
    orderDate: new Date().toISOString().slice(0, 10),
    expectedDelivery: '',
    items: [{ ...emptyItem }],
  });

  const supplierProducts = products.filter(
    (p) => values.supplierId && String(p.supplierId) === String(values.supplierId)
  );

  useEffect(() => {
    const load = async () => {
      try {
        const [supplierData, productData] = await Promise.all([getSuppliers(), getProducts()]);
        setSuppliers(supplierData);
        setProducts(productData);
      } catch (error) {
        toast.error(parseApiError(error));
      }
    };
    load();
  }, []);

  const updateItem = (index, key, value) => {
    setValues((prev) => {
      const nextItems = [...prev.items];
      nextItems[index] = { ...nextItems[index], [key]: value };

      if (key === 'productId') {
        const selected = products.find((item) => item.id === Number(value));
        if (selected && !nextItems[index].unitCost) {
          nextItems[index].unitCost = selected.costPrice ?? '';
        }
      }

      return { ...prev, items: nextItems };
    });
  };

  const addItem = () => setValues((prev) => ({ ...prev, items: [...prev.items, { ...emptyItem }] }));
  const removeItem = (index) => setValues((prev) => ({ ...prev, items: prev.items.filter((_, i) => i !== index) }));

  const onSubmit = async (event) => {
    event.preventDefault();
    if (!values.supplierId) {
      toast.error('Supplier is required');
      return;
    }

    if (!values.items.length) {
      toast.error('At least one item is required');
      return;
    }

    const nextErrors = validateOrder(values);
    if (Object.keys(nextErrors).length) {
      setErrors(nextErrors);
      return;
    }
    setErrors({});

    try {
      setLoading(true);
      const payload = {
        supplierId: Number(values.supplierId),
        orderDate: values.orderDate,
        expectedDelivery: values.expectedDelivery || null,
        items: values.items.map((item) => ({
          productId: Number(item.productId),
          quantityOrdered: Number(item.quantityOrdered),
          unitCost: Number(item.unitCost),
          quantityReceived: null,
        })),
      };
      const created = await createOrder(payload);
      toast.success('Purchase order created');
      navigate(`/orders/${created.id}`);
    } catch (error) {
      toast.error(parseApiError(error));
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="d-flex flex-column gap-3 products-clean-shell">
      <PageHeader
        eyebrow="Procurement"
        title="Create Purchase Order"
        description="Build a purchase order with supplier, schedule, and item lines in one clean form."
        actions={<ActionButton as={Link} to="/orders" icon={FaArrowLeft} variant="secondary">Back</ActionButton>}
      />

      <FilterPanel>
        <div className="d-flex flex-wrap gap-2">
          <span className="metric-chip">{suppliers.length} suppliers</span>
          <span className="metric-chip">{products.length} products</span>
          <span className="metric-chip">{values.items.length} order lines</span>
        </div>
      </FilterPanel>

      <form className="card border-0 surface-card" onSubmit={onSubmit}>
        <div className="card-body d-flex flex-column gap-3">
          <div className="row g-3">
            <div className="col-md-4">
              <label className="form-label">Supplier</label>
              <select
                required
                className={`form-select${errors.supplierId ? ' is-invalid' : ''}`}
                value={values.supplierId}
                onChange={(e) =>
                  setValues((prev) => ({
                    ...prev,
                    supplierId: e.target.value,
                    items: [{ ...emptyItem }],
                  }))
                }
              >
                <option value="">Select supplier</option>
                {suppliers.map((item) => <option key={item.id} value={item.id}>{item.name}</option>)}
              </select>
              {errors.supplierId && <div className="invalid-feedback d-block">{errors.supplierId}</div>}
            </div>
            <div className="col-md-4">
              <label className="form-label">Order Date</label>
              <input required type="date" className={`form-control${errors.orderDate ? ' is-invalid' : ''}`} value={values.orderDate} onChange={(e) => setValues((prev) => ({ ...prev, orderDate: e.target.value }))} />
              {errors.orderDate && <div className="invalid-feedback d-block">{errors.orderDate}</div>}
            </div>
            <div className="col-md-4">
              <label className="form-label">Expected Delivery</label>
              <input type="date" className="form-control" value={values.expectedDelivery} onChange={(e) => setValues((prev) => ({ ...prev, expectedDelivery: e.target.value }))} />
            </div>
          </div>

          <div className="d-flex justify-content-between align-items-center">
            <h5 className="m-0">Items</h5>
            <button type="button" className="btn btn-sm btn-modern btn-modern-secondary" onClick={addItem}><FaPlus className="me-1" />Add Item</button>
          </div>
          <div className="small text-muted">Unit Cost is the procurement cost per unit for this PO, while Product Unit Price is the selling price used for retail.</div>

          {values.items.map((item, index) => {
            const itemErr = errors.itemErrors?.[index] || {};
            return (
              <div className="row g-2 border rounded-3 p-2 bg-white" key={index}>
                <div className="col-md-1 d-flex align-items-center text-muted fw-semibold">#{index + 1}</div>
                <div className="col-md-5">
                  <select
                    className={`form-select${itemErr.productId ? ' is-invalid' : ''}`}
                    value={item.productId}
                    onChange={(e) => updateItem(index, 'productId', e.target.value)}
                    disabled={!values.supplierId}
                  >
                    <option value="">{values.supplierId ? 'Select product' : 'Select a supplier first'}</option>
                    {supplierProducts.map((p) => <option key={p.id} value={p.id}>{p.sku} — {p.name}</option>)}
                  </select>
                  {itemErr.productId && <div className="invalid-feedback d-block">{itemErr.productId}</div>}
                </div>
                <div className="col-md-2">
                  <input min="1" type="number" className={`form-control${itemErr.quantityOrdered ? ' is-invalid' : ''}`} placeholder="Qty" value={item.quantityOrdered} onChange={(e) => updateItem(index, 'quantityOrdered', e.target.value)} />
                  {itemErr.quantityOrdered && <div className="invalid-feedback d-block">{itemErr.quantityOrdered}</div>}
                </div>
                <div className="col-md-3">
                  <input min="0.01" step="0.01" type="number" className={`form-control${itemErr.unitCost ? ' is-invalid' : ''}`} placeholder="Unit Cost (INR)" value={item.unitCost} onChange={(e) => updateItem(index, 'unitCost', e.target.value)} />
                  {itemErr.unitCost && <div className="invalid-feedback d-block">{itemErr.unitCost}</div>}
                </div>
                <div className="col-md-1"><button type="button" className="btn btn-modern btn-modern-danger icon-btn w-100" onClick={() => removeItem(index)} disabled={values.items.length === 1} aria-label="Remove line item"><FaTrash /></button></div>
              </div>
            );
          })}
        </div>
        <div className="card-footer bg-transparent border-0 d-flex justify-content-end gap-2">
          <Link to="/orders" className="btn btn-modern btn-modern-secondary">Cancel</Link>
          <button className="btn btn-modern btn-modern-success" disabled={loading}><FaFloppyDisk className="me-1" />{loading ? 'Saving...' : 'Create PO'}</button>
        </div>
      </form>
    </div>
  );
}
