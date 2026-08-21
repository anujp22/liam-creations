import { beforeEach, describe, expect, it, vi } from 'vitest';
import { buildWhatsAppUrl, linesFromCart, linesFromOrder } from './whatsapp';
import type { CustomerDetails } from './whatsapp';
import type { Order } from '../api/orders';
import type { CartItem } from '../context/CartContext';
import type { Product } from '../api/products';

const CUSTOMER: CustomerDetails = {
  name: 'Asha Menon',
  phone: '9876543210',
  email: 'asha@example.com',
  address: '12 Rose Street, Kochi',
  notes: 'Deliver after 6pm',
};

function product(overrides: Partial<Product> = {}): Product {
  return {
    productNumber: 'PRD-1000',
    title: 'Red Bridal Saree',
    description: 'd',
    price: 5000,
    salePrice: null,
    currency: 'INR',
    status: 'IN_STOCK',
    featured: false,
    category: 'BRIDAL_SAREES',
    ...overrides,
  };
}

function order(overrides: Partial<Order> = {}): Order {
  return {
    id: 'id',
    orderCode: 'LC-1042',
    customerName: CUSTOMER.name,
    customerPhone: CUSTOMER.phone,
    customerEmail: CUSTOMER.email,
    customerAddress: CUSTOMER.address,
    notes: CUSTOMER.notes,
    status: 'NEW',
    total: 11500,
    currency: 'INR',
    items: [
      { productNumber: 'PRD-1000', title: 'Red Bridal Saree', unitPrice: 5000, quantity: 2, lineTotal: 10000 },
      { productNumber: 'PRD-1001', title: 'Gold Mandap Drape', unitPrice: 1500, quantity: 1, lineTotal: 1500 },
    ],
    createdAt: '2026-08-21T12:00:00Z',
    updatedAt: '2026-08-21T12:00:00Z',
    ...overrides,
  };
}

/** The message is URL-encoded into the link; read it back to assert on it. */
function messageOf(url: string): string {
  return decodeURIComponent(new URL(url).searchParams.get('text') ?? '');
}

beforeEach(() => {
  vi.stubEnv('VITE_OWNER_WHATSAPP', '919876543210');
});

describe('buildWhatsAppUrl', () => {
  it('puts the order code at the top of the message', () => {
    // The code is the only thing tying this conversation to a row the owner can act
    // on. If it is missing, the message is just text again.
    const message = messageOf(
      buildWhatsAppUrl(linesFromOrder(order()), 11500, CUSTOMER, 'LC-1042'),
    );

    expect(message).toContain('*Order LC-1042*');
    expect(message.indexOf('LC-1042')).toBeLessThan(message.indexOf('CUSTOMER DETAILS'));
  });

  it('omits the order line entirely when there is no code', () => {
    const message = messageOf(buildWhatsAppUrl(linesFromOrder(order()), 11500, CUSTOMER, null));

    expect(message).not.toContain('Order LC-');
    expect(message).not.toContain('*Order');
  });

  it('sends the server total, not one recomputed here', () => {
    const message = messageOf(
      buildWhatsAppUrl(linesFromOrder(order()), 11500, CUSTOMER, 'LC-1042'),
    );

    expect(message).toContain('*Total: ₹11,500*');
    expect(message).toContain('2x Red Bridal Saree — ₹10,000');
    expect(message).toContain('1x Gold Mandap Drape — ₹1,500');
  });

  it('includes the customer details the owner needs to fulfil the order', () => {
    const message = messageOf(buildWhatsAppUrl(linesFromOrder(order()), 11500, CUSTOMER, 'LC-1042'));

    expect(message).toContain('Name: Asha Menon');
    expect(message).toContain('Phone: 9876543210');
    expect(message).toContain('Email: asha@example.com');
    expect(message).toContain('Address: 12 Rose Street, Kochi');
    expect(message).toContain('Note: Deliver after 6pm');
  });

  it('leaves out the optional fields when they are blank', () => {
    const message = messageOf(
      buildWhatsAppUrl(linesFromOrder(order()), 11500, { ...CUSTOMER, email: '', notes: '  ' }, 'LC-1042'),
    );

    expect(message).not.toContain('Email:');
    expect(message).not.toContain('Note:');
    expect(message).toContain('Phone: 9876543210');
  });

  it('addresses the owner number from the environment', () => {
    const url = buildWhatsAppUrl(linesFromOrder(order()), 11500, CUSTOMER, 'LC-1042');

    expect(url.startsWith('https://wa.me/919876543210?text=')).toBe(true);
  });

  it('refuses to build a link when the owner number is not configured', () => {
    // Silently opening wa.me with no recipient would look like it worked.
    vi.stubEnv('VITE_OWNER_WHATSAPP', '');

    expect(() => buildWhatsAppUrl(linesFromOrder(order()), 11500, CUSTOMER, 'LC-1042'))
      .toThrow(/VITE_OWNER_WHATSAPP/);
  });
});

describe('linesFromCart (the fallback used when the order could not be saved)', () => {
  const cart: CartItem[] = [
    { product: product(), quantity: 2 },
    { product: product({ productNumber: 'PRD-1001', title: 'Gold Mandap Drape', price: 2000, salePrice: 1500 }), quantity: 1 },
  ];

  it('prices lines from the cart and marks the discounted one', () => {
    expect(linesFromCart(cart)).toEqual([
      { title: 'Red Bridal Saree', quantity: 2, lineTotal: 10000, onSale: false },
      { title: 'Gold Mandap Drape', quantity: 1, lineTotal: 1500, onSale: true },
    ]);
  });

  it('renders the sale marker in the message', () => {
    const message = messageOf(buildWhatsAppUrl(linesFromCart(cart), 11500, CUSTOMER, null));

    expect(message).toContain('1x Gold Mandap Drape (sale) — ₹1,500');
  });
});

describe('linesFromOrder', () => {
  it('never claims a line was on sale', () => {
    // The stored snapshot does not record whether the price was a sale price, and
    // guessing would risk telling the customer something untrue about their own order.
    expect(linesFromOrder(order()).every((line) => line.onSale === false)).toBe(true);
  });
});
