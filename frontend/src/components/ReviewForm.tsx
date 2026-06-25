import { useState, type FormEvent } from 'react';
import { submitReview } from '../api/reviews';
import { track } from '../utils/analytics';
import { StarRating } from './StarRating';

interface Props {
  productNumber: string;
  /** Called after a successful submission (e.g. to refresh the reviews list later). */
  onSubmitted?: () => void;
}

/** Public "Write a review" form: star rating + comment. Submissions await admin approval. */
export function ReviewForm({ productNumber, onSubmitted }: Props) {
  const [rating, setRating] = useState(0);
  const [comment, setComment] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [done, setDone] = useState(false);

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    if (rating < 1) { setError('Please choose a star rating.'); return; }
    if (!comment.trim()) { setError('Please write a short comment.'); return; }

    setError(null);
    setSubmitting(true);
    try {
      await submitReview(productNumber, { rating, comment: comment.trim() });
      track('review-submitted', 'Review submitted');
      setDone(true);
      onSubmitted?.();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Could not submit your review.');
    } finally {
      setSubmitting(false);
    }
  };

  if (done) {
    return (
      <div className="review-form review-form--done">
        <p className="review-thanks">Thank you! Your review will appear here once it's approved.</p>
      </div>
    );
  }

  return (
    <form className="review-form" onSubmit={handleSubmit}>
      <h3 className="review-form-title">Write a review</h3>

      <div className="review-field">
        <span className="review-label">Your rating</span>
        <StarRating value={rating} onChange={setRating} />
      </div>

      <label className="review-field">
        <span className="review-label">Your review</span>
        <textarea
          className="review-textarea"
          rows={3}
          placeholder="What did you love about this piece?"
          value={comment}
          onChange={(e) => setComment(e.target.value)}
          maxLength={2000}
        />
      </label>

      {error && <p className="review-error">{error}</p>}

      <button type="submit" className="review-submit" disabled={submitting}>
        {submitting ? 'Submitting…' : 'Submit review'}
      </button>
    </form>
  );
}
