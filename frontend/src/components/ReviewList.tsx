import { useState } from 'react';
import { useReviews } from '../hooks/useReviews';
import { StarRating } from './StarRating';

function formatDate(iso: string): string {
  return new Date(iso).toLocaleDateString('en-IN', { day: 'numeric', month: 'long', year: 'numeric' });
}

/** Approved reviews for a product, with simple pagination. */
export function ReviewList({ productNumber }: { productNumber: string }) {
  const [page, setPage] = useState(0);
  const { data, isPending, isError } = useReviews(productNumber, page);

  // Stay quiet while loading or on error so the review form below is still the focus.
  if (isPending || isError) return null;

  const reviews = data?.reviews ?? [];
  const totalPages = data?.totalPages ?? 0;
  const totalElements = data?.totalElements ?? 0;

  if (totalElements === 0) {
    return <p className="review-empty">No reviews yet — be the first to share yours.</p>;
  }

  return (
    <div className="review-list">
      <h3 className="review-list-title">Customer reviews ({totalElements})</h3>

      {reviews.map((r) => (
        <div key={r.id} className="review-item">
          <div className="review-item-head">
            <StarRating value={r.rating} size={16} />
            <span className="review-item-date">{formatDate(r.createdAt)}</span>
          </div>
          <p className="review-item-comment">{r.comment}</p>
        </div>
      ))}

      {totalPages > 1 && (
        <div className="pagination">
          <button className="pagination-btn" onClick={() => setPage((p) => p - 1)} disabled={page === 0}>← Prev</button>
          <span className="pagination-info">Page {page + 1} of {totalPages}</span>
          <button className="pagination-btn" onClick={() => setPage((p) => p + 1)} disabled={page >= totalPages - 1}>Next →</button>
        </div>
      )}
    </div>
  );
}
