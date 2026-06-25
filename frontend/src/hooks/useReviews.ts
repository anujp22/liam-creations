import { keepPreviousData, useQuery } from '@tanstack/react-query';
import { fetchRatingSummaries, fetchReviews } from '../api/reviews';

/** Approved reviews for a product (paginated, newest first). */
export function useReviews(productNumber: string | undefined, page = 0) {
  return useQuery({
    queryKey: ['reviews', productNumber, page],
    queryFn: () => fetchReviews(productNumber!, page),
    enabled: Boolean(productNumber),
    placeholderData: keepPreviousData,
  });
}

/** Approved-only rating summaries for a set of products (one batched call). */
export function useRatingSummaries(productNumbers: string[]) {
  const key = [...productNumbers].sort().join(',');
  return useQuery({
    queryKey: ['rating-summaries', key],
    queryFn: () => fetchRatingSummaries(productNumbers),
    enabled: productNumbers.length > 0,
    staleTime: 60_000,
  });
}
