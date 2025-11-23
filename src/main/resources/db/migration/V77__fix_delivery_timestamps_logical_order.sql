-- =====================================================
-- V77: Corrigir Timestamps das Deliveries
-- =====================================================
-- Garante que todos os timestamps seguem ordem lógica:
-- created_at < accepted_at < picked_up_at < in_transit_at < completed_at
-- =====================================================

-- Limpar deliveries e recriar com timestamps corretos
DELETE FROM deliveries;

-- ===== DELIVERIES PENDING (sem courier, sem organizer, sem timestamps de ação) =====

-- Delivery 1: PENDING - Posto para residência
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

-- Delivery 2: PENDING - Farmácia para residência
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

-- Delivery 3: PENDING - Sushi para residência
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

-- ===== DELIVERIES ACCEPTED =====
-- Ordem: created_at → accepted_at

-- Delivery 4: ACCEPTED - Padaria1
INSERT INTO deliveries (
    client_id, courier_id, organizer_id,
    from_address, from_lat, from_lng,
    to_address, to_lat, to_lng,
    recipient_name, recipient_phone, item_description,
    total_amount, shipping_fee, distance_km, status,
    created_at, updated_at, accepted_at
) VALUES (
    '189c7d79-cb21-40c1-9b7c-006ebaa3289a'::uuid,
    '6186c7af-2311-4756-bfc6-ce98bd31ed27'::uuid,
    '6d401ff4-5c77-486d-9d0f-ddb2dbb13b24'::uuid,
    'Padaria Central, Rua Coronel Mont''Alverne, 500, Centro, Sobral - CE, 62010-000',
    -3.686667, -40.346111,
    'Rua Benjamin Constant, 234, Centro, Sobral - CE, 62010-000',
    -3.688333, -40.347778,
    'Ana Paula',
    '85988444444',
    'Pães e bolos',
    45.00, 5.00, 0.35,
    'ACCEPTED',
    NOW() - INTERVAL '20 minutes', -- created_at
    NOW() - INTERVAL '5 minutes',  -- updated_at
    NOW() - INTERVAL '5 minutes'   -- accepted_at (depois de created_at)
);

-- Delivery 5: ACCEPTED - Posto
INSERT INTO deliveries (
    client_id, courier_id, organizer_id,
    from_address, from_lat, from_lng,
    to_address, to_lat, to_lng,
    recipient_name, recipient_phone, item_description,
    total_amount, shipping_fee, distance_km, status,
    created_at, updated_at, accepted_at
) VALUES (
    'c1111111-1111-1111-1111-111111111111'::uuid,
    'a1b2c3d4-e5f6-7890-abcd-ef1234567890'::uuid,
    '6d401ff4-5c77-486d-9d0f-ddb2dbb13b24'::uuid,
    'Posto Ipiranga, Av. John Sanford, Centro, Sobral - CE, 62010-000',
    -3.688056, -40.345833,
    'Rua Padre Ibiapina, 890, Centro, Sobral - CE, 62010-000',
    -3.690278, -40.348333,
    'Carlos Mendes',
    '85988555555',
    'Acessórios automotivos',
    230.00, 7.00, 0.52,
    'ACCEPTED',
    NOW() - INTERVAL '12 minutes', -- created_at
    NOW() - INTERVAL '3 minutes',  -- updated_at
    NOW() - INTERVAL '3 minutes'   -- accepted_at
);

-- ===== DELIVERIES PICKED_UP =====
-- Ordem: created_at → accepted_at → picked_up_at

-- Delivery 6: PICKED_UP - Farmácia
INSERT INTO deliveries (
    client_id, courier_id, organizer_id,
    from_address, from_lat, from_lng,
    to_address, to_lat, to_lng,
    recipient_name, recipient_phone, item_description,
    total_amount, shipping_fee, distance_km, status,
    created_at, updated_at, accepted_at, picked_up_at
) VALUES (
    'c2222222-2222-2222-2222-222222222222'::uuid,
    '6186c7af-2311-4756-bfc6-ce98bd31ed27'::uuid,
    '6d401ff4-5c77-486d-9d0f-ddb2dbb13b24'::uuid,
    'Farmácia Pague Menos, Av. Dom José, Dom Expedito, Sobral - CE, 62050-000',
    -3.692778, -40.351944,
    'Rua São João, 567, Dom Expedito, Sobral - CE, 62050-000',
    -3.694444, -40.353611,
    'Beatriz Oliveira',
    '85988666666',
    'Vitaminas e suplementos',
    89.50, 5.50, 0.28,
    'PICKED_UP',
    NOW() - INTERVAL '30 minutes', -- created_at
    NOW() - INTERVAL '2 minutes',  -- updated_at
    NOW() - INTERVAL '10 minutes', -- accepted_at (depois de created_at)
    NOW() - INTERVAL '2 minutes'   -- picked_up_at (depois de accepted_at)
);

