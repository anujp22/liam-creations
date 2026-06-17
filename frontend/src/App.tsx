import { Routes, Route } from 'react-router-dom';
import './App.css';
import { ProductGrid } from './components/ProductGrid';
import { ProductDetailPage } from './pages/ProductDetailPage';

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
        </nav>
      </header>
      <main>
        <Routes>
          <Route path="/" element={<ProductGrid />} />
          <Route path="/products/:productNumber" element={<ProductDetailPage />} />
        </Routes>
      </main>
    </div>
  );
}

export default App;
