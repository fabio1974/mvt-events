-- V3: Atualizar users e organizations com estabelecimentos e endereços reais de Sobral-CE
-- City ID de Sobral: 1058

-- ==============================================================================
-- 1. ATUALIZAR CLIENTES COM ESTABELECIMENTOS REAIS DE SOBRAL
-- ==============================================================================

-- Cliente 1: Farmácia Pague Menos (Centro de Sobral)
UPDATE users SET
    name = 'Farmácia Pague Menos Centro',
    address = 'Rua Coronel Mont''Alverne, 654',
    city_id = 1058,
    state = 'Ceará',
    latitude = -3.6886,
    longitude = -40.3493,
    phone = '(88) 3611-2345',
    document_number = '07.123.456/0001-89'
WHERE username = 'client1@mvt.com';

-- Cliente 2: Posto Ipiranga (Av. John Sanford)
UPDATE users SET
    name = 'Posto Ipiranga John Sanford',
    address = 'Avenida John Sanford, 1523',
    city_id = 1058,
    state = 'Ceará',
    latitude = -3.6912,
    longitude = -40.3515,
    phone = '(88) 3614-7890',
    document_number = '08.234.567/0001-90'
WHERE username = 'client2@mvt.com';

-- Cliente 3: Padaria e Confeitaria Dom Pão
UPDATE users SET
    name = 'Padaria Dom Pão',
    address = 'Rua Conselheiro José Júlio, 892',
    city_id = 1058,
    state = 'Ceará',
    latitude = -3.6895,
    longitude = -40.3478,
    phone = '(88) 3613-4567',
    document_number = '09.345.678/0001-01'
WHERE username = 'client3@mvt.com';

-- Cliente 4: Supermercado São Vicente (Derby)
UPDATE users SET
    name = 'Supermercado São Vicente',
    address = 'Rua Desembargador Lustosa, 445',
    city_id = 1058,
    state = 'Ceará',
    latitude = -3.6923,
    longitude = -40.3502,
    phone = '(88) 3612-8901',
    document_number = '10.456.789/0001-12'
WHERE username = 'client4@mvt.com';

-- ==============================================================================
-- 2. ATUALIZAR COURIERS COM ENDEREÇOS REAIS DE SOBRAL
-- ==============================================================================

-- Courier 1: Motoboy João (Residência no Alto do Cristo)
UPDATE users SET
    name = 'João Silva Santos',
    address = 'Rua Padre Ibiapina, 234',
    city_id = 1058,
    state = 'Ceará',
    latitude = -3.6858,
    longitude = -40.3445,
    phone = '(88) 99876-5432',
    document_number = '123.456.789-01'
WHERE username = 'courier1@mvt.com';

-- Courier 2: Motoboy Carlos (Residência no Pedrinhas)
UPDATE users SET
    name = 'Carlos Eduardo Lima',
    address = 'Rua Deputado José Parente, 567',
    city_id = 1058,
    state = 'Ceará',
    latitude = -3.6945,
    longitude = -40.3528,
    phone = '(88) 99765-4321',
    document_number = '234.567.890-12'
WHERE username = 'courier2@mvt.com';

-- ==============================================================================
-- 3. ATUALIZAR ORGANIZERS COM ENDEREÇOS REAIS DE SOBRAL
-- ==============================================================================

-- Organizer 1: Rodrigo (Escritório no Centro)
UPDATE users SET
    name = 'Rodrigo Alves Sousa',
    address = 'Rua do Patrocínio, 123',
    city_id = 1058,
    state = 'Ceará',
    latitude = -3.6901,
    longitude = -40.3485,
    phone = '(88) 99123-4567',
    document_number = '345.678.901-23'
WHERE username = 'organizer1@mvt.com';

-- Organizer 2: Samuel (Escritório próximo ao Derby)
UPDATE users SET
    name = 'Samuel Ferreira Costa',
    address = 'Rua Cel. Mont''Alverne, 890',
    city_id = 1058,
    state = 'Ceará',
    latitude = -3.6915,
    longitude = -40.3497,
    phone = '(88) 99234-5678',
    document_number = '456.789.012-34'
WHERE username = 'organizer2@mvt.com';

