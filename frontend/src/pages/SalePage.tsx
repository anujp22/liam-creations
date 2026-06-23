import { Link } from 'react-router-dom';
import { ProductCard } from '../components/ProductCard';
import { useProducts } from '../hooks/useProducts';
import { useTitle } from '../hooks/useTitle';

export function SalePage() {
  useTitle('Sale');
  const { data, isPending: loading, isError, error } = useProducts({ sort: 'createdAt,desc', onSale: true });
  const products = data?.products ?? [];

  return (
    <div className="sale-page">
      <div className="sale-page-head">
        <h1 className="sale-page-title">On Sale</h1>
        <p className="sale-page-sub">Limited-time prices on selected pieces.</p>
      </div>

      {loading && <p className="grid-message">Loading…</p>}
      {isError && <p className="grid-message grid-error">{(error as Error).message}</p>}
      {!loading && !isError && products.length === 0 && (
        <div className="grid-message">
          <p>No items are on sale right now.</p>
          <Link to="/" className="cart-back-link">← Browse the catalog</Link>
        </div>
      )}

      {!loading && !isError && products.length > 0 && (
        <div className="product-grid">
          {products.map((p) => (
            <ProductCard key={p.productNumber} product={p} />
          ))}
        </div>
      )}
    </div>
  );
}
