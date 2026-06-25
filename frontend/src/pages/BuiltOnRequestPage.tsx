import { Link } from 'react-router-dom';
import { ProductCard } from '../components/ProductCard';
import { useProducts } from '../hooks/useProducts';
import { useRatingSummaries } from '../hooks/useReviews';
import { useTitle } from '../hooks/useTitle';

export function BuiltOnRequestPage() {
  useTitle('Built on Request');
  const { data, isPending: loading, isError, error } = useProducts({
    status: 'BUILT_ON_REQUEST',
    sort: 'createdAt,desc',
  });
  const products = data?.products ?? [];
  const { data: ratings } = useRatingSummaries(products.map((p) => p.productNumber));

  return (
    <div className="sale-page">
      <div className="sale-page-head">
        <h1 className="sale-page-title">Built on Request</h1>
        <p className="sale-page-sub">Made to order, crafted just for you.</p>
      </div>

      {loading && <p className="grid-message">Loading…</p>}
      {isError && <p className="grid-message grid-error">{(error as Error).message}</p>}
      {!loading && !isError && products.length === 0 && (
        <div className="grid-message">
          <p>Nothing here right now.</p>
          <Link to="/" className="cart-back-link">← Browse the catalog</Link>
        </div>
      )}

      {!loading && !isError && products.length > 0 && (
        <div className="product-grid">
          {products.map((p) => (
            <ProductCard key={p.productNumber} product={p} rating={ratings?.[p.productNumber]} />
          ))}
        </div>
      )}
    </div>
  );
}
