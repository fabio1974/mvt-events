--
-- PostgreSQL database dump
--

\restrict JrPjXn6rrbB3qCwyh55z3YZJcGfEbro9Ctxb8kVakUvxQkMqftVCAioEiNP7whc

-- Dumped from database version 16.10
-- Dumped by pg_dump version 16.10

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

--
-- Data for Name: users; Type: TABLE DATA; Schema: public; Owner: mvt
--

INSERT INTO public.users VALUES ('5a9ec5f8-6a5f-44d4-bb76-82ff3e872d57', '2025-11-05 08:54:14.449814', true, '$2a$10$IatMvd4Qk0ISJ49U7y8MEuFk5pH6zleGIwLSGNZLup0PHN5yNILfe', 'ADMIN', '2025-11-05 08:54:14.449814', 'moveltrack@gmail.com', NULL, NULL, NULL, NULL, NULL, NULL, 'Fábio Barros', NULL, NULL, NULL, NULL, 1058, NULL, NULL);
INSERT INTO public.users VALUES ('189c7d79-cb21-40c1-9b7c-006ebaa3289a', '2025-11-05 09:16:41.36388', true, '$2a$10$xaB3qr3ifPgBOkE8Owae/.Od4i07KcvUyl8r5SNruhMG8mM4YhoRK', 'CLIENT', '2025-11-21 20:47:27.629003', 'padaria1@gmail.com', 'Rod. Gov. César Cals de O Filho, 721 - Ubajara, CE, 62350-000, Brazil', NULL, NULL, '2025-10-28', '12345678909', 'MALE', 'Padaria1', '85997572919', NULL, NULL, NULL, NULL, -3.8483262533465727, -40.91063976287842);
INSERT INTO public.users VALUES ('6d401ff4-5c77-486d-9d0f-ddb2dbb13b24', '2025-11-05 09:11:30.177739', true, '$2a$10$y2su4xnOChW1csv6NSS5nusxUqdpsduSPaCI/5RpLx7qguW6vXlLS', 'ORGANIZER', '2025-11-05 09:11:30.177745', 'samuel@gmail.com', NULL, NULL, NULL, NULL, '28272738880', NULL, 'Samuel', NULL, NULL, NULL, NULL, 1058, NULL, NULL);
INSERT INTO public.users VALUES ('c3333333-3333-3333-3333-333333333333', '2025-11-23 19:23:21.45398', true, '$2a$10$xWZ9pqH8kC7vK5nL3mR4tO0yT9uW1xV8yN2fQ6sE3rD7cA4bB5hC6', 'CLIENT', '2025-11-23 19:23:21.45398', 'sushimania@gmail.com', NULL, NULL, NULL, NULL, '333.333.333-33', NULL, 'Sushi Mania', '85987003333', NULL, -3.698333, -40.354722, 1058, NULL, NULL);
INSERT INTO public.users VALUES ('a1b2c3d4-e5f6-7890-abcd-ef1234567890', '2025-11-23 19:23:21.428805', true, '$2a$10$xWZ9pqH8kC7vK5nL3mR4tO0yT9uW1xV8yN2fQ6sE3rD7cA4bB5hC6', 'COURIER', '2025-11-23 17:10:46.538936', 'motoboy2@gmail.com', NULL, NULL, NULL, NULL, '987.654.321-00', NULL, 'Motoboy2', '85988776655', NULL, -3.6969445, -40.3581945, 1058, NULL, NULL);
INSERT INTO public.users VALUES ('6186c7af-2311-4756-bfc6-ce98bd31ed27', '2025-11-05 09:14:03.068882', true, '$2a$10$hpCrBK2I33zy1/K1R51Js.2FEFE6JN.OODQXhUHWovm54Vs2x.wmW', 'COURIER', '2025-11-23 17:10:46.538936', 'motoboy1@gmail.com', 'Rua Bruno Porto, 555, Apto 1604-B', NULL, NULL, '2025-11-05', '78181143353', 'MALE', 'Motoboy1', '85997572919', NULL, -3.6969445, -40.3494445, 1058, NULL, NULL);
INSERT INTO public.users VALUES ('208f16bd-13a5-4887-83e7-aa095e3eeb6d', '2025-11-24 00:16:21.292022', true, '$2a$10$llbtxYDwOsld82G5rMrKE.5RFAPwddA4Jd5yXSSifCAQv8vpGk5RG', 'ORGANIZER', '2025-11-24 00:16:21.292047', 'rodrigo@gmail.com', NULL, NULL, NULL, NULL, '98765432100', NULL, 'Rodrigo Sousa', NULL, NULL, NULL, NULL, NULL, NULL, NULL);
INSERT INTO public.users VALUES ('c2222222-2222-2222-2222-222222222222', '2025-11-23 19:23:21.452665', true, '$2a$10$2/Ssqtrap5r.cYapJTR5euty0iXuFL/gAjyoNRJ5wIhNHH42Rs6Oi', 'CLIENT', '2025-11-23 19:23:21.452665', 'farmaciapaguemenos@gmail.com', NULL, NULL, NULL, NULL, '222.222.222-22', NULL, 'Farmácia Pague Menos', '85987002222', NULL, -3.692778, -40.351944, 1058, NULL, NULL);
INSERT INTO public.users VALUES ('c1111111-1111-1111-1111-111111111111', '2025-11-23 19:23:21.451069', true, '$2a$10$mymgqOC3ph/0nHyugNEvMu3aHYcVs77xW3XxcKiuNpKQ0iEQPd5YC', 'CLIENT', '2025-11-23 19:23:21.451069', 'postoipiranga@gmail.com', NULL, NULL, NULL, NULL, '111.111.111-11', NULL, 'Posto Ipiranga Centro', '85987001111', NULL, -3.688056, -40.345833, 1058, NULL, NULL);


