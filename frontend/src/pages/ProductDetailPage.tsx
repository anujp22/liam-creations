import { useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useCart } from '../context/CartContext';
import { formatINR } from '../utils/money';
import { useProduct } from '../hooks/useProducts';
import { useTitle } from '../hooks/useTitle';

export function ProductDetailPage() {
  const { productNumber } = useParams<{ productNumber: string }>();
  const [activeImage, setActiveImage] = useState(0);
  const navigate = useNavigate();
  const { addToCart, isInCart } = useCart();
  const { data: product, isPending, isError, error } = useProduct(productNumber);
  useTitle(product?.title ?? 'Product');

  if (isPending) return <p className="grid-message">Loading...</p>;
  if (isError) return <p className="grid-message grid-error">{(error as Error).message}</p>;
  if (!product) return null;

  const inCart = isInCart(product.productNumber);
  const gallery = product.images && product.images.length > 0
    ? product.images
    : (product.imageUrl ? [product.imageUrl] : []);
  const mainImage = gallery[Math.min(activeImage, gallery.length - 1)];

  return (
    <div className="detail">
      <button onClick={() => navigate(-1)} className="detail-back">
        ← Back to shop
      </button>
      <div className="detail-card">
        {mainImage && (
          <img src={mainImage} alt={product.title} className="detail-image" />
        )}
        {gallery.length > 1 && (
          <div className="detail-thumbs">
            {gallery.map((url, i) => (
              <button
                key={url}
                type="button"
                className={`detail-thumb${i === activeImage ? ' detail-thumb--active' : ''}`}
                onClick={() => setActiveImage(i)}
              >
                <img src={url} alt="" />
              </button>
            ))}
          </div>
        )}
        <span className="product-badges">
          <span className={`product-status product-status--${product.status}`}>{product.status.replace(/_/g, ' ')}</span>
          {product.salePrice != null && <span className="product-sale-badge">Sale</span>}
          {product.featured && <span className="product-featured">Featured</span>}
        </span>
        <h2 className="detail-title">{product.title}</h2>
        <p className="detail-description">{product.description}</p>
        <div className="detail-footer">
          {product.salePrice != null ? (
            <span className="product-price">
              <span className="product-price-was">{formatINR(Number(product.price))}</span>
              <span className="product-price-now">{formatINR(Number(product.salePrice))}</span>
            </span>
          ) : (
            <span className="product-price">{formatINR(Number(product.price))}</span>
          )}
          <button
            className={`add-to-cart-btn${inCart ? ' add-to-cart-btn--in-cart' : ''}`}
            onClick={() => addToCart(product)}
          >
            {inCart ? '✓ Added to cart' : 'Add to cart'}
          </button>
        </div>
        {product.createdAt && (
          <p className="detail-meta">
            Added {new Date(product.createdAt).toLocaleDateString('en-IN', { day: 'numeric', month: 'long', year: 'numeric' })}
          </p>
        )}
        {product.updatedAt && product.updatedAt !== product.createdAt && (
          <p className="detail-meta">
            Updated {new Date(product.updatedAt).toLocaleDateString('en-IN', { day: 'numeric', month: 'long', year: 'numeric' })}
          </p>
        )}
      </div>
    </div>
  );
}
