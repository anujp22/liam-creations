import { Routes, Route, Link, Outlet } from 'react-router-dom';
import './App.css';
import logo from './assets/logo.png';
import { ProductGrid } from './components/ProductGrid';
import { ProductDetailPage } from './pages/ProductDetailPage';
import { CartPage } from './pages/CartPage';
import { RequireAdmin } from './components/RequireAdmin';
import { AdminLayout } from './pages/admin/AdminLayout';
import { AdminLoginPage } from './pages/admin/AdminLoginPage';
import { AdminDashboard } from './pages/admin/AdminDashboard';
import { AdminProductFormPage } from './pages/admin/AdminProductFormPage';
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

function ShopLayout() {
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
        <Outlet />
      </main>
      <footer className="catalog-footer">
        <img src={logo} alt="" className="footer-logo" />
        <p className="footer-name">Liams Creations</p>
        <p className="footer-tagline">Marriage Essentials</p>
        <div className="footer-social">
          <a
            href="https://www.instagram.com/_liamcreations"
            target="_blank"
            rel="noopener noreferrer"
            className="footer-instagram"
          >
            Follow @_liamcreations ↗
          </a>
          <a
            href="https://chat.whatsapp.com/ET3oMppZfDAIjlpqmoHfs7"
            target="_blank"
            rel="noopener noreferrer"
            className="footer-community"
          >
            <svg className="footer-community-icon" viewBox="0 0 24 24" fill="currentColor" aria-hidden="true">
              <path d="M17.472 14.382c-.297-.149-1.758-.867-2.03-.967-.273-.099-.471-.148-.67.15-.197.297-.767.966-.94 1.164-.173.199-.347.223-.644.075-.297-.15-1.255-.463-2.39-1.475-.883-.788-1.48-1.761-1.653-2.059-.173-.297-.018-.458.13-.606.134-.133.298-.347.446-.52.149-.174.198-.298.298-.497.099-.198.05-.371-.025-.52-.075-.149-.669-1.612-.916-2.207-.242-.579-.487-.5-.669-.51-.173-.008-.371-.01-.57-.01-.198 0-.52.074-.792.372-.272.297-1.04 1.016-1.04 2.479 0 1.462 1.065 2.875 1.213 3.074.149.198 2.096 3.2 5.077 4.487.709.306 1.262.489 1.694.625.712.227 1.36.195 1.871.118.571-.085 1.758-.719 2.006-1.413.248-.694.248-1.289.173-1.413-.074-.124-.272-.198-.57-.347m-5.421 7.403h-.004a9.87 9.87 0 01-5.031-1.378l-.361-.214-3.741.982.998-3.648-.235-.374a9.86 9.86 0 01-1.51-5.26c.001-5.45 4.436-9.884 9.888-9.884 2.64 0 5.122 1.03 6.988 2.898a9.825 9.825 0 012.893 6.994c-.003 5.45-4.437 9.884-9.885 9.884m8.413-18.297A11.815 11.815 0 0012.05 0C5.495 0 .16 5.335.157 11.892c0 2.096.547 4.142 1.588 5.945L.057 24l6.305-1.654a11.882 11.882 0 005.683 1.448h.005c6.554 0 11.89-5.335 11.893-11.893a11.821 11.821 0 00-3.48-8.413z" />
            </svg>
            Join our WhatsApp community ↗
          </a>
        </div>
        <p className="footer-copy">© {new Date().getFullYear()} Liams Creations. Crafted with love.</p>
      </footer>
    </div>
  );
}

function App() {
  return (
    <Routes>
      <Route element={<ShopLayout />}>
        <Route path="/" element={<ProductGrid />} />
        <Route path="/products/:productNumber" element={<ProductDetailPage />} />
        <Route path="/cart" element={<CartPage />} />
      </Route>

      <Route path="/admin/login" element={<AdminLoginPage />} />
      <Route element={<RequireAdmin />}>
        <Route element={<AdminLayout />}>
          <Route path="/admin" element={<AdminDashboard />} />
          <Route path="/admin/products/new" element={<AdminProductFormPage />} />
          <Route path="/admin/products/:productNumber/edit" element={<AdminProductFormPage />} />
        </Route>
      </Route>
    </Routes>
  );
}

export default App;
