-- V112: Add payment method for table orders (informed by waiter when closing the bill)
ALTER TABLE orders ADD COLUMN table_payment_method VARCHAR(20);
