import { Link } from 'react-router-dom';
import type { Product } from '../api/products';
import { useCart } from '../context/CartContext';

interface Props {
  product: Product;
}

export function ProductCard({ product }: Props) {
  const { addToCart, isInCart } = useCart();
  const inCart = isInCart(product.productNumber);

  return (
    <div className="product-card">
      <Link to={`/products/${product.productNumber}`} className="product-card-link">
        {product.imageUrl && (
          <img src={product.imageUrl} alt={product.title} className="product-image" />
        )}
        <span className="product-status">{product.status.replace(/_/g, ' ')}</span>
        <h2 className="product-title">{product.title}</h2>
        <p className="product-description">{product.description}</p>
        <div className="product-footer">
          <span className="product-price">
            ₹{Number(product.price).toLocaleString('en-IN')}
          </span>
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
