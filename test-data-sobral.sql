-- =====================================================
-- DADOS DE TESTE REALISTAS PARA SOBRAL-CE
-- =====================================================

-- Limpar deliveries existentes
DELETE FROM deliveries;

-- =====================================================
-- 1. CRIAR NOVO MOTOBOY (Motoboy2)
-- =====================================================

-- Criar usuário Motoboy2
INSERT INTO users (id, name, username, password, document_number, phone, role, city_id, organization_id, enabled, created_at, updated_at, gps_latitude, gps_longitude)
VALUES (
    'a1b2c3d4-e5f6-7890-abcd-ef1234567890'::uuid,
    'Motoboy2',
    'motoboy2@gmail.com',
    '$2a$10$xWZ9pqH8kC7vK5nL3mR4tO0yT9uW1xV8yN2fQ6sE3rD7cA4bB5hC6', -- senha: 123456
    '987.654.321-00',
    '85988776655',
    'COURIER',
    1058, -- Sobral
    NULL, -- Couriers não têm organization_id segundo constraint
    true,
    NOW(),
    NOW(),
    -3.686389, -- Centro de Sobral
    -40.349167
);

-- Criar perfil de courier para Motoboy2
INSERT INTO courier_profiles (user_id, vehicle_type, vehicle_plate, status, rating, total_deliveries, completed_deliveries, cancelled_deliveries, created_at, updated_at)
VALUES (
    'a1b2c3d4-e5f6-7890-abcd-ef1234567890'::uuid,
    'MOTORCYCLE',
    'QRS-4321',
    'AVAILABLE',
    0.0,
    0,
    0,
    0,
    NOW(),
    NOW()
);

-- Criar contrato de emprego para Motoboy2
INSERT INTO employment_contracts (courier_id, organization_id, is_active, linked_at, created_at, updated_at)
VALUES (
    'a1b2c3d4-e5f6-7890-abcd-ef1234567890'::uuid,
    1, -- Organization 1
    true,
    NOW(),
    NOW(),
    NOW()
);

-- =====================================================
-- 2. CRIAR 3 NOVOS CLIENTES DE SOBRAL
-- =====================================================

-- Cliente 1: Posto de Gasolina Ipiranga (Centro)
INSERT INTO users (id, name, username, password, document_number, phone, role, city_id, enabled, created_at, updated_at, gps_latitude, gps_longitude)
VALUES (
    'c1111111-1111-1111-1111-111111111111'::uuid,
    'Posto Ipiranga Centro',
    'postoipiranga@gmail.com',
    '$2a$10$xWZ9pqH8kC7vK5nL3mR4tO0yT9uW1xV8yN2fQ6sE3rD7cA4bB5hC6',
    '111.111.111-11',
    '85987001111',
    'CLIENT',
    1058, -- Sobral
    true,
    NOW(),
    NOW(),
    -3.688056, -- Posto Centro Sobral
    -40.345833
);

-- Cliente 2: Farmácia Pague Menos (Dom Expedito)
INSERT INTO users (id, name, username, password, document_number, phone, role, city_id, enabled, created_at, updated_at, gps_latitude, gps_longitude)
VALUES (
    'c2222222-2222-2222-2222-222222222222'::uuid,
    'Farmácia Pague Menos',
    'farmaciapaguemenos@gmail.com',
    '$2a$10$xWZ9pqH8kC7vK5nL3mR4tO0yT9uW1xV8yN2fQ6sE3rD7cA4bB5hC6',
    '222.222.222-22',
    '85987002222',
    'CLIENT',
    1058, -- Sobral
    true,
    NOW(),
    NOW(),
    -3.692778, -- Dom Expedito
    -40.351944
);

-- Cliente 3: Sushi Mania (Junco)
INSERT INTO users (id, name, username, password, document_number, phone, role, city_id, enabled, created_at, updated_at, gps_latitude, gps_longitude)
VALUES (
    'c3333333-3333-3333-3333-333333333333'::uuid,
    'Sushi Mania',
    'sushimania@gmail.com',
    '$2a$10$xWZ9pqH8kC7vK5nL3mR4tO0yT9uW1xV8yN2fQ6sE3rD7cA4bB5hC6',
    '333.333.333-33',
    '85987003333',
    'CLIENT',
    1058, -- Sobral
    true,
    NOW(),
    NOW(),
    -3.698333, -- Junco
    -40.354722
);

