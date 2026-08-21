import '@testing-library/jest-dom/vitest';
import { afterEach, vi } from 'vitest';
import { cleanup } from '@testing-library/react';

/**
 * Node 24 ships an experimental global `localStorage` that resolves to `undefined`
 * unless the process is started with `--localstorage-file`. It shadows the one jsdom
 * installs, so `localStorage` is undefined inside tests even though `sessionStorage`,
 * `window` and `document` are all present and correct.
 *
 * The app reads and writes `localStorage` directly — the cart and the saved customer
 * details both live there — so tests need a real one. This is a plain in-memory
 * implementation, which is all the app ever needs from it.
 */
function installLocalStorage() {
  if (typeof globalThis.localStorage?.setItem === 'function') return;

  const store = new Map<string, string>();
  const storage: Storage = {
    get length() {
      return store.size;
    },
    key: (index) => [...store.keys()][index] ?? null,
    getItem: (key) => (store.has(key) ? store.get(key)! : null),
    setItem: (key, value) => void store.set(key, String(value)),
    removeItem: (key) => void store.delete(key),
    clear: () => store.clear(),
  };

  Object.defineProperty(globalThis, 'localStorage', { value: storage, configurable: true });
  Object.defineProperty(window, 'localStorage', { value: storage, configurable: true });
}

installLocalStorage();

// The cart and the saved customer details persist in storage, so without this tests
// leak state into each other in whatever order they happen to run.
afterEach(() => {
  cleanup();
  localStorage.clear();
  sessionStorage.clear();
  vi.restoreAllMocks();
});
