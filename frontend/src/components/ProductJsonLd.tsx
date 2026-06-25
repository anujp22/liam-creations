import { useEffect } from 'react';
import type { Product } from '../api/products';
import type { RatingSummary } from '../api/reviews';
import { effectivePrice } from '../api/products';

const AVAILABILITY: Record<Product['status'], string> = {
  IN_STOCK: 'https://schema.org/InStock',
  OUT_OF_STOCK: 'https://schema.org/OutOfStock',
  BUILT_ON_REQUEST: 'https://schema.org/PreOrder',
};

/**
 * Injects schema.org Product structured data so Google can show price,
 * availability and (when present) a star rating in search results. Googlebot
 * renders client-side JS, so this works without server rendering.
 */
export function ProductJsonLd({ product, rating }: { product: Product; rating?: RatingSummary | null }) {
  useEffect(() => {
    const image = product.images && product.images.length > 0 ? product.images : product.imageUrl ? [product.imageUrl] : [];
    const data: Record<string, unknown> = {
      '@context': 'https://schema.org/',
      '@type': 'Product',
      name: product.title,
      description: product.description,
      image: image.map((src) => `${window.location.origin}${src}`),
      sku: product.productNumber,
      offers: {
        '@type': 'Offer',
        priceCurrency: product.currency,
        price: effectivePrice(product),
        availability: AVAILABILITY[product.status],
        url: `${window.location.origin}/products/${product.productNumber}`,
      },
    };
    if (rating && rating.count > 0 && rating.average != null) {
      data.aggregateRating = {
        '@type': 'AggregateRating',
        ratingValue: rating.average,
        reviewCount: rating.count,
      };
    }

    const script = document.createElement('script');
    script.type = 'application/ld+json';
    script.text = JSON.stringify(data);
    document.head.appendChild(script);
    return () => { document.head.removeChild(script); };
  }, [product, rating]);

  return null;
}
