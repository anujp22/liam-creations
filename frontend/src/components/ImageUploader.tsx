import { useRef, useState, type DragEvent } from 'react';
import { uploadImages } from '../api/admin';

interface Props {
  images: string[];
  onChange: (images: string[]) => void;
}

/** Drag-and-drop / browse uploader for multiple product images. First image is the primary. */
export function ImageUploader({ images, onChange }: Props) {
  const inputRef = useRef<HTMLInputElement>(null);
  const [dragging, setDragging] = useState(false);
  const [uploading, setUploading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleFiles = async (fileList: FileList | null) => {
    if (!fileList || fileList.length === 0) return;
    setError(null);
    setUploading(true);
    try {
      const urls = await uploadImages(Array.from(fileList));
      onChange([...images, ...urls]);
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Upload failed.');
    } finally {
      setUploading(false);
    }
  };

  const onDrop = (e: DragEvent) => {
    e.preventDefault();
    setDragging(false);
    handleFiles(e.dataTransfer.files);
  };

  const remove = (url: string) => onChange(images.filter((u) => u !== url));
  const makePrimary = (url: string) => onChange([url, ...images.filter((u) => u !== url)]);

  return (
    <div className="uploader">
      <div
        className={`uploader-drop${dragging ? ' uploader-drop--active' : ''}`}
        onDragOver={(e) => { e.preventDefault(); setDragging(true); }}
        onDragLeave={() => setDragging(false)}
        onDrop={onDrop}
        onClick={() => inputRef.current?.click()}
        role="button"
        tabIndex={0}
      >
        <p className="uploader-hint">
          {uploading ? 'Uploading…' : 'Drag & drop images here, or click to browse'}
        </p>
        <p className="uploader-sub">JPEG, PNG, WEBP or GIF · up to 8 MB each</p>
        <input
          ref={inputRef}
          type="file"
          accept="image/*"
          multiple
          hidden
          onChange={(e) => { handleFiles(e.target.files); e.target.value = ''; }}
        />
      </div>

      {error && <p className="uploader-error">{error}</p>}

      {images.length > 0 && (
        <div className="uploader-grid">
          {images.map((url, i) => (
            <div key={url} className={`uploader-thumb${i === 0 ? ' uploader-thumb--primary' : ''}`}>
              <img src={url} alt="" />
              {i === 0 && <span className="uploader-badge">Primary</span>}
              <div className="uploader-thumb-actions">
                {i !== 0 && (
                  <button type="button" onClick={() => makePrimary(url)} title="Set as primary">★</button>
                )}
                <button type="button" onClick={() => remove(url)} title="Remove">✕</button>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