-- ===== DELIVERIES IN_TRANSIT =====
-- Ordem: created_at → accepted_at → picked_up_at → in_transit_at

-- Delivery 7: IN_TRANSIT - Sushi (Motoboy2)
INSERT INTO deliveries (
    client_id, courier_id, organizer_id,
    from_address, from_lat, from_lng,
    to_address, to_lat, to_lng,
    recipient_name, recipient_phone, item_description,
    total_amount, shipping_fee, distance_km, status,
    created_at, updated_at, accepted_at, picked_up_at, in_transit_at
) VALUES (
    'c3333333-3333-3333-3333-333333333333'::uuid,
    'a1b2c3d4-e5f6-7890-abcd-ef1234567890'::uuid,
    '6d401ff4-5c77-486d-9d0f-ddb2dbb13b24'::uuid,
    'Sushi Mania, Rua Osvaldo Cruz, Junco, Sobral - CE, 62030-000',
    -3.698333, -40.354722,
    'Av. Deputado José Martins Rodrigues, 1234, Sumaré, Sobral - CE, 62042-000',
    -3.695556, -40.361667,
    'Ricardo Lima',
    '85988777777',
    'Combo Executivo',
    78.90, 7.50, 1.12,
    'IN_TRANSIT',
    NOW() - INTERVAL '40 minutes', -- created_at
    NOW() - INTERVAL '15 minutes', -- updated_at
    NOW() - INTERVAL '25 minutes', -- accepted_at (depois de created_at)
    NOW() - INTERVAL '18 minutes', -- picked_up_at (depois de accepted_at)
    NOW() - INTERVAL '15 minutes'  -- in_transit_at (depois de picked_up_at)
);

-- Delivery 8: IN_TRANSIT - Padaria1 (Motoboy1)
INSERT INTO deliveries (
    client_id, courier_id, organizer_id,
    from_address, from_lat, from_lng,
    to_address, to_lat, to_lng,
    recipient_name, recipient_phone, item_description,
    total_amount, shipping_fee, distance_km, status,
    created_at, updated_at, accepted_at, picked_up_at, in_transit_at
) VALUES (
    '189c7d79-cb21-40c1-9b7c-006ebaa3289a'::uuid,
    '6186c7af-2311-4756-bfc6-ce98bd31ed27'::uuid,
    '6d401ff4-5c77-486d-9d0f-ddb2dbb13b24'::uuid,
    'Padaria Central, Rua Coronel Mont''Alverne, 500, Centro, Sobral - CE, 62010-000',
    -3.686667, -40.346111,
    'Rua Perimetral, 456, Terrenos Novos, Sobral - CE, 62030-000',
    -3.707222, -40.352778,
    'Fernanda Rocha',
    '85988888888',
    'Lanches e refrigerantes',
    52.00, 8.50, 2.85,
    'IN_TRANSIT',
    NOW() - INTERVAL '50 minutes', -- created_at
    NOW() - INTERVAL '25 minutes', -- updated_at
    NOW() - INTERVAL '35 minutes', -- accepted_at (depois de created_at)
    NOW() - INTERVAL '28 minutes', -- picked_up_at (depois de accepted_at)
    NOW() - INTERVAL '25 minutes'  -- in_transit_at (depois de picked_up_at)
);

-- ===== DELIVERIES COMPLETED =====
-- Ordem: created_at → accepted_at → picked_up_at → in_transit_at → completed_at

-- Delivery 9: COMPLETED - Posto
INSERT INTO deliveries (
    client_id, courier_id, organizer_id,
    from_address, from_lat, from_lng,
    to_address, to_lat, to_lng,
    recipient_name, recipient_phone, item_description,
    total_amount, shipping_fee, distance_km, status,
    created_at, updated_at, accepted_at, picked_up_at, in_transit_at, completed_at
) VALUES (
    'c1111111-1111-1111-1111-111111111111'::uuid,
    '6186c7af-2311-4756-bfc6-ce98bd31ed27'::uuid,
    '6d401ff4-5c77-486d-9d0f-ddb2dbb13b24'::uuid,
    'Posto Ipiranga, Av. John Sanford, Centro, Sobral - CE, 62010-000',
    -3.688056, -40.345833,
    'Rua Cel. José Sabóia, 321, Dom Expedito, Sobral - CE, 62050-000',
    -3.691389, -40.350556,
    'Gabriel Santos',
    '85988999999',
    'Filtro de ar',
    65.00, 6.00, 0.68,
    'COMPLETED',
    NOW() - INTERVAL '2 hours 15 minutes', -- created_at
    NOW() - INTERVAL '1 hour 30 minutes',  -- updated_at
    NOW() - INTERVAL '2 hours',            -- accepted_at (depois de created_at)
    NOW() - INTERVAL '1 hour 50 minutes',  -- picked_up_at (depois de accepted_at)
    NOW() - INTERVAL '1 hour 45 minutes',  -- in_transit_at (depois de picked_up_at)
    NOW() - INTERVAL '1 hour 30 minutes'   -- completed_at (depois de in_transit_at)
);

