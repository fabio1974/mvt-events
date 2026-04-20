-- Flag pra marcar itens que devem ser empacotados pra viagem (embalo)
ALTER TABLE order_items
    ADD COLUMN packaged BOOLEAN NOT NULL DEFAULT FALSE;
