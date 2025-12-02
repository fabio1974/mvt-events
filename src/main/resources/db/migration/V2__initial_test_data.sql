-- ================================================================
-- V2: Initial Test Data for MVT Events Platform
-- ================================================================
-- Description: Provides anonymized test data for development and testing
-- Created: 2025-01-21
-- Purpose: Populate fresh database instances with realistic test accounts
--
-- Test Accounts Created:
-- 1. ADMIN user (admin@mvt.com / 123456)
-- 2. Two ORGANIZER users with organizations
-- 3. Four CLIENT users with contracts
-- 4. Two COURIER users with employment contracts
-- 5. Eight sample deliveries in different statuses:
--    - COMPLETED (2 deliveries)
--    - IN_TRANSIT (1 delivery)
--    - PICKED_UP (1 delivery)
--    - ACCEPTED (1 delivery)
--    - PENDING (2 deliveries: 1 CONTRACT, 1 ON_DEMAND)
--    - CANCELLED (1 delivery)
--
-- All passwords: 123456 (hashed with BCrypt)
-- All personal data (CPFs, emails, phones) is anonymized
-- Delivery dates are coherent with their status transitions
-- Geographic coordinates are generic points in Sobral-CE, Brazil
-- ================================================================

--
-- Data for Name: users
-- Passwords: All users have password "123456"
-- Note: city_id set to NULL - cities will be loaded by application startup
--

INSERT INTO public.users (id, created_at, enabled, password, role, updated_at, username, address, city_old, country, date_of_birth, document_number, gender, name, phone, state, gps_latitude, gps_longitude, city_id, latitude, longitude) VALUES 
  ('5a9ec5f8-6a5f-44d4-bb76-82ff3e872d57', '2025-11-05 08:54:14.449814', true, '$2b$10$jBRSc0Q72/6XUMo6eyLuhOyhNDsXT02rB/Rzs3OsJbaKMoprHqsOi', 'ADMIN', '2025-11-05 08:54:14.449814', 'admin@mvt.com', NULL, NULL, NULL, NULL, NULL, NULL, 'Admin User', NULL, NULL, NULL, NULL, NULL, NULL, NULL),
  ('6d401ff4-5c77-486d-9d0f-ddb2dbb13b24', '2025-11-05 09:11:30.177739', true, '$2b$10$jBRSc0Q72/6XUMo6eyLuhOyhNDsXT02rB/Rzs3OsJbaKMoprHqsOi', 'ORGANIZER', '2025-11-05 09:11:30.177745', 'organizer1@mvt.com', NULL, NULL, NULL, NULL, '11111111111', NULL, 'Organizer One', NULL, NULL, NULL, NULL, NULL, NULL, NULL),
  ('208f16bd-13a5-4887-83e7-aa095e3eeb6d', '2025-11-24 00:16:21.292022', true, '$2b$10$jBRSc0Q72/6XUMo6eyLuhOyhNDsXT02rB/Rzs3OsJbaKMoprHqsOi', 'ORGANIZER', '2025-11-24 00:16:21.292047', 'organizer2@mvt.com', NULL, NULL, NULL, NULL, '22222222222', NULL, 'Organizer Two', NULL, NULL, NULL, NULL, NULL, NULL, NULL),
  ('189c7d79-cb21-40c1-9b7c-006ebaa3289a', '2025-11-05 09:16:41.36388', true, '$2b$10$jBRSc0Q72/6XUMo6eyLuhOyhNDsXT02rB/Rzs3OsJbaKMoprHqsOi', 'CLIENT', '2025-11-21 20:47:27.629003', 'client1@mvt.com', 'Rua Exemplo, 100 - Centro, Sobral-CE', NULL, NULL, '1990-01-01', '33333333333', 'MALE', 'Client One', '85991111111', NULL, -3.686389, -40.349167, NULL, NULL, NULL),
  ('c1111111-1111-1111-1111-111111111111', '2025-11-23 19:23:21.451069', true, '$2b$10$jBRSc0Q72/6XUMo6eyLuhOyhNDsXT02rB/Rzs3OsJbaKMoprHqsOi', 'CLIENT', '2025-11-23 19:23:21.451069', 'client2@mvt.com', NULL, NULL, NULL, NULL, '44444444444', NULL, 'Client Two', '85992222222', NULL, -3.688056, -40.345833, NULL, NULL, NULL),
  ('c2222222-2222-2222-2222-222222222222', '2025-11-23 19:23:21.452665', true, '$2b$10$jBRSc0Q72/6XUMo6eyLuhOyhNDsXT02rB/Rzs3OsJbaKMoprHqsOi', 'CLIENT', '2025-11-23 19:23:21.452665', 'client3@mvt.com', NULL, NULL, NULL, NULL, '55555555555', NULL, 'Client Three', '85993333333', NULL, -3.692778, -40.351944, NULL, NULL, NULL),
  ('c3333333-3333-3333-3333-333333333333', '2025-11-23 19:23:21.45398', true, '$2b$10$jBRSc0Q72/6XUMo6eyLuhOyhNDsXT02rB/Rzs3OsJbaKMoprHqsOi', 'CLIENT', '2025-11-23 19:23:21.45398', 'client4@mvt.com', NULL, NULL, NULL, NULL, '66666666666', NULL, 'Client Four', '85994444444', NULL, -3.698333, -40.354722, NULL, NULL, NULL),
  ('6186c7af-2311-4756-bfc6-ce98bd31ed27', '2025-11-05 09:14:03.068882', true, '$2b$10$jBRSc0Q72/6XUMo6eyLuhOyhNDsXT02rB/Rzs3OsJbaKMoprHqsOi', 'COURIER', '2025-11-23 17:10:46.538936', 'courier1@mvt.com', 'Rua dos Entregadores, 200', NULL, NULL, '1995-05-05', '77777777777', 'MALE', 'Courier One', '85995555555', NULL, -3.6969445, -40.3494445, NULL, NULL, NULL),
  ('a1b2c3d4-e5f6-7890-abcd-ef1234567890', '2025-11-23 19:23:21.428805', true, '$2b$10$jBRSc0Q72/6XUMo6eyLuhOyhNDsXT02rB/Rzs3OsJbaKMoprHqsOi', 'COURIER', '2025-11-23 17:10:46.538936', 'courier2@mvt.com', NULL, NULL, NULL, NULL, '88888888888', NULL, 'Courier Two', '85996666666', NULL, -3.6969445, -40.3581945, NULL, NULL, NULL);

