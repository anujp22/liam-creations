-- Optional discounted price. When set (and below price) the product shows on the
-- storefront Sale section with the original price struck through.
ALTER TABLE products ADD COLUMN sale_price NUMERIC(19, 2);
