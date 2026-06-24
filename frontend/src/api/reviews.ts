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