--
-- Data for Name: organizations
-- Each ORGANIZER user owns one organization
-- Note: city_id set to NULL - cities will be loaded by application startup
--

INSERT INTO public.organizations (id, created_at, updated_at, contact_email, description, logo_url, name, phone, slug, website, city_id, commission_percentage, status, owner_id) VALUES 
  (1, '2025-11-05 08:54:14.449814', '2025-11-05 09:15:26.358295', 'org1@mvt.com', 'Organization One - Test delivery service', NULL, 'Organization One', '8533334444', 'org-one', 'https://org1.mvt.com', NULL, 5.00, 'ACTIVE', '6d401ff4-5c77-486d-9d0f-ddb2dbb13b24'),
  (2, '2025-11-24 00:18:11.53823', '2025-11-24 00:20:34.093078', 'org2@mvt.com', 'Organization Two - Test delivery service', NULL, 'Organization Two', '8533335555', 'org-two', 'https://org2.mvt.com', NULL, 5.00, 'ACTIVE', '208f16bd-13a5-4887-83e7-aa095e3eeb6d');

--
-- Data for Name: client_contracts
-- Links clients to organizations
--

INSERT INTO public.client_contracts (id, created_at, updated_at, client_id, organization_id, is_primary, status, contract_date, start_date, end_date) VALUES 
  (2, '2025-11-06 00:46:29.1835', '2025-11-06 00:46:29.1835', '189c7d79-cb21-40c1-9b7c-006ebaa3289a', 1, true, 'ACTIVE', '2025-11-06', '2025-11-05', NULL),
  (3, '2025-11-23 19:23:21.455335', '2025-11-23 19:23:21.455335', 'c1111111-1111-1111-1111-111111111111', 1, false, 'ACTIVE', '2025-11-23', '2025-11-23', NULL),
  (4, '2025-11-23 19:23:21.455335', '2025-11-23 19:23:21.455335', 'c2222222-2222-2222-2222-222222222222', 1, false, 'ACTIVE', '2025-11-23', '2025-11-23', NULL),
  (5, '2025-11-23 19:23:21.455335', '2025-11-23 19:23:21.455335', 'c3333333-3333-3333-3333-333333333333', 1, false, 'ACTIVE', '2025-11-23', '2025-11-23', NULL),
  (11, '2025-11-24 00:38:24.266176', '2025-11-24 00:38:24.266216', 'c2222222-2222-2222-2222-222222222222', 2, false, 'ACTIVE', '2025-11-24', '2025-11-19', NULL);

