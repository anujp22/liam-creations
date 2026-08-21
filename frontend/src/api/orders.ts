export type OrderStatus = 'NEW' | 'CONFIRMED' | 'PAID' | 'SHIPPED' | 'DELIVERED' | 'CANCELLED';

/** The lifecycle in the order the owner walks it, for building status controls. */
export const ORDER_STATUSES: OrderStatus[] = [
  'NEW',
  'CONFIRMED',
  'PAID',
  'SHIPPED',
  'DELIVERED',
  'CANCELLED',
];

export const ORDER_STATUS_LABELS: Record<OrderStatus, string> = {
  NEW: 'New',
  CONFIRMED: 'Confirmed',
  PAID: 'Paid',
  SHIPPED: 'Shipped',
  DELIVERED: 'Delivered',
  CANCELLED: 'Cancelled',
};

/** A stored line. These prices are the snapshot from order time, not today's. */
export interface OrderItem {
  productNumber: string;
  title: string;
  unitPrice: number;
  quantity: number;
  lineTotal: number;
}

export interface Order {
  id: string;
  orderCode: string;
  customerName: string;
  customerPhone: string;
  customerEmail: string | null;
  customerAddress: string;
  notes: string | null;
  status: OrderStatus;
  total: number;
  currency: string;
  items: OrderItem[];
  createdAt: string;
  updatedAt: string;
}

/** What the client is allowed to say: which products, and how many. Never prices. */
export interface OrderInput {
  items: { productNumber: string; quantity: number }[];
  customerName: string;
  customerPhone: string;
  customerEmail: string;
  customerAddress: string;
  notes: string;
}

/**
 * Records the order before the WhatsApp handoff and returns it with the server's
 * own prices, total and order code.
 *
 * <p>The caller builds the WhatsApp message from this response rather than from the
 * cart it already has, so the figure the customer sends is by construction the figure
 * on the admin screen.
 */
export async function placeOrder(input: OrderInput): Promise<Order> {
  const res = await fetch('/api/orders', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(input),
  });
  if (!res.ok) {
    let message = `Could not save your order (${res.status})`;
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
