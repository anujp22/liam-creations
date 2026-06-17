ALTER TABLE products ADD COLUMN category VARCHAR(50);

UPDATE products SET category = 'BRIDAL_SAREES'   WHERE product_number BETWEEN 'PN-001' AND 'PN-020';
UPDATE products SET category = 'BRIDAL_LEHENGAS'  WHERE product_number BETWEEN 'PN-021' AND 'PN-030';
UPDATE products SET category = 'HALDI_MEHENDI'    WHERE product_number BETWEEN 'PN-031' AND 'PN-045';
UPDATE products SET category = 'JEWELLERY'        WHERE product_number BETWEEN 'PN-046' AND 'PN-060';
UPDATE products SET category = 'CLAY_POTTERY'     WHERE product_number BETWEEN 'PN-061' AND 'PN-070';
UPDATE products SET category = 'PUJA_RITUALS'     WHERE product_number BETWEEN 'PN-071' AND 'PN-080';
UPDATE products SET category = 'WEDDING_DECOR'    WHERE product_number BETWEEN 'PN-081' AND 'PN-090';
UPDATE products SET category = 'SWEETS_GIFTS'     WHERE product_number BETWEEN 'PN-091' AND 'PN-100';

ALTER TABLE products ALTER COLUMN category SET NOT NULL;
