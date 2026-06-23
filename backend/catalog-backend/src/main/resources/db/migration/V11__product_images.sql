-- Gallery images per product (ordered). imageUrl on products stays as the
-- primary/thumbnail (kept in sync with the first image).
CREATE TABLE product_images (
    product_number VARCHAR(255) NOT NULL REFERENCES products (product_number) ON DELETE CASCADE,
    image_url VARCHAR(1000) NOT NULL,
    sort_order INT NOT NULL,
    PRIMARY KEY (product_number, sort_order)
);
