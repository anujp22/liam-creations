import { useEffect, useState } from 'react';
import type { ProductCategory } from '../api/products';
import { ProductCard } from './ProductCard';
import { CategoryFilter } from './CategoryFilter';
import { SortSelect } from './SortSelect';
import { useProducts } from '../hooks/useProducts';
import { useRatingSummaries } from '../hooks/useReviews';
import { useTitle } from '../hooks/useTitle';

export function ProductGrid() {
  const [category, setCategory] = useState<ProductCategory | undefined>(undefined);
  const [searchInput, setSearchInput] = useState('');
  const [search, setSearch] = useState<string | undefined>(undefined);
  const [sort, setSort] = useState('title,asc');
  const [page, setPage] = useState(0);
  const handleCategoryChange = (c: ProductCategory | undefined) => { setCategory(c); setPage(0); };
  const handleSortChange = (s: string) => { setSort(s); setPage(0); };
  useTitle('Liams Creations — Marriage Essentials', { brandOnly: true });

  useEffect(() => {
    const timer = setTimeout(() => {
      setSearch(searchInput.trim() || undefined);
      setPage(0);
    }, 400);
    return () => clearTimeout(timer);
  }, [searchInput]);

  const { data, isPending, isError, error } = useProducts({ category, page, search, sort });
  const products = data?.products ?? [];
  const totalPages = data?.totalPages ?? 0;
  const totalElements = data?.totalElements ?? 0;
  const { data: ratings } = useRatingSummaries(products.map((p) => p.productNumber));

  return (
    <>
      <input
        type="search"
        className="search-input"
        placeholder="Search products..."
        value={searchInput}
        onChange={(e) => setSearchInput(e.target.value)}
      />
      <div className="toolbar">
        <CategoryFilter active={category} onChange={handleCategoryChange} />
        <SortSelect value={sort} onChange={handleSortChange} />
      </div>
      {isPending && <p className="grid-message">Loading products...</p>}
      {isError && <p className="grid-message grid-error">{(error as Error).message}</p>}
      {!isPending && !isError && products.length === 0 && (
        <p className="grid-message">No products found.</p>
      )}
      {!isPending && !isError && products.length > 0 && (
        <>
          <p className="grid-count">Showing {totalElements} products</p>
          <div className="product-grid">
            {products.map((p) => (
              <ProductCard key={p.productNumber} product={p} rating={ratings?.[p.productNumber]} />
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
