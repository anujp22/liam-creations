import { useState } from 'react';
import { Link } from 'react-router-dom';
import type { Product } from '../api/products';
import { useCart } from '../context/CartContext';
import { formatINR } from '../utils/money';

interface Props {
  product: Product;
}

/** Full image gallery: prefer the images[] list, falling back to the legacy single imageUrl. */
function galleryOf(product: Product): string[] {
  if (product.images && product.images.length > 0) return product.images;
  return product.imageUrl ? [product.imageUrl] : [];
}

export function ProductCard({ product }: Props) {
  const { addToCart, isInCart } = useCart();
  const inCart = isInCart(product.productNumber);
  const [hover, setHover] = useState(false);

  const gallery = galleryOf(product);
  const hasMultiple = gallery.length > 1;
  // On hover, peek at the second photo so shoppers see there's more than one.
  const shownImage = hover && hasMultiple ? gallery[1] : gallery[0];

  return (
    <div className="product-card">
      <Link to={`/products/${product.productNumber}`} className="product-card-link">
        {shownImage && (
          <span
            className="product-image-wrap"
            onMouseEnter={() => setHover(true)}
            onMouseLeave={() => setHover(false)}
          >
            <img src={shownImage} alt={product.title} className="product-image" />
            {hasMultiple && (
              <span className="product-photo-count" aria-label={`${gallery.length} photos`}>
                <svg viewBox="0 0 24 24" width="12" height="12" fill="currentColor" aria-hidden="true">
                  <path d="M4 6h12v10H4z" opacity="0.5" />
                  <path d="M8 4h12v10h-2V6H8z" />
                </svg>
                {gallery.length}
              </span>
            )}
          </span>
        )}
        <span className="product-badges">
          <span className={`product-status product-status--${product.status}`}>{product.status.replace(/_/g, ' ')}</span>
          {product.salePrice != null && <span className="product-sale-badge">Sale</span>}
        </span>
        <h2 className="product-title">{product.title}</h2>
        <p className="product-description">{product.description}</p>
        <div className="product-footer">
          {product.salePrice != null ? (
            <span className="product-price">
              <span className="product-price-was">{formatINR(Number(product.price))}</span>
              <span className="product-price-now">{formatINR(Number(product.salePrice))}</span>
            </span>
          ) : (
            <span className="product-price">{formatINR(Number(product.price))}</span>
          )}
          {product.featured && <span className="product-featured">Featured</span>}
        </div>
      </Link>
      <button
        className={`add-to-cart-btn${inCart ? ' add-to-cart-btn--in-cart' : ''}`}
        onClick={() => addToCart(product)}
      >
        {inCart ? '✓ Added to cart' : 'Add to cart'}
      </button>
    </div>
  );
}
