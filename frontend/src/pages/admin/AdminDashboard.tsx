import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import type { Product, ProductStatus } from '../../api/products';
import { deleteProduct, patchFeatured, patchStatus } from '../../api/admin';
import { useProducts } from '../../hooks/useProducts';
import { formatINR } from '../../utils/money';
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
  const [page, setPage] = useState(0);
  const [search, setSearch] = useState('');
  const [query, setQuery] = useState<string | undefined>(undefined);
  const [busy, setBusy] = useState<string | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);
  const queryClient = useQueryClient();
  useTitle('Admin Products');

  useEffect(() => {
    const t = setTimeout(() => { setQuery(search.trim() || undefined); setPage(0); }, 400);
    return () => clearTimeout(t);
  }, [search]);

  const { data, isPending, isError, error } = useProducts({ page, search: query, sort: 'createdAt,desc' });
  const products = data?.products ?? [];
  const totalPages = data?.totalPages ?? 0;

  const invalidate = () => queryClient.invalidateQueries({ queryKey: ['products'] });

  const statusMutation = useMutation({
    mutationFn: ({ productNumber, status }: { productNumber: string; status: ProductStatus }) =>
      patchStatus(productNumber, status),
    onSuccess: invalidate,
  });
  const featuredMutation = useMutation({
    mutationFn: (product: Product) => patchFeatured(product.productNumber, !product.featured),
    onSuccess: invalidate,
  });
  const deleteMutation = useMutation({
    mutationFn: (productNumber: string) => deleteProduct(productNumber),
    onSuccess: invalidate,
  });

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

  const handleStatus = (productNumber: string, status: ProductStatus) =>
    run(productNumber, () => statusMutation.mutateAsync({ productNumber, status }), 'Failed to update status.');

  const handleFeatured = (product: Product) =>
    run(product.productNumber, () => featuredMutation.mutateAsync(product), 'Failed to update featured.');

  const handleDelete = (product: Product) => {
    if (!window.confirm(`Move "${product.title}" (${product.productNumber}) to Deleted? You can restore it later.`)) return;
    run(product.productNumber, () => deleteMutation.mutateAsync(product.productNumber), 'Failed to delete.');
  };

  const errorMessage = actionError ?? (isError ? (error as Error).message : null);

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

      {errorMessage && <p className="admin-error">{errorMessage}</p>}
      {isPending && <p className="admin-placeholder">Loading…</p>}

      {!isPending && products.length === 0 && (
        <p className="admin-placeholder">No products found.</p>
      )}

      {!isPending && products.length > 0 && (
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
                    <span className="admin-price-was">{formatINR(Number(p.price))}</span>
                    <span className="admin-price-now">{formatINR(Number(p.salePrice))}</span>
                  </span>
                ) : (
                  <>{formatINR(Number(p.price))}</>
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
