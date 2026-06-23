import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { fetchProducts } from '../api/products';
import type { Product } from '../api/products';
import { ProductCard } from '../components/ProductCard';
import { useTitle } from '../hooks/useTitle';

export function BuiltOnRequestPage() {
  const [products, setProducts] = useState<Product[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  useTitle('Built on Request');

  useEffect(() => {
    fetchProducts('BUILT_ON_REQUEST', undefined, 0, undefined, 'createdAt,desc')
      .then(({ products: data }) => setProducts(data))
      .catch((e: Error) => setError(e.message))
      .finally(() => setLoading(false));
  }, []);

  return (
    <div className="sale-page">
      <div className="sale-page-head">
        <h1 className="sale-page-title">Built on Request</h1>
        <p className="sale-page-sub">Made to order, crafted just for you.</p>
      </div>

      {loading && <p className="grid-message">Loading…</p>}
      {error && <p className="grid-message grid-error">{error}</p>}
      {!loading && !error && products.length === 0 && (
        <div className="grid-message">
          <p>Nothing here right now.</p>
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
