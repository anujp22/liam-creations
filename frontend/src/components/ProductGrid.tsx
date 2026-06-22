import { useEffect, useState } from 'react';
import { fetchProducts } from '../api/products';
import type { Product, ProductCategory, ProductStatus } from '../api/products';
import { ProductCard } from './ProductCard';
import { CategoryFilter } from './CategoryFilter';
import { SortSelect } from './SortSelect';
import { StatusFilter } from './StatusFilter';

interface FetchState {
  loading: boolean;
  error: string | null;
  products: Product[];
  totalPages: number;
}

export function ProductGrid() {
  const [status, setStatus] = useState<ProductStatus | undefined>(undefined);
  const [category, setCategory] = useState<ProductCategory | undefined>(undefined);
  const [searchInput, setSearchInput] = useState('');
  const [search, setSearch] = useState<string | undefined>(undefined);
  const [sort, setSort] = useState('title,asc');
  const [page, setPage] = useState(0);
  const [count, setCount] = useState<number>(0);
  const [{ loading, error, products, totalPages }, setFetchState] = useState<FetchState>({
    loading: true,
    error: null,
    products: [],
    totalPages: 0,
  });
  const [saleItems, setSaleItems] = useState<Product[]>([]);

  useEffect(() => {
    fetchProducts(undefined, undefined, 0, undefined, 'createdAt,desc', true)
      .then(({ products: data }) => setSaleItems(data))
      .catch(() => {});
  }, []);

  const handleStatusChange = (s: ProductStatus | undefined) => { setStatus(s); setPage(0); };
  const handleCategoryChange = (c: ProductCategory | undefined) => { setCategory(c); setPage(0); };
  const handleSortChange = (s: string) => { setSort(s); setPage(0); };

  useEffect(() => {
    const timer = setTimeout(() => {
      setSearch(searchInput.trim() || undefined);
      setPage(0);
    }, 400);
    return () => clearTimeout(timer);
  }, [searchInput]);

  useEffect(() => {
    setFetchState((prev) => ({ ...prev, loading: true }));
    fetchProducts(status, category, page, search, sort)
      .then(({ products: data, totalPages: tp }) => {
        setFetchState({ loading: false, error: null, products: data, totalPages: tp });
        setCount(data.length);
      })
      .catch((e: Error) => setFetchState({ loading: false, error: e.message, products: [], totalPages: 0 }));
  }, [status, category, page, search, sort]);

  useEffect(() => {
    document.title = `Instagram Catalog — ${count} products`;
  }, [count]);

  return (
    <>
      {saleItems.length > 0 && (
        <section className="sale-section">
          <div className="sale-section-head">
            <h2 className="sale-section-title">On Sale</h2>
            <span className="sale-section-sub">Limited-time prices</span>
          </div>
          <div className="product-grid">
            {saleItems.map((p) => (
              <ProductCard key={p.productNumber} product={p} />
            ))}
          </div>
        </section>
      )}
      <input
        type="search"
        className="search-input"
        placeholder="Search products..."
        value={searchInput}
        onChange={(e) => setSearchInput(e.target.value)}
      />
      <div className="toolbar">
        <StatusFilter active={status} onChange={handleStatusChange} />
        <CategoryFilter active={category} onChange={handleCategoryChange} />
        <SortSelect value={sort} onChange={handleSortChange} />
      </div>
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
          {totalPages > 1 && (
            <div className="pagination">
              <button
                className="pagination-btn"
                onClick={() => setPage((p) => p - 1)}
                disabled={page === 0}
              >
                ← Prev
              </button>
              <span className="pagination-info">Page {page + 1} of {totalPages}</span>
              <button
                className="pagination-btn"
                onClick={() => setPage((p) => p + 1)}
                disabled={page >= totalPages - 1}
              >
                Next →
              </button>
            </div>
          )}
        </>
      )}
    </>
  );
}
