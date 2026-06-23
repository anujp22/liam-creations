import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { fetchProducts } from '../../api/products';
import type { Product, ProductStatus } from '../../api/products';
import { deleteProduct, patchFeatured, patchStatus } from '../../api/admin';
import { useTitle } from '../../hooks/useTitle';

const STATUS_OPTIONS: { value: ProductStatus; label: string }[] = [
  { value: 'IN_STOCK', label: 'In stock' },
  { value: 'OUT_OF_STOCK', label: 'Out of stock' },
  { value: 'BUILT_ON_REQUEST', label: 'Built on request' },
];

const CATEGORY_LABELS: Record<string, string> = {
  BRIDAL_SAREES: 'Bridal Sarees',
  WEDDING_DECOR: 'Wedding Decor',
};

export function AdminDashboard() {
  const [products, setProducts] = useState<Product[]>([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [search, setSearch] = useState('');
  const [query, setQuery] = useState<string | undefined>(undefined);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState<string | null>(null);
  useTitle('Admin Products');

  useEffect(() => {
    const t = setTimeout(() => { setQuery(search.trim() || undefined); setPage(0); }, 400);
    return () => clearTimeout(t);
  }, [search]);

  useEffect(() => {
    setLoading(true);
    fetchProducts(undefined, undefined, page, query, 'createdAt,desc')
      .then(({ products: data, totalPages: tp }) => {
        setProducts(data);
        setTotalPages(tp);
        setError(null);
      })
      .catch((e: Error) => setError(e.message))
      .finally(() => setLoading(false));
  }, [page, query]);

  const replaceProduct = (updated: Product) =>
    setProducts((prev) => prev.map((p) => (p.productNumber === updated.productNumber ? updated : p)));

  const handleStatus = async (productNumber: string, status: ProductStatus) => {
    setBusy(productNumber);
    try {
      replaceProduct(await patchStatus(productNumber, status));
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Failed to update status.');
    } finally {
      setBusy(null);
    }
  };

  const handleFeatured = async (product: Product) => {
    setBusy(product.productNumber);
    try {
      replaceProduct(await patchFeatured(product.productNumber, !product.featured));
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Failed to update featured.');
    } finally {
      setBusy(null);
    }
  };

  const handleDelete = async (product: Product) => {
    if (!window.confirm(`Move "${product.title}" (${product.productNumber}) to Deleted? You can restore it later.`)) return;
    setBusy(product.productNumber);
    try {
      await deleteProduct(product.productNumber);
      setProducts((prev) => prev.filter((p) => p.productNumber !== product.productNumber));
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Failed to delete.');
    } finally {
      setBusy(null);
    }
  };

  return (
    <div className="admin-dash">
      <div className="admin-dash-head">
        <h1 className="admin-dash-title">Products</h1>
        <Link to="/admin/products/new" className="admin-primary-btn">+ New product</Link>
      </div>

      <input
        type="search"
        className="admin-search"
        placeholder="Search products…"
        value={search}
        onChange={(e) => setSearch(e.target.value)}
      />

      {error && <p className="admin-error">{error}</p>}
      {loading && <p className="admin-placeholder">Loading…</p>}

      {!loading && products.length === 0 && (
        <p className="admin-placeholder">No products found.</p>
      )}

      {!loading && products.length > 0 && (
        <div className="admin-table">
          <div className="admin-row admin-row--head">
            <span>Product</span>
            <span>Category</span>
            <span>Price</span>
            <span>Status</span>
            <span>Featured</span>
            <span></span>
          </div>
          {products.map((p) => (
            <div key={p.productNumber} className={`admin-row${busy === p.productNumber ? ' admin-row--busy' : ''}`}>
              <span className="admin-cell-product">
                {p.imageUrl
                  ? <img src={p.imageUrl} alt="" className="admin-thumb" />
                  : <span className="admin-thumb admin-thumb--empty" />}
                <span className="admin-cell-text">
                  <span className="admin-cell-name">{p.title}</span>
                  <span className="admin-cell-num">{p.productNumber}</span>
                </span>
              </span>
              <span className="admin-cell-muted">{CATEGORY_LABELS[p.category] ?? p.category}</span>
              <span>
                {p.salePrice != null ? (
                  <span className="admin-price-sale">
                    <span className="admin-price-was">₹{Number(p.price).toLocaleString('en-IN')}</span>
                    <span className="admin-price-now">₹{Number(p.salePrice).toLocaleString('en-IN')}</span>
                  </span>
                ) : (
                  <>₹{Number(p.price).toLocaleString('en-IN')}</>
                )}
              </span>
              <span>
                <select
                  className="admin-status-select"
                  value={p.status}
                  disabled={busy === p.productNumber}
                  onChange={(e) => handleStatus(p.productNumber, e.target.value as ProductStatus)}
                >
                  {STATUS_OPTIONS.map((o) => (
                    <option key={o.value} value={o.value}>{o.label}</option>
                  ))}
                </select>
              </span>
              <span>
                <button
                  className={`admin-star${p.featured ? ' admin-star--on' : ''}`}
                  disabled={busy === p.productNumber}
                  onClick={() => handleFeatured(p)}
                  aria-label={p.featured ? 'Unfeature' : 'Feature'}
                  title={p.featured ? 'Featured' : 'Not featured'}
                >
                  {p.featured ? '★' : '☆'}
                </button>
              </span>
              <span className="admin-cell-actions">
                <Link to={`/admin/products/${p.productNumber}/edit`} className="admin-link-btn">Edit</Link>
                <button
                  className="admin-link-btn admin-link-btn--danger"
                  disabled={busy === p.productNumber}
                  onClick={() => handleDelete(p)}
                >
                  Delete
                </button>
              </span>
            </div>
          ))}
        </div>
      )}

      {totalPages > 1 && (
        <div className="pagination">
          <button className="pagination-btn" onClick={() => setPage((p) => p - 1)} disabled={page === 0}>← Prev</button>
          <span className="pagination-info">Page {page + 1} of {totalPages}</span>
          <button className="pagination-btn" onClick={() => setPage((p) => p + 1)} disabled={page >= totalPages - 1}>Next →</button>
        </div>
      )}
    </div>
  );
}
