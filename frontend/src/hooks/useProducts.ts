import { keepPreviousData, useQuery, type QueryClient } from '@tanstack/react-query';
import {
  fetchProduct,
  fetchProducts,
  type ProductCategory,
  type ProductStatus,
} from '../api/products';
import { listDeletedProducts, fetchMetrics } from '../api/admin';

export interface ProductListParams {
  status?: ProductStatus;
  category?: ProductCategory;
  page?: number;
  search?: string;
  sort?: string;
  onSale?: boolean;
}

/** Paginated/filterable product list. Keeps the previous page visible while the next loads. */
export function useProducts(params: ProductListParams) {
  const { status, category, page = 0, search, sort, onSale } = params;
  return useQuery({
    queryKey: ['products', { status, category, page, search, sort, onSale }],
    queryFn: () => fetchProducts(status, category, page, search, sort, onSale),
    placeholderData: keepPreviousData,
  });
}

/** A single product by its number. */
export function useProduct(productNumber: string | undefined) {
  return useQuery({
    queryKey: ['product', productNumber],
    queryFn: () => fetchProduct(productNumber!),
    enabled: Boolean(productNumber),
  });
}

/** Soft-deleted products (admin Deleted tab). */
export function useDeletedProducts(page = 0) {
  return useQuery({
    queryKey: ['deleted-products', page],
    queryFn: () => listDeletedProducts(page),
    placeholderData: keepPreviousData,
  });
}

/** Admin inventory metrics. */
export function useMetrics() {
  return useQuery({ queryKey: ['metrics'], queryFn: fetchMetrics });
}

/**
 * Invalidate every cache a product mutation can affect: the active lists, the
 * inventory metrics, and the deleted tab. Any admin write (create/update/delete/
 * restore/status/featured/remove-sale) crosses at least one of these, so all
 * admin mutations use this to keep the whole UI consistent.
 */
export function invalidateProductData(queryClient: QueryClient) {
  queryClient.invalidateQueries({ queryKey: ['products'] });
  queryClient.invalidateQueries({ queryKey: ['metrics'] });
  queryClient.invalidateQueries({ queryKey: ['deleted-products'] });
}
