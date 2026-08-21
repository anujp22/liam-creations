import { Link } from 'react-router-dom';
import { useTitle } from '../hooks/useTitle';

/**
 * Catch-all for unknown URLs. Rendered inside ShopLayout so the header and footer
 * stay put and the visitor can navigate out — previously any unknown path rendered
 * a completely blank page.
 */
export function NotFoundPage() {
  useTitle('Page not found');

  return (
    <div className="sale-page">
      <div className="sale-page-head">
        <h1 className="sale-page-title">Page not found</h1>
        <p className="sale-page-sub">
          That page doesn’t exist, or the piece may have been taken down.
        </p>
      </div>

      <div className="grid-message">
        <p>Nothing to show here.</p>
        <Link to="/" className="cart-back-link">← Browse the catalog</Link>
      </div>
    </div>
  );
}
