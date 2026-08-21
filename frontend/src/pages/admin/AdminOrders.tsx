import { useState } from 'react';
import { ORDER_STATUSES, ORDER_STATUS_LABELS, type Order, type OrderStatus } from '../../api/orders';
import { useOrders, useSetOrderStatus } from '../../hooks/useOrders';
import { formatINR } from '../../utils/money';
import { useTitle } from '../../hooks/useTitle';

const TABS: { value: OrderStatus | 'ALL'; label: string }[] = [
  { value: 'ALL', label: 'All' },
  ...ORDER_STATUSES.map((s) => ({ value: s as OrderStatus | 'ALL', label: ORDER_STATUS_LABELS[s] })),
];

function formatDateTime(iso: string): string {
  return new Date(iso).toLocaleString('en-IN', {
    day: 'numeric',
    month: 'short',
    year: 'numeric',
    hour: 'numeric',
    minute: '2-digit',
  });
}

export function AdminOrders() {
  const [status, setStatus] = useState<OrderStatus | 'ALL'>('ALL');
  const [page, setPage] = useState(0);
  const [expanded, setExpanded] = useState<string | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);
  useTitle('Admin Orders');

  const { data, isPending, isError, error } = useOrders(status, page);
  const statusMutation = useSetOrderStatus();

  const orders = data?.orders ?? [];
  const totalPages = data?.totalPages ?? 0;
  const errorMessage = actionError ?? (isError ? (error as Error).message : null);

  const changeTab = (next: OrderStatus | 'ALL') => {
    setStatus(next);
    setPage(0);
  };

  const moveTo = async (order: Order, next: OrderStatus) => {
    if (next === order.status) return;
    // Cancelling is the one move the owner is unlikely to want by accident, and the
    // only one a customer would notice. Everything else is freely reversible.
    if (next === 'CANCELLED' && !window.confirm(`Cancel order ${order.orderCode}?`)) return;

    setActionError(null);
    try {
      await statusMutation.mutateAsync({ orderCode: order.orderCode, status: next });
    } catch (e) {
      setActionError(e instanceof Error ? e.message : 'Failed to update the order.');
    }
  };

  return (
    <div className="admin-orders">
      <h1 className="admin-dash-title">Orders</h1>

      <div className="status-filter admin-orders-tabs">
        {TABS.map((t) => (
          <button
            key={t.value}
            className={`filter-btn${status === t.value ? ' filter-btn--active' : ''}`}
            onClick={() => changeTab(t.value)}
          >
            {t.label}
          </button>
        ))}
      </div>

      {errorMessage && <p className="admin-error">{errorMessage}</p>}

      {isPending && <p className="grid-message">Loading orders…</p>}

      {!isPending && orders.length === 0 && (
        <p className="grid-message">
          {status === 'ALL'
            ? 'No orders yet. They appear here as soon as a customer sends one from the cart.'
            : `No ${ORDER_STATUS_LABELS[status as OrderStatus].toLowerCase()} orders.`}
        </p>
      )}

      <div className="admin-order-list">
        {orders.map((order) => {
          const isOpen = expanded === order.orderCode;
          return (
            <article key={order.orderCode} className="admin-order">
              <button
                type="button"
                className="admin-order-head"
                aria-expanded={isOpen}
                onClick={() => setExpanded(isOpen ? null : order.orderCode)}
              >
                <span className="admin-order-code">{order.orderCode}</span>
                <span className={`admin-order-status admin-order-status--${order.status.toLowerCase()}`}>
                  {ORDER_STATUS_LABELS[order.status]}
                </span>
                <span className="admin-order-customer">{order.customerName}</span>
                <span className="admin-order-total">{formatINR(order.total)}</span>
                <span className="admin-order-date">{formatDateTime(order.createdAt)}</span>
              </button>

              {isOpen && (
                <div className="admin-order-body">
                  <div className="admin-order-contact">
                    <p>
                      <strong>Phone:</strong>{' '}
                      {/* The owner works from a phone; make the number tappable. */}
                      <a href={`tel:${order.customerPhone}`}>{order.customerPhone}</a>
                      {' · '}
                      <a
                        href={`https://wa.me/${order.customerPhone.replace(/\D/g, '')}`}
                        target="_blank"
                        rel="noopener noreferrer"
                      >
                        WhatsApp ↗
                      </a>
                    </p>
                    {order.customerEmail && (
                      <p>
                        <strong>Email:</strong>{' '}
                        <a href={`mailto:${order.customerEmail}`}>{order.customerEmail}</a>
                      </p>
                    )}
                    <p>
                      <strong>Deliver to:</strong> {order.customerAddress}
                    </p>
                    {order.notes && (
                      <p>
                        <strong>Note:</strong> {order.notes}
                      </p>
                    )}
                  </div>

                  <table className="admin-order-items">
                    <thead>
                      <tr>
                        <th scope="col">Item</th>
                        <th scope="col">Qty</th>
                        <th scope="col">Unit</th>
                        <th scope="col">Line</th>
                      </tr>
                    </thead>
                    <tbody>
                      {order.items.map((item) => (
                        <tr key={item.productNumber}>
                          <td>
                            {item.title}{' '}
                            <span className="admin-order-sku">{item.productNumber}</span>
                          </td>
                          <td>{item.quantity}</td>
                          <td>{formatINR(item.unitPrice)}</td>
                          <td>{formatINR(item.lineTotal)}</td>
                        </tr>
                      ))}
                    </tbody>
                    <tfoot>
                      <tr>
                        <td colSpan={3}>Total</td>
                        <td>{formatINR(order.total)}</td>
                      </tr>
                    </tfoot>
                  </table>

                  {/* Prices here are what the customer was charged, not today's catalog
                      price. Saying so stops the owner "correcting" a discrepancy that
                      is actually the record working as intended. */}
                  <p className="admin-order-snapshot-note">
                    Prices as they were when this order was placed.
                  </p>

                  {/* The current status is deliberately not among the options. Rendering
                      it as a highlighted-but-disabled button made it read as the thing to
                      press, and the badge in the header already says where the order is. */}
                  <div className="admin-order-actions">
                    <span className="admin-order-actions-label">Move to:</span>
                    {ORDER_STATUSES.filter((s) => s !== order.status).map((s) => (
                      <button
                        key={s}
                        type="button"
                        className="filter-btn"
                        disabled={statusMutation.isPending}
                        onClick={() => moveTo(order, s)}
                      >
                        {ORDER_STATUS_LABELS[s]}
                      </button>
                    ))}
                  </div>
                </div>
              )}
            </article>
          );
        })}
      </div>

      {totalPages > 1 && (
        <div className="pagination">
          <button className="pagination-btn" disabled={page === 0} onClick={() => setPage((p) => p - 1)}>
            ← Previous
          </button>
          <span className="pagination-info">
            Page {page + 1} of {totalPages}
          </span>
          <button
            className="pagination-btn"
            disabled={page >= totalPages - 1}
            onClick={() => setPage((p) => p + 1)}
          >
            Next →
          </button>
        </div>
      )}
    </div>
  );
}
