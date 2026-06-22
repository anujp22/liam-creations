import { useEffect, useState } from 'react';
import type { Product } from '../../api/products';
import { hardDeleteProduct, listDeletedProducts, restoreProduct } from '../../api/admin';

export function AdminDeleted() {
  const [products, setProducts] = useState<Product[]>([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState<string | null>(null);

  const load = (p: number) => {
    setLoading(true);
    listDeletedProducts(p)
      .then(({ products: data, totalPages: tp }) => {
        setProducts(data);
        setTotalPages(tp);
        setError(null);
      })
      .catch((e: Error) => setError(e.message))
      .finally(() => setLoading(false));
  };

  useEffect(() => { load(page); }, [page]);

  const handleRestore = async (product: Product) => {
    setBusy(product.productNumber);
    try {
      await restoreProduct(product.productNumber);
      setProducts((prev) => prev.filter((p) => p.productNumber !== product.productNumber));
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Failed to restore.');
    } finally {
      setBusy(null);
    }
  };

  const handlePurge = async (product: Product) => {
    if (!window.confirm(`Permanently delete "${product.title}" (${product.productNumber})? This cannot be undone. Its number stays reserved.`)) return;
    setBusy(product.productNumber);
    try {
      await hardDeleteProduct(product.productNumber);
      setProducts((prev) => prev.filter((p) => p.productNumber !== product.productNumber));
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Failed to delete.');
    } finally {
      setBusy(null);
    }
  };

  return (
    <div className="admin-deleted">
      <h1 className="admin-dash-title">Deleted products</h1>

      {error && <p className="admin-error">{error}</p>}
      {loading && <p className="admin-placeholder">Loading…</p>}
      {!loading && products.length === 0 && <p className="admin-placeholder">Nothing in the trash.</p>}

      {!loading && products.length > 0 && (
        <div className="admin-table">
          {products.map((p) => (
            <div key={p.productNumber} className={`admin-row admin-row--deleted${busy === p.productNumber ? ' admin-row--busy' : ''}`}>
              <span className="admin-cell-product">
                {p.imageUrl
                  ? <img src={p.imageUrl} alt="" className="admin-thumb" />
                  : <span className="admin-thumb admin-thumb--empty" />}
                <span className="admin-cell-text">
                  <span className="admin-cell-name">{p.title}</span>
                  <span className="admin-cell-num">{p.productNumber}</span>
                </span>
              </span>
              <span className="admin-cell-actions">
                <button
                  className="admin-link-btn"
                  disabled={busy === p.productNumber}
                  onClick={() => handleRestore(p)}
                >
                  Restore
                </button>
                <button
                  className="admin-link-btn admin-link-btn--danger"
                  disabled={busy === p.productNumber}
                  onClick={() => handlePurge(p)}
                >
                  Delete permanently
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
