/**
 * Every uploaded image is stored twice: a ~1600px web version and a 400px thumbnail.
 * The two are linked by filename rather than by a second field on the product, so the
 * thumbnail URL is derived here — this must stay in step with ImageVariants.java on
 * the backend, which builds the same name when it writes and deletes the pair.
 */
const THUMB_SUFFIX = '-thumb';

/**
 * Returns the thumbnail URL for a stored image, or the original URL when there is no
 * extension to insert before (an external image, say, which has no thumbnail).
 */
export function thumbUrl(url: string): string {
  const dot = url.lastIndexOf('.');
  const slash = url.lastIndexOf('/');
  if (dot <= slash) return url;
  return `${url.slice(0, dot)}${THUMB_SUFFIX}${url.slice(dot)}`;
}