-- Criar contratos de cliente para a Organization 1
INSERT INTO client_contracts (client_id, organization_id, status, contract_date, start_date, created_at, updated_at)
VALUES 
    ('c1111111-1111-1111-1111-111111111111'::uuid, 1, 'ACTIVE', CURRENT_DATE, CURRENT_DATE, NOW(), NOW()),
    ('c2222222-2222-2222-2222-222222222222'::uuid, 1, 'ACTIVE', CURRENT_DATE, CURRENT_DATE, NOW(), NOW()),
    ('c3333333-3333-3333-3333-333333333333'::uuid, 1, 'ACTIVE', CURRENT_DATE, CURRENT_DATE, NOW(), NOW());

-- =====================================================
-- 3. CRIAR DELIVERIES COM STATUS DIVERSOS
-- =====================================================

-- IDs dos usuários existentes (do seu sistema)
-- Motoboy1: 6186c7af-2311-4756-bfc6-ce98bd31ed27
-- Motoboy2: a1b2c3d4-e5f6-7890-abcd-ef1234567890
-- Samuel (Organizer): 6d401ff4-5c77-486d-9d0f-ddb2dbb13b24
-- Padaria1 (cliente existente): 189c7d79-cb21-40c1-9b7c-006ebaa3289a

-- ===== DELIVERIES PENDING (sem courier, sem organizer) =====

-- Delivery 1: PENDING - Posto para residência (Centro → Derby)
INSERT INTO deliveries (
    client_id, from_address, from_lat, from_lng,
    to_address, to_lat, to_lng,
    recipient_name, recipient_phone, item_description,
    total_amount, shipping_fee, distance_km, status,
    created_at, updated_at
) VALUES (
    'c1111111-1111-1111-1111-111111111111'::uuid,
    'Posto Ipiranga, Av. John Sanford, Centro, Sobral - CE, 62010-000',
    -3.688056, -40.345833,
    'Rua Barão do Rio Branco, 450, Derby, Sobral - CE, 62042-000',
    -3.693611, -40.344167,
    'João Silva',
    '85988111111',
    'Óleo de motor 5W30',
    89.90, 6.50, 0.78,
    'PENDING',
    NOW() - INTERVAL '15 minutes',
    NOW() - INTERVAL '15 minutes'
);

-- Delivery 2: PENDING - Farmácia para residência (Dom Expedito → Pedrinhas)
INSERT INTO deliveries (
    client_id, from_address, from_lat, from_lng,
    to_address, to_lat, to_lng,
    recipient_name, recipient_phone, item_description,
    total_amount, shipping_fee, distance_km, status,
    created_at, updated_at
) VALUES (
    'c2222222-2222-2222-2222-222222222222'::uuid,
    'Farmácia Pague Menos, Av. Dom José, Dom Expedito, Sobral - CE, 62050-000',
    -3.692778, -40.351944,
    'Rua Conselheiro José Júlio, 123, Pedrinhas, Sobral - CE, 62030-000',
    -3.701111, -40.347222,
    'Maria Santos',
    '85988222222',
    'Medicamentos diversos',
    156.70, 8.00, 1.25,
    'PENDING',
    NOW() - INTERVAL '8 minutes',
    NOW() - INTERVAL '8 minutes'
);

-- Delivery 3: PENDING - Sushi para residência (Junco → Campo dos Velhos)
INSERT INTO deliveries (
    client_id, from_address, from_lat, from_lng,
    to_address, to_lat, to_lng,
    recipient_name, recipient_phone, item_description,
    total_amount, shipping_fee, distance_km, status,
    created_at, updated_at
) VALUES (
    'c3333333-3333-3333-3333-333333333333'::uuid,
    'Sushi Mania, Rua Osvaldo Cruz, Junco, Sobral - CE, 62030-000',
    -3.698333, -40.354722,
    'Av. Monsenhor Aloísio Pinto, 789, Campo dos Velhos, Sobral - CE, 62030-000',
    -3.708889, -40.349444,
    'Pedro Costa',
    '85988333333',
    'Combo Sushi 40 peças',
    125.00, 9.00, 1.45,
    'PENDING',
    NOW() - INTERVAL '3 minutes',
    NOW() - INTERVAL '3 minutes'
);

-- ===== DELIVERIES ACCEPTED (com courier e organizer) =====

