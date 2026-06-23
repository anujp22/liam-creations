import { useEffect, useState, type FormEvent } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { fetchProduct } from '../../api/products';
import type { ProductCategory, ProductStatus } from '../../api/products';
import { createProduct, updateProduct } from '../../api/admin';
import { useTitle } from '../../hooks/useTitle';

const STATUS_OPTIONS: { value: ProductStatus; label: string }[] = [
  { value: 'IN_STOCK', label: 'In stock' },
  { value: 'OUT_OF_STOCK', label: 'Out of stock' },
  { value: 'BUILT_ON_REQUEST', label: 'Built on request' },
];

const CATEGORY_OPTIONS: { value: ProductCategory; label: string }[] = [
  { value: 'BRIDAL_SAREES', label: 'Bridal Sarees' },
  { value: 'WEDDING_DECOR', label: 'Wedding Decor' },
];

interface FormState {
  title: string;
  description: string;
  price: string;
  salePrice: string;
  currency: string;
  status: ProductStatus;
  category: ProductCategory;
  featured: boolean;
  imageUrl: string;
}

const EMPTY: FormState = {
  title: '',
  description: '',
  price: '',
  salePrice: '',
  currency: 'INR',
  status: 'IN_STOCK',
  category: 'BRIDAL_SAREES',
  featured: false,
  imageUrl: '',
};

export function AdminProductFormPage() {
  const { productNumber } = useParams<{ productNumber: string }>();
  const isEdit = Boolean(productNumber);
  const navigate = useNavigate();
  useTitle(isEdit ? 'Edit Product' : 'New Product');

  const [form, setForm] = useState<FormState>(EMPTY);
  const [loading, setLoading] = useState(isEdit);
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    if (!productNumber) return;
    fetchProduct(productNumber)
      .then((p) =>
        setForm({
          title: p.title,
          description: p.description,
          price: String(p.price),
          salePrice: p.salePrice != null ? String(p.salePrice) : '',
          currency: p.currency,
          status: p.status,
          category: p.category,
          featured: p.featured,
          imageUrl: p.imageUrl ?? '',
        }),
      )
      .catch((e: Error) => setError(e.message))
      .finally(() => setLoading(false));
  }, [productNumber]);

  const set = <K extends keyof FormState>(key: K, value: FormState[K]) =>
    setForm((prev) => ({ ...prev, [key]: value }));

  const validate = (): string | null => {
    if (!form.title.trim()) return 'Title is required.';
    if (!form.description.trim()) return 'Description is required.';
    const price = Number(form.price);
    if (!Number.isFinite(price) || price <= 0) return 'Price must be greater than 0.';
    if (!form.currency.trim()) return 'Currency is required.';
    if (form.salePrice.trim()) {
      const sale = Number(form.salePrice);
      if (!Number.isFinite(sale) || sale <= 0) return 'Sale price must be greater than 0.';
      if (sale >= price) return 'Sale price must be below the regular price.';
    }
    return null;
  };

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    const validationError = validate();
    if (validationError) { setError(validationError); return; }

    setError(null);
    setSubmitting(true);
    const payload = {
      title: form.title.trim(),
      description: form.description.trim(),
      price: Number(form.price),
      salePrice: form.salePrice.trim() ? Number(form.salePrice) : null,
      currency: form.currency.trim(),
      status: form.status,
      category: form.category,
      featured: form.featured,
      imageUrl: form.imageUrl.trim() || undefined,
    };
    try {
      if (isEdit) {
        await updateProduct(productNumber!, payload);
      } else {
        await createProduct(payload);
      }
      navigate('/admin', { replace: true });
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Save failed.');
    } finally {
      setSubmitting(false);
    }
  };

  if (loading) return <p className="admin-placeholder">Loading…</p>;

  return (
    <div className="admin-form-page">
      <Link to="/admin" className="admin-back">← Back to products</Link>
      <h1 className="admin-dash-title">{isEdit ? 'Edit product' : 'New product'}</h1>

      {error && <p className="admin-error">{error}</p>}

      <form className="admin-form" onSubmit={handleSubmit}>
        {isEdit ? (
          <p className="admin-form-note">Product number <strong>{productNumber}</strong></p>
        ) : (
          <p className="admin-form-note">A product number is assigned automatically when you save.</p>
        )}

        <label className="admin-field">
          <span className="admin-field-label">Title</span>
          <input className="admin-input" value={form.title} onChange={(e) => set('title', e.target.value)} required />
        </label>

        <label className="admin-field">
          <span className="admin-field-label">Description</span>
          <textarea
            className="admin-input admin-textarea"
            value={form.description}
            onChange={(e) => set('description', e.target.value)}
            rows={4}
            required
          />
        </label>

        <div className="admin-form-grid">
          <label className="admin-field">
            <span className="admin-field-label">Price</span>
            <input
              className="admin-input"
              type="number"
              min="0.01"
              step="0.01"
              value={form.price}
              onChange={(e) => set('price', e.target.value)}
              required
            />
          </label>

          <label className="admin-field">
            <span className="admin-field-label">Sale price <span className="admin-field-hint">(optional, below price)</span></span>
            <input
              className="admin-input"
              type="number"
              min="0.01"
              step="0.01"
              value={form.salePrice}
              onChange={(e) => set('salePrice', e.target.value)}
              placeholder="—"
            />
          </label>

          <label className="admin-field">
            <span className="admin-field-label">Currency</span>
            <input className="admin-input" value={form.currency} onChange={(e) => set('currency', e.target.value)} required />
          </label>

          <label className="admin-field">
            <span className="admin-field-label">Status</span>
            <select className="admin-input" value={form.status} onChange={(e) => set('status', e.target.value as ProductStatus)}>
              {STATUS_OPTIONS.map((o) => <option key={o.value} value={o.value}>{o.label}</option>)}
            </select>
          </label>

          <label className="admin-field">
            <span className="admin-field-label">Category</span>
            <select className="admin-input" value={form.category} onChange={(e) => set('category', e.target.value as ProductCategory)}>
              {CATEGORY_OPTIONS.map((o) => <option key={o.value} value={o.value}>{o.label}</option>)}
            </select>
          </label>
        </div>

        <label className="admin-field">
          <span className="admin-field-label">Image URL</span>
          <input
            className="admin-input"
            value={form.imageUrl}
            onChange={(e) => set('imageUrl', e.target.value)}
            placeholder="https://…"
          />
        </label>

        {form.imageUrl.trim() && (
          <img src={form.imageUrl} alt="" className="admin-form-preview" />
        )}

        <label className="admin-checkbox">
          <input type="checkbox" checked={form.featured} onChange={(e) => set('featured', e.target.checked)} />
          <span>Featured product</span>
        </label>

        <div className="admin-form-actions">
          <button type="submit" className="admin-login-btn admin-form-submit" disabled={submitting}>
            {submitting ? 'Saving…' : isEdit ? 'Save changes' : 'Create product'}
          </button>
          <Link to="/admin" className="admin-link-btn">Cancel</Link>
        </div>
      </form>
    </div>
  );
}
