import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { fetchProducts } from '../../api/products';
import type { Product } from '../../api/products';
import { updateProduct } from '../../api/admin';
import { useTitle } from '../../hooks/useTitle';

export function AdminOnSale() {
  const [products, setProducts] = useState<Product[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState<string | null>(null);
  useTitle('Admin On Sale');

  useEffect(() => {
    fetchProducts(undefined, undefined, 0, undefined, 'updatedAt,desc', true)
      .then(({ products: data }) => setProducts(data))
      .catch((e: Error) => setError(e.message))
      .finally(() => setLoading(false));
  }, []);

  const removeSale = async (p: Product) => {
    if (!window.confirm(`Remove the sale price from "${p.title}"?`)) return;
    setBusy(p.productNumber);
    try {
      await updateProduct(p.productNumber, {
        title: p.title,
        description: p.description,
        price: p.price,
        salePrice: null,
        currency: p.currency,
        status: p.status,
        featured: p.featured,
        imageUrl: p.imageUrl,
        category: p.category,
      });
      setProducts((prev) => prev.filter((x) => x.productNumber !== p.productNumber));
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Failed to remove sale.');
    } finally {
      setBusy(null);
    }
  };

  return (
    <div className="admin-onsale">
      <h1 className="admin-dash-title">On Sale</h1>

      {error && <p className="admin-error">{error}</p>}
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
                <span className="admin-price-was">₹{Number(p.price).toLocaleString('en-IN')}</span>
                <span className="admin-price-now">₹{Number(p.salePrice).toLocaleString('en-IN')}</span>
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
