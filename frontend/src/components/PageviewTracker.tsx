import { useEffect, useRef } from 'react';
import { useLocation } from 'react-router-dom';
import { trackPageview } from '../utils/analytics';

/** Reports SPA route changes to analytics. The initial load is counted by the script. */
export function PageviewTracker() {
  const { pathname } = useLocation();
  const first = useRef(true);
  useEffect(() => {
    if (first.current) {
      first.current = false;
      return;
    }
    trackPageview(pathname);
  }, [pathname]);
  return null;
}
