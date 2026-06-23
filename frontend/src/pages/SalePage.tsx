import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { fetchProducts } from '../api/products';
import type { Product } from '../api/products';
import { ProductCard } from '../components/ProductCard';
import { useTitle } from '../hooks/useTitle';

export function SalePage() {
  const [products, setProducts] = useState<Product[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  useTitle('Sale');

  useEffect(() => {
    fetchProducts(undefined, undefined, 0, undefined, 'createdAt,desc', true)
      .then(({ products: data }) => setProducts(data))
      .catch((e: Error) => setError(e.message))
      .finally(() => setLoading(false));
  }, []);

  return (
    <div className="sale-page">
      <div className="sale-page-head">
        <h1 className="sale-page-title">On Sale</h1>
        <p className="sale-page-sub">Limited-time prices on selected pieces.</p>
      </div>

      {loading && <p className="grid-message">Loading…</p>}
      {error && <p className="grid-message grid-error">{error}</p>}
      {!loading && !error && products.length === 0 && (
        <div className="grid-message">
          <p>No items are on sale right now.</p>
          <Link to="/" className="cart-back-link">← Browse the catalog</Link>
        </div>
      )}

      {!loading && !error && products.length > 0 && (
        <div className="product-grid">
          {products.map((p) => (
            <ProductCard key={p.productNumber} product={p} />
          ))}
        </div>
      )}
    </div>
  );
}
