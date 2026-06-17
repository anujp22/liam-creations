import type { ProductStatus } from '../api/products';

interface Props {
  active: ProductStatus | undefined;
  onChange: (status: ProductStatus | undefined) => void;
}

const OPTIONS: { label: string; value: ProductStatus | undefined }[] = [
  { label: 'All', value: undefined },
  { label: 'In Stock', value: 'IN_STOCK' },
  { label: 'Out of Stock', value: 'OUT_OF_STOCK' },
  { label: 'Built on Demand', value: 'BUILT_ON_REQUEST' },
];

export function StatusFilter({ active, onChange }: Props) {
  return (
    <div className="status-filter">
      {OPTIONS.map(({ label, value }) => (
        <button
          key={label}
          className={`filter-btn${active === value ? ' filter-btn--active' : ''}`}
          onClick={() => onChange(value)}
        >
          {label}
        </button>
      ))}
    </div>
  );
}
