-- Canal de venda do produto: DELIVERY, TABLE ou ALL
ALTER TABLE products ADD COLUMN sales_channel VARCHAR(20) NOT NULL DEFAULT 'ALL';
