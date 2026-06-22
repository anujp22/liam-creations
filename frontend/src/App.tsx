import { Routes, Route, Link } from 'react-router-dom';
import './App.css';
import logo from './assets/logo.png';
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
        <Link to="/" className="brand-lockup" aria-label="Liams Creations — Marriage Essentials, home">
          <img src={logo} alt="" className="brand-logo" />
          <span className="brand-text">
            <span className="brand-name">Liams Creations</span>
            <span className="brand-tagline">Marriage Essentials</span>
          </span>
        </Link>
        <nav className="catalog-nav">
          <a
            href="https://www.instagram.com/_liamcreations"
            target="_blank"
            rel="noopener noreferrer"
            className="nav-instagram"
          >
            Follow @_liamcreations ↗
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
      <footer className="catalog-footer">
        <img src={logo} alt="" className="footer-logo" />
        <p className="footer-name">Liams Creations</p>
        <p className="footer-tagline">Marriage Essentials</p>
        <a
          href="https://www.instagram.com/_liamcreations"
          target="_blank"
          rel="noopener noreferrer"
          className="footer-instagram"
        >
          Follow @_liamcreations ↗
        </a>
        <p className="footer-copy">© {new Date().getFullYear()} Liams Creations. Crafted with love.</p>
      </footer>
    </div>
  );
}

export default App;
