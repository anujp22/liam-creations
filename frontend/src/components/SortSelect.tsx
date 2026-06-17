interface Props {
  value: string;
  onChange: (sort: string) => void;
}

const OPTIONS: { label: string; value: string }[] = [
  { label: 'Name A → Z',        value: 'title,asc' },
  { label: 'Name Z → A',        value: 'title,desc' },
  { label: 'Price: Low to High', value: 'price,asc' },
  { label: 'Price: High to Low', value: 'price,desc' },
  { label: 'Featured First',     value: 'featured,desc' },
  { label: 'Newest First',       value: 'createdAt,desc' },
];

export function SortSelect({ value, onChange }: Props) {
  return (
    <select
      className="sort-select"
      value={value}
      onChange={(e) => onChange(e.target.value)}
    >
      {OPTIONS.map((o) => (
        <option key={o.value} value={o.value}>{o.label}</option>
      ))}
    </select>
  );
}
