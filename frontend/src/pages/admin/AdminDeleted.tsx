import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import type { Product } from '../../api/products';
import { hardDeleteProduct, restoreProduct } from '../../api/admin';
import { invalidateProductData, useDeletedProducts } from '../../hooks/useProducts';
import { useTitle } from '../../hooks/useTitle';

export function AdminDeleted() {
  const [page, setPage] = useState(0);
  const [busy, setBusy] = useState<string | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);
  const queryClient = useQueryClient();
  useTitle('Admin Deleted');

  const { data, isPending, isError, error } = useDeletedProducts(page);
  const products = data?.products ?? [];
  const totalPages = data?.totalPages ?? 0;

  const invalidate = () => invalidateProductData(queryClient);

  const restoreMutation = useMutation({ mutationFn: restoreProduct, onSuccess: invalidate });
  const purgeMutation = useMutation({ mutationFn: hardDeleteProduct, onSuccess: invalidate });

  const run = async (productNumber: string, action: () => Promise<unknown>, failMsg: string) => {
    setBusy(productNumber);
    setActionError(null);
    try {
      await action();
    } catch (e) {
      setActionError(e instanceof Error ? e.message : failMsg);
    } finally {
      setBusy(null);
    }
  };

  const handleRestore = (product: Product) =>
    run(product.productNumber, () => restoreMutation.mutateAsync(product.productNumber), 'Failed to restore.');

  const handlePurge = (product: Product) => {
    if (!window.confirm(`Permanently delete "${product.title}" (${product.productNumber})? This cannot be undone. Its number stays reserved.`)) return;
    run(product.productNumber, () => purgeMutation.mutateAsync(product.productNumber), 'Failed to delete.');
  };

  const errorMessage = actionError ?? (isError ? (error as Error).message : null);

  return (
    <div className="admin-deleted">
      <h1 className="admin-dash-title">Deleted products</h1>

      {errorMessage && <p className="admin-error">{errorMessage}</p>}
      {isPending && <p className="admin-placeholder">Loading…</p>}
      {!isPending && products.length === 0 && <p className="admin-placeholder">Nothing in the trash.</p>}

      {!isPending && products.length > 0 && (
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
