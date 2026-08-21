import { describe, expect, it } from 'vitest';
import { thumbUrl } from './images';

/**
 * The "-thumb before the extension" convention is load-bearing in three places:
 * here, and ImageVariants.java on the backend, which uses it both to write and to
 * delete the pair. Changing it in one place orphans every existing thumbnail.
 */
describe('thumbUrl', () => {
  it('inserts -thumb before the extension', () => {
    expect(thumbUrl('/uploads/products/abc123.jpg')).toBe('/uploads/products/abc123-thumb.jpg');
  });

  it('handles an uppercase extension', () => {
    expect(thumbUrl('/uploads/products/abc.JPEG')).toBe('/uploads/products/abc-thumb.JPEG');
  });

  it('returns the url unchanged when there is no extension to insert before', () => {
    // An externally hosted image has no generated thumbnail to point at.
    expect(thumbUrl('/uploads/products/no-extension')).toBe('/uploads/products/no-extension');
  });

  it('is not fooled by a dot earlier in the path', () => {
    // The dot in "v1.2" comes before the last slash, so it is not an extension.
    expect(thumbUrl('/uploads/v1.2/image')).toBe('/uploads/v1.2/image');
    expect(thumbUrl('/uploads/v1.2/image.png')).toBe('/uploads/v1.2/image-thumb.png');
  });
});
