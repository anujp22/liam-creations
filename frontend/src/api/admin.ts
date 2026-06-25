import type { Product, ProductCategory, ProductPage, ProductStatus } from './products';
import type { Review, ReviewStatus } from './reviews';

const TOKEN_KEY = 'lc-admin-auth';

/** Payload for create and update. productNumber is assigned by the server. */
export interface ProductInput {
  title: string;
  description: string;
  price: number;
  salePrice?: number | null;
  currency: string;
  status: ProductStatus;
  featured: boolean;
  imageUrl?: string;
  images?: string[];
  category: ProductCategory;
}

export interface Metrics {
  totalActive: number;
  byStatus: Record<string, number>;
  byCategory: Record<string, number>;
  featured: number;
  onSale: number;
  deleted: number;
}

interface PagedResponse {
  content: Product[];
  page: { totalElements: number; totalPages: number; number: number; size: number };
}

/** Thrown when the backend rejects credentials (401). */
export class AdminAuthError extends Error {}

// ── credential storage (sessionStorage: cleared when the tab closes) ──────────

export function getStoredToken(): string | null {
  return sessionStorage.getItem(TOKEN_KEY);
}

export function storeToken(token: string) {
  sessionStorage.setItem(TOKEN_KEY, token);
}

export function clearToken() {
  sessionStorage.removeItem(TOKEN_KEY);
}

export function encodeBasicToken(username: string, password: string): string {
  return btoa(`${username}:${password}`);
}

// A handler the provider registers so any 401 anywhere logs the admin out.
let onUnauthorized: (() => void) | null = null;
export function setUnauthorizedHandler(fn: (() => void) | null) {
  onUnauthorized = fn;
}

// ── request helper ────────────────────────────────────────────────────────────

async function adminRequest<T>(path: string, init: RequestInit = {}): Promise<T> {
  const token = getStoredToken();
  const headers = new Headers(init.headers);
  if (token) headers.set('Authorization', `Basic ${token}`);
  if (init.body) headers.set('Content-Type', 'application/json');

  const res = await fetch(path, { ...init, headers });

  if (res.status === 401) {
    onUnauthorized?.();
    throw new AdminAuthError('Your session has expired. Please log in again.');
  }
  if (!res.ok) {
    let message = `Request failed (${res.status})`;
    try {
      const body = await res.json();
      if (body?.message) message = body.message;
    } catch {
      /* non-JSON error body */
    }
    throw new Error(message);
  }

  if (res.status === 204) return undefined as T;
  return res.json() as Promise<T>;
}

/** Verifies credentials against GET /api/admin/me; returns the username. */
export async function verifyCredentials(token: string): Promise<string> {
  const res = await fetch('/api/admin/me', {
    headers: { Authorization: `Basic ${token}` },
  });
  if (res.status === 401) throw new AdminAuthError('Invalid username or password.');
  if (!res.ok) throw new Error(`Login check failed (${res.status})`);
  const body: { username: string } = await res.json();
  return body.username;
}

// ── product CRUD ──────────────────────────────────────────────────────────────

export function createProduct(input: ProductInput): Promise<Product> {
  return adminRequest<Product>('/api/admin/products', {
    method: 'POST',
    body: JSON.stringify(input),
  });
}

export function updateProduct(productNumber: string, input: ProductInput): Promise<Product> {
  return adminRequest<Product>(`/api/admin/products/${productNumber}`, {
    method: 'PUT',
    body: JSON.stringify(input),
  });
}

/** Soft delete — moves the product to the Deleted tab. */
export function deleteProduct(productNumber: string): Promise<void> {
  return adminRequest<void>(`/api/admin/products/${productNumber}`, { method: 'DELETE' });
}

export function restoreProduct(productNumber: string): Promise<Product> {
  return adminRequest<Product>(`/api/admin/products/${productNumber}/restore`, { method: 'POST' });
}

export function hardDeleteProduct(productNumber: string): Promise<void> {
  return adminRequest<void>(`/api/admin/products/${productNumber}/permanent`, { method: 'DELETE' });
}

export async function listDeletedProducts(page = 0): Promise<ProductPage> {
  const data = await adminRequest<PagedResponse>(`/api/admin/products/deleted?page=${page}`);
  return {
    products: data.content,
    totalPages: data.page.totalPages,
    currentPage: data.page.number,
    totalElements: data.page.totalElements,
  };
}

export function fetchMetrics(): Promise<Metrics> {
  return adminRequest<Metrics>('/api/admin/metrics');
}

// ── review moderation ─────────────────────────────────────────────────────────

interface ReviewPagedResponse {
  content: Review[];
  page: { totalElements: number; totalPages: number; number: number; size: number };
}

export interface AdminReviewPage {
  reviews: Review[];
  totalPages: number;
  currentPage: number;
  totalElements: number;
}

export async function listReviews(status: ReviewStatus, page = 0): Promise<AdminReviewPage> {
  const data = await adminRequest<ReviewPagedResponse>(`/api/admin/reviews?status=${status}&page=${page}`);
  return {
    reviews: data.content,
    totalPages: data.page.totalPages,
    currentPage: data.page.number,
    totalElements: data.page.totalElements,
  };
}

export function setReviewStatus(id: string, status: ReviewStatus): Promise<Review> {
  return adminRequest<Review>(`/api/admin/reviews/${id}`, {
    method: 'PATCH',
    body: JSON.stringify({ status }),
  });
}

export function deleteReview(id: string): Promise<void> {
  return adminRequest<void>(`/api/admin/reviews/${id}`, { method: 'DELETE' });
}

export async function fetchPendingReviewCount(): Promise<number> {
  const data = await adminRequest<{ count: number }>('/api/admin/reviews/pending-count');
  return data.count;
}

/** Uploads image files (multipart) and returns their public URLs in order. */
export async function uploadImages(files: File[]): Promise<string[]> {
  const token = getStoredToken();
  const form = new FormData();
  files.forEach((f) => form.append('files', f));

  // Note: do not set Content-Type — the browser adds the multipart boundary.
  const res = await fetch('/api/admin/uploads', {
    method: 'POST',
    headers: token ? { Authorization: `Basic ${token}` } : undefined,
    body: form,
  });

  if (res.status === 401) {
    onUnauthorized?.();
    throw new AdminAuthError('Your session has expired. Please log in again.');
  }
  if (!res.ok) {
    let message = `Upload failed (${res.status})`;
    try {
      const body = await res.json();
      if (body?.message) message = body.message;
    } catch {
      /* non-JSON error body */
    }
    throw new Error(message);
  }
  const body: { urls: string[] } = await res.json();
  return body.urls;
}

export function patchFeatured(productNumber: string, featured: boolean): Promise<Product> {
  return adminRequest<Product>(`/api/admin/products/${productNumber}/featured`, {
    method: 'PATCH',
    body: JSON.stringify({ featured }),
  });
}

export function patchStatus(productNumber: string, status: ProductStatus): Promise<Product> {
  return adminRequest<Product>(`/api/admin/products/${productNumber}/status`, {
    method: 'PATCH',
    body: JSON.stringify({ status }),
  });
}
