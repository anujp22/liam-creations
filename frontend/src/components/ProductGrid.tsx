import { useEffect, useState } from 'react';
import { fetchProducts } from '../api/products';
import type { Product, ProductStatus } from '../api/products';
import { ProductCard } from './ProductCard';
import { StatusFilter } from './StatusFilter';

interface FetchState {
  loading: boolean;
  error: string | null;
  products: Product[];
}

export function ProductGrid() {
  const [status, setStatus] = useState<ProductStatus | undefined>(undefined);
  const [count, setCount] = useState<number>(0);
  const [{ loading, error, products }, setFetchState] = useState<FetchState>({
    loading: true,
    error: null,
    products: [],
  });

  useEffect(() => {
    fetchProducts(status)
      .then((data) => {
        setFetchState({ loading: false, error: null, products: data });
        setCount(data.length);
      })
      .catch((e: Error) => setFetchState({ loading: false, error: e.message, products: [] }));
  }, [status]);
  useEffect(() => {
    document.title = `Instagram Catalog — ${count} products`;
  }, [count]);

  return (
    <>
      <StatusFilter active={status} onChange={setStatus} />
      {loading && <p className="grid-message">Loading products...</p>}
      {error && <p className="grid-message grid-error">{error}</p>}
      {!loading && !error && products.length === 0 && (
        <p className="grid-message">No products found.</p>
      )}
      {!loading && !error && products.length > 0 && (
        <>
          <p className="grid-count">Showing {count} products</p>
          <div className="product-grid">
            {products.map((p) => (
              <ProductCard key={p.productNumber} product={p} />
            ))}
          </div>
        </>
      )}
    </>
  );
}
