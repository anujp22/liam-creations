-- Orders placed through the storefront's "Send order via WhatsApp" button.
--
-- There are no customer accounts, so the order carries its own contact details and
-- an order code (LC-1000, LC-1001, …) that goes into the WhatsApp message. That code
-- is what ties a chat the owner is reading to a row on the admin Orders screen.
--
-- Payment happens outside the app entirely. Status is moved by hand by the owner.
--
-- Kept portable across Postgres and H2 (tests): the id is assigned by the application
-- (Hibernate), and timestamps use plain TIMESTAMP like the rest of the schema.
CREATE TABLE orders (
    id               UUID          NOT NULL,
    order_code       VARCHAR(20)   NOT NULL UNIQUE,

    -- Personal data. Collected only to fulfil the order; see the privacy policy.
    customer_name    VARCHAR(120)  NOT NULL,
    customer_phone   VARCHAR(30)   NOT NULL,
    customer_email   VARCHAR(200),
    customer_address VARCHAR(500)  NOT NULL,
    notes            VARCHAR(1000),

    status           VARCHAR(20)   NOT NULL DEFAULT 'NEW',
    -- Server-computed from the item snapshots below, never sent by the client.
    total            NUMERIC(12, 2) NOT NULL,
    currency         VARCHAR(3)    NOT NULL,

    created_at       TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);

-- One line per product ordered, with the title and price frozen at order time.
--
-- Deliberately NO foreign key to products. An order is a historical record of what was
-- agreed and for how much; it must not change or disappear when the catalog does, and
-- the admin screen can permanently delete a product. The price snapshot is the same
-- reasoning that took the price out of the cart (A11): prices move, orders must not.
CREATE TABLE order_items (
    id             UUID           NOT NULL,
    order_id       UUID           NOT NULL REFERENCES orders (id) ON DELETE CASCADE,
    product_number VARCHAR(255)   NOT NULL,
    title          VARCHAR(255)   NOT NULL,
    unit_price     NUMERIC(12, 2) NOT NULL,
    quantity       INTEGER        NOT NULL CHECK (quantity > 0),
    PRIMARY KEY (id)
);

CREATE INDEX idx_order_items_order ON order_items (order_id);

-- The admin Orders screen lists newest first and filters by status.
CREATE INDEX idx_orders_status_created ON orders (status, created_at DESC);

-- Order codes, reserved forever and never reused, exactly like product numbers (V7).
-- A customer quotes "LC-1042" in a WhatsApp chat weeks later; that has to still mean
-- one specific order. Starts at 1000 so every code is the same width.
CREATE SEQUENCE IF NOT EXISTS order_code_seq START WITH 1000;
