import type { ProductCategory } from '../api/products';

interface Props {
  active: ProductCategory | undefined;
  onChange: (category: ProductCategory | undefined) => void;
}

const OPTIONS: { label: string; value: ProductCategory | undefined }[] = [
  { label: 'All Categories', value: undefined },
  { label: 'Bridal Sarees', value: 'BRIDAL_SAREES' },
  { label: 'Wedding Decor', value: 'WEDDING_DECOR' },
];

export function CategoryFilter({ active, onChange }: Props) {
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
