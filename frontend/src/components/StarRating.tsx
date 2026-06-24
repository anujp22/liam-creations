import { useState } from 'react';

interface Props {
  value: number;
  /** When provided, the stars become an interactive input. */
  onChange?: (value: number) => void;
  size?: number;
  label?: string;
}

/** Five-star rating. Read-only display, or an interactive picker when onChange is given. */
export function StarRating({ value, onChange, size = 22, label }: Props) {
  const [hover, setHover] = useState(0);
  const interactive = Boolean(onChange);
  const shown = hover || value;

  if (!interactive) {
    return (
      <span className="stars" role="img" aria-label={label ?? `${value} out of 5 stars`} style={{ fontSize: size }}>
        {[1, 2, 3, 4, 5].map((n) => (
          <span key={n} className={`star${n <= value ? ' star--on' : ''}`}>★</span>
        ))}
      </span>
    );
  }

  return (
    <span className="stars stars--input" style={{ fontSize: size }} onMouseLeave={() => setHover(0)}>
      {[1, 2, 3, 4, 5].map((n) => (
        <button
          key={n}
          type="button"
          className={`star star--btn${n <= shown ? ' star--on' : ''}`}
          aria-label={`${n} star${n > 1 ? 's' : ''}`}
          aria-pressed={value === n}
          onMouseEnter={() => setHover(n)}
          onClick={() => onChange!(n)}
        >
          ★
        </button>
      ))}
    </span>
  );
}