-- Delivery 4: ACCEPTED - Padaria1 para cliente (Centro)
INSERT INTO deliveries (
    client_id, courier_id, organizer_id,
    from_address, from_lat, from_lng,
    to_address, to_lat, to_lng,
    recipient_name, recipient_phone, item_description,
    total_amount, shipping_fee, distance_km, status,
    accepted_at, created_at, updated_at
) VALUES (
    '189c7d79-cb21-40c1-9b7c-006ebaa3289a'::uuid, -- Padaria1
    '6186c7af-2311-4756-bfc6-ce98bd31ed27'::uuid, -- Motoboy1
    '6d401ff4-5c77-486d-9d0f-ddb2dbb13b24'::uuid, -- Samuel
    'Padaria Central, Rua Coronel Mont''Alverne, 500, Centro, Sobral - CE, 62010-000',
    -3.686667, -40.346111,
    'Rua Benjamin Constant, 234, Centro, Sobral - CE, 62010-000',
    -3.688333, -40.347778,
    'Ana Paula',
    '85988444444',
    'Pães e bolos',
    45.00, 5.00, 0.35,
    'ACCEPTED',
    NOW() - INTERVAL '5 minutes',
    NOW() - INTERVAL '20 minutes',
    NOW() - INTERVAL '5 minutes'
);

-- Delivery 5: ACCEPTED - Posto para cliente
INSERT INTO deliveries (
    client_id, courier_id, organizer_id,
    from_address, from_lat, from_lng,
    to_address, to_lat, to_lng,
    recipient_name, recipient_phone, item_description,
    total_amount, shipping_fee, distance_km, status,
    accepted_at, created_at, updated_at
) VALUES (
    'c1111111-1111-1111-1111-111111111111'::uuid,
    'a1b2c3d4-e5f6-7890-abcd-ef1234567890'::uuid, -- Motoboy2
    '6d401ff4-5c77-486d-9d0f-ddb2dbb13b24'::uuid, -- Samuel
    'Posto Ipiranga, Av. John Sanford, Centro, Sobral - CE, 62010-000',
    -3.688056, -40.345833,
    'Rua Padre Ibiapina, 890, Centro, Sobral - CE, 62010-000',
    -3.690278, -40.348333,
    'Carlos Mendes',
    '85988555555',
    'Acessórios automotivos',
    230.00, 7.00, 0.52,
    'ACCEPTED',
    NOW() - INTERVAL '3 minutes',
    NOW() - INTERVAL '12 minutes',
    NOW() - INTERVAL '3 minutes'
);

-- ===== DELIVERIES PICKED_UP =====

-- Delivery 6: PICKED_UP - Farmácia
INSERT INTO deliveries (
    client_id, courier_id, organizer_id,
    from_address, from_lat, from_lng,
    to_address, to_lat, to_lng,
    recipient_name, recipient_phone, item_description,
    total_amount, shipping_fee, distance_km, status,
    accepted_at, picked_up_at, created_at, updated_at
) VALUES (
    'c2222222-2222-2222-2222-222222222222'::uuid,
    '6186c7af-2311-4756-bfc6-ce98bd31ed27'::uuid, -- Motoboy1
    '6d401ff4-5c77-486d-9d0f-ddb2dbb13b24'::uuid, -- Samuel
    'Farmácia Pague Menos, Av. Dom José, Dom Expedito, Sobral - CE, 62050-000',
    -3.692778, -40.351944,
    'Rua São João, 567, Dom Expedito, Sobral - CE, 62050-000',
    -3.694444, -40.353611,
    'Beatriz Oliveira',
    '85988666666',
    'Vitaminas e suplementos',
    89.50, 5.50, 0.28,
    'PICKED_UP',
    NOW() - INTERVAL '10 minutes',
    NOW() - INTERVAL '2 minutes',
    NOW() - INTERVAL '30 minutes',
    NOW() - INTERVAL '2 minutes'
);

-- ===== DELIVERIES IN_TRANSIT =====

