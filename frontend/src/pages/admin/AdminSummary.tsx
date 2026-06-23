import { Link } from 'react-router-dom';
import { formatINR } from '../../utils/money';
import { useMetrics, useProducts } from '../../hooks/useProducts';
import { useTitle } from '../../hooks/useTitle';

export function AdminSummary() {
  useTitle('Admin Summary');
  const { data: metrics, isError, error } = useMetrics();
  const { data: recentData } = useProducts({ sort: 'updatedAt,desc' });
  const recent = (recentData?.products ?? []).slice(0, 5);

  const cards = metrics
    ? [
        { label: 'Active products', value: metrics.totalActive },
        { label: 'Out of stock', value: metrics.byStatus.OUT_OF_STOCK ?? 0 },
        { label: 'On sale', value: metrics.onSale },
        { label: 'Featured', value: metrics.featured },
        { label: 'Deleted', value: metrics.deleted },
      ]
    : [];

  return (
    <div className="admin-summary">
      <div className="admin-dash-head">
        <h1 className="admin-dash-title">Summary</h1>
        <Link to="/admin/products/new" className="admin-primary-btn">+ New product</Link>
      </div>

      {isError && <p className="admin-error">{(error as Error).message}</p>}

      <div className="admin-cards">
        {cards.map((c) => (
          <div key={c.label} className="admin-card">
            <span className="admin-card-value">{c.value}</span>
            <span className="admin-card-label">{c.label}</span>
          </div>
        ))}
      </div>

      <div className="admin-section">
        <div className="admin-section-head">
          <h2 className="admin-section-title">Recently edited</h2>
          <Link to="/admin/products" className="admin-seeall">View all →</Link>
        </div>
        {recent.length === 0 ? (
          <p className="admin-placeholder">No products yet.</p>
        ) : (
          <div className="admin-recent">
            {recent.map((p) => (
              <Link key={p.productNumber} to={`/admin/products/${p.productNumber}/edit`} className="admin-recent-row">
                {p.imageUrl
                  ? <img src={p.imageUrl} alt="" className="admin-thumb" />
                  : <span className="admin-thumb admin-thumb--empty" />}
                <span className="admin-recent-name">{p.title}</span>
                <span className="admin-recent-num">{p.productNumber}</span>
                <span className="admin-recent-price">{formatINR(Number(p.salePrice ?? p.price))}</span>
              </Link>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}
