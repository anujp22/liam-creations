import { useMetrics } from '../../hooks/useProducts';
import { useTitle } from '../../hooks/useTitle';

const STATUS_LABELS: Record<string, string> = {
  IN_STOCK: 'In stock',
  OUT_OF_STOCK: 'Out of stock',
  BUILT_ON_REQUEST: 'Built on request',
};

const CATEGORY_LABELS: Record<string, string> = {
  BRIDAL_SAREES: 'Bridal Sarees',
  WEDDING_DECOR: 'Wedding Decor',
};

function Bars({ data, labels }: { data: Record<string, number>; labels: Record<string, string> }) {
  const entries = Object.entries(data);
  const max = Math.max(1, ...entries.map(([, v]) => v));
  return (
    <div className="admin-bars">
      {entries.map(([key, value]) => (
        <div key={key} className="admin-bar-row">
          <span className="admin-bar-label">{labels[key] ?? key}</span>
          <span className="admin-bar-track">
            <span className="admin-bar-fill" style={{ width: `${(value / max) * 100}%` }} />
          </span>
          <span className="admin-bar-value">{value}</span>
        </div>
      ))}
    </div>
  );
}

export function AdminInventory() {
  useTitle('Admin Inventory');
  const { data: metrics, isError, error } = useMetrics();

  if (isError) return <p className="admin-error">{(error as Error).message}</p>;
  if (!metrics) return <p className="admin-placeholder">Loading…</p>;

  const cards = [
    { label: 'Active products', value: metrics.totalActive },
    { label: 'Featured', value: metrics.featured },
    { label: 'On sale', value: metrics.onSale },
    { label: 'Deleted', value: metrics.deleted },
  ];

  return (
    <div className="admin-inventory">
      <h1 className="admin-dash-title">Inventory</h1>

      <div className="admin-cards">
        {cards.map((c) => (
          <div key={c.label} className="admin-card">
            <span className="admin-card-value">{c.value}</span>
            <span className="admin-card-label">{c.label}</span>
          </div>
        ))}
      </div>

      <div className="admin-section">
        <h2 className="admin-section-title">By status</h2>
        <Bars data={metrics.byStatus} labels={STATUS_LABELS} />
      </div>

      <div className="admin-section">
        <h2 className="admin-section-title">By category</h2>
        <Bars data={metrics.byCategory} labels={CATEGORY_LABELS} />
      </div>
    </div>
  );
}
