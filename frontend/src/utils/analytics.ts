/**
 * Privacy-friendly analytics via GoatCounter. Dormant until VITE_GOATCOUNTER_URL
 * is set (your own site's count URL, e.g. https://liams.goatcounter.com/count),
 * so nothing loads or sends in dev or before launch configuration.
 */

const GC_URL = (import.meta.env.VITE_GOATCOUNTER_URL as string | undefined)?.trim();

declare global {
  interface Window {
    goatcounter?: {
      count: (opts: { path: string; title?: string; event?: boolean }) => void;
    };
  }
}

let loaded = false;

/** Loads the GoatCounter script once (counts the initial pageview). */
export function initAnalytics() {
  if (loaded || !GC_URL) return;
  loaded = true;
  const script = document.createElement('script');
  script.async = true;
  script.src = '//gc.zgo.at/count.js';
  script.setAttribute('data-goatcounter', GC_URL);
  document.head.appendChild(script);
}

/** Records a single-page-app navigation (the script counts only the first load). */
export function trackPageview(path: string) {
  if (!GC_URL) return;
  window.goatcounter?.count({ path, title: document.title });
}

/** Records a custom event, e.g. a "Send order via WhatsApp" click. */
export function track(event: string, title?: string) {
  if (!GC_URL) return;
  window.goatcounter?.count({ path: event, title: title ?? event, event: true });
}
