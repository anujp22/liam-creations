export type ProductStatus = 'IN_STOCK' | 'OUT_OF_STOCK' | 'BUILT_ON_REQUEST';

export type ProductCategory =
  | 'BRIDAL_SAREES'
  | 'BRIDAL_LEHENGAS'
  | 'HALDI_MEHENDI'
  | 'JEWELLERY'
  | 'CLAY_POTTERY'
  | 'PUJA_RITUALS'
  | 'WEDDING_DECOR'
  | 'SWEETS_GIFTS';

export interface Product {
  productNumber: string;
  title: string;
  description: string;
  price: number;
  currency: string;
  status: ProductStatus;
  featured: boolean;
  imageUrl?: string;
  category: ProductCategory;
  createdAt?: string;
  updatedAt?: string;
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

export interface ProductPage {
  products: Product[];
  totalPages: number;
  currentPage: number;
}

export async function fetchProducts(
  status?: ProductStatus,
  category?: ProductCategory,
  page = 0,
  search?: string,
  sort?: string
): Promise<ProductPage> {
  const params = new URLSearchParams();
  if (status) params.set('status', status);
  if (category) params.set('category', category);
  if (search) params.set('search', search);
  if (sort) params.set('sort', sort);
  params.set('page', String(page));
  const url = `/api/products?${params.toString()}`;
  const res = await fetch(url);
  if (!res.ok) throw new Error(`Failed to fetch products: ${res.status}`);
  const data: PagedResponse<Product> = await res.json();
  return { products: data.content, totalPages: data.page.totalPages, currentPage: data.page.number };
}

export async function fetchProduct(productNumber: string): Promise<Product> {
  const res = await fetch(`/api/products/${productNumber}`);
  if (!res.ok) throw new Error(`Product not found: ${productNumber}`);
  return res.json();
}