-- Delivery 10: COMPLETED - Farmácia
INSERT INTO deliveries (
    client_id, courier_id, organizer_id,
    from_address, from_lat, from_lng,
    to_address, to_lat, to_lng,
    recipient_name, recipient_phone, item_description,
    total_amount, shipping_fee, distance_km, status,
    created_at, updated_at, accepted_at, picked_up_at, in_transit_at, completed_at
) VALUES (
    'c2222222-2222-2222-2222-222222222222'::uuid,
    'a1b2c3d4-e5f6-7890-abcd-ef1234567890'::uuid,
    '6d401ff4-5c77-486d-9d0f-ddb2dbb13b24'::uuid,
    'Farmácia Pague Menos, Av. Dom José, Dom Expedito, Sobral - CE, 62050-000',
    -3.692778, -40.351944,
    'Av. da Universidade, 1500, Betânia, Sobral - CE, 62040-000',
    -3.700833, -40.357500,
    'Luciana Martins',
    '85987111222',
    'Medicamentos controlados',
    215.80, 9.50, 1.48,
    'COMPLETED',
    NOW() - INTERVAL '3 hours 20 minutes', -- created_at
    NOW() - INTERVAL '2 hours 20 minutes', -- updated_at
    NOW() - INTERVAL '3 hours',            -- accepted_at (depois de created_at)
    NOW() - INTERVAL '2 hours 50 minutes', -- picked_up_at (depois de accepted_at)
    NOW() - INTERVAL '2 hours 45 minutes', -- in_transit_at (depois de picked_up_at)
    NOW() - INTERVAL '2 hours 20 minutes'  -- completed_at (depois de in_transit_at)
);

-- Delivery 11: COMPLETED - Sushi
INSERT INTO deliveries (
    client_id, courier_id, organizer_id,
    from_address, from_lat, from_lng,
    to_address, to_lat, to_lng,
    recipient_name, recipient_phone, item_description,
    total_amount, shipping_fee, distance_km, status,
    created_at, updated_at, accepted_at, picked_up_at, in_transit_at, completed_at
) VALUES (
    'c3333333-3333-3333-3333-333333333333'::uuid,
    '6186c7af-2311-4756-bfc6-ce98bd31ed27'::uuid,
    '6d401ff4-5c77-486d-9d0f-ddb2dbb13b24'::uuid,
    'Sushi Mania, Rua Osvaldo Cruz, Junco, Sobral - CE, 62030-000',
    -3.698333, -40.354722,
    'Rua Barão de Camocim, 678, Centro, Sobral - CE, 62010-000',
    -3.687500, -40.347222,
    'Patrícia Alves',
    '85987222333',
    'Festival 60 peças',
    198.00, 8.00, 1.32,
    'COMPLETED',
    NOW() - INTERVAL '5 hours 30 minutes', -- created_at
    NOW() - INTERVAL '4 hours 15 minutes', -- updated_at
    NOW() - INTERVAL '5 hours',            -- accepted_at (depois de created_at)
    NOW() - INTERVAL '4 hours 50 minutes', -- picked_up_at (depois de accepted_at)
    NOW() - INTERVAL '4 hours 45 minutes', -- in_transit_at (depois de picked_up_at)
    NOW() - INTERVAL '4 hours 15 minutes'  -- completed_at (depois de in_transit_at)
);

-- ===== DELIVERIES CANCELLED =====
-- Ordem: created_at → cancelled_at (sem accepted_at pois foi cancelada antes)

-- Delivery 12: CANCELLED - Cliente cancelou antes de aceitar
INSERT INTO deliveries (
    client_id,
    from_address, from_lat, from_lng,
    to_address, to_lat, to_lng,
    recipient_name, recipient_phone, item_description,
    total_amount, shipping_fee, distance_km, status,
    created_at, updated_at, cancelled_at, cancellation_reason
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
    NOW() - INTERVAL '1 hour 30 minutes', -- created_at
    NOW() - INTERVAL '1 hour',            -- updated_at
    NOW() - INTERVAL '1 hour',            -- cancelled_at (depois de created_at)
    'Cliente cancelou o pedido'
);

-- =====================================================
-- RESUMO DA MIGRATION V77
-- =====================================================
-- ✅ Todos os timestamps seguem ordem lógica crescente:
--    PENDING: created_at apenas
--    ACCEPTED: created_at < accepted_at
--    PICKED_UP: created_at < accepted_at < picked_up_at
--    IN_TRANSIT: created_at < accepted_at < picked_up_at < in_transit_at
--    COMPLETED: created_at < accepted_at < picked_up_at < in_transit_at < completed_at
--    CANCELLED: created_at < cancelled_at
-- ✅ 12 deliveries com histórico temporal correto
-- =====================================================
