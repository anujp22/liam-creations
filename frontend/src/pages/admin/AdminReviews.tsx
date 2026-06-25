import { useState } from 'react';
import { useMutation, useQuery, useQueryClient, keepPreviousData } from '@tanstack/react-query';
import type { ReviewStatus } from '../../api/reviews';
import { deleteReview, listReviews, setReviewStatus } from '../../api/admin';
import { StarRating } from '../../components/StarRating';
import { useTitle } from '../../hooks/useTitle';

const TABS: { value: ReviewStatus; label: string }[] = [
  { value: 'PENDING', label: 'Pending' },
  { value: 'APPROVED', label: 'Approved' },
  { value: 'REJECTED', label: 'Rejected' },
];

function formatDate(iso: string): string {
  return new Date(iso).toLocaleDateString('en-IN', { day: 'numeric', month: 'long', year: 'numeric' });
}

export function AdminReviews() {
  const [status, setStatus] = useState<ReviewStatus>('PENDING');
  const [page, setPage] = useState(0);
  const [busy, setBusy] = useState<string | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);
  const queryClient = useQueryClient();
  useTitle('Admin Reviews');

  const { data, isPending, isError, error } = useQuery({
    queryKey: ['admin-reviews', status, page],
    queryFn: () => listReviews(status, page),
    placeholderData: keepPreviousData,
  });
  const reviews = data?.reviews ?? [];
  const totalPages = data?.totalPages ?? 0;

  const invalidate = () => {
    queryClient.invalidateQueries({ queryKey: ['admin-reviews'] });
    queryClient.invalidateQueries({ queryKey: ['admin-pending-reviews'] });
    // Storefront caches change too once a review is approved/rejected/removed.
    queryClient.invalidateQueries({ queryKey: ['reviews'] });
    queryClient.invalidateQueries({ queryKey: ['rating-summaries'] });
  };

  const statusMutation = useMutation({
    mutationFn: ({ id, next }: { id: string; next: ReviewStatus }) => setReviewStatus(id, next),
    onSuccess: invalidate,
  });
  const deleteMutation = useMutation({ mutationFn: deleteReview, onSuccess: invalidate });

  const run = async (id: string, action: () => Promise<unknown>, failMsg: string) => {
    setBusy(id);
    setActionError(null);
    try {
      await action();
    } catch (e) {
      setActionError(e instanceof Error ? e.message : failMsg);
    } finally {
      setBusy(null);
    }
  };

  const moveTo = (id: string, next: ReviewStatus) =>
    run(id, () => statusMutation.mutateAsync({ id, next }), 'Failed to update review.');

  const remove = (id: string) => {
    if (!window.confirm('Permanently delete this review? This cannot be undone.')) return;
    run(id, () => deleteMutation.mutateAsync(id), 'Failed to delete review.');
  };

  const changeTab = (next: ReviewStatus) => { setStatus(next); setPage(0); };
  const errorMessage = actionError ?? (isError ? (error as Error).message : null);

  return (
    <div className="admin-reviews">
      <h1 className="admin-dash-title">Reviews</h1>

      <div className="status-filter admin-reviews-tabs">
        {TABS.map((t) => (
          <button
            key={t.value}
            className={`filter-btn${status === t.value ? ' filter-btn--active' : ''}`}
            onClick={() => changeTab(t.value)}
          >
            {t.label}
          </button>
        ))}
      </div>

      {errorMessage && <p className="admin-error">{errorMessage}</p>}
      {isPending && <p className="admin-placeholder">Loading…</p>}
      {!isPending && reviews.length === 0 && (
        <p className="admin-placeholder">No {status.toLowerCase()} reviews.</p>
      )}

      {!isPending && reviews.length > 0 && (
        <div className="admin-review-list">
          {reviews.map((r) => (
            <div key={r.id} className={`admin-review${busy === r.id ? ' admin-row--busy' : ''}`}>
              <div className="admin-review-head">
                <StarRating value={r.rating} size={15} />
                <span className="admin-review-meta">{r.productNumber} · {formatDate(r.createdAt)}</span>
              </div>
              <p className="admin-review-comment">{r.comment}</p>
              <div className="admin-cell-actions">
                {r.status !== 'APPROVED' && (
                  <button className="admin-link-btn" disabled={busy === r.id} onClick={() => moveTo(r.id, 'APPROVED')}>
                    Approve
                  </button>
                )}
                {r.status !== 'REJECTED' && (
                  <button className="admin-link-btn" disabled={busy === r.id} onClick={() => moveTo(r.id, 'REJECTED')}>
                    Reject
                  </button>
                )}
                <button
                  className="admin-link-btn admin-link-btn--danger"
                  disabled={busy === r.id}
                  onClick={() => remove(r.id)}
                >
                  Delete
                </button>
              </div>
            </div>
          ))}
        </div>
      )}

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
