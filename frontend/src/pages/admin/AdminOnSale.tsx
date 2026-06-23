import { useState } from 'react';
import { Link } from 'react-router-dom';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import type { Product } from '../../api/products';
import { updateProduct } from '../../api/admin';
import { formatINR } from '../../utils/money';
import { invalidateProductData, useProducts } from '../../hooks/useProducts';
import { useTitle } from '../../hooks/useTitle';

export function AdminOnSale() {
  const [busy, setBusy] = useState<string | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);
  const queryClient = useQueryClient();
  useTitle('Admin On Sale');

  const { data, isPending: loading, isError, error } = useProducts({ sort: 'updatedAt,desc', onSale: true });
  const products = data?.products ?? [];

  const removeSaleMutation = useMutation({
    mutationFn: (p: Product) =>
      updateProduct(p.productNumber, {
        title: p.title,
        description: p.description,
        price: p.price,
        salePrice: null,
        currency: p.currency,
        status: p.status,
        featured: p.featured,
        imageUrl: p.imageUrl,
        images: p.images ?? [],
        category: p.category,
      }),
    onSuccess: () => invalidateProductData(queryClient),
  });

  const removeSale = async (p: Product) => {
    if (!window.confirm(`Remove the sale price from "${p.title}"?`)) return;
    setBusy(p.productNumber);
    setActionError(null);
    try {
      await removeSaleMutation.mutateAsync(p);
    } catch (e) {
      setActionError(e instanceof Error ? e.message : 'Failed to remove sale.');
    } finally {
      setBusy(null);
    }
  };

  const errorMessage = actionError ?? (isError ? (error as Error).message : null);

  return (
    <div className="admin-onsale">
      <h1 className="admin-dash-title">On Sale</h1>

      {errorMessage && <p className="admin-error">{errorMessage}</p>}
      {loading && <p className="admin-placeholder">Loading…</p>}
      {!loading && products.length === 0 && (
        <p className="admin-placeholder">No products are on sale. Set a sale price when editing a product.</p>
      )}

      {!loading && products.length > 0 && (
        <div className="admin-table">
          {products.map((p) => (
            <div key={p.productNumber} className={`admin-row admin-row--onsale${busy === p.productNumber ? ' admin-row--busy' : ''}`}>
              <span className="admin-cell-product">
                {p.imageUrl
                  ? <img src={p.imageUrl} alt="" className="admin-thumb" />
                  : <span className="admin-thumb admin-thumb--empty" />}
                <span className="admin-cell-text">
                  <span className="admin-cell-name">{p.title}</span>
                  <span className="admin-cell-num">{p.productNumber}</span>
                </span>
              </span>
              <span className="admin-price-sale">
                <span className="admin-price-was">{formatINR(Number(p.price))}</span>
                <span className="admin-price-now">{formatINR(Number(p.salePrice))}</span>
              </span>
              <span className="admin-cell-actions">
                <Link to={`/admin/products/${p.productNumber}/edit`} className="admin-link-btn">Edit</Link>
                <button
                  className="admin-link-btn admin-link-btn--danger"
                  disabled={busy === p.productNumber}
                  onClick={() => removeSale(p)}
                >
                  Remove sale
                </button>
              </span>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
