export type ProductStatus = 'IN_STOCK' | 'OUT_OF_STOCK' | 'BUILT_ON_REQUEST';

export interface Product {
  productNumber: string;
  title: string;
  description: string;
  price: number;
  currency: string;
  status: ProductStatus;
  featured: boolean;
}

interface PagedResponse<T> {
  content: T[];
  page: {
    totalElements: number;
    totalPages: number;
    number: number;
    size: number;
  };
}

export async function fetchProducts(status?: ProductStatus): Promise<Product[]> {
  const url = status ? `/api/products?status=${status}` : '/api/products';
  const res = await fetch(url);
  if (!res.ok) throw new Error(`Failed to fetch products: ${res.status}`);
  const data: PagedResponse<Product> = await res.json();
  return data.content;
}

export async function fetchProduct(productNumber: string): Promise<Product> {
  const res = await fetch(`/api/products/${productNumber}`);
  if (!res.ok) throw new Error(`Product not found: ${productNumber}`);
  return res.json();
}