-- ==============================================================================
-- 4. ATUALIZAR ADMIN COM ENDEREÇO REAL DE SOBRAL
-- ==============================================================================

-- Admin: Fábio Barros (Escritório MovelTrack)
UPDATE users SET
    name = 'Fábio Barros',
    address = 'Avenida John Sanford, 800 - Sala 201',
    city_id = 1058,
    state = 'Ceará',
    latitude = -3.6890,
    longitude = -40.3505,
    phone = '(88) 99345-6789',
    document_number = '567.890.123-45'
WHERE username = 'admin@mvt.com';

-- ==============================================================================
-- 5. ATUALIZAR ORGANIZATIONS COM DADOS REAIS DE SOBRAL
-- ==============================================================================

-- Organization 1: MovelTrack Sobral
UPDATE organizations SET
    name = 'MovelTrack Sobral',
    city_id = 1058,
    phone = '(88) 3614-5000',
    contact_email = 'contato@moveltrack-sobral.com.br',
    description = 'Empresa de logística e entregas rápidas em Sobral-CE',
    website = 'https://moveltrack-sobral.com.br'
WHERE id = 1;

-- Organization 2: Express Delivery Sobral
UPDATE organizations SET
    name = 'Express Delivery Sobral',
    city_id = 1058,
    phone = '(88) 3615-6000',
    contact_email = 'contato@expressdelivery-sobral.com.br',
    description = 'Serviço de entregas expressas e agendadas em Sobral-CE',
    website = 'https://expressdelivery-sobral.com.br'
WHERE id = 2;

-- ==============================================================================
-- 6. CRIAR NOVAS DELIVERIES COM ENDEREÇOS RESIDENCIAIS REAIS DE SOBRAL
-- ==============================================================================

-- Delivery 9: Farmácia Pague Menos → Residência no Bairro Dom Expedito
INSERT INTO deliveries (
    id, created_at, updated_at, status,
    accepted_at, picked_up_at, in_transit_at, completed_at,
    from_address, from_lat, from_lng,
    to_address, to_lat, to_lng,
    delivery_type, shipping_fee, distance_km,
    recipient_name, recipient_phone,
    item_description, total_amount,
    client_id, courier_id, organizer_id
) VALUES (
    9,
    '2025-12-02 08:30:00', '2025-12-02 08:30:00', 'PENDING',
    NULL, NULL, NULL, NULL,
    'Rua Coronel Mont''Alverne, 654 - Centro, Sobral-CE', -3.6886, -40.3493,
    'Rua Barão de Aquiraz, 234 - Dom Expedito, Sobral-CE', -3.6825, -40.3412,
    'ON_DEMAND', 8.50, 4.2,
    'Maria Santos Silva', '(88) 98765-4321',
    'Medicamentos de uso contínuo (2 caixas)', 8.50,
    (SELECT id FROM users WHERE username = 'client1@mvt.com'),
    NULL,
    (SELECT id FROM users WHERE username = 'organizer1@mvt.com')
);

-- Delivery 10: Posto Ipiranga → Residência no Bairro Sinhá Sabóia
INSERT INTO deliveries (
    id, created_at, updated_at, status,
    accepted_at, picked_up_at, in_transit_at, completed_at,
    from_address, from_lat, from_lng,
    to_address, to_lat, to_lng,
    delivery_type, shipping_fee, distance_km, total_amount,
    recipient_name, recipient_phone, item_description,
    client_id, courier_id, organizer_id
) VALUES (
    10,
    '2025-12-02 09:15:00', '2025-12-02 09:20:00', 'ACCEPTED',
    '2025-12-02 09:20:00', NULL, NULL, NULL,
    'Avenida John Sanford, 1523 - Centro, Sobral-CE', -3.6912, -40.3515,
    'Rua José Maria Alcântara, 678 - Sinhá Sabóia, Sobral-CE', -3.6978, -40.3568,
    'ON_DEMAND', 12.00, 5.8, 12.00,
    'Pedro Henrique Costa', '(88) 98654-3210', 'Galão de água mineral 20L',
    (SELECT id FROM users WHERE username = 'client2@mvt.com'),
    (SELECT id FROM users WHERE username = 'courier1@mvt.com'),
    (SELECT id FROM users WHERE username = 'organizer1@mvt.com')
);

