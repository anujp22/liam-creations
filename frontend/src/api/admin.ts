import type { Order, OrderStatus } from './orders';
import type { Product, ProductCategory, ProductPage, ProductStatus } from './products';
import type { Review, ReviewStatus } from './reviews';

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

// ── the session (A12) ─────────────────────────────────────────────────────────
//
// There is no token to store here any more. The admin session is a server-side
// session behind an HttpOnly cookie, which this code deliberately cannot read: the
// browser attaches it, and an XSS hole in the admin UI has nothing to steal.
//
// The price of cookies is CSRF — the browser attaches them to a cross-site request
// just as happily as to ours — so every mutating request echoes the double-submit
// token back to the server. The backend writes it as a readable XSRF-TOKEN cookie on
// any response, including the 401 the login page's session check gets.

const CSRF_COOKIE = 'XSRF-TOKEN';
const CSRF_HEADER = 'X-XSRF-TOKEN';

function readCsrfToken(): string | null {
  const match = document.cookie.split('; ').find((c) => c.startsWith(`${CSRF_COOKIE}=`));
  return match ? decodeURIComponent(match.slice(CSRF_COOKIE.length + 1)) : null;
}

/**
 * Makes sure a CSRF token exists before a write that might be the first call this page
 * ever makes. Every admin response carries the cookie — including the 401 this gets when
 * logged out — so one round trip is enough. Without it, a login submitted before the
 * provider's session check came back would be rejected with an unexplained 403.
 */
async function ensureCsrfToken(): Promise<void> {
  if (readCsrfToken()) return;
  try {
    await fetch('/api/admin/me', { credentials: 'same-origin' });
  } catch {
    /* offline — the request below will fail with a better message than this one */
  }
}

// A handler the provider registers so any 401 anywhere logs the admin out.
let onUnauthorized: (() => void) | null = null;
export function setUnauthorizedHandler(fn: (() => void) | null) {
  onUnauthorized = fn;
}

/** Cookie plus CSRF token — everything an authenticated request needs. */
function authenticatedInit(init: RequestInit = {}): RequestInit {
  const headers = new Headers(init.headers);
  const method = (init.method ?? 'GET').toUpperCase();
  // GET and HEAD are CSRF-safe, and the server does not check them.
  if (method !== 'GET' && method !== 'HEAD') {
    const token = readCsrfToken();
    if (token) headers.set(CSRF_HEADER, token);
  }
  return { ...init, headers, credentials: 'same-origin' };
}

// ── request helper ────────────────────────────────────────────────────────────

async function adminRequest<T>(path: string, init: RequestInit = {}): Promise<T> {
  const withAuth = authenticatedInit(init);
  const headers = new Headers(withAuth.headers);
  if (init.body) headers.set('Content-Type', 'application/json');

  const res = await fetch(path, { ...withAuth, headers });

  if (res.status === 401) {
    onUnauthorized?.();
    throw new AdminAuthError('Your session has expired. Please log in again.');
  }
  if (!res.ok) {
    throw new Error(await errorMessage(res, `Request failed (${res.status})`));
  }

  if (res.status === 204) return undefined as T;
  return res.json() as Promise<T>;
}

async function errorMessage(res: Response, fallback: string): Promise<string> {
  try {
    const body = await res.json();
    if (body?.message) return body.message as string;
  } catch {
    /* non-JSON error body */
  }
  return fallback;
}

// ── login / logout / session check ────────────────────────────────────────────

/** Exchanges the password for a session cookie. Returns the signed-in username. */
export async function login(username: string, password: string): Promise<string> {
  await ensureCsrfToken();
  const res = await fetch(
    '/api/admin/login',
    authenticatedInit({
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username, password }),
    }),
  );

  if (res.status === 401) throw new AdminAuthError('Invalid username or password.');
  if (!res.ok) throw new Error(await errorMessage(res, `Login failed (${res.status})`));
  const body: { username: string } = await res.json();
  return body.username;
}

/**
 * Ends the session on the server, not just in this tab. Failures are swallowed on
 * purpose: whatever happened, the owner asked to be logged out, and the UI must let
 * them go rather than trapping them in the admin because a request failed.
 */
export async function logout(): Promise<void> {
  try {
    await ensureCsrfToken();
    await fetch('/api/admin/logout', authenticatedInit({ method: 'POST' }));
  } catch {
    /* offline or server down — the local state is cleared either way */
  }
}

/**
 * Who the cookie belongs to, or null if there is no session. Also the call that seeds
 * the CSRF cookie for the login form, which is why it runs even when logged out.
 */
export async function fetchCurrentAdmin(): Promise<string | null> {
  const res = await fetch('/api/admin/me', { credentials: 'same-origin' });
  if (res.status === 401) return null;
  if (!res.ok) throw new Error(`Session check failed (${res.status})`);
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
  const form = new FormData();
  files.forEach((f) => form.append('files', f));

  // Not adminRequest: that one sets a JSON Content-Type, and multipart needs the
  // browser to write the header itself so the boundary matches the body.
  const res = await fetch('/api/admin/uploads', authenticatedInit({ method: 'POST', body: form }));

  if (res.status === 401) {
    onUnauthorized?.();
    throw new AdminAuthError('Your session has expired. Please log in again.');
  }
  if (!res.ok) {
    throw new Error(await errorMessage(res, `Upload failed (${res.status})`));
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

// ── orders ────────────────────────────────────────────────────────────────────

interface OrderPagedResponse {
  content: Order[];
  page: { totalElements: number; totalPages: number; number: number; size: number };
}

export interface AdminOrderPage {
  orders: Order[];
  totalPages: number;
  currentPage: number;
  totalElements: number;
}

/** Orders newest first. Pass a status to see only that queue. */
export async function listOrders(status: OrderStatus | 'ALL', page = 0): Promise<AdminOrderPage> {
  const query = status === 'ALL' ? `page=${page}` : `status=${status}&page=${page}`;
  const data = await adminRequest<OrderPagedResponse>(`/api/admin/orders?${query}`);
  return {
    orders: data.content,
    totalPages: data.page.totalPages,
    currentPage: data.page.number,
    totalElements: data.page.totalElements,
  };
}

/** Looked up by the code the customer quotes in WhatsApp, not the internal id. */
export function fetchOrder(orderCode: string): Promise<Order> {
  return adminRequest<Order>(`/api/admin/orders/${orderCode}`);
}

export function setOrderStatus(orderCode: string, status: OrderStatus): Promise<Order> {
  return adminRequest<Order>(`/api/admin/orders/${orderCode}/status`, {
    method: 'PATCH',
    body: JSON.stringify({ status }),
  });
}

export async function fetchNewOrderCount(): Promise<number> {
  const data = await adminRequest<{ count: number }>('/api/admin/orders/new-count');
  return data.count;
}
