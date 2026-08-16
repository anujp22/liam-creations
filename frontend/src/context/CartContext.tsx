import { createContext, useContext, useState, type ReactNode } from 'react';
import { useQueries } from '@tanstack/react-query';
import { effectivePrice, fetchProduct } from '../api/products';
import { roundMoney } from '../utils/money';
import { track } from '../utils/analytics';
import type { Product } from '../api/products';

export interface CartItem {
  product: Product;
  quantity: number;
}

/**
 * What actually gets persisted: product numbers and quantities, nothing else.
 *
 * The cart used to store a whole Product snapshot including its price, so a
 * returning customer saw — and WhatsApp-quoted — whatever the price was when they
 * first added the item, however long ago. Prices are now always re-fetched from the
 * server, so the cart cannot disagree with the catalog.
 */
type StoredCart = Record<string, number>;

interface CartContextValue {
  items: CartItem[];
  itemCount: number;
  total: number;
  /** Items that no longer exist or were taken down since they were added. */
  unavailable: string[];
  /** True while prices are being re-fetched; totals are not final yet. */
  isLoading: boolean;
  isInCart: (productNumber: string) => boolean;
  addToCart: (product: Product) => void;
  removeFromCart: (productNumber: string) => void;
  updateQuantity: (productNumber: string, quantity: number) => void;
  clearCart: () => void;
}

const CartContext = createContext<CartContextValue | null>(null);

const STORAGE_KEY = 'shaadi-cart';

/**
 * Reads the stored cart, tolerating the older format that held full Product
 * objects — an existing customer's saved cart must not break or silently empty
 * when they next visit.
 */
function loadCart(): StoredCart {
  try {
    const raw = JSON.parse(localStorage.getItem(STORAGE_KEY) ?? '{}');
    if (raw === null || typeof raw !== 'object') return {};

    const cart: StoredCart = {};
    for (const [productNumber, value] of Object.entries(raw)) {
      if (typeof value === 'number' && value > 0) {
        cart[productNumber] = value;
      } else if (value && typeof value === 'object' && 'quantity' in value) {
        // Legacy shape: { product, quantity }. Keep the quantity, drop the stale price.
        const quantity = Number((value as { quantity: unknown }).quantity);
        if (Number.isFinite(quantity) && quantity > 0) cart[productNumber] = quantity;
      }
    }
    return cart;
  } catch {
    return {};
  }
}

function saveCart(cart: StoredCart) {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(cart));
}

export function CartProvider({ children }: { children: ReactNode }) {
  const [cart, setCart] = useState<StoredCart>(loadCart);

  const productNumbers = Object.keys(cart);

  // One query per line item, so a single removed product cannot blank the whole cart.
  const results = useQueries({
    queries: productNumbers.map((productNumber) => ({
      queryKey: ['product', productNumber],
      queryFn: () => fetchProduct(productNumber),
      // A deleted product legitimately 404s; retrying just delays showing the notice.
      retry: false,
    })),
  });

  const isLoading = results.some((r) => r.isPending);

  const items: CartItem[] = [];
  const unavailable: string[] = [];

  productNumbers.forEach((productNumber, i) => {
    const result = results[i];
    if (result.data) {
      items.push({ product: result.data, quantity: cart[productNumber] });
    } else if (result.isError) {
      // Soft-deleted or removed entirely. Surfaced to the customer rather than
      // crashing or quietly dropping the line.
      unavailable.push(productNumber);
    }
  });

  const update = (next: StoredCart) => {
    saveCart(next);
    setCart(next);
  };

  const addToCart = (product: Product) => {
    track('add-to-cart', 'Add to cart');
    setCart((prev) => {
      const next = { ...prev, [product.productNumber]: (prev[product.productNumber] ?? 0) + 1 };
      saveCart(next);
      return next;
    });
  };

  const removeFromCart = (productNumber: string) => {
    setCart((prev) => {
      const next = { ...prev };
      delete next[productNumber];
      saveCart(next);
      return next;
    });
  };

  const updateQuantity = (productNumber: string, quantity: number) => {
    if (quantity <= 0) {
      removeFromCart(productNumber);
      return;
    }
    setCart((prev) => {
      // Guarded: previously this spread prev[productNumber] blindly, so calling it
      // for a key that is not in the cart produced an item with no product at all.
      if (!(productNumber in prev)) return prev;
      const next = { ...prev, [productNumber]: quantity };
      saveCart(next);
      return next;
    });
  };

  const clearCart = () => update({});

  const itemCount = items.reduce((sum, i) => sum + i.quantity, 0);
  const total = roundMoney(items.reduce((sum, i) => sum + i.quantity * effectivePrice(i.product), 0));
  const isInCart = (productNumber: string) => productNumber in cart;

  return (
    <CartContext.Provider
      value={{
        items,
        itemCount,
        total,
        unavailable,
        isLoading,
        isInCart,
        addToCart,
        removeFromCart,
        updateQuantity,
        clearCart,
      }}
    >
      {children}
    </CartContext.Provider>
  );
}

// Hook colocated with its provider by design; the fast-refresh rule only cares
// about mixed exports, which is harmless here.
// eslint-disable-next-line react-refresh/only-export-components
export function useCart() {
  const ctx = useContext(CartContext);
  if (!ctx) throw new Error('useCart must be used within CartProvider');
  return ctx;
}