-- Delivery 7: IN_TRANSIT - Sushi
INSERT INTO deliveries (
    client_id, courier_id, organizer_id,
    from_address, from_lat, from_lng,
    to_address, to_lat, to_lng,
    recipient_name, recipient_phone, item_description,
    total_amount, shipping_fee, distance_km, status,
    accepted_at, picked_up_at, in_transit_at, created_at, updated_at
) VALUES (
    'c3333333-3333-3333-3333-333333333333'::uuid,
    'a1b2c3d4-e5f6-7890-abcd-ef1234567890'::uuid, -- Motoboy2
    '6d401ff4-5c77-486d-9d0f-ddb2dbb13b24'::uuid, -- Samuel
    'Sushi Mania, Rua Osvaldo Cruz, Junco, Sobral - CE, 62030-000',
    -3.698333, -40.354722,
    'Av. Deputado José Martins Rodrigues, 1234, Sumaré, Sobral - CE, 62042-000',
    -3.695556, -40.361667,
    'Ricardo Lima',
    '85988777777',
    'Combo Executivo',
    78.90, 7.50, 1.12,
    'IN_TRANSIT',
    NOW() - INTERVAL '25 minutes',
    NOW() - INTERVAL '18 minutes',
    NOW() - INTERVAL '15 minutes',
    NOW() - INTERVAL '40 minutes',
    NOW() - INTERVAL '15 minutes'
);

-- Delivery 8: IN_TRANSIT - Padaria1
INSERT INTO deliveries (
    client_id, courier_id, organizer_id,
    from_address, from_lat, from_lng,
    to_address, to_lat, to_lng,
    recipient_name, recipient_phone, item_description,
    total_amount, shipping_fee, distance_km, status,
    accepted_at, picked_up_at, in_transit_at, created_at, updated_at
) VALUES (
    '189c7d79-cb21-40c1-9b7c-006ebaa3289a'::uuid, -- Padaria1
    '6186c7af-2311-4756-bfc6-ce98bd31ed27'::uuid, -- Motoboy1
    '6d401ff4-5c77-486d-9d0f-ddb2dbb13b24'::uuid, -- Samuel
    'Padaria Central, Rua Coronel Mont''Alverne, 500, Centro, Sobral - CE, 62010-000',
    -3.686667, -40.346111,
    'Rua Perimetral, 456, Terrenos Novos, Sobral - CE, 62030-000',
    -3.707222, -40.352778,
    'Fernanda Rocha',
    '85988888888',
    'Lanches e refrigerantes',
    52.00, 8.50, 2.85,
    'IN_TRANSIT',
    NOW() - INTERVAL '35 minutes',
    NOW() - INTERVAL '28 minutes',
    NOW() - INTERVAL '25 minutes',
    NOW() - INTERVAL '50 minutes',
    NOW() - INTERVAL '25 minutes'
);

-- ===== DELIVERIES COMPLETED =====

-- Delivery 9: COMPLETED - Posto
INSERT INTO deliveries (
    client_id, courier_id, organizer_id,
    from_address, from_lat, from_lng,
    to_address, to_lat, to_lng,
    recipient_name, recipient_phone, item_description,
    total_amount, shipping_fee, distance_km, status,
    accepted_at, picked_up_at, in_transit_at, completed_at,
    created_at, updated_at
) VALUES (
    'c1111111-1111-1111-1111-111111111111'::uuid,
    '6186c7af-2311-4756-bfc6-ce98bd31ed27'::uuid, -- Motoboy1
    '6d401ff4-5c77-486d-9d0f-ddb2dbb13b24'::uuid, -- Samuel
    'Posto Ipiranga, Av. John Sanford, Centro, Sobral - CE, 62010-000',
    -3.688056, -40.345833,
    'Rua Cel. José Sabóia, 321, Dom Expedito, Sobral - CE, 62050-000',
    -3.691389, -40.350556,
    'Gabriel Santos',
    '85988999999',
    'Filtro de ar',
    65.00, 6.00, 0.68,
    'COMPLETED',
    NOW() - INTERVAL '2 hours',
    NOW() - INTERVAL '1 hour 50 minutes',
    NOW() - INTERVAL '1 hour 45 minutes',
    NOW() - INTERVAL '1 hour 30 minutes',
    NOW() - INTERVAL '2 hours 15 minutes',
    NOW() - INTERVAL '1 hour 30 minutes'
);

