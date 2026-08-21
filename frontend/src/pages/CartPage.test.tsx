import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { CartPage } from './CartPage';
import { CartProvider } from '../context/CartContext';
import type { Product } from '../api/products';

/**
 * The cart → order → WhatsApp handoff.
 *
 * <p>This is where a bug is invisible until it costs money: a wrong payload, a total
 * that disagrees with the stored order, or a backend hiccup that silently swallows the
 * sale. None of it shows up in a type check or a build.
 */

const SAREE: Product = {
  productNumber: 'PRD-1000',
  title: 'Red Bridal Saree',
  description: 'd',
  price: 5000,
  salePrice: null,
  currency: 'INR',
  status: 'IN_STOCK',
  featured: false,
  category: 'BRIDAL_SAREES',
};

const DRAPE: Product = {
  ...SAREE,
  productNumber: 'PRD-1001',
  title: 'Gold Mandap Drape',
  price: 2000,
  salePrice: 1500,
  category: 'WEDDING_DECOR',
};

const PRODUCTS: Record<string, Product> = {
  'PRD-1000': SAREE,
  'PRD-1001': DRAPE,
};

const SAVED_ORDER = {
  id: 'id',
  orderCode: 'LC-1042',
  customerName: 'Asha Menon',
  customerPhone: '9876543210',
  customerEmail: '',
  customerAddress: '12 Rose Street',
  notes: '',
  status: 'NEW',
  total: 11500,
  currency: 'INR',
  items: [
    { productNumber: 'PRD-1000', title: 'Red Bridal Saree', unitPrice: 5000, quantity: 2, lineTotal: 10000 },
    { productNumber: 'PRD-1001', title: 'Gold Mandap Drape', unitPrice: 1500, quantity: 1, lineTotal: 1500 },
  ],
  createdAt: '2026-08-21T12:00:00Z',
  updatedAt: '2026-08-21T12:00:00Z',
};

/** What the POST /api/orders stub should do this run. */
let orderResponse: { status: number; body: unknown };
let openSpy: ReturnType<typeof vi.fn>;

function jsonResponse(status: number, body: unknown) {
  return {
    ok: status >= 200 && status < 300,
    status,
    json: async () => body,
  } as Response;
}

beforeEach(() => {
  localStorage.setItem('shaadi-cart', JSON.stringify({ 'PRD-1000': 2, 'PRD-1001': 1 }));
  orderResponse = { status: 201, body: SAVED_ORDER };
  vi.stubEnv('VITE_OWNER_WHATSAPP', '919876543210');

  openSpy = vi.fn();
  vi.stubGlobal('open', openSpy);

  vi.stubGlobal(
    'fetch',
    vi.fn(async (input: RequestInfo | URL) => {
      const url = String(input);
      if (url.startsWith('/api/orders')) {
        return jsonResponse(orderResponse.status, orderResponse.body);
      }
      const match = url.match(/^\/api\/products\/([^?]+)/);
      if (match && PRODUCTS[match[1]]) return jsonResponse(200, PRODUCTS[match[1]]);
      return jsonResponse(404, { message: 'Not found' });
    }),
  );
});

function renderCart() {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  });
  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter>
        <CartProvider>
          <CartPage />
        </CartProvider>
      </MemoryRouter>
    </QueryClientProvider>,
  );
}

async function fillDetailsAndSend(user: ReturnType<typeof userEvent.setup>) {
  await user.type(screen.getByLabelText(/full name/i), 'Asha Menon');
  await user.type(screen.getByLabelText(/^phone/i), '9876543210');
  await user.type(screen.getByLabelText(/delivery address/i), '12 Rose Street');
  await user.click(screen.getByRole('button', { name: /send order via whatsapp/i }));
}

/** The message the customer would send, decoded out of the wa.me link. */
function sentMessage(): string {
  const url = new URL(openSpy.mock.calls[0][0] as string);
  return decodeURIComponent(url.searchParams.get('text') ?? '');
}

function orderRequestBody() {
  const call = (fetch as unknown as ReturnType<typeof vi.fn>).mock.calls.find(
    (args: unknown[]) => String(args[0]).startsWith('/api/orders'),
  );
  return JSON.parse((call![1] as RequestInit).body as string);
}

