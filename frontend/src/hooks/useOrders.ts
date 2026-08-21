import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { fetchNewOrderCount, listOrders, setOrderStatus } from '../api/admin';
import type { OrderStatus } from '../api/orders';

/** Admin Orders list: everything, or one status queue. */
export function useOrders(status: OrderStatus | 'ALL', page = 0) {
  return useQuery({
    queryKey: ['admin-orders', status, page],
    queryFn: () => listOrders(status, page),
    placeholderData: keepPreviousData,
  });
}

/** Count of orders still to be dealt with, for the admin badge. */
export function useNewOrderCount() {
  return useQuery({
    queryKey: ['admin-orders', 'new-count'],
    queryFn: fetchNewOrderCount,
    staleTime: 30_000,
  });
}

/**
 * Moves an order along. Invalidates every order query rather than patching the
 * cache, because a status change moves the row between filtered lists and the
 * badge count as well — reconciling all of that by hand is how stale UI happens.
 */
export function useSetOrderStatus() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ orderCode, status }: { orderCode: string; status: OrderStatus }) =>
      setOrderStatus(orderCode, status),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin-orders'] });
    },
  });
}