-- Delivery 10: COMPLETED - Farmácia
INSERT INTO deliveries (
    client_id, courier_id, organizer_id,
    from_address, from_lat, from_lng,
    to_address, to_lat, to_lng,
    recipient_name, recipient_phone, item_description,
    total_amount, shipping_fee, distance_km, status,
    accepted_at, picked_up_at, in_transit_at, completed_at,
    created_at, updated_at
) VALUES (
    'c2222222-2222-2222-2222-222222222222'::uuid,
    'a1b2c3d4-e5f6-7890-abcd-ef1234567890'::uuid, -- Motoboy2
    '6d401ff4-5c77-486d-9d0f-ddb2dbb13b24'::uuid, -- Samuel
    'Farmácia Pague Menos, Av. Dom José, Dom Expedito, Sobral - CE, 62050-000',
    -3.692778, -40.351944,
    'Av. da Universidade, 1500, Betânia, Sobral - CE, 62040-000',
    -3.700833, -40.357500,
    'Luciana Martins',
    '85987111222',
    'Medicamentos controlados',
    215.80, 9.50, 1.48,
    'COMPLETED',
    NOW() - INTERVAL '3 hours',
    NOW() - INTERVAL '2 hours 50 minutes',
    NOW() - INTERVAL '2 hours 45 minutes',
    NOW() - INTERVAL '2 hours 20 minutes',
    NOW() - INTERVAL '3 hours 20 minutes',
    NOW() - INTERVAL '2 hours 20 minutes'
);

-- Delivery 11: COMPLETED - Sushi
INSERT INTO deliveries (
    client_id, courier_id, organizer_id,
    from_address, from_lat, from_lng,
    to_address, to_lat, to_lng,
    recipient_name, recipient_phone, item_description,
    total_amount, shipping_fee, distance_km, status,
    accepted_at, picked_up_at, in_transit_at, completed_at,
    created_at, updated_at
) VALUES (
    'c3333333-3333-3333-3333-333333333333'::uuid,
    '6186c7af-2311-4756-bfc6-ce98bd31ed27'::uuid, -- Motoboy1
    '6d401ff4-5c77-486d-9d0f-ddb2dbb13b24'::uuid, -- Samuel
    'Sushi Mania, Rua Osvaldo Cruz, Junco, Sobral - CE, 62030-000',
    -3.698333, -40.354722,
    'Rua Barão de Camocim, 678, Centro, Sobral - CE, 62010-000',
    -3.687500, -40.347222,
    'Patrícia Alves',
    '85987222333',
    'Festival 60 peças',
    198.00, 8.00, 1.32,
    'COMPLETED',
    NOW() - INTERVAL '5 hours',
    NOW() - INTERVAL '4 hours 50 minutes',
    NOW() - INTERVAL '4 hours 45 minutes',
    NOW() - INTERVAL '4 hours 15 minutes',
    NOW() - INTERVAL '5 hours 30 minutes',
    NOW() - INTERVAL '4 hours 15 minutes'
);

-- ===== DELIVERIES CANCELLED =====

-- Delivery 12: CANCELLED - Cliente cancelou
INSERT INTO deliveries (
    client_id,
    from_address, from_lat, from_lng,
    to_address, to_lat, to_lng,
    recipient_name, recipient_phone, item_description,
    total_amount, shipping_fee, distance_km, status,
    cancelled_at, cancellation_reason, created_at, updated_at
) VALUES (
    'c3333333-3333-3333-3333-333333333333'::uuid,
    'Sushi Mania, Rua Osvaldo Cruz, Junco, Sobral - CE, 62030-000',
    -3.698333, -40.354722,
    'Rua Dr. Guarany, 234, Centro, Sobral - CE, 62010-000',
    -3.689167, -40.346667,
    'Roberto Silva',
    '85987333444',
    'Combo Salmão',
    145.00, 7.50, 1.18,
    'CANCELLED',
    NOW() - INTERVAL '1 hour',
    'Cliente cancelou o pedido',
    NOW() - INTERVAL '1 hour 30 minutes',
    NOW() - INTERVAL '1 hour'
);

-- =====================================================
-- RESUMO DOS DADOS CRIADOS
-- =====================================================
-- 1 novo motoboy (Motoboy2)
-- 3 novos clientes (Posto, Farmácia, Sushi)
-- 12 deliveries com status:
--   - 3 PENDING (sem courier/organizer)
--   - 2 ACCEPTED (com courier/organizer)
--   - 1 PICKED_UP
--   - 2 IN_TRANSIT
--   - 3 COMPLETED (com rating)
--   - 1 CANCELLED
-- Todos com endereços reais de Sobral-CE
-- =====================================================

SELECT 'Dados de teste criados com sucesso!' as resultado;
