-- ============================================================================
-- V74: Demo accounts for Apple App Store review
-- ============================================================================
-- Creates 4 demo users with all required data for Apple reviewers to test
-- the app's functionality during review. Each user has a different role:
--   - CUSTOMER (João Demo)   → demo.customer@zapi10.com / Demo@123
--   - CLIENT   (Maria Demo)  → demo.client@zapi10.com   / Demo@123
--   - COURIER  (Pedro Demo)  → demo.courier@zapi10.com  / Demo@123
--   - ORGANIZER(Ana Demo)    → demo.organizer@zapi10.com/ Demo@123
--
-- Convention: Demo accounts use email pattern demo.*@zapi10.com
-- These accounts are protected from self-deletion via DELETE /api/users/me
-- ============================================================================

DO $$
DECLARE
    v_customer_id  UUID := 'a0000000-0000-0000-0000-000000000001';
    v_client_id    UUID := 'a0000000-0000-0000-0000-000000000002';
    v_courier_id   UUID := 'a0000000-0000-0000-0000-000000000003';
    v_organizer_id UUID := 'a0000000-0000-0000-0000-000000000004';
    -- BCrypt hash of 'Demo@123' (cost 10)
    v_pw TEXT := '$2a$10$VD3nTQsAOlGiszN5S9r.sOz9HDfyhhxW5vbj6ERcqz8VllT/PJjqu';
    v_org_id   BIGINT;
    v_city_id  BIGINT;
    v_del1 BIGINT;
    v_del2 BIGINT;
    v_del3 BIGINT;
    v_del4 BIGINT;
    v_del5 BIGINT;
    v_pay1 BIGINT;
    v_pay2 BIGINT;
    v_pay3 BIGINT;
    v_pay4 BIGINT;
    v_pay5 BIGINT;
    v_vehicle_id BIGINT;
