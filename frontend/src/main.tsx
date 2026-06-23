import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { BrowserRouter } from 'react-router-dom'
import './index.css'
import App from './App.tsx'
import { CartProvider } from './context/CartContext.tsx'
import { AdminAuthProvider } from './context/AdminAuthContext.tsx'

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <BrowserRouter>
      <AdminAuthProvider>
        <CartProvider>
          <App />
        </CartProvider>
      </AdminAuthProvider>
    </BrowserRouter>
  </StrictMode>,
)
