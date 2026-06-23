-- Category list reduced to BRIDAL_SAREES and WEDDING_DECOR for the live site.
-- Reassign any product in a removed category so existing rows stay valid.
UPDATE products
SET category = 'WEDDING_DECOR'
WHERE category NOT IN ('BRIDAL_SAREES', 'WEDDING_DECOR');
