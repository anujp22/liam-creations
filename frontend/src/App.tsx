import { Routes, Route, Link } from 'react-router-dom';
import './App.css';
import { ProductGrid } from './components/ProductGrid';
import { ProductDetailPage } from './pages/ProductDetailPage';
import { CartPage } from './pages/CartPage';
import { useCart } from './context/CartContext';

function CartIcon() {
  const { itemCount } = useCart();
  return (
    <Link to="/cart" className="cart-icon-link" aria-label={`Cart — ${itemCount} items`}>
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" className="cart-icon-svg" aria-hidden="true">
        <path strokeLinecap="round" strokeLinejoin="round" d="M3 3h2l.4 2M7 13h10l4-8H5.4M7 13L5.4 5M7 13l-2.293 2.293c-.63.63-.184 1.707.707 1.707H17m0 0a2 2 0 100 4 2 2 0 000-4zm-8 2a2 2 0 11-4 0 2 2 0 014 0z" />
      </svg>
      {itemCount > 0 && <span className="cart-badge">{itemCount}</span>}
    </Link>
  );
}

function App() {
  return (
    <div className="catalog">
      <header className="catalog-header">
        <h1>Shaadi Catalog</h1>
        <nav className="catalog-nav">
          <a
            href="https://www.instagram.com/"
            target="_blank"
            rel="noopener noreferrer"
            className="nav-instagram"
          >
            Follow us on Instagram ↗
          </a>
          <CartIcon />
        </nav>
      </header>
      <main>
        <Routes>
          <Route path="/" element={<ProductGrid />} />
          <Route path="/products/:productNumber" element={<ProductDetailPage />} />
          <Route path="/cart" element={<CartPage />} />
        </Routes>
      </main>
    </div>
  );
}

export default App;
