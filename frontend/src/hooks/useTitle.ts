import { useEffect } from 'react';

const BRAND = 'Liams Creations';

/**
 * Sets a consistent, professional browser-tab title: "<page> · Liams Creations".
 * Pass the brand-only title (no separator) to show just "Liams Creations …".
 */
export function useTitle(page: string, opts: { brandOnly?: boolean } = {}) {
  useEffect(() => {
    document.title = opts.brandOnly ? page : `${page} · ${BRAND}`;
  }, [page, opts.brandOnly]);
}