--
-- Data for Name: employment_contracts
-- Links couriers to organizations
--

INSERT INTO public.employment_contracts (id, created_at, updated_at, courier_id, organization_id, linked_at, is_active) VALUES 
  (4, '2025-11-06 00:43:39.06694', '2025-11-06 00:43:39.066971', '6186c7af-2311-4756-bfc6-ce98bd31ed27', 1, '2025-11-05 09:12:52.496771', true),
  (5, '2025-11-23 19:23:21.445942', '2025-11-23 19:23:21.445942', 'a1b2c3d4-e5f6-7890-abcd-ef1234567890', 1, '2025-11-23 19:23:21.445942', true),
  (15, '2025-11-24 00:38:24.141657', '2025-11-24 00:38:24.141729', '6186c7af-2311-4756-bfc6-ce98bd31ed27', 2, '2025-11-24 00:19:10.766356', true),
  (16, '2025-11-24 00:38:24.207832', '2025-11-24 00:38:24.207878', 'a1b2c3d4-e5f6-7890-abcd-ef1234567890', 2, '2025-11-24 00:19:10.766356', true);

--
-- Data for Name: deliveries
-- Sample deliveries in different statuses with coherent dates
--

INSERT INTO public.deliveries (
  id, created_at, updated_at, client_id, courier_id, from_address, from_lat, from_lng,
  to_address, to_lat, to_lng, distance_km, estimated_time_minutes, item_description,
  recipient_name, recipient_phone, total_amount, status, accepted_at, picked_up_at,
  in_transit_at, completed_at, cancelled_at, cancellation_reason, payment_id,
  scheduled_pickup_at, delivery_type, shipping_fee, organizer_id
) VALUES
  -- COMPLETED delivery (Organization 1, Client 1, Courier 1)
  (1, '2025-11-25 10:00:00', '2025-11-25 11:30:00', '189c7d79-cb21-40c1-9b7c-006ebaa3289a', '6186c7af-2311-4756-bfc6-ce98bd31ed27',
   'Rua Exemplo, 100 - Centro, Sobral-CE', -3.686389, -40.349167,
   'Av. Principal, 500 - Bairro Novo, Sobral-CE', -3.690000, -40.355000,
   3.50, 15, 'Documentos importantes',
   'João Silva', '85991111111', 25.00, 'COMPLETED',
   '2025-11-25 10:05:00', '2025-11-25 10:15:00', '2025-11-25 10:20:00', '2025-11-25 11:30:00',
   NULL, NULL, NULL, NULL, 'CONTRACT', 10.00, NULL),

  -- IN_TRANSIT delivery (Organization 1, Client 2, Courier 2)
  (2, '2025-11-30 14:00:00', '2025-11-30 14:30:00', 'c1111111-1111-1111-1111-111111111111', 'a1b2c3d4-e5f6-7890-abcd-ef1234567890',
   'Posto Ipiranga Centro - Sobral-CE', -3.688056, -40.345833,
   'Rua das Flores, 200 - Centro, Sobral-CE', -3.692000, -40.350000,
   2.80, 12, 'Encomenda pequena',
   'Maria Santos', '85992222222', 18.50, 'IN_TRANSIT',
   '2025-11-30 14:05:00', '2025-11-30 14:20:00', '2025-11-30 14:30:00', NULL,
   NULL, NULL, NULL, NULL, 'CONTRACT', 8.00, NULL),

  -- PICKED_UP delivery (Organization 1, Client 3, Courier 1)
  (3, '2025-12-01 09:00:00', '2025-12-01 09:25:00', 'c2222222-2222-2222-2222-222222222222', '6186c7af-2311-4756-bfc6-ce98bd31ed27',
   'Farmácia Pague Menos - Sobral-CE', -3.692778, -40.351944,
   'Hospital Regional, Sobral-CE', -3.695000, -40.348000,
   2.20, 10, 'Medicamentos urgentes',
   'Dr. Carlos', '85993333333', 15.00, 'PICKED_UP',
   '2025-12-01 09:10:00', '2025-12-01 09:25:00', NULL, NULL,
   NULL, NULL, NULL, NULL, 'CONTRACT', 7.50, NULL),

  -- ACCEPTED delivery (Organization 1, Client 4, Courier 2)
  (4, '2025-12-01 15:00:00', '2025-12-01 15:10:00', 'c3333333-3333-3333-3333-333333333333', 'a1b2c3d4-e5f6-7890-abcd-ef1234567890',
   'Sushi Mania - Sobral-CE', -3.698333, -40.354722,
   'Condomínio Residencial, Sobral-CE', -3.700000, -40.360000,
   4.10, 18, 'Pedido de sushi (20 peças)',
   'Ana Paula', '85994444444', 85.00, 'ACCEPTED',
   '2025-12-01 15:10:00', NULL, NULL, NULL,
   NULL, NULL, NULL, NULL, 'CONTRACT', 12.00, NULL),

  -- PENDING delivery (Organization 1, Client 1, no courier yet)
  (5, '2025-12-02 08:00:00', '2025-12-02 08:00:00', '189c7d79-cb21-40c1-9b7c-006ebaa3289a', NULL,
   'Rua Exemplo, 100 - Centro, Sobral-CE', -3.686389, -40.349167,
   'Shopping Sobral', -3.685000, -40.342000,
   5.50, 20, 'Pacote grande',
   'Pedro Costa', '85995555555', 30.00, 'PENDING',
   NULL, NULL, NULL, NULL,
   NULL, NULL, NULL, '2025-12-02 10:00:00', 'CONTRACT', 15.00, NULL),

  -- CANCELLED delivery (Organization 2, Client 3, was assigned to Courier 1)
  (6, '2025-11-28 16:00:00', '2025-11-28 16:30:00', 'c2222222-2222-2222-2222-222222222222', '6186c7af-2311-4756-bfc6-ce98bd31ed27',
   'Farmácia Pague Menos - Sobral-CE', -3.692778, -40.351944,
   'Rua Distante, 1000 - Periferia, Sobral-CE', -3.710000, -40.370000,
   12.50, 35, 'Entrega cancelada pelo cliente',
   'José Lima', '85996666666', 45.00, 'CANCELLED',
   '2025-11-28 16:10:00', NULL, NULL, NULL,
   '2025-11-28 16:30:00', 'Cliente desistiu da compra', NULL, NULL, 'CONTRACT', 20.00, NULL),

  -- ON_DEMAND PENDING (waiting for courier acceptance)
  (7, '2025-12-02 09:30:00', '2025-12-02 09:30:00', '189c7d79-cb21-40c1-9b7c-006ebaa3289a', NULL,
   'Rua Exemplo, 100 - Centro, Sobral-CE', -3.686389, -40.349167,
   'Aeroporto de Sobral', -3.700000, -40.320000,
   8.00, 25, 'Mala de viagem urgente',
   'Roberto Alves', '85997777777', 40.00, 'PENDING',
   NULL, NULL, NULL, NULL,
   NULL, NULL, NULL, NULL, 'ON_DEMAND', 25.00, NULL),

  -- COMPLETED delivery Organization 2
  (8, '2025-11-29 11:00:00', '2025-11-29 12:45:00', 'c2222222-2222-2222-2222-222222222222', 'a1b2c3d4-e5f6-7890-abcd-ef1234567890',
   'Farmácia Pague Menos - Sobral-CE', -3.692778, -40.351944,
   'Clinica Médica Centro', -3.688000, -40.346000,
   3.20, 14, 'Material médico',
   'Enf. Juliana', '85998888888', 22.00, 'COMPLETED',
   '2025-11-29 11:08:00', '2025-11-29 11:25:00', '2025-11-29 11:30:00', '2025-11-29 12:45:00',
   NULL, NULL, NULL, NULL, 'CONTRACT', 9.00, NULL);

--
-- Sequence updates to match inserted IDs
--

SELECT pg_catalog.setval('public.contracts_id_seq', 11, true);
SELECT pg_catalog.setval('public.employment_contracts_id_seq', 16, true);
SELECT pg_catalog.setval('public.organizations_id_seq', 2, true);
SELECT pg_catalog.setval('public.deliveries_id_seq', 8, true);

-- ================================================================
-- End of V2 Migration
-- ================================================================