-- Delivery 11: Padaria Dom Pão → Residência no Bairro Junco
INSERT INTO deliveries (
    id, created_at, updated_at, status,
    accepted_at, picked_up_at, in_transit_at, completed_at,
    from_address, from_lat, from_lng,
    to_address, to_lat, to_lng,
    delivery_type, shipping_fee, distance_km, total_amount,
    recipient_name, recipient_phone, item_description,
    client_id, courier_id, organizer_id
) VALUES (
    11,
    '2025-12-02 07:00:00', '2025-12-02 07:05:00', 'PICKED_UP',
    '2025-12-02 07:02:00', '2025-12-02 07:05:00', NULL, NULL,
    'Rua Conselheiro José Júlio, 892 - Centro, Sobral-CE', -3.6895, -40.3478,
    'Rua Francisco Sá, 1234 - Junco, Sobral-CE', -3.6795, -40.3395,
    'CONTRACT', 7.50, 3.5, 7.50,
    'Ana Paula Oliveira', '(88) 98543-2109', 'Pães franceses (50 unidades) e 2 bolos',
    (SELECT id FROM users WHERE username = 'client3@mvt.com'),
    (SELECT id FROM users WHERE username = 'courier2@mvt.com'),
    (SELECT id FROM users WHERE username = 'organizer1@mvt.com')
);

-- Delivery 12: Supermercado São Vicente → Residência no Campo dos Velhos
INSERT INTO deliveries (
    id, created_at, updated_at, status,
    accepted_at, picked_up_at, in_transit_at, completed_at,
    from_address, from_lat, from_lng,
    to_address, to_lat, to_lng,
    delivery_type, shipping_fee, distance_km, total_amount,
    recipient_name, recipient_phone, item_description,
    client_id, courier_id, organizer_id
) VALUES (
    12,
    '2025-12-02 10:00:00', '2025-12-02 10:05:00', 'IN_TRANSIT',
    '2025-12-02 10:02:00', '2025-12-02 10:05:00', '2025-12-02 10:08:00', NULL,
    'Rua Desembargador Lustosa, 445 - Derby, Sobral-CE', -3.6923, -40.3502,
    'Rua Coronel Beviláqua, 456 - Campo dos Velhos, Sobral-CE', -3.6952, -40.3445,
    'CONTRACT', 10.00, 4.8, 10.00,
    'José Carlos Mendes', '(88) 98432-1098', 'Compras do mês (3 sacolas)',
    (SELECT id FROM users WHERE username = 'client4@mvt.com'),
    (SELECT id FROM users WHERE username = 'courier1@mvt.com'),
    (SELECT id FROM users WHERE username = 'organizer2@mvt.com')
);

-- Delivery 13: Farmácia Pague Menos → Residência no Cohab II
INSERT INTO deliveries (
    id, created_at, updated_at, status,
    accepted_at, picked_up_at, in_transit_at, completed_at,
    from_address, from_lat, from_lng,
    to_address, to_lat, to_lng,
    delivery_type, shipping_fee, distance_km, total_amount,
    recipient_name, recipient_phone, item_description,
    client_id, courier_id, organizer_id
) VALUES (
    13,
    '2025-12-01 16:30:00', '2025-12-01 17:15:00', 'COMPLETED',
    '2025-12-01 16:35:00', '2025-12-01 16:40:00', '2025-12-01 16:45:00', '2025-12-01 17:15:00',
    'Rua Coronel Mont''Alverne, 654 - Centro, Sobral-CE', -3.6886, -40.3493,
    'Rua Monsenhor Aloísio Pinto, 789 - Cohab II, Sobral-CE', -3.7025, -40.3625,
    'ON_DEMAND', 15.00, 7.2, 15.00,
    'Francisca das Chagas', '(88) 98321-0987', 'Medicamentos refrigerados',
    (SELECT id FROM users WHERE username = 'client1@mvt.com'),
    (SELECT id FROM users WHERE username = 'courier2@mvt.com'),
    (SELECT id FROM users WHERE username = 'organizer1@mvt.com')
);

