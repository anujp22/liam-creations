import { keepPreviousData, useQuery } from '@tanstack/react-query';
import { fetchReviews } from '../api/reviews';

/** Approved reviews for a product (paginated, newest first). */
export function useReviews(productNumber: string | undefined, page = 0) {
  return useQuery({
    queryKey: ['reviews', productNumber, page],
    queryFn: () => fetchReviews(productNumber!, page),
    enabled: Boolean(productNumber),
    placeholderData: keepPreviousData,
  });
}
