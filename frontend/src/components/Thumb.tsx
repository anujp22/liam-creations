import { useState } from 'react';
import { thumbUrl } from '../utils/images';

interface Props {
  src: string;
  alt: string;
  className?: string;
}

/**
 * Shows the 400px thumbnail of a stored image instead of the full-size one.
 *
 * <p>Worth doing: a 20-product grid pulls ~4MB of web-sized images but only ~200KB of
 * thumbnails, and the catalog grid is the page most likely to be opened on mobile data.
 *
 * <p>Falls back to the full image if the thumbnail 404s. Uploads always write both
 * files, so this only covers images stored before resizing existed, or a half-finished
 * upload — but a broken image is a bad enough failure to be worth the guard.
 */
export function Thumb({ src, alt, className }: Props) {
  // Remembers which src failed rather than a bare flag, so a reused component instance
  // (a changed product in the same grid slot) retries the new thumbnail instead of
  // inheriting the previous image's failure.
  const [failedSrc, setFailedSrc] = useState<string | null>(null);

  return (
    <img
      src={failedSrc === src ? src : thumbUrl(src)}
      alt={alt}
      className={className}
      loading="lazy"
      onError={() => setFailedSrc(src)}
    />
  );
}