-- Delivery 14: Posto Ipiranga → Residência na Vila União
INSERT INTO deliveries (
    id, created_at, updated_at, status,
    accepted_at, picked_up_at, in_transit_at, completed_at,
    from_address, from_lat, from_lng,
    to_address, to_lat, to_lng,
    delivery_type, shipping_fee, distance_km, total_amount,
    recipient_name, recipient_phone, item_description,
    client_id, courier_id, organizer_id
) VALUES (
    14,
    '2025-12-01 14:20:00', '2025-12-01 15:10:00', 'COMPLETED',
    '2025-12-01 14:25:00', '2025-12-01 14:30:00', '2025-12-01 14:35:00', '2025-12-01 15:10:00',
    'Avenida John Sanford, 1523 - Centro, Sobral-CE', -3.6912, -40.3515,
    'Rua Pernambuco, 345 - Vila União, Sobral-CE', -3.6998, -40.3548,
    'CONTRACT', 9.50, 4.5, 9.50,
    'Antônio Marcos Silva', '(88) 98210-9876', 'Botijão de gás 13kg',
    (SELECT id FROM users WHERE username = 'client2@mvt.com'),
    (SELECT id FROM users WHERE username = 'courier1@mvt.com'),
    (SELECT id FROM users WHERE username = 'organizer2@mvt.com')
);

-- Delivery 15: Padaria Dom Pão → Residência no Centro (próximo à Praça do Patrocínio)
INSERT INTO deliveries (
    id, created_at, updated_at, status,
    accepted_at, picked_up_at, in_transit_at, completed_at,
    from_address, from_lat, from_lng,
    to_address, to_lat, to_lng,
    delivery_type, shipping_fee, distance_km, total_amount,
    recipient_name, recipient_phone, item_description,
    client_id, courier_id, organizer_id
) VALUES (
    15,
    '2025-12-02 11:45:00', '2025-12-02 11:45:00', 'PENDING',
    NULL, NULL, NULL, NULL,
    'Rua Conselheiro José Júlio, 892 - Centro, Sobral-CE', -3.6895, -40.3478,
    'Rua do Patrocínio, 567 - Centro, Sobral-CE', -3.6905, -40.3488,
    'ON_DEMAND', 5.50, 1.2, 5.50,
    'Lúcia Maria Souza', '(88) 98109-8765', 'Salgados variados para festa (50 unidades)',
    (SELECT id FROM users WHERE username = 'client3@mvt.com'),
    NULL,
    (SELECT id FROM users WHERE username = 'organizer2@mvt.com')
);

-- Delivery 16: Supermercado São Vicente → Residência no Padre Palhano
INSERT INTO deliveries (
    id, created_at, updated_at, status,
    accepted_at, picked_up_at, in_transit_at, completed_at,
    from_address, from_lat, from_lng,
    to_address, to_lat, to_lng,
    delivery_type, shipping_fee, distance_km, total_amount,
    recipient_name, recipient_phone, item_description,
    client_id, courier_id, organizer_id
) VALUES (
    16,
    '2025-12-02 12:30:00', '2025-12-02 12:35:00', 'ACCEPTED',
    '2025-12-02 12:35:00', NULL, NULL, NULL,
    'Rua Desembargador Lustosa, 445 - Derby, Sobral-CE', -3.6923, -40.3502,
    'Rua Padre Palhano, 890 - Padre Palhano, Sobral-CE', -3.6868, -40.3532,
    'CONTRACT', 11.50, 5.5, 11.50,
    'Roberto Carlos Lima', '(88) 98098-7654', 'Lista completa de supermercado (5 sacolas)',
    (SELECT id FROM users WHERE username = 'client4@mvt.com'),
    (SELECT id FROM users WHERE username = 'courier2@mvt.com'),
    (SELECT id FROM users WHERE username = 'organizer1@mvt.com')
);

-- ==============================================================================
-- 7. ATUALIZAR SEQUENCE PARA PRÓXIMAS DELIVERIES
-- ==============================================================================

SELECT setval('deliveries_id_seq', 16, true);

-- ==============================================================================
-- COMMIT
-- ==============================================================================
-- Migration V3 completed successfully
-- Updated all users and organizations with real Sobral-CE addresses
-- Created 8 new deliveries with real pickup and delivery addresses in Sobral

-- Update delivery sequence to reflect new records
SELECT setval('deliveries_id_seq', (SELECT MAX(id) FROM deliveries));
