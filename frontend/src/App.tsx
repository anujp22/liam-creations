import { Routes, Route } from 'react-router-dom';
import './App.css';
import { ProductGrid } from './components/ProductGrid';
import { ProductDetailPage } from './pages/ProductDetailPage';

function App() {
  return (
    <div className="catalog">
      <header className="catalog-header">
        <h1>Shop</h1>
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
