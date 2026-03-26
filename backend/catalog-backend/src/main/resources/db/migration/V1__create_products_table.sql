CREATE TABLE products (
    product_number VARCHAR(255) PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    price NUMERIC(19, 2) NOT NULL,
    currency VARCHAR(10) NOT NULL,
    status VARCHAR(50) NOT NULL,
    featured BOOLEAN NOT NULL,
    instagram_post_url TEXT NOT NULL
);
