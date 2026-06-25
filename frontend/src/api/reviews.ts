export type ReviewStatus = 'PENDING' | 'APPROVED' | 'REJECTED';

export interface Review {
  id: string;
  productNumber: string;
  rating: number;
  comment: string;
  status: ReviewStatus;
  createdAt: string;
}

export interface ReviewInput {
  rating: number;
  comment: string;
}

interface PagedResponse<T> {
  content: T[];
  page: { totalElements: number; totalPages: number; number: number; size: number };
}

export interface ReviewPage {
  reviews: Review[];
  totalPages: number;
  currentPage: number;
  totalElements: number;
}

export interface RatingSummary {
  average: number | null;
  count: number;
}

/** Approved-only average + count for several products in one call. */
export async function fetchRatingSummaries(
  productNumbers: string[],
): Promise<Record<string, RatingSummary>> {
  if (productNumbers.length === 0) return {};
  const params = new URLSearchParams();
  productNumbers.forEach((n) => params.append('productNumbers', n));
  const res = await fetch(`/api/reviews/summary?${params.toString()}`);
  if (!res.ok) throw new Error(`Failed to load ratings (${res.status})`);
  return res.json();
}

/** Approved reviews for a product, newest first. */
export async function fetchReviews(productNumber: string, page = 0): Promise<ReviewPage> {
  const res = await fetch(`/api/products/${productNumber}/reviews?page=${page}`);
  if (!res.ok) throw new Error(`Failed to load reviews (${res.status})`);
  const data: PagedResponse<Review> = await res.json();
  return {
    reviews: data.content,
    totalPages: data.page.totalPages,
    currentPage: data.page.number,
    totalElements: data.page.totalElements,
  };
}

/** Submits a review for a product. It is stored as PENDING until an admin approves it. */
export async function submitReview(productNumber: string, input: ReviewInput): Promise<Review> {
  const res = await fetch(`/api/products/${productNumber}/reviews`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(input),
  });
  if (!res.ok) {
    let message = `Could not submit review (${res.status})`;
    try {
      const body = await res.json();
      if (body?.message) message = body.message;
    } catch {
      /* non-JSON error body */
    }
    throw new Error(message);
  }
  return res.json();
}
