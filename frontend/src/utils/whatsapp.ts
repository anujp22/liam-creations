import type { CartItem } from '../context/CartContext';

export function buildWhatsAppUrl(items: CartItem[], total: number): string {
  const ownerNumber = import.meta.env.VITE_OWNER_WHATSAPP as string;

  const lines = items.map(({ product, quantity }) => {
    const lineTotal = quantity * Number(product.price);
    return `${quantity}x ${product.title} — ₹${lineTotal.toLocaleString('en-IN')}`;
  });

  const message = [
    'Hi! I would like to place an order from your catalog.',
    '',
    '*ORDER SUMMARY*',
    '─────────────────────',
    ...lines,
    '─────────────────────',
    `*Total: ₹${total.toLocaleString('en-IN')}*`,
    '',
    'Please confirm availability and share payment details. Thank you!',
  ].join('\n');

  return `https://wa.me/${ownerNumber}?text=${encodeURIComponent(message)}`;
}
