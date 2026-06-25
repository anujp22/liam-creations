import { createContext, useContext, useState, type ReactNode } from 'react';
import { effectivePrice } from '../api/products';
import { roundMoney } from '../utils/money';
import { track } from '../utils/analytics';
import type { Product } from '../api/products';

export interface CartItem {
  product: Product;
  quantity: number;
}

type Cart = Record<string, CartItem>;

interface CartContextValue {
  items: CartItem[];
  itemCount: number;
  total: number;
  isInCart: (productNumber: string) => boolean;
  addToCart: (product: Product) => void;
  removeFromCart: (productNumber: string) => void;
  updateQuantity: (productNumber: string, quantity: number) => void;
  clearCart: () => void;
}

const CartContext = createContext<CartContextValue | null>(null);

const STORAGE_KEY = 'shaadi-cart';

function loadCart(): Cart {
  try {
    return JSON.parse(localStorage.getItem(STORAGE_KEY) ?? '{}');
  } catch {
    return {};
  }
}

function saveCart(cart: Cart) {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(cart));
}

export function CartProvider({ children }: { children: ReactNode }) {
  const [cart, setCart] = useState<Cart>(loadCart);

  const update = (next: Cart) => {
    saveCart(next);
    setCart(next);
  };

  const addToCart = (product: Product) => {
    track('add-to-cart', 'Add to cart');
    setCart(prev => {
      const existing = prev[product.productNumber];
      const next = {
        ...prev,
        [product.productNumber]: {
          product,
          quantity: existing ? existing.quantity + 1 : 1,
        },
      };
      saveCart(next);
      return next;
    });
  };

  const removeFromCart = (productNumber: string) => {
    setCart(prev => {
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
    setCart(prev => {
      const next = { ...prev, [productNumber]: { ...prev[productNumber], quantity } };
      saveCart(next);
      return next;
    });
  };

  const clearCart = () => update({});

  const items = Object.values(cart);
  const itemCount = items.reduce((sum, i) => sum + i.quantity, 0);
  const total = roundMoney(items.reduce((sum, i) => sum + i.quantity * effectivePrice(i.product), 0));
  const isInCart = (productNumber: string) => productNumber in cart;

  return (
    <CartContext.Provider value={{ items, itemCount, total, isInCart, addToCart, removeFromCart, updateQuantity, clearCart }}>
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