--
-- Data for Name: organizations; Type: TABLE DATA; Schema: public; Owner: mvt
--

INSERT INTO public.organizations VALUES (1, '2025-11-05 08:54:14.449814', '2025-11-05 09:15:26.358295', 'samuel@gmail.com', 'Grupo do Samuel Desc', NULL, 'Grupo do Samuel', '55119999999', 'grupo-samuel', 'https://moveltrack.com.br', 1058, 5.00, 'ACTIVE', '6d401ff4-5c77-486d-9d0f-ddb2dbb13b24');
INSERT INTO public.organizations VALUES (2, '2025-11-24 00:18:11.53823', '2025-11-24 00:20:34.093078', 'gruporodrigo@gmail.com', NULL, NULL, 'Grupo do Rodrigo', '85997572919', 'grupo-do-rodrigo', NULL, 1058, 5.00, 'ACTIVE', '208f16bd-13a5-4887-83e7-aa095e3eeb6d');


--
-- Data for Name: client_contracts; Type: TABLE DATA; Schema: public; Owner: mvt
--

INSERT INTO public.client_contracts VALUES (2, '2025-11-06 00:46:29.1835', '2025-11-06 00:46:29.1835', '189c7d79-cb21-40c1-9b7c-006ebaa3289a', 1, true, 'ACTIVE', '2025-11-06', '2025-11-05', NULL);
INSERT INTO public.client_contracts VALUES (3, '2025-11-23 19:23:21.455335', '2025-11-23 19:23:21.455335', 'c1111111-1111-1111-1111-111111111111', 1, false, 'ACTIVE', '2025-11-23', '2025-11-23', NULL);
INSERT INTO public.client_contracts VALUES (4, '2025-11-23 19:23:21.455335', '2025-11-23 19:23:21.455335', 'c2222222-2222-2222-2222-222222222222', 1, false, 'ACTIVE', '2025-11-23', '2025-11-23', NULL);
INSERT INTO public.client_contracts VALUES (5, '2025-11-23 19:23:21.455335', '2025-11-23 19:23:21.455335', 'c3333333-3333-3333-3333-333333333333', 1, false, 'ACTIVE', '2025-11-23', '2025-11-23', NULL);
INSERT INTO public.client_contracts VALUES (11, '2025-11-24 00:38:24.266176', '2025-11-24 00:38:24.266216', 'c2222222-2222-2222-2222-222222222222', 2, false, 'ACTIVE', '2025-11-24', '2025-11-19', NULL);


--
-- Data for Name: employment_contracts; Type: TABLE DATA; Schema: public; Owner: mvt
--

INSERT INTO public.employment_contracts VALUES (4, '2025-11-06 00:43:39.06694', '2025-11-06 00:43:39.066971', '6186c7af-2311-4756-bfc6-ce98bd31ed27', 1, '2025-11-05 09:12:52.496771', true);
INSERT INTO public.employment_contracts VALUES (5, '2025-11-23 19:23:21.445942', '2025-11-23 19:23:21.445942', 'a1b2c3d4-e5f6-7890-abcd-ef1234567890', 1, '2025-11-23 19:23:21.445942', true);
INSERT INTO public.employment_contracts VALUES (15, '2025-11-24 00:38:24.141657', '2025-11-24 00:38:24.141729', '6186c7af-2311-4756-bfc6-ce98bd31ed27', 2, '2025-11-24 00:19:10.766356', true);
INSERT INTO public.employment_contracts VALUES (16, '2025-11-24 00:38:24.207832', '2025-11-24 00:38:24.207878', 'a1b2c3d4-e5f6-7890-abcd-ef1234567890', 2, '2025-11-24 00:19:10.766356', true);


--
-- Name: contracts_id_seq; Type: SEQUENCE SET; Schema: public; Owner: mvt
--

SELECT pg_catalog.setval('public.contracts_id_seq', 11, true);


--
-- Name: employment_contracts_id_seq; Type: SEQUENCE SET; Schema: public; Owner: mvt
--

SELECT pg_catalog.setval('public.employment_contracts_id_seq', 16, true);


--
-- Name: organizations_id_seq; Type: SEQUENCE SET; Schema: public; Owner: mvt
--

SELECT pg_catalog.setval('public.organizations_id_seq', 2, true);


--
-- PostgreSQL database dump complete
--

\unrestrict JrPjXn6rrbB3qCwyh55z3YZJcGfEbro9Ctxb8kVakUvxQkMqftVCAioEiNP7whc

