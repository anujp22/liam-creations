import { Routes, Route, Link, Outlet } from 'react-router-dom';
import './App.css';
import logo from './assets/logo.png';
import { ProductGrid } from './components/ProductGrid';
import { ProductDetailPage } from './pages/ProductDetailPage';
import { CartPage } from './pages/CartPage';
import { SalePage } from './pages/SalePage';
import { BuiltOnRequestPage } from './pages/BuiltOnRequestPage';
import { SiteFooter } from './components/SiteFooter';
import { CanonicalLink } from './components/CanonicalLink';
import { RequireAdmin } from './components/RequireAdmin';
import { AdminLayout } from './pages/admin/AdminLayout';
import { AdminLoginPage } from './pages/admin/AdminLoginPage';
import { AdminSummary } from './pages/admin/AdminSummary';
import { AdminDashboard } from './pages/admin/AdminDashboard';
import { AdminInventory } from './pages/admin/AdminInventory';
import { AdminDeleted } from './pages/admin/AdminDeleted';
import { AdminOnSale } from './pages/admin/AdminOnSale';
import { AdminReviews } from './pages/admin/AdminReviews';
import { AdminProductFormPage } from './pages/admin/AdminProductFormPage';
import { useCart } from './context/CartContext';
import { useAdminAuth } from './context/AdminAuthContext';

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
  const { isAuthenticated } = useAdminAuth();
  return (
    <div className="catalog">
      <header className="catalog-header">
        <div className="catalog-header-inner">
          <Link to="/" className="brand-lockup" aria-label="Liams Creations — Marriage Essentials, home">
            <img src={logo} alt="" className="brand-logo" />
            <span className="brand-text">
              <span className="brand-name">Liams Creations</span>
              <span className="brand-tagline">Marriage Essentials</span>
            </span>
          </Link>
          <nav className="catalog-nav">
            <Link to="/" className="nav-link">Shop</Link>
            <Link to="/built-on-request" className="nav-link">Built on Request</Link>
            <Link to="/sale" className="nav-link nav-link--sale">Sale</Link>
            {isAuthenticated && <Link to="/admin" className="nav-admin">Admin</Link>}
            <CartIcon />
          </nav>
        </div>
      </header>
      <main>
        <Outlet />
      </main>
      <SiteFooter />
    </div>
  );
}

function App() {
  return (
    <>
      <CanonicalLink />
      <Routes>
      <Route element={<ShopLayout />}>
        <Route path="/" element={<ProductGrid />} />
        <Route path="/products/:productNumber" element={<ProductDetailPage />} />
        <Route path="/sale" element={<SalePage />} />
        <Route path="/built-on-request" element={<BuiltOnRequestPage />} />
        <Route path="/cart" element={<CartPage />} />
      </Route>

      <Route path="/admin/login" element={<AdminLoginPage />} />
      <Route element={<RequireAdmin />}>
        <Route element={<AdminLayout />}>
          <Route path="/admin" element={<AdminSummary />} />
          <Route path="/admin/products" element={<AdminDashboard />} />
          <Route path="/admin/products/new" element={<AdminProductFormPage />} />
          <Route path="/admin/products/:productNumber/edit" element={<AdminProductFormPage />} />
          <Route path="/admin/on-sale" element={<AdminOnSale />} />
          <Route path="/admin/reviews" element={<AdminReviews />} />
          <Route path="/admin/inventory" element={<AdminInventory />} />
          <Route path="/admin/deleted" element={<AdminDeleted />} />
        </Route>
      </Route>
      </Routes>
    </>
  );
}

export default App;
