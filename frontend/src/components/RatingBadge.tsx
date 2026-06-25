import type { RatingSummary } from '../api/reviews';
import { StarRating } from './StarRating';

interface Props {
  rating?: RatingSummary | null;
  size?: number;
}

/** Compact average-rating display: stars + "4.5 (12)". Renders nothing with no reviews. */
export function RatingBadge({ rating, size = 15 }: Props) {
  if (!rating || rating.count === 0 || rating.average == null) return null;
  return (
    <span className="rating-badge">
      <StarRating value={Math.round(rating.average)} size={size} label={`${rating.average.toFixed(1)} out of 5`} />
      <span className="rating-badge-text">
        {rating.average.toFixed(1)} <span className="rating-badge-count">({rating.count})</span>
      </span>
    </span>
  );
}
