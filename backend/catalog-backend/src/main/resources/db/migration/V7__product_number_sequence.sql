-- Auto-incrementing product numbers, reserved forever (never reused).
-- Starts at 1000 to sit clearly above all existing seed numbers (< 1000),
-- so generated numbers never collide. Portable across Postgres and H2 (tests).
CREATE SEQUENCE IF NOT EXISTS product_number_seq START WITH 1000;
