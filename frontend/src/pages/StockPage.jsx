import { useEffect, useMemo, useState } from 'react';
import toast from 'react-hot-toast';
import { FaWarehouse, FaRightLeft, FaArrowRotateRight, FaArrowRotateLeft } from 'react-icons/fa6';
import DataTable from '../components/DataTable';
import LoadingSpinner from '../components/LoadingSpinner';
import PageHeader from '../components/PageHeader';
import FilterPanel from '../components/FilterPanel';
import ActionButton from '../components/ActionButton';
import SearchBar from '../components/SearchBar';
import { getStockLevels, getStockMovements } from '../services/inventoryService';
import { getProducts } from '../services/productService';
import { formatDateTime, humanizeEnum } from '../utils/formatters';
import { parseApiError } from '../utils/apiError';
import { CATEGORIES } from '../utils/constants';

export default function StockPage() {
  const [loading, setLoading] = useState(true);
  const [stockLevels, setStockLevels] = useState([]);
  const [movements, setMovements] = useState([]);
  const [products, setProducts] = useState([]);
  const [search, setSearch] = useState('');
  const [category, setCategory] = useState('');
  const [lowStockOnly, setLowStockOnly] = useState(false);

  const load = async () => {
    try {
      setLoading(true);
      const [levels, movementRows, productRows] = await Promise.all([getStockLevels(), getStockMovements(), getProducts()]);
      setStockLevels(levels);
      setMovements(movementRows);
      setProducts(productRows);
    } catch (error) {
      toast.error(parseApiError(error));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    load();
  }, []);

  const productById = useMemo(() => new Map(products.map((item) => [item.id, item])), [products]);

  const filteredStockLevels = useMemo(() => {
    const searchTerm = search.trim().toLowerCase();
    return stockLevels.filter((row) => {
      const product = productById.get(row.productId);
      const matchesSearch = !searchTerm
        || row.productSku?.toLowerCase().includes(searchTerm)
        || row.productName?.toLowerCase().includes(searchTerm);
      const matchesCategory = !category || product?.category === category;
      const matchesLowStock = !lowStockOnly || Number(row.quantityAvailable || 0) <= Number(product?.reorderPoint || 0);
      return matchesSearch && matchesCategory && matchesLowStock;
    });
  }, [stockLevels, productById, search, category, lowStockOnly]);

  const filteredMovements = useMemo(() => {
    const visibleProductIds = new Set(filteredStockLevels.map((row) => row.productId));
    const searchTerm = search.trim().toLowerCase();
    return movements.filter((row) => {
      const movementMatchesSearch = !searchTerm
        || row.productSku?.toLowerCase().includes(searchTerm)
        || row.productName?.toLowerCase().includes(searchTerm)
        || row.referenceNumber?.toLowerCase().includes(searchTerm);
      return visibleProductIds.has(row.productId) && movementMatchesSearch;
    });
  }, [movements, filteredStockLevels, search]);

  const resetFilters = () => {
    setSearch('');
    setCategory('');
    setLowStockOnly(false);
  };

  if (loading) return <LoadingSpinner message="Loading stock data..." />;

  return (
    <div className="d-flex flex-column gap-4 products-clean-shell">
      <PageHeader
        eyebrow="Operations"
        title="Stock monitoring"
        description="Keep an eye on available quantities and movement history across the warehouse."
        actions={
          <ActionButton icon={FaArrowRotateRight} variant="secondary" onClick={load}>
            Refresh
          </ActionButton>
        }
      />

      <FilterPanel>
        <div className="row g-3 align-items-center">
          <div className="col-lg-7 col-md-8">
            <SearchBar value={search} onChange={setSearch} placeholder="Search by product name, SKU, or reference..." />
          </div>
          <div className="col-lg-5 col-md-4 d-flex justify-content-md-end">
            <button type="button" className="btn btn-modern btn-modern-secondary" onClick={resetFilters}><FaArrowRotateLeft className="me-1" />Clear</button>
          </div>
        </div>
      </FilterPanel>

      <FilterPanel>
        <div className="row g-3 align-items-end">
          <div className="col-md-4">
            <label className="form-label text-muted small mb-1">Category</label>
            <select className="form-select" value={category} onChange={(e) => setCategory(e.target.value)}>
              <option value="">All categories</option>
              {CATEGORIES.map((item) => <option key={item} value={item}>{humanizeEnum(item)}</option>)}
            </select>
          </div>
          <div className="col-md-8 d-flex align-items-end">
            <div className="form-check mb-2">
              <input id="stockLowOnly" className="form-check-input" type="checkbox" checked={lowStockOnly} onChange={(e) => setLowStockOnly(e.target.checked)} />
              <label className="form-check-label" htmlFor="stockLowOnly">Low stock only (available less than or equal to reorder point)</label>
            </div>
          </div>
        </div>
      </FilterPanel>

      <FilterPanel>
        <div className="d-flex flex-wrap gap-2">
          <span className="metric-chip">{filteredStockLevels.length} stock records</span>
          <span className="metric-chip">{filteredMovements.length} movements</span>
        </div>
      </FilterPanel>

      <div className="d-flex flex-column gap-2">
        <h5 className="d-flex align-items-center gap-2"><FaWarehouse />Stock Levels</h5>
        <DataTable
          data={filteredStockLevels}
          columns={[
            { key: 'productSku', title: 'SKU' },
            { key: 'productName', title: 'Product' },
            { key: 'warehouse', title: 'Warehouse' },
            { key: 'quantityOnHand', title: 'On Hand' },
            { key: 'quantityReserved', title: 'Reserved' },
            {
              key: 'quantityAvailable',
              title: 'Available',
              render: (row) => <span className="badge rounded-pill text-bg-light border">{row.quantityAvailable}</span>,
            },
            { key: 'lastUpdated', title: 'Last Updated', render: (row) => formatDateTime(row.lastUpdated) },
          ]}
        />
      </div>

      <div className="d-flex flex-column gap-2">
        <h5 className="d-flex align-items-center gap-2"><FaRightLeft />Stock Movement History</h5>
        <DataTable
          data={filteredMovements}
          columns={[
            { key: 'productSku', title: 'SKU' },
            { key: 'productName', title: 'Product' },
            { key: 'recordedAt', title: 'Recorded At', render: (row) => formatDateTime(row.recordedAt) },
            { key: 'movementType', title: 'Type', render: (row) => <span className="badge rounded-pill text-bg-light border">{humanizeEnum(row.movementType)}</span> },
            { key: 'quantity', title: 'Quantity' },
            { key: 'referenceNumber', title: 'Reference' },
            { key: 'recordedBy', title: 'Recorded By' },
          ]}
        />
      </div>
    </div>
  );
}
