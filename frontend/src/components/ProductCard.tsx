import { Link } from 'react-router-dom';
import type { Product } from '../api/products';

interface Props {
  product: Product;
}

export function ProductCard({ product }: Props) {
  return (
    <Link to={`/products/${product.productNumber}`} className="product-card">
      <span className="product-status">{product.status.replace(/_/g, ' ')}</span>
      <h2 className="product-title">{product.title}</h2>
      <p className="product-description">{product.description}</p>
      <div className="product-footer">
        <span className="product-price">
          {product.currency} {Number(product.price).toFixed(2)}
        </span>
        {product.featured && <span className="product-featured">Featured</span>}
      </div>
    </Link>
  );
}
