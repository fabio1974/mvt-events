-- Criar dados de teste para debugging do sistema courier
-- Executar este script diretamente no PostgreSQL

-- 1. Verificar se já existe organização de teste
INSERT INTO organizations (
    id, name, slug, contact_email, created_at, updated_at
) VALUES (
    6, 'Test Organization', 'test-org', 'test@org.com', NOW(), NOW()
) ON CONFLICT (id) DO NOTHING;

-- 2. Criar courier de teste se não existe
INSERT INTO users (
    id, username, password, email, name, role, enabled, organization_id, created_at, updated_at
) VALUES (
    '60085b55-9f67-4489-b1e4-e310ee5c4f27',
    'courier@test.com',
    '$2a$10$2X8wNBhT5p5Nw8aKzE3/xOQh7L.jF9yV2sL8mJ6qN3pR4tY7uZ9wG', -- password123
    'courier@test.com',
    'Test Courier',
    'COURIER',
    true,
    6,
    NOW(),
    NOW()
) ON CONFLICT (id) DO UPDATE SET
    organization_id = 6,
    role = 'COURIER',
    enabled = true;

-- 3. Criar contrato de emprego
INSERT INTO employment_contracts (
    courier_id, organization_id, is_active, created_at, updated_at
) VALUES (
    '60085b55-9f67-4489-b1e4-e310ee5c4f27',
    6,
    true,
    NOW(),
    NOW()
) ON CONFLICT (courier_id, organization_id) DO UPDATE SET
    is_active = true;

-- 4. Criar alguns clientes de teste na mesma organização
INSERT INTO users (
    id, username, password, email, name, role, enabled, organization_id, created_at, updated_at
) VALUES (
    gen_random_uuid(),
    'client1@test.com',
    '$2a$10$2X8wNBhT5p5Nw8aKzE3/xOQh7L.jF9yV2sL8mJ6qN3pR4tY7uZ9wG', -- password123
    'client1@test.com',
    'Test Client 1',
    'CLIENT',
    true,
    6,
    NOW(),
    NOW()
), (
    gen_random_uuid(),
    'client2@test.com',
    '$2a$10$2X8wNBhT5p5Nw8aKzE3/xOQh7L.jF9yV2sL8mJ6qN3pR4tY7uZ9wG', -- password123
    'client2@test.com',
    'Test Client 2',
    'CLIENT',
    true,
    6,
    NOW(),
    NOW()
) ON CONFLICT (email) DO NOTHING;

-- 5. Criar algumas deliveries PENDING para teste
INSERT INTO deliveries (
    id, 
    client_id, 
    status, 
    created_at, 
    updated_at,
    pickup_address,
    delivery_address,
    package_description
) 
SELECT 
    gen_random_uuid(),
    u.id,
    'PENDING',
    NOW(),
    NOW(),
    'Test Pickup Address ' || u.name,
    'Test Delivery Address ' || u.name,
    'Test Package for ' || u.name
FROM users u 
WHERE u.role = 'CLIENT' AND u.organization_id = 6
LIMIT 3;

-- Verificar dados criados
SELECT 'Organizations' as table_name, count(*) as count FROM organizations WHERE id = 6
UNION ALL
SELECT 'Users COURIER', count(*) FROM users WHERE role = 'COURIER' AND organization_id = 6
UNION ALL
SELECT 'Users CLIENT', count(*) FROM users WHERE role = 'CLIENT' AND organization_id = 6
UNION ALL
SELECT 'Employment Contracts', count(*) FROM employment_contracts WHERE organization_id = 6
UNION ALL
SELECT 'Deliveries PENDING', count(*) FROM deliveries d 
    JOIN users c ON d.client_id = c.id 
    WHERE d.status = 'PENDING' AND c.organization_id = 6;