-- Soft delete: products are hidden, not removed, so their numbers stay reserved
-- and they can be restored from the admin Deleted tab.
ALTER TABLE products ADD COLUMN deleted BOOLEAN NOT NULL DEFAULT false;