BEGIN

    -- Resolve a city (nullable, best-effort)
    SELECT id INTO v_city_id FROM cities LIMIT 1;

    -- ========================================================================
    -- 1. USERS
    -- ========================================================================

    INSERT INTO users (id, created_at, updated_at, username, name, password, role,
                       enabled, blocked, confirmed, document_number,
                       date_of_birth, gender, phone_ddd, phone_number)
    VALUES
        -- CUSTOMER
        (v_customer_id, NOW(), NOW(), 'demo.customer@zapi10.com', 'João Demo', v_pw, 'CUSTOMER',
         true, false, true, '00100200301',
         '1990-05-15', 'MALE', '11', '999990001'),
        -- CLIENT
        (v_client_id, NOW(), NOW(), 'demo.client@zapi10.com', 'Maria Demo', v_pw, 'CLIENT',
         true, false, true, '00200300402',
         '1988-03-22', 'FEMALE', '11', '999990002'),
        -- COURIER
        (v_courier_id, NOW(), NOW(), 'demo.courier@zapi10.com', 'Pedro Demo', v_pw, 'COURIER',
         true, false, true, '00300400503',
         '1995-08-10', 'MALE', '11', '999990003'),
        -- ORGANIZER
        (v_organizer_id, NOW(), NOW(), 'demo.organizer@zapi10.com', 'Ana Demo', v_pw, 'ORGANIZER',
         true, false, true, '00400500604',
         '1992-11-30', 'FEMALE', '11', '999990004');

    -- Set COURIER-specific fields
    UPDATE users SET service_type = 'DELIVERY',
                     pagarme_recipient_id = 'rp_demo_courier_001',
                     pagarme_status = 'active'
    WHERE id = v_courier_id;

    -- Set ORGANIZER-specific fields
    UPDATE users SET pagarme_recipient_id = 'rp_demo_organizer_001',
                     pagarme_status = 'active'
    WHERE id = v_organizer_id;

    -- ========================================================================
    -- 2. BANK ACCOUNTS (COURIER + ORGANIZER)
    -- ========================================================================

    INSERT INTO bank_accounts (created_at, updated_at, user_id, bank_code, bank_name,
                               agency, account_number, account_type, status,
                               transfer_interval, transfer_day)
    VALUES
        (NOW(), NOW(), v_courier_id, '001', 'Banco do Brasil',
         '1234', '567890', 'CHECKING', 'ACTIVE', 'Daily', 0),
        (NOW(), NOW(), v_organizer_id, '341', 'Itaú Unibanco',
         '5678', '123456', 'CHECKING', 'ACTIVE', 'Daily', 0);

    -- ========================================================================
    -- 3. VEHICLE (COURIER)
    -- ========================================================================

    INSERT INTO vehicles (created_at, updated_at, owner_id, type, plate, brand, model, color, year, is_active)
    VALUES (NOW(), NOW(), v_courier_id, 'MOTORCYCLE', 'DMO1A23', 'Honda', 'CG 160 Fan', 'PRETO', '2023', true)
    RETURNING id INTO v_vehicle_id;

    -- ========================================================================
    -- 4. ORGANIZATION (ORGANIZER)
    -- ========================================================================

    INSERT INTO organizations (created_at, updated_at, name, slug,
                               status, commission_percentage, owner_id)
    VALUES (NOW(), NOW(), 'Demo Express Entregas', 'demo-express-entregas',
            'ACTIVE', 5.00, v_organizer_id)
    RETURNING id INTO v_org_id;

    -- ========================================================================
    -- 5. EMPLOYMENT CONTRACT (COURIER <-> ORGANIZATION)
    -- ========================================================================

    INSERT INTO employment_contracts (created_at, updated_at, courier_id, organization_id, linked_at, is_active)
    VALUES (NOW(), NOW(), v_courier_id, v_org_id, NOW(), true);

    -- ========================================================================
    -- 6. CLIENT CONTRACT (CLIENT <-> ORGANIZATION)
    -- ========================================================================

    INSERT INTO client_contracts (created_at, updated_at, client_id, organization_id,
                                  is_primary, status, start_date)
    VALUES (NOW(), NOW(), v_client_id, v_org_id, true, 'ACTIVE', CURRENT_DATE);

    -- ========================================================================
    -- 7. CUSTOMER PAYMENT PREFERENCES (CUSTOMER + CLIENT → PIX)
    -- ========================================================================

    INSERT INTO customer_payment_preferences (user_id, preferred_payment_type, created_at, updated_at)
    VALUES
        (v_customer_id, 'PIX', NOW(), NOW()),
        (v_client_id, 'PIX', NOW(), NOW());

    -- ========================================================================
    -- 8. ADDRESSES (one default address per user)
    -- ========================================================================

    INSERT INTO addresses (created_at, updated_at, user_id, city_id, street, number,
                           neighborhood, zip_code, latitude, longitude, is_default)
    VALUES
        (NOW(), NOW(), v_customer_id, v_city_id, 'Rua das Flores', '100',
         'Centro', '01001000', -23.5505, -46.6333, true),
        (NOW(), NOW(), v_client_id, v_city_id, 'Av. Paulista', '1000',
         'Bela Vista', '01310100', -23.5614, -46.6558, true),
        (NOW(), NOW(), v_courier_id, v_city_id, 'Rua Augusta', '500',
         'Consolação', '01304000', -23.5536, -46.6580, true),
        (NOW(), NOW(), v_organizer_id, v_city_id, 'Rua Oscar Freire', '200',
         'Jardins', '01426001', -23.5628, -46.6690, true);

    -- ========================================================================
    -- 9. DELIVERIES (5 sample deliveries)
    -- ========================================================================

    -- Delivery 1: COMPLETED (client → courier, with organizer)
    INSERT INTO deliveries (created_at, updated_at, client_id, courier_id, organizer_id,
                            from_address, from_lat, from_lng,
                            to_address, to_lat, to_lng,
                            distance_km, estimated_time_minutes, item_description,
                            recipient_name, recipient_phone,
                            total_amount, shipping_fee, status,
                            delivery_type, vehicle_id,
                            accepted_at, in_transit_at, completed_at)
    VALUES (NOW() - INTERVAL '5 days', NOW() - INTERVAL '5 days',
            v_client_id, v_courier_id, v_organizer_id,
            'Av. Paulista, 1000 - Bela Vista, São Paulo', -23.5614, -46.6558,
            'Rua Augusta, 500 - Consolação, São Paulo', -23.5536, -46.6580,
            3.5, 15, 'Documentos importantes',
            'Pedro Demo', '(11) 99999-0003',
            25.00, 25.00, 'COMPLETED',
            'CONTRACT', v_vehicle_id,
            NOW() - INTERVAL '5 days',
            NOW() - INTERVAL '5 days', NOW() - INTERVAL '5 days')
    RETURNING id INTO v_del1;

    -- Delivery 2: COMPLETED (customer → courier)
    INSERT INTO deliveries (created_at, updated_at, client_id, courier_id, organizer_id,
                            from_address, from_lat, from_lng,
                            to_address, to_lat, to_lng,
                            distance_km, estimated_time_minutes, item_description,
                            recipient_name, recipient_phone,
                            total_amount, shipping_fee, status,
                            delivery_type, vehicle_id,
                            accepted_at, in_transit_at, completed_at)
    VALUES (NOW() - INTERVAL '3 days', NOW() - INTERVAL '3 days',
            v_customer_id, v_courier_id, v_organizer_id,
            'Rua das Flores, 100 - Centro, São Paulo', -23.5505, -46.6333,
            'Rua Oscar Freire, 200 - Jardins, São Paulo', -23.5628, -46.6690,
            5.2, 20, 'Encomenda frágil',
            'Ana Demo', '(11) 99999-0004',
            35.00, 35.00, 'COMPLETED',
            'DELIVERY', v_vehicle_id,
            NOW() - INTERVAL '3 days',
            NOW() - INTERVAL '3 days', NOW() - INTERVAL '3 days')
    RETURNING id INTO v_del2;

    -- Delivery 3: IN_TRANSIT
    INSERT INTO deliveries (created_at, updated_at, client_id, courier_id, organizer_id,
                            from_address, from_lat, from_lng,
                            to_address, to_lat, to_lng,
                            distance_km, estimated_time_minutes, item_description,
                            recipient_name, recipient_phone,
                            total_amount, shipping_fee, status,
                            delivery_type, vehicle_id,
                            accepted_at, in_transit_at)
    VALUES (NOW() - INTERVAL '1 hour', NOW() - INTERVAL '1 hour',
            v_client_id, v_courier_id, v_organizer_id,
            'Av. Paulista, 1000 - Bela Vista, São Paulo', -23.5614, -46.6558,
            'Rua das Flores, 100 - Centro, São Paulo', -23.5505, -46.6333,
            4.0, 18, 'Caixa pequena',
            'João Demo', '(11) 99999-0001',
            30.00, 30.00, 'IN_TRANSIT',
            'CONTRACT', v_vehicle_id,
            NOW() - INTERVAL '40 minutes',
            NOW() - INTERVAL '20 minutes')
    RETURNING id INTO v_del3;

    -- Delivery 4: PENDING (waiting for courier)
    INSERT INTO deliveries (created_at, updated_at, client_id, organizer_id,
                            from_address, from_lat, from_lng,
                            to_address, to_lat, to_lng,
                            distance_km, estimated_time_minutes, item_description,
                            recipient_name, recipient_phone,
                            total_amount, shipping_fee, status,
                            delivery_type)
    VALUES (NOW() - INTERVAL '10 minutes', NOW() - INTERVAL '10 minutes',
            v_customer_id, v_organizer_id,
            'Rua das Flores, 100 - Centro, São Paulo', -23.5505, -46.6333,
            'Rua Augusta, 500 - Consolação, São Paulo', -23.5536, -46.6580,
            2.8, 12, 'Envelope',
            'Pedro Demo', '(11) 99999-0003',
            20.00, 20.00, 'PENDING',
            'DELIVERY')
    RETURNING id INTO v_del4;

    -- Delivery 5: CANCELLED
    INSERT INTO deliveries (created_at, updated_at, client_id, organizer_id,
                            from_address, from_lat, from_lng,
                            to_address, to_lat, to_lng,
                            distance_km, estimated_time_minutes, item_description,
                            recipient_name, recipient_phone,
                            total_amount, shipping_fee, status,
                            delivery_type,
                            cancelled_at, cancellation_reason)
    VALUES (NOW() - INTERVAL '7 days', NOW() - INTERVAL '7 days',
            v_client_id, v_organizer_id,
            'Av. Paulista, 1000 - Bela Vista, São Paulo', -23.5614, -46.6558,
            'Rua Oscar Freire, 200 - Jardins, São Paulo', -23.5628, -46.6690,
            3.0, 14, 'Pacote médio',
            'Ana Demo', '(11) 99999-0004',
            28.00, 28.00, 'CANCELLED',
            'CONTRACT',
            NOW() - INTERVAL '7 days', 'Cliente cancelou - teste demo')
    RETURNING id INTO v_del5;

    -- ========================================================================
    -- 10. PAYMENTS (for completed + in-transit deliveries)
    -- ========================================================================

    -- Payment 1: PAID (delivery 1 - completed)
    INSERT INTO payments (created_at, updated_at, amount, currency, status,
                          payment_method, payer_id,
                          transaction_id, payment_date)
    VALUES (NOW() - INTERVAL '5 days', NOW() - INTERVAL '5 days',
            25.00, 'BRL', 'PAID', 'PIX',
            v_client_id,
            'txn_demo_001', NOW() - INTERVAL '5 days')
    RETURNING id INTO v_pay1;

    -- Payment 2: PAID (delivery 2 - completed)
    INSERT INTO payments (created_at, updated_at, amount, currency, status,
                          payment_method, payer_id,
                          transaction_id, payment_date)
    VALUES (NOW() - INTERVAL '3 days', NOW() - INTERVAL '3 days',
            35.00, 'BRL', 'PAID', 'PIX',
            v_customer_id,
            'txn_demo_002', NOW() - INTERVAL '3 days')
    RETURNING id INTO v_pay2;

    -- Payment 3: PENDING (delivery 3 - in transit)
    INSERT INTO payments (created_at, updated_at, amount, currency, status,
                          payment_method, payer_id,
                          transaction_id)
    VALUES (NOW() - INTERVAL '1 hour', NOW() - INTERVAL '1 hour',
            30.00, 'BRL', 'PENDING', 'PIX',
            v_client_id,
            'txn_demo_003')
    RETURNING id INTO v_pay3;

    -- Payment 4: PENDING (delivery 4 - pending)
    INSERT INTO payments (created_at, updated_at, amount, currency, status,
                          payment_method, payer_id,
                          transaction_id)
    VALUES (NOW() - INTERVAL '10 minutes', NOW() - INTERVAL '10 minutes',
            20.00, 'BRL', 'PENDING', 'PIX',
            v_customer_id,
            'txn_demo_004')
    RETURNING id INTO v_pay4;

    -- Payment 5: CANCELLED (delivery 5 - cancelled)
    INSERT INTO payments (created_at, updated_at, amount, currency, status,
                          payment_method, payer_id,
                          transaction_id)
    VALUES (NOW() - INTERVAL '7 days', NOW() - INTERVAL '7 days',
            28.00, 'BRL', 'CANCELLED', 'PIX',
            v_client_id,
            'txn_demo_005')
    RETURNING id INTO v_pay5;

    -- Link payments to deliveries via junction table
    INSERT INTO payment_deliveries (payment_id, delivery_id)
    VALUES
        (v_pay1, v_del1),
        (v_pay2, v_del2),
        (v_pay3, v_del3),
        (v_pay4, v_del4),
        (v_pay5, v_del5);

    -- Also set the payment_id on deliveries
    UPDATE deliveries SET payment_id = v_pay1 WHERE id = v_del1;
    UPDATE deliveries SET payment_id = v_pay2 WHERE id = v_del2;
    UPDATE deliveries SET payment_id = v_pay3 WHERE id = v_del3;
    UPDATE deliveries SET payment_id = v_pay4 WHERE id = v_del4;
    UPDATE deliveries SET payment_id = v_pay5 WHERE id = v_del5;

    RAISE NOTICE '✅ V74: Created 4 demo accounts with full data for Apple review';
    RAISE NOTICE '   demo.customer@zapi10.com  (CUSTOMER) - João Demo';
    RAISE NOTICE '   demo.client@zapi10.com    (CLIENT)   - Maria Demo';
    RAISE NOTICE '   demo.courier@zapi10.com   (COURIER)  - Pedro Demo';
    RAISE NOTICE '   demo.organizer@zapi10.com (ORGANIZER)- Ana Demo';
    RAISE NOTICE '   Password for all: Demo@123';

END $$;
