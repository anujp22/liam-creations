import { useEffect, useState } from 'react';
import { useParams, useNavigate} from 'react-router-dom';
import { fetchProduct } from '../api/products';
import type { Product } from '../api/products';

export function ProductDetailPage() {
  const { productNumber } = useParams<{ productNumber: string }>();
  const [product, setProduct] = useState<Product | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const navigate = useNavigate();

  useEffect(() => {
    if (!productNumber) return;
    fetchProduct(productNumber)
      .then(setProduct)
      .catch((e: Error) => setError(e.message))
      .finally(() => setLoading(false));
  }, [productNumber]);

  if (loading) return <p className="grid-message">Loading...</p>;
  if (error) return <p className="grid-message grid-error">{error}</p>;
  if (!product) return null;

  return (
    <div className="detail">
      <button onClick={() => navigate(-1)} className="detail-back">
        ← Back to shop
      </button>
      <div className="detail-card">
        <span className="product-status">{product.status.replace(/_/g, ' ')}</span>
        {product.featured && <span className="product-featured">Featured</span>}
        <h2 className="detail-title">{product.title}</h2>
        <p className="detail-description">{product.description}</p>
        <div className="detail-footer">
          <span className="product-price">
            {product.currency} {Number(product.price).toFixed(2)}
          </span>
          <a
            href={product.instagramPostUrl}
            target="_blank"
            rel="noopener noreferrer"
            className="product-link"
          >
            View on Instagram ↗
          </a>
        </div>
      </div>
    </div>
  );
}
