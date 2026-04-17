-- Catálogo de serviços recorrentes prestados pela plataforma
CREATE TABLE platform_services (
    id          BIGSERIAL PRIMARY KEY,
    code        VARCHAR(50)    NOT NULL UNIQUE,
    name        VARCHAR(100)   NOT NULL,
    description TEXT,
    default_monthly_price DECIMAL(10,2) NOT NULL,
    active      BOOLEAN        NOT NULL DEFAULT true,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

-- Seed: primeiro serviço
INSERT INTO platform_services (code, name, description, default_monthly_price)
VALUES ('TABLE_SERVICE', 'Serviço de Mesas', 'Gestão de mesas, pedidos e garçons para restaurantes e bares', 150.00);
