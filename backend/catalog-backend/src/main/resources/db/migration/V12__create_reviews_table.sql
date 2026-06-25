-- Customer reviews. Submitted publicly as PENDING; only APPROVED reviews are
-- shown on the storefront. No login/PII: rating + comment only.
-- Kept portable across Postgres and H2 (tests): the id is assigned by the
-- application (Hibernate), and timestamps use plain TIMESTAMP like the rest.
CREATE TABLE reviews (
    id             UUID          NOT NULL,
    product_number VARCHAR(255)  NOT NULL REFERENCES products (product_number) ON DELETE CASCADE,
    rating         SMALLINT      NOT NULL CHECK (rating BETWEEN 1 AND 5),
    comment        VARCHAR(2000) NOT NULL,
    status         VARCHAR(20)   NOT NULL DEFAULT 'PENDING',
    created_at     TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);

-- Storefront reads approved reviews per product; admin reads the pending queue.
CREATE INDEX idx_reviews_product_status ON reviews (product_number, status);
