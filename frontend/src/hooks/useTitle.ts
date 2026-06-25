import { useEffect } from 'react';

const BRAND = 'Liams Creations';

function setMetaDescription(content: string) {
  let tag = document.querySelector<HTMLMetaElement>('meta[name="description"]');
  if (!tag) {
    tag = document.createElement('meta');
    tag.name = 'description';
    document.head.appendChild(tag);
  }
  tag.content = content;
}

/**
 * Sets a consistent, professional browser-tab title: "<page> · Liams Creations".
 * Pass the brand-only title (no separator) to show just "Liams Creations …".
 * An optional description updates the page's meta description for SEO.
 */
export function useTitle(page: string, opts: { brandOnly?: boolean; description?: string } = {}) {
  const { brandOnly, description } = opts;
  useEffect(() => {
    document.title = brandOnly ? page : `${page} · ${BRAND}`;
    if (description) setMetaDescription(description);
  }, [page, brandOnly, description]);
}
