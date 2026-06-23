import { effectivePrice } from '../api/products';
import { formatINR } from './money';
import type { CartItem } from '../context/CartContext';

export interface CustomerDetails {
  name: string;
  phone: string;
  email: string;
  address: string;
  notes: string;
}

export function buildWhatsAppUrl(items: CartItem[], total: number, customer: CustomerDetails): string {
  const ownerNumber = (import.meta.env.VITE_OWNER_WHATSAPP as string | undefined)?.trim();
  if (!ownerNumber) {
    throw new Error('WhatsApp ordering is not configured. Please set VITE_OWNER_WHATSAPP.');
  }

  const lines = items.map(({ product, quantity }) => {
    const lineTotal = quantity * effectivePrice(product);
    const onSale = product.salePrice != null ? ' (sale)' : '';
    return `${quantity}x ${product.title}${onSale} — ${formatINR(lineTotal)}`;
  });

  const customerLines = [
    `Name: ${customer.name}`,
    `Phone: ${customer.phone}`,
    ...(customer.email.trim() ? [`Email: ${customer.email}`] : []),
    `Address: ${customer.address}`,
    ...(customer.notes.trim() ? [`Note: ${customer.notes}`] : []),
  ];

  const message = [
    'Hi! I would like to place an order from your catalog.',
    '',
    '*CUSTOMER DETAILS*',
    '─────────────────────',
    ...customerLines,
    '',
    '*ORDER SUMMARY*',
    '─────────────────────',
    ...lines,
    '─────────────────────',
    `*Total: ${formatINR(total)}*`,
    '',
    'Please confirm availability and share payment details. Thank you!',
  ].join('\n');

  return `https://wa.me/${ownerNumber}?text=${encodeURIComponent(message)}`;
}