describe('placing an order', () => {
  it('sends only product numbers and quantities — never prices', async () => {
    // The server prices the order. A client that could name its own total would be
    // a discount button.
    const user = userEvent.setup();
    renderCart();
    await screen.findByText('Red Bridal Saree');

    await fillDetailsAndSend(user);

    await waitFor(() => expect(openSpy).toHaveBeenCalled());
    const body = orderRequestBody();
    expect(body.items).toEqual([
      { productNumber: 'PRD-1000', quantity: 2 },
      { productNumber: 'PRD-1001', quantity: 1 },
    ]);
    expect(JSON.stringify(body)).not.toMatch(/price|total/i);
  });

  it('builds the WhatsApp message from the saved order, not the cart', async () => {
    // The server is the one that prices the order, so when the two disagree — a price
    // changed between loading the cart and pressing the button — the message must carry
    // the server's figure. Otherwise the customer quotes one number and the admin screen
    // shows another.
    //
    // The disagreement here is deliberate and load-bearing: with a server total that
    // merely matched the cart, this test would pass even if the message were built
    // from the cart, which is exactly the bug it exists to catch.
    orderResponse = {
      status: 201,
      body: {
        ...SAVED_ORDER,
        total: 21498,
        items: [
          { productNumber: 'PRD-1000', title: 'Red Bridal Saree', unitPrice: 9999, quantity: 2, lineTotal: 19998 },
          { productNumber: 'PRD-1001', title: 'Gold Mandap Drape', unitPrice: 1500, quantity: 1, lineTotal: 1500 },
        ],
      },
    };
    const user = userEvent.setup();
    renderCart();
    await screen.findByText('Red Bridal Saree');

    await fillDetailsAndSend(user);

    await waitFor(() => expect(openSpy).toHaveBeenCalled());
    const message = sentMessage();
    expect(message).toContain('*Order LC-1042*');
    expect(message).toContain('*Total: ₹21,498*');
    expect(message).toContain('2x Red Bridal Saree — ₹19,998');
    expect(message).not.toContain('₹11,500');
  });

  it('shows the order code so the customer can quote it later', async () => {
    const user = userEvent.setup();
    renderCart();
    await screen.findByText('Red Bridal Saree');

    await fillDetailsAndSend(user);

    expect(await screen.findByText(/LC-1042/)).toBeInTheDocument();
  });

  it('opens WhatsApp anyway when the order could not be saved', async () => {
    // A backend hiccup must not cost the shop a sale. Losing the record is bad;
    // losing the order is worse.
    orderResponse = { status: 500, body: { message: 'Internal Server Error' } };
    const user = userEvent.setup();
    renderCart();
    await screen.findByText('Red Bridal Saree');

    await fillDetailsAndSend(user);

    await waitFor(() => expect(openSpy).toHaveBeenCalled());
    const message = sentMessage();
    expect(message).not.toContain('*Order');
    // Falls back to the cart's own prices so the owner still sees what was wanted.
    expect(message).toContain('2x Red Bridal Saree');
    expect(await screen.findByText(/could not record your order/i)).toBeInTheDocument();
  });

  it('does not open WhatsApp when an item has genuinely gone', async () => {
    // A 409 means the order cannot be fulfilled as asked. Sending it anyway would
    // hand the owner an order they have to unpick by hand.
    orderResponse = { status: 409, body: { message: 'No longer available: PRD-1001' } };
    const user = userEvent.setup();
    renderCart();
    await screen.findByText('Red Bridal Saree');

    await fillDetailsAndSend(user);

    expect(await screen.findByText(/no longer available: PRD-1001/i)).toBeInTheDocument();
    expect(openSpy).not.toHaveBeenCalled();
  });

  it('refuses to submit without a name, phone and address', async () => {
    const user = userEvent.setup();
    renderCart();
    await screen.findByText('Red Bridal Saree');

    await user.click(screen.getByRole('button', { name: /send order via whatsapp/i }));

    expect(await screen.findByText(/please enter your name/i)).toBeInTheDocument();
    expect(screen.getByText(/please enter a phone number/i)).toBeInTheDocument();
    expect(screen.getByText(/please enter a delivery address/i)).toBeInTheDocument();
    expect(fetch).not.toHaveBeenCalledWith('/api/orders', expect.anything());
    expect(openSpy).not.toHaveBeenCalled();
  });

  it('rejects a malformed email before reaching the server', async () => {
    const user = userEvent.setup();
    renderCart();
    await screen.findByText('Red Bridal Saree');

    await user.type(screen.getByLabelText(/full name/i), 'Asha');
    await user.type(screen.getByLabelText(/^phone/i), '9876543210');
    await user.type(screen.getByLabelText(/email/i), 'not-an-email');
    await user.type(screen.getByLabelText(/delivery address/i), '12 Rose Street');
    await user.click(screen.getByRole('button', { name: /send order via whatsapp/i }));

    expect(await screen.findByText(/valid email/i)).toBeInTheDocument();
    expect(openSpy).not.toHaveBeenCalled();
  });

  it('re-fetches prices rather than trusting what the cart was stored with', async () => {
    // A11: the cart holds product numbers and quantities only. This is the guard
    // that stops a price sneaking back into storage.
    renderCart();
    await screen.findByText('Red Bridal Saree');

    expect(JSON.parse(localStorage.getItem('shaadi-cart')!)).toEqual({
      'PRD-1000': 2,
      'PRD-1001': 1,
    });
    // The discounted item shows its sale price, which only the server knows.
    expect(await screen.findByText(/₹1,500 each/)).toBeInTheDocument();
  });
});
