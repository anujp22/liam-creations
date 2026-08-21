import { effectivePrice } from '../api/products';
import { formatINR } from './money';
import type { CartItem } from '../context/CartContext';
import type { Order } from '../api/orders';

export interface CustomerDetails {
  name: string;
  phone: string;
  email: string;
  address: string;
  notes: string;
}

/** One line of the message, already priced. Both sources below reduce to this. */
interface MessageLine {
  title: string;
  quantity: number;
  lineTotal: number;
  onSale: boolean;
}

/**
 * The message built from the saved order: the server's prices, its total, and the
 * code the owner can search for. This is the path we want.
 */
export function linesFromOrder(order: Order): MessageLine[] {
  return order.items.map((item) => ({
    title: item.title,
    quantity: item.quantity,
    lineTotal: item.lineTotal,
    // The snapshot does not record whether the price was a sale price, and inventing
    // that here would risk telling the customer something untrue about their own order.
    onSale: false,
  }));
}

/**
 * The fallback: the cart's own prices, used only when saving the order failed.
 * Slightly less trustworthy — these came from a page load that may be minutes old —
 * but a backend hiccup must not cost the shop a sale.
 */
export function linesFromCart(items: CartItem[]): MessageLine[] {
  return items.map(({ product, quantity }) => ({
    title: product.title,
    quantity,
    lineTotal: quantity * effectivePrice(product),
    onSale: product.salePrice != null,
  }));
}

/**
 * Builds the wa.me link the customer's WhatsApp opens with.
 *
 * @param orderCode the saved order's code, or null when the order could not be
 *        saved. When present it heads the message, because it is the only thing
 *        tying the conversation to a row on the admin Orders screen.
 */
export function buildWhatsAppUrl(
  lines: MessageLine[],
  total: number,
  customer: CustomerDetails,
  orderCode: string | null,
): string {
  const ownerNumber = (import.meta.env.VITE_OWNER_WHATSAPP as string | undefined)?.trim();
  if (!ownerNumber) {
    throw new Error('WhatsApp ordering is not configured. Please set VITE_OWNER_WHATSAPP.');
  }

  const itemLines = lines.map(
    ({ title, quantity, lineTotal, onSale }) =>
      `${quantity}x ${title}${onSale ? ' (sale)' : ''} — ${formatINR(lineTotal)}`,
  );

  const customerLines = [
    `Name: ${customer.name}`,
    `Phone: ${customer.phone}`,
    ...(customer.email.trim() ? [`Email: ${customer.email}`] : []),
    `Address: ${customer.address}`,
    ...(customer.notes.trim() ? [`Note: ${customer.notes}`] : []),
  ];

  const message = [
    'Hi! I would like to place an order from your catalog.',
    ...(orderCode ? ['', `*Order ${orderCode}*`] : []),
    '',
    '*CUSTOMER DETAILS*',
    '─────────────────────',
    ...customerLines,
    '',
    '*ORDER SUMMARY*',
    '─────────────────────',
    ...itemLines,
    '─────────────────────',
    `*Total: ${formatINR(total)}*`,
    '',
    'Please confirm availability and share payment details. Thank you!',
  ].join('\n');

  return `https://wa.me/${ownerNumber}?text=${encodeURIComponent(message)}`;
}
