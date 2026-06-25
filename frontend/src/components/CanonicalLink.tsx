import { useEffect } from 'react';
import { useLocation } from 'react-router-dom';

/**
 * Keeps a <link rel="canonical"> in sync with the current route so search
 * engines index one clean URL per page. Renders nothing.
 */
export function CanonicalLink() {
  const { pathname } = useLocation();
  useEffect(() => {
    let link = document.querySelector<HTMLLinkElement>('link[rel="canonical"]');
    if (!link) {
      link = document.createElement('link');
      link.rel = 'canonical';
      document.head.appendChild(link);
    }
    link.href = `${window.location.origin}${pathname}`;
  }, [pathname]);
  return null;
}
