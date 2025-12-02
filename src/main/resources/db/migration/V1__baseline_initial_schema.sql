-- ============================================================================
-- MVT-EVENTS - BASELINE MIGRATION V1
-- ============================================================================
-- 
-- Description:
--   Complete database schema for MVT-Events application (Zapi10 platform).
--   This is a consolidation of all previous migrations (V01-V81) into a
--   single baseline migration for simplified deployment and maintenance.
--
-- System Overview:
--   - Multi-tenant delivery management system
--   - Organization-based tenancy with RLS (Row Level Security)
--   - User roles: ADMIN, ORGANIZER, COURIER, CLIENT
--   - Core entities: Organization, User, Delivery, Contract (Employment/Client)
--
-- Key Features:
--   - Organization ownership: Organization.owner -> User (reverse relationship)
--   - Contract-based relationships (no direct user.organization)
--   - City-based geographical references
--   - Comprehensive audit trails (created_at, updated_at)
--
-- Generated: 2025-12-01
-- Database Version: PostgreSQL 16.10
-- Application Version: Spring Boot 3.5.6
-- ============================================================================

--
-- Schema DDL starts below
--

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
-- Name: SCHEMA public; Type: COMMENT; Schema: -; Owner: -
--

COMMENT ON SCHEMA public IS 'transfers table removed - was related to Events system (now removed)';


--
-- Name: check_primary_client_contract(); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION public.check_primary_client_contract() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
        BEGIN
            -- Se está marcando como primário
            IF NEW.is_primary = TRUE THEN
                -- Desmarca todos os outros contratos deste cliente
                UPDATE client_contracts
                SET is_primary = FALSE
                WHERE client_id = NEW.client_id
                  AND id != NEW.id
                  AND is_primary = TRUE;
            END IF;
            RETURN NEW;
        END;
        $$;


--
-- Name: update_updated_at_column(); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION public.update_updated_at_column() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$;


SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: adm_profiles; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.adm_profiles (
    id bigint NOT NULL,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    user_id uuid NOT NULL,
    region character varying(100) NOT NULL,
    region_code character varying(20),
    commission_percentage numeric(5,2) DEFAULT 10.00 NOT NULL,
    total_commission numeric(12,2) DEFAULT 0.00,
    total_clients_managed integer DEFAULT 0,
    total_couriers_managed integer DEFAULT 0,
    total_deliveries_managed integer DEFAULT 0,
    status character varying(20) DEFAULT 'ACTIVE'::character varying NOT NULL,
    CONSTRAINT chk_adm_commission CHECK (((commission_percentage >= (0)::numeric) AND (commission_percentage <= (100)::numeric))),
    CONSTRAINT chk_adm_status CHECK (((status)::text = ANY ((ARRAY['ACTIVE'::character varying, 'INACTIVE'::character varying, 'SUSPENDED'::character varying])::text[]))),
    CONSTRAINT chk_adm_total_clients CHECK ((total_clients_managed >= 0)),
    CONSTRAINT chk_adm_total_commission CHECK ((total_commission >= (0)::numeric)),
    CONSTRAINT chk_adm_total_couriers CHECK ((total_couriers_managed >= 0)),
    CONSTRAINT chk_adm_total_deliveries CHECK ((total_deliveries_managed >= 0))
);


--
-- Name: TABLE adm_profiles; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.adm_profiles IS 'Perfil especializado para usuários com role ADM (gerente local - TENANT)';


--
-- Name: COLUMN adm_profiles.user_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.adm_profiles.user_id IS 'FK para users - deve ter role = ADM';


--
-- Name: COLUMN adm_profiles.region; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.adm_profiles.region IS 'Região de atuação do ADM - usado para multi-tenancy';


--
-- Name: COLUMN adm_profiles.commission_percentage; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.adm_profiles.commission_percentage IS 'Percentual de comissão sobre entregas (0 a 100)';


--
-- Name: COLUMN adm_profiles.status; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.adm_profiles.status IS 'Status do ADM: ACTIVE, INACTIVE, SUSPENDED';


--
-- Name: adm_profiles_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.adm_profiles_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: adm_profiles_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.adm_profiles_id_seq OWNED BY public.adm_profiles.id;


--
-- Name: deliveries; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.deliveries (
    id bigint NOT NULL,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    client_id uuid NOT NULL,
    courier_id uuid,
    from_address text NOT NULL,
    from_lat double precision,
    from_lng double precision,
    to_address text NOT NULL,
    to_lat double precision,
    to_lng double precision,
    distance_km numeric(6,2),
    estimated_time_minutes integer,
    item_description character varying(500),
    recipient_name character varying(150),
    recipient_phone character varying(20),
    total_amount numeric(10,2) NOT NULL,
    status character varying(20) DEFAULT 'PENDING'::character varying NOT NULL,
    accepted_at timestamp without time zone,
    picked_up_at timestamp without time zone,
    completed_at timestamp without time zone,
    cancelled_at timestamp without time zone,
    cancellation_reason text,
    payment_id bigint,
    scheduled_pickup_at timestamp(6) without time zone,
    delivery_type character varying(20) DEFAULT 'CONTRACT'::character varying NOT NULL,
    in_transit_at timestamp without time zone,
    shipping_fee numeric(10,2),
    organizer_id uuid,
    CONSTRAINT chk_delivery_distance CHECK (((distance_km IS NULL) OR (distance_km >= (0)::numeric))),
    CONSTRAINT chk_delivery_estimated_time CHECK (((estimated_time_minutes IS NULL) OR (estimated_time_minutes >= 0))),
    CONSTRAINT chk_delivery_status CHECK (((status)::text = ANY ((ARRAY['PENDING'::character varying, 'ACCEPTED'::character varying, 'PICKED_UP'::character varying, 'IN_TRANSIT'::character varying, 'COMPLETED'::character varying, 'CANCELLED'::character varying])::text[]))),
    CONSTRAINT chk_delivery_total_amount CHECK ((total_amount >= 0.01)),
    CONSTRAINT chk_delivery_type CHECK (((delivery_type)::text = ANY ((ARRAY['CONTRACT'::character varying, 'ON_DEMAND'::character varying])::text[]))),
    CONSTRAINT deliveries_shipping_fee_positive CHECK (((shipping_fee IS NULL) OR (shipping_fee >= 0.01)))
);


--
-- Name: TABLE deliveries; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.deliveries IS 'Entidade CORE do Zapi10 - entregas/deliveries (TENANT via adm_id)';


--
-- Name: COLUMN deliveries.from_lat; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.deliveries.from_lat IS 'Latitude do ponto de coleta - usado no algoritmo de raio para entregas ON_DEMAND';


--
-- Name: COLUMN deliveries.from_lng; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.deliveries.from_lng IS 'Longitude do ponto de coleta - usado no algoritmo de raio para entregas ON_DEMAND';


--
-- Name: COLUMN deliveries.to_lat; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.deliveries.to_lat IS 'Latitude do endereço de destino (DOUBLE PRECISION)';


--
-- Name: COLUMN deliveries.to_lng; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.deliveries.to_lng IS 'Longitude do endereço de destino (DOUBLE PRECISION)';


--
-- Name: COLUMN deliveries.status; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.deliveries.status IS 'Status: PENDING, ACCEPTED, PICKED_UP, IN_TRANSIT, COMPLETED, CANCELLED';


--
-- Name: COLUMN deliveries.accepted_at; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.deliveries.accepted_at IS 'Data/hora em que o courier aceitou a delivery';


--
-- Name: COLUMN deliveries.cancellation_reason; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.deliveries.cancellation_reason IS 'Motivo do cancelamento da delivery';


--
-- Name: COLUMN deliveries.payment_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.deliveries.payment_id IS 'Referência ao pagamento desta entrega (pode ser NULL)';


--
-- Name: COLUMN deliveries.scheduled_pickup_at; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.deliveries.scheduled_pickup_at IS 'Data e hora agendada para coleta da entrega';


--
-- Name: COLUMN deliveries.delivery_type; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.deliveries.delivery_type IS 'Tipo de entrega:
- CONTRACT: Cliente possui service_contract com ORGANIZER (público ou privado)
- ON_DEMAND: Cliente sem service_contract, entrega avulsa';


--
-- Name: COLUMN deliveries.in_transit_at; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.deliveries.in_transit_at IS 'Data/hora em que o courier iniciou o transporte';


--
-- Name: COLUMN deliveries.shipping_fee; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.deliveries.shipping_fee IS 'Valor do frete da entrega';


--
-- Name: COLUMN deliveries.organizer_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.deliveries.organizer_id IS 'User que é owner da organização comum entre courier e client';


--
-- Name: users; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.users (
    id uuid NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    enabled boolean NOT NULL,
    password character varying(255) NOT NULL,
    role character varying(255) NOT NULL,
    updated_at timestamp(6) without time zone NOT NULL,
    username character varying(255) NOT NULL,
    address text,
    city_old character varying(100),
    country character varying(100),
    date_of_birth date,
    document_number character varying(20),
    gender character varying(10),
    name character varying(255),
    phone character varying(255),
    state character varying(100),
    gps_latitude double precision,
    gps_longitude double precision,
    city_id bigint,
    latitude double precision,
    longitude double precision,
    CONSTRAINT chk_user_role CHECK (((role)::text = ANY ((ARRAY['USER'::character varying, 'ORGANIZER'::character varying, 'ADMIN'::character varying, 'CLIENT'::character varying, 'COURIER'::character varying])::text[]))),
    CONSTRAINT users_gender_check CHECK (((gender)::text = ANY ((ARRAY['MALE'::character varying, 'FEMALE'::character varying, 'OTHER'::character varying])::text[]))),
    CONSTRAINT users_role_check CHECK (((role)::text = ANY ((ARRAY['USER'::character varying, 'ORGANIZER'::character varying, 'ADMIN'::character varying, 'CLIENT'::character varying, 'COURIER'::character varying, 'ADM'::character varying])::text[])))
);


--
-- Name: TABLE users; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.users IS 'Users table. Organization relationship is now through organizations.owner_id instead of users.organization_id';


--
-- Name: COLUMN users.role; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.users.role IS 'User role: USER (athlete), ORGANIZER (can create events and register as athlete), ADMIN';


--
-- Name: COLUMN users.city_old; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.users.city_old IS 'BACKUP - campo city antigo (será removido em migração futura)';


--
-- Name: COLUMN users.date_of_birth; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.users.date_of_birth IS 'Date of birth for athletes';


--
-- Name: COLUMN users.document_number; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.users.document_number IS 'Document number (CPF, passport, etc.) - unique when not null';


--
-- Name: COLUMN users.gender; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.users.gender IS 'Gender: MALE, FEMALE, or OTHER';


--
-- Name: COLUMN users.name; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.users.name IS 'Full name of the user/athlete';


--
-- Name: COLUMN users.gps_latitude; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.users.gps_latitude IS 'Latitude da localização do usuário (DOUBLE PRECISION)';


--
-- Name: COLUMN users.gps_longitude; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.users.gps_longitude IS 'Longitude da localização do usuário (DOUBLE PRECISION)';


--
-- Name: COLUMN users.city_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.users.city_id IS 'FK para cities - cidade do usuário';


--
-- Name: CONSTRAINT users_role_check ON users; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON CONSTRAINT users_role_check ON public.users IS 'Valores válidos: USER (atleta), ORGANIZER (organizador), ADMIN (administrador sistema), CLIENT (cliente Zapi10), COURIER (entregador), ADM (gerente local Zapi10)';


--
-- Name: available_on_demand_deliveries; Type: VIEW; Schema: public; Owner: -
--

CREATE VIEW public.available_on_demand_deliveries AS
 SELECT d.id,
    d.client_id,
    d.from_address,
    d.from_lat,
    d.from_lng,
    d.to_address,
    d.to_lat,
    d.to_lng,
    d.distance_km,
    d.total_amount,
    d.item_description,
    d.created_at,
    u.username AS client_email,
    u.name AS client_name,
    u.phone AS client_phone
   FROM (public.deliveries d
     JOIN public.users u ON ((u.id = d.client_id)))
  WHERE (((d.delivery_type)::text = 'ON_DEMAND'::text) AND ((d.status)::text = 'PENDING'::text) AND (d.courier_id IS NULL))
  ORDER BY d.created_at;


--
-- Name: VIEW available_on_demand_deliveries; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON VIEW public.available_on_demand_deliveries IS 'Entregas ON_DEMAND disponíveis para COURIERs aceitarem';


--
-- Name: cities; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.cities (
    id bigint NOT NULL,
    name character varying(100) NOT NULL,
    state character varying(50) NOT NULL,
    state_code character varying(2) NOT NULL,
    ibge_code character varying(10),
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: TABLE cities; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.cities IS 'Brazilian cities data populated automatically from IBGE API on startup';


--
-- Name: COLUMN cities.name; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.cities.name IS 'City name';


--
-- Name: COLUMN cities.state; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.cities.state IS 'Full state name';


--
-- Name: COLUMN cities.state_code; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.cities.state_code IS 'Two-letter state code (UF)';


--
-- Name: COLUMN cities.ibge_code; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.cities.ibge_code IS 'Official IBGE municipality code';


--
-- Name: COLUMN cities.created_at; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.cities.created_at IS 'Record creation timestamp';


--
-- Name: COLUMN cities.updated_at; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.cities.updated_at IS 'Last update timestamp';


--
-- Name: cities_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.cities_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: cities_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.cities_id_seq OWNED BY public.cities.id;


--
-- Name: client_contracts; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.client_contracts (
    id bigint NOT NULL,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    client_id uuid NOT NULL,
    organization_id bigint NOT NULL,
    is_primary boolean DEFAULT false NOT NULL,
    status character varying(20) DEFAULT 'ACTIVE'::character varying NOT NULL,
    contract_date date DEFAULT CURRENT_DATE NOT NULL,
    start_date date NOT NULL,
    end_date date,
    CONSTRAINT chk_client_contract_dates CHECK (((end_date IS NULL) OR (end_date >= start_date))),
    CONSTRAINT chk_client_contract_status CHECK (((status)::text = ANY ((ARRAY['ACTIVE'::character varying, 'SUSPENDED'::character varying, 'CANCELLED'::character varying])::text[])))
);


--
-- Name: TABLE client_contracts; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.client_contracts IS 'Contratos de serviço entre CLIENT e Organization (relação cliente-fornecedor)';


--
-- Name: COLUMN client_contracts.is_primary; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.client_contracts.is_primary IS 'Se este é o contrato titular do cliente (apenas 1 por cliente)';


--
-- Name: COLUMN client_contracts.status; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.client_contracts.status IS 'Status do contrato: ACTIVE, SUSPENDED, CANCELLED';


--
-- Name: COLUMN client_contracts.start_date; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.client_contracts.start_date IS 'Data de início da vigência do contrato';


--
-- Name: COLUMN client_contracts.end_date; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.client_contracts.end_date IS 'Data de fim da vigência do contrato (NULL = indeterminado)';


--
-- Name: contracts_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.contracts_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: contracts_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.contracts_id_seq OWNED BY public.client_contracts.id;


--
-- Name: courier_profiles; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.courier_profiles (
    id bigint NOT NULL,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    user_id uuid NOT NULL,
    vehicle_type character varying(20),
    vehicle_plate character varying(10),
    vehicle_model character varying(50),
    vehicle_color character varying(30),
    rating numeric(3,2) DEFAULT 0.00,
    total_deliveries integer DEFAULT 0,
    completed_deliveries integer DEFAULT 0,
    cancelled_deliveries integer DEFAULT 0,
    status character varying(20) DEFAULT 'OFFLINE'::character varying NOT NULL,
    last_location_update timestamp without time zone,
    current_latitude double precision,
    current_longitude double precision,
    CONSTRAINT chk_courier_cancelled_deliveries CHECK ((cancelled_deliveries >= 0)),
    CONSTRAINT chk_courier_completed_deliveries CHECK ((completed_deliveries >= 0)),
    CONSTRAINT chk_courier_rating CHECK (((rating >= (0)::numeric) AND (rating <= (5)::numeric))),
    CONSTRAINT chk_courier_status CHECK (((status)::text = ANY ((ARRAY['AVAILABLE'::character varying, 'ON_DELIVERY'::character varying, 'OFFLINE'::character varying, 'SUSPENDED'::character varying])::text[]))),
    CONSTRAINT chk_courier_total_deliveries CHECK ((total_deliveries >= 0))
);


--
-- Name: TABLE courier_profiles; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.courier_profiles IS 'Perfil especializado para usuários com role COURIER';


--
-- Name: COLUMN courier_profiles.user_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.courier_profiles.user_id IS 'FK para users - deve ter role = COURIER';


--
-- Name: COLUMN courier_profiles.rating; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.courier_profiles.rating IS 'Avaliação média do courier (0 a 5)';


--
-- Name: COLUMN courier_profiles.status; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.courier_profiles.status IS 'Status do courier: AVAILABLE, ON_DELIVERY, OFFLINE, SUSPENDED';


--
-- Name: COLUMN courier_profiles.current_latitude; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.courier_profiles.current_latitude IS 'Latitude atual do motoboy (DOUBLE PRECISION)';


--
-- Name: COLUMN courier_profiles.current_longitude; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.courier_profiles.current_longitude IS 'Longitude atual do motoboy (DOUBLE PRECISION)';


--
-- Name: courier_profiles_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.courier_profiles_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: courier_profiles_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.courier_profiles_id_seq OWNED BY public.courier_profiles.id;


--
-- Name: deliveries_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.deliveries_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: deliveries_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.deliveries_id_seq OWNED BY public.deliveries.id;


--
-- Name: employment_contracts; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.employment_contracts (
    id bigint NOT NULL,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    courier_id uuid NOT NULL,
    organization_id bigint NOT NULL,
    linked_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    is_active boolean DEFAULT true NOT NULL
);


--
-- Name: TABLE employment_contracts; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.employment_contracts IS 'Contratos de trabalho entre COURIER e Organization (relação empregado-empresa)';


--
-- Name: COLUMN employment_contracts.linked_at; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.employment_contracts.linked_at IS 'Data/hora que o courier foi contratado';


--
-- Name: COLUMN employment_contracts.is_active; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.employment_contracts.is_active IS 'Se o contrato de trabalho está ativo';


--
-- Name: employment_contracts_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.employment_contracts_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: employment_contracts_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.employment_contracts_id_seq OWNED BY public.employment_contracts.id;


--
-- Name: evaluations; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.evaluations (
    id bigint NOT NULL,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    delivery_id bigint NOT NULL,
    evaluator_id uuid NOT NULL,
    evaluated_id uuid NOT NULL,
    rating integer NOT NULL,
    comment text,
    evaluation_type character varying(20) NOT NULL,
    CONSTRAINT chk_evaluation_rating CHECK (((rating >= 1) AND (rating <= 5))),
    CONSTRAINT chk_evaluation_type CHECK (((evaluation_type)::text = ANY ((ARRAY['CLIENT_TO_COURIER'::character varying, 'COURIER_TO_CLIENT'::character varying])::text[])))
);


--
-- Name: TABLE evaluations; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.evaluations IS 'Avaliações de entregas (1:1 com deliveries)';


--
-- Name: COLUMN evaluations.rating; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.evaluations.rating IS 'Nota de 1 a 5 estrelas';


--
-- Name: COLUMN evaluations.evaluation_type; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.evaluations.evaluation_type IS 'CLIENT_TO_COURIER (cliente avalia motoboy) ou COURIER_TO_CLIENT (vice-versa)';


--
-- Name: evaluations_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.evaluations_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: evaluations_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.evaluations_id_seq OWNED BY public.evaluations.id;


--
-- Name: organizations; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.organizations (
    id bigint NOT NULL,
    created_at timestamp(6) without time zone,
    updated_at timestamp(6) without time zone,
    contact_email character varying(255) NOT NULL,
    description text,
    logo_url character varying(255),
    name character varying(255) NOT NULL,
    phone character varying(255),
    slug character varying(100) NOT NULL,
    website character varying(255),
    city_id bigint,
    commission_percentage numeric(5,2) DEFAULT 5.00 NOT NULL,
    status character varying(20) DEFAULT 'ACTIVE'::character varying NOT NULL,
    owner_id uuid,
    CONSTRAINT chk_organization_status CHECK (((status)::text = ANY ((ARRAY['ACTIVE'::character varying, 'INACTIVE'::character varying, 'SUSPENDED'::character varying])::text[])))
);


--
-- Name: TABLE organizations; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.organizations IS 'Table of organizations that create events';


--
-- Name: COLUMN organizations.owner_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.organizations.owner_id IS 'User (ORGANIZER) responsável pela organização';


--
-- Name: organizations_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.organizations ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME public.organizations_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: payments; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.payments (
    id bigint NOT NULL,
    created_at timestamp(6) without time zone,
    updated_at timestamp(6) without time zone,
    amount numeric(10,2) NOT NULL,
    currency character varying(3) NOT NULL,
    gateway_fee numeric(10,2),
    provider_payment_id character varying(255),
    provider character varying(50),
    gateway_response jsonb,
    payment_method character varying(50),
    processed_at timestamp(6) without time zone,
    refund_amount numeric(10,2),
    refund_reason text,
    refunded_at timestamp(6) without time zone,
    status character varying(20) NOT NULL,
    registration_id bigint NOT NULL,
    courier_amount numeric(12,2),
    adm_amount numeric(12,2),
    platform_amount numeric(12,2),
    delivery_id bigint,
    payer_id uuid,
    organization_id bigint,
    transaction_id character varying(100),
    notes text,
    metadata jsonb,
    payment_date timestamp without time zone,
    CONSTRAINT chk_payment_adm_amount CHECK (((adm_amount IS NULL) OR (adm_amount >= (0)::numeric))),
    CONSTRAINT chk_payment_courier_amount CHECK (((courier_amount IS NULL) OR (courier_amount >= (0)::numeric))),
    CONSTRAINT chk_payment_platform_amount CHECK (((platform_amount IS NULL) OR (platform_amount >= (0)::numeric))),
    CONSTRAINT chk_payment_split_sum CHECK ((((courier_amount IS NULL) AND (adm_amount IS NULL) AND (platform_amount IS NULL)) OR (amount = ((COALESCE(courier_amount, (0)::numeric) + COALESCE(adm_amount, (0)::numeric)) + COALESCE(platform_amount, (0)::numeric))))),
    CONSTRAINT payments_payment_method_check CHECK (((payment_method)::text = ANY ((ARRAY['CREDIT_CARD'::character varying, 'DEBIT_CARD'::character varying, 'PIX'::character varying, 'BANK_TRANSFER'::character varying, 'PAYPAL_ACCOUNT'::character varying, 'CASH'::character varying, 'OTHER'::character varying])::text[]))),
    CONSTRAINT payments_payment_status_check CHECK (((status)::text = ANY ((ARRAY['PENDING'::character varying, 'PROCESSING'::character varying, 'COMPLETED'::character varying, 'FAILED'::character varying, 'CANCELLED'::character varying, 'REFUNDED'::character varying, 'PARTIALLY_REFUNDED'::character varying])::text[])))
);


--
-- Name: TABLE payments; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.payments IS 'Pagamentos de entregas e registros (sistema unificado)';


--
-- Name: COLUMN payments.courier_amount; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.payments.courier_amount IS 'Valor destinado ao entregador (parte do amount total)';


--
-- Name: COLUMN payments.adm_amount; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.payments.adm_amount IS 'Comissão do ADM (parte do amount total)';


--
-- Name: COLUMN payments.platform_amount; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.payments.platform_amount IS 'Taxa da plataforma (parte do amount total)';


--
-- Name: COLUMN payments.delivery_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.payments.delivery_id IS 'ID da entrega relacionada (novo sistema)';


--
-- Name: COLUMN payments.payer_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.payments.payer_id IS 'ID do usuário que está pagando';


--
-- Name: COLUMN payments.organization_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.payments.organization_id IS 'ID da organização que receberá o pagamento';


--
-- Name: COLUMN payments.transaction_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.payments.transaction_id IS 'ID único da transação (gerado internamente)';


--
-- Name: COLUMN payments.notes; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.payments.notes IS 'Observações sobre o pagamento';


--
-- Name: COLUMN payments.metadata; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.payments.metadata IS 'Dados adicionais em formato JSON';


--
-- Name: COLUMN payments.payment_date; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.payments.payment_date IS 'Data/hora em que o pagamento foi concluído';


--
-- Name: payments_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.payments ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME public.payments_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: payout_items; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.payout_items (
    id bigint NOT NULL,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    payment_id bigint NOT NULL,
    item_value numeric(12,2) NOT NULL,
    value_type character varying(20) NOT NULL,
    beneficiary_id uuid,
    status character varying(20) DEFAULT 'PENDING'::character varying NOT NULL,
    paid_at timestamp without time zone,
    payment_reference character varying(100),
    payment_method character varying(20),
    notes text,
    CONSTRAINT chk_item_value CHECK ((item_value >= (0)::numeric)),
    CONSTRAINT chk_value_type CHECK (((value_type)::text = ANY ((ARRAY['COURIER_AMOUNT'::character varying, 'ADM_COMMISSION'::character varying, 'PLATFORM_AMOUNT'::character varying])::text[])))
);


--
-- Name: TABLE payout_items; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.payout_items IS 'Items de repasse individual. Cada item representa um repasse específico para um beneficiário (courier, ADM, sistema, etc)';


--
-- Name: COLUMN payout_items.payment_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.payout_items.payment_id IS 'Pagamento de origem deste repasse';


--
-- Name: COLUMN payout_items.item_value; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.payout_items.item_value IS 'Valor específico deste item (parte do payment total)';


--
-- Name: COLUMN payout_items.value_type; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.payout_items.value_type IS 'Tipos: COURIER_AMOUNT, ADM_COMMISSION, SYSTEM_FEE, PLATFORM_FEE, OTHER';


--
-- Name: COLUMN payout_items.beneficiary_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.payout_items.beneficiary_id IS 'Beneficiário que receberá este repasse';


--
-- Name: COLUMN payout_items.status; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.payout_items.status IS 'Status do repasse: PENDING, PROCESSING, PAID, FAILED, CANCELLED';


--
-- Name: COLUMN payout_items.payment_method; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.payout_items.payment_method IS 'Métodos: PIX, BANK_TRANSFER, CASH, WALLET, OTHER';


--
-- Name: payout_items_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.payout_items_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: payout_items_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.payout_items_id_seq OWNED BY public.payout_items.id;


--
-- Name: site_configurations; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.site_configurations (
    id bigint NOT NULL,
    price_per_km numeric(10,2) NOT NULL,
    organizer_percentage numeric(5,2) NOT NULL,
    platform_percentage numeric(5,2) NOT NULL,
    is_active boolean DEFAULT true NOT NULL,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamp without time zone,
    updated_by character varying(255),
    notes text,
    danger_fee_percentage numeric(5,2) DEFAULT 0.00 NOT NULL,
    high_income_fee_percentage numeric(5,2) DEFAULT 0.00 NOT NULL,
    minimum_shipping_fee numeric(10,2) DEFAULT 5.00 NOT NULL,
    CONSTRAINT site_configurations_danger_fee_percentage_check CHECK (((danger_fee_percentage >= (0)::numeric) AND (danger_fee_percentage <= (100)::numeric))),
    CONSTRAINT site_configurations_high_income_fee_percentage_check CHECK (((high_income_fee_percentage >= (0)::numeric) AND (high_income_fee_percentage <= (100)::numeric))),
    CONSTRAINT site_configurations_minimum_shipping_fee_check CHECK ((minimum_shipping_fee >= (0)::numeric)),
    CONSTRAINT site_configurations_organizer_percentage_check CHECK (((organizer_percentage >= (0)::numeric) AND (organizer_percentage <= (100)::numeric))),
    CONSTRAINT site_configurations_platform_percentage_check CHECK (((platform_percentage >= (0)::numeric) AND (platform_percentage <= (100)::numeric))),
    CONSTRAINT site_configurations_price_per_km_check CHECK ((price_per_km > (0)::numeric))
);


--
-- Name: TABLE site_configurations; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.site_configurations IS 'Configurações globais do site/plataforma (histórico de mudanças)';


--
-- Name: COLUMN site_configurations.price_per_km; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.site_configurations.price_per_km IS 'Preço por km para cálculo do frete (em Reais)';


--
-- Name: COLUMN site_configurations.organizer_percentage; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.site_configurations.organizer_percentage IS 'Percentual de comissão para o gerente/organizador';


--
-- Name: COLUMN site_configurations.platform_percentage; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.site_configurations.platform_percentage IS 'Percentual de comissão para a plataforma';


--
-- Name: COLUMN site_configurations.is_active; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.site_configurations.is_active IS 'Indica se esta é a configuração ativa (apenas uma por vez - garantido por índice único)';


--
-- Name: COLUMN site_configurations.danger_fee_percentage; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.site_configurations.danger_fee_percentage IS 'Taxa de periculosidade: percentual de acréscimo para áreas perigosas';


--
-- Name: COLUMN site_configurations.high_income_fee_percentage; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.site_configurations.high_income_fee_percentage IS 'Taxa de renda alta: percentual de acréscimo para bairros de alta renda';


--
-- Name: COLUMN site_configurations.minimum_shipping_fee; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.site_configurations.minimum_shipping_fee IS 'Valor mínimo do frete em Reais (ex: 5.00 = R$ 5,00 mínimo)';


--
-- Name: site_configurations_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.site_configurations_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: site_configurations_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.site_configurations_id_seq OWNED BY public.site_configurations.id;


--
-- Name: special_zones; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.special_zones (
    id bigint NOT NULL,
    latitude double precision NOT NULL,
    longitude double precision NOT NULL,
    address text NOT NULL,
    zone_type character varying(20) NOT NULL,
    is_active boolean DEFAULT true NOT NULL,
    notes text,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamp without time zone,
    radius_meters double precision DEFAULT 300.0 NOT NULL,
    CONSTRAINT check_positive_radius CHECK ((radius_meters > (0)::double precision)),
    CONSTRAINT check_valid_coordinates CHECK ((((latitude >= ('-90'::integer)::double precision) AND (latitude <= (90)::double precision)) AND ((longitude >= ('-180'::integer)::double precision) AND (longitude <= (180)::double precision)))),
    CONSTRAINT special_zones_zone_type_check CHECK (((zone_type)::text = ANY ((ARRAY['DANGER'::character varying, 'HIGH_INCOME'::character varying])::text[])))
);


--
-- Name: TABLE special_zones; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.special_zones IS 'Zonas especiais que afetam o cálculo do frete (periculosidade ou alta renda)';


--
-- Name: COLUMN special_zones.latitude; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.special_zones.latitude IS 'Latitude do ponto central da zona';


--
-- Name: COLUMN special_zones.longitude; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.special_zones.longitude IS 'Longitude do ponto central da zona';


--
-- Name: COLUMN special_zones.address; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.special_zones.address IS 'Endereço descritivo da zona';


--
-- Name: COLUMN special_zones.zone_type; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.special_zones.zone_type IS 'DANGER (periculosidade) ou HIGH_INCOME (alta renda)';


--
-- Name: COLUMN special_zones.is_active; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.special_zones.is_active IS 'Indica se a zona está ativa para cálculos';


--
-- Name: COLUMN special_zones.notes; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.special_zones.notes IS 'Observações sobre a zona';


--
-- Name: COLUMN special_zones.radius_meters; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.special_zones.radius_meters IS 'Raio de cobertura da zona em metros. Define a distância máxima do ponto central para aplicar a taxa especial.';


--
-- Name: special_zones_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.special_zones_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: special_zones_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.special_zones_id_seq OWNED BY public.special_zones.id;


--
-- Name: user_push_tokens; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.user_push_tokens (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    user_id uuid NOT NULL,
    token character varying(500) NOT NULL,
    platform character varying(20) NOT NULL,
    device_type character varying(20) NOT NULL,
    is_active boolean DEFAULT true NOT NULL,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    web_endpoint character varying(1000),
    web_p256dh character varying(500),
    web_auth character varying(500),
    CONSTRAINT user_push_tokens_device_type_check CHECK (((device_type)::text = ANY ((ARRAY['MOBILE'::character varying, 'WEB'::character varying, 'TABLET'::character varying])::text[]))),
    CONSTRAINT user_push_tokens_platform_check CHECK (((platform)::text = ANY ((ARRAY['IOS'::character varying, 'ANDROID'::character varying, 'WEB'::character varying])::text[])))
);


--
-- Name: TABLE user_push_tokens; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.user_push_tokens IS 'Armazena tokens de dispositivos para notificações push';


--
-- Name: COLUMN user_push_tokens.id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.user_push_tokens.id IS 'Identificador único do token';


--
-- Name: COLUMN user_push_tokens.user_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.user_push_tokens.user_id IS 'ID do usuário proprietário do token';


--
-- Name: COLUMN user_push_tokens.token; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.user_push_tokens.token IS 'Token do dispositivo (Expo Push Token)';


--
-- Name: COLUMN user_push_tokens.platform; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.user_push_tokens.platform IS 'Plataforma do dispositivo (ios, android, web)';


--
-- Name: COLUMN user_push_tokens.device_type; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.user_push_tokens.device_type IS 'Tipo do dispositivo (mobile, web, tablet)';


--
-- Name: COLUMN user_push_tokens.is_active; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.user_push_tokens.is_active IS 'Indica se o token está ativo';


--
-- Name: COLUMN user_push_tokens.created_at; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.user_push_tokens.created_at IS 'Data/hora de criação do registro';


--
-- Name: COLUMN user_push_tokens.updated_at; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.user_push_tokens.updated_at IS 'Data/hora da última atualização';


--
-- Name: CONSTRAINT user_push_tokens_device_type_check ON user_push_tokens; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON CONSTRAINT user_push_tokens_device_type_check ON public.user_push_tokens IS 'Valores válidos: MOBILE, WEB, TABLET (em maiúscula conforme enum Java)';


--
-- Name: CONSTRAINT user_push_tokens_platform_check ON user_push_tokens; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON CONSTRAINT user_push_tokens_platform_check ON public.user_push_tokens IS 'Valores válidos: IOS, ANDROID, WEB (em maiúscula conforme enum Java)';


--
-- Name: adm_profiles id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.adm_profiles ALTER COLUMN id SET DEFAULT nextval('public.adm_profiles_id_seq'::regclass);


--
-- Name: cities id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.cities ALTER COLUMN id SET DEFAULT nextval('public.cities_id_seq'::regclass);


--
-- Name: client_contracts id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.client_contracts ALTER COLUMN id SET DEFAULT nextval('public.contracts_id_seq'::regclass);


--
-- Name: courier_profiles id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.courier_profiles ALTER COLUMN id SET DEFAULT nextval('public.courier_profiles_id_seq'::regclass);


--
-- Name: deliveries id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.deliveries ALTER COLUMN id SET DEFAULT nextval('public.deliveries_id_seq'::regclass);


--
-- Name: employment_contracts id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.employment_contracts ALTER COLUMN id SET DEFAULT nextval('public.employment_contracts_id_seq'::regclass);


--
-- Name: evaluations id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.evaluations ALTER COLUMN id SET DEFAULT nextval('public.evaluations_id_seq'::regclass);


--
-- Name: payout_items id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.payout_items ALTER COLUMN id SET DEFAULT nextval('public.payout_items_id_seq'::regclass);


--
-- Name: site_configurations id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.site_configurations ALTER COLUMN id SET DEFAULT nextval('public.site_configurations_id_seq'::regclass);


--
-- Name: special_zones id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.special_zones ALTER COLUMN id SET DEFAULT nextval('public.special_zones_id_seq'::regclass);


--
-- Name: adm_profiles adm_profiles_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.adm_profiles
    ADD CONSTRAINT adm_profiles_pkey PRIMARY KEY (id);


--
-- Name: adm_profiles adm_profiles_user_id_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.adm_profiles
    ADD CONSTRAINT adm_profiles_user_id_key UNIQUE (user_id);


--
-- Name: cities cities_ibge_code_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.cities
    ADD CONSTRAINT cities_ibge_code_key UNIQUE (ibge_code);


--
-- Name: cities cities_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.cities
    ADD CONSTRAINT cities_pkey PRIMARY KEY (id);


--
-- Name: client_contracts contracts_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.client_contracts
    ADD CONSTRAINT contracts_pkey PRIMARY KEY (id);


--
-- Name: courier_profiles courier_profiles_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.courier_profiles
    ADD CONSTRAINT courier_profiles_pkey PRIMARY KEY (id);


--
-- Name: courier_profiles courier_profiles_user_id_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.courier_profiles
    ADD CONSTRAINT courier_profiles_user_id_key UNIQUE (user_id);


--
-- Name: deliveries deliveries_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.deliveries
    ADD CONSTRAINT deliveries_pkey PRIMARY KEY (id);


--
-- Name: employment_contracts employment_contracts_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.employment_contracts
    ADD CONSTRAINT employment_contracts_pkey PRIMARY KEY (id);


--
-- Name: evaluations evaluations_delivery_id_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.evaluations
    ADD CONSTRAINT evaluations_delivery_id_key UNIQUE (delivery_id);


--
-- Name: evaluations evaluations_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.evaluations
    ADD CONSTRAINT evaluations_pkey PRIMARY KEY (id);


--
-- Name: organizations organizations_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.organizations
    ADD CONSTRAINT organizations_pkey PRIMARY KEY (id);


--
-- Name: payments payments_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.payments
    ADD CONSTRAINT payments_pkey PRIMARY KEY (id);


--
-- Name: payments payments_transaction_id_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.payments
    ADD CONSTRAINT payments_transaction_id_key UNIQUE (transaction_id);


--
-- Name: payout_items payout_items_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.payout_items
    ADD CONSTRAINT payout_items_pkey PRIMARY KEY (id);


--
-- Name: site_configurations site_configurations_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.site_configurations
    ADD CONSTRAINT site_configurations_pkey PRIMARY KEY (id);


--
-- Name: special_zones special_zones_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.special_zones
    ADD CONSTRAINT special_zones_pkey PRIMARY KEY (id);


--
-- Name: user_push_tokens uk_user_push_tokens_user_token; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_push_tokens
    ADD CONSTRAINT uk_user_push_tokens_user_token UNIQUE (user_id, token);


--
-- Name: users ukr43af9ap4edm43mmtq01oddj6; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT ukr43af9ap4edm43mmtq01oddj6 UNIQUE (username);


--
-- Name: organizations uksfr9257mbjkowos3ci3e22ay2; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.organizations
    ADD CONSTRAINT uksfr9257mbjkowos3ci3e22ay2 UNIQUE (slug);


--
-- Name: client_contracts uq_client_contract_client_org; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.client_contracts
    ADD CONSTRAINT uq_client_contract_client_org UNIQUE (client_id, organization_id);


--
-- Name: employment_contracts uq_employment_courier_org; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.employment_contracts
    ADD CONSTRAINT uq_employment_courier_org UNIQUE (courier_id, organization_id);


--
-- Name: user_push_tokens user_push_tokens_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_push_tokens
    ADD CONSTRAINT user_push_tokens_pkey PRIMARY KEY (id);


--
-- Name: users users_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_pkey PRIMARY KEY (id);


--
-- Name: idx_adm_region; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_adm_region ON public.adm_profiles USING btree (region);


--
-- Name: idx_adm_region_code; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_adm_region_code ON public.adm_profiles USING btree (region_code);


--
-- Name: idx_adm_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_adm_status ON public.adm_profiles USING btree (status);


--
-- Name: idx_adm_user; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_adm_user ON public.adm_profiles USING btree (user_id);


--
-- Name: idx_cities_ibge_code; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_cities_ibge_code ON public.cities USING btree (ibge_code);


--
-- Name: idx_cities_name_lower; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_cities_name_lower ON public.cities USING btree (lower((name)::text));


--
-- Name: idx_cities_state_code; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_cities_state_code ON public.cities USING btree (state_code);


--
-- Name: idx_cities_state_name; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_cities_state_name ON public.cities USING btree (state_code, lower((name)::text));


--
-- Name: idx_client_contract_client; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_client_contract_client ON public.client_contracts USING btree (client_id);


--
-- Name: idx_client_contract_organization; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_client_contract_organization ON public.client_contracts USING btree (organization_id);


--
-- Name: idx_client_contract_primary; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_client_contract_primary ON public.client_contracts USING btree (is_primary);


--
-- Name: idx_client_contract_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_client_contract_status ON public.client_contracts USING btree (status);


--
-- Name: idx_courier_location; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_courier_location ON public.courier_profiles USING btree (current_latitude, current_longitude) WHERE ((current_latitude IS NOT NULL) AND (current_longitude IS NOT NULL));


--
-- Name: idx_courier_rating; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_courier_rating ON public.courier_profiles USING btree (rating DESC);


--
-- Name: idx_courier_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_courier_status ON public.courier_profiles USING btree (status);


--
-- Name: idx_courier_user; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_courier_user ON public.courier_profiles USING btree (user_id);


--
-- Name: idx_deliveries_organizer_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_deliveries_organizer_id ON public.deliveries USING btree (organizer_id);


--
-- Name: idx_delivery_client; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_delivery_client ON public.deliveries USING btree (client_id);


--
-- Name: idx_delivery_client_created; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_delivery_client_created ON public.deliveries USING btree (client_id, created_at DESC);


--
-- Name: idx_delivery_completed_at; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_delivery_completed_at ON public.deliveries USING btree (completed_at DESC) WHERE (completed_at IS NOT NULL);


--
-- Name: idx_delivery_courier; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_delivery_courier ON public.deliveries USING btree (courier_id);


--
-- Name: idx_delivery_courier_scheduled; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_delivery_courier_scheduled ON public.deliveries USING btree (courier_id, scheduled_pickup_at) WHERE ((courier_id IS NOT NULL) AND (scheduled_pickup_at IS NOT NULL));


--
-- Name: INDEX idx_delivery_courier_scheduled; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON INDEX public.idx_delivery_courier_scheduled IS 'Índice para consultas do courier por data agendada';


--
-- Name: idx_delivery_courier_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_delivery_courier_status ON public.deliveries USING btree (courier_id, status) WHERE ((courier_id IS NOT NULL) AND ((status)::text = ANY ((ARRAY['ASSIGNED'::character varying, 'PICKED_UP'::character varying, 'IN_TRANSIT'::character varying])::text[])));


--
-- Name: idx_delivery_created_at; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_delivery_created_at ON public.deliveries USING btree (created_at DESC);


--
-- Name: idx_delivery_on_demand_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_delivery_on_demand_status ON public.deliveries USING btree (delivery_type, status, created_at DESC) WHERE (((delivery_type)::text = 'ON_DEMAND'::text) AND ((status)::text = ANY ((ARRAY['PENDING'::character varying, 'ACCEPTED'::character varying])::text[])));


--
-- Name: INDEX idx_delivery_on_demand_status; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON INDEX public.idx_delivery_on_demand_status IS 'Índice para busca rápida de entregas ON_DEMAND disponíveis para COURIERs';


--
-- Name: idx_delivery_payment; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_delivery_payment ON public.deliveries USING btree (payment_id);


--
-- Name: idx_delivery_scheduled_pickup; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_delivery_scheduled_pickup ON public.deliveries USING btree (scheduled_pickup_at) WHERE (scheduled_pickup_at IS NOT NULL);


--
-- Name: INDEX idx_delivery_scheduled_pickup; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON INDEX public.idx_delivery_scheduled_pickup IS 'Índice para consultas ordenadas por data agendada';


--
-- Name: idx_delivery_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_delivery_status ON public.deliveries USING btree (status);


--
-- Name: idx_employment_active; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_employment_active ON public.employment_contracts USING btree (is_active);


--
-- Name: idx_employment_courier; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_employment_courier ON public.employment_contracts USING btree (courier_id);


--
-- Name: idx_employment_organization; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_employment_organization ON public.employment_contracts USING btree (organization_id);


--
-- Name: idx_evaluation_delivery; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_evaluation_delivery ON public.evaluations USING btree (delivery_id);


--
-- Name: idx_evaluation_evaluated; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_evaluation_evaluated ON public.evaluations USING btree (evaluated_id);


--
-- Name: idx_evaluation_evaluator; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_evaluation_evaluator ON public.evaluations USING btree (evaluator_id);


--
-- Name: idx_evaluation_rating; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_evaluation_rating ON public.evaluations USING btree (rating);


--
-- Name: idx_evaluation_type; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_evaluation_type ON public.evaluations USING btree (evaluation_type);


--
-- Name: idx_organizations_owner_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_organizations_owner_id ON public.organizations USING btree (owner_id);


--
-- Name: idx_organizations_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_organizations_status ON public.organizations USING btree (status);


--
-- Name: idx_payment_adm_amount; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_payment_adm_amount ON public.payments USING btree (adm_amount) WHERE (adm_amount IS NOT NULL);


--
-- Name: idx_payment_courier_amount; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_payment_courier_amount ON public.payments USING btree (courier_amount) WHERE (courier_amount IS NOT NULL);


--
-- Name: idx_payment_date; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_payment_date ON public.payments USING btree (payment_date);


--
-- Name: idx_payment_delivery; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_payment_delivery ON public.payments USING btree (delivery_id);


--
-- Name: idx_payment_organization; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_payment_organization ON public.payments USING btree (organization_id);


--
-- Name: idx_payment_payer; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_payment_payer ON public.payments USING btree (payer_id);


--
-- Name: idx_payment_platform_amount; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_payment_platform_amount ON public.payments USING btree (platform_amount) WHERE (platform_amount IS NOT NULL);


--
-- Name: idx_payment_provider; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_payment_provider ON public.payments USING btree (provider);


--
-- Name: idx_payment_registration_method; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_payment_registration_method ON public.payments USING btree (registration_id, payment_method) WHERE (registration_id IS NOT NULL);


--
-- Name: idx_payment_transaction; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_payment_transaction ON public.payments USING btree (transaction_id);


--
-- Name: idx_payments_registration_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_payments_registration_id ON public.payments USING btree (registration_id);


--
-- Name: idx_payout_item_payment; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_payout_item_payment ON public.payout_items USING btree (payment_id);


--
-- Name: idx_payout_item_value_type; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_payout_item_value_type ON public.payout_items USING btree (value_type);


--
-- Name: idx_payout_items_beneficiary_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_payout_items_beneficiary_id ON public.payout_items USING btree (beneficiary_id);


--
-- Name: idx_payout_items_paid_at; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_payout_items_paid_at ON public.payout_items USING btree (paid_at);


--
-- Name: idx_payout_items_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_payout_items_status ON public.payout_items USING btree (status);


--
-- Name: idx_payout_items_value_type; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_payout_items_value_type ON public.payout_items USING btree (value_type);


--
-- Name: idx_site_configurations_single_active; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX idx_site_configurations_single_active ON public.site_configurations USING btree (is_active) WHERE (is_active = true);


--
-- Name: idx_special_zones_active; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_special_zones_active ON public.special_zones USING btree (is_active);


--
-- Name: idx_special_zones_active_type; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_special_zones_active_type ON public.special_zones USING btree (is_active, zone_type) WHERE (is_active = true);


--
-- Name: idx_special_zones_coordinates; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_special_zones_coordinates ON public.special_zones USING btree (latitude, longitude);


--
-- Name: idx_special_zones_type; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_special_zones_type ON public.special_zones USING btree (zone_type);


--
-- Name: idx_user_push_tokens_device_type; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_user_push_tokens_device_type ON public.user_push_tokens USING btree (device_type);


--
-- Name: idx_user_push_tokens_is_active; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_user_push_tokens_is_active ON public.user_push_tokens USING btree (is_active);


--
-- Name: idx_user_push_tokens_platform; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_user_push_tokens_platform ON public.user_push_tokens USING btree (platform);


--
-- Name: idx_user_push_tokens_token; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_user_push_tokens_token ON public.user_push_tokens USING btree (token);


--
-- Name: idx_user_push_tokens_user_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_user_push_tokens_user_id ON public.user_push_tokens USING btree (user_id);


--
-- Name: idx_user_push_tokens_web_endpoint; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_user_push_tokens_web_endpoint ON public.user_push_tokens USING btree (web_endpoint) WHERE (web_endpoint IS NOT NULL);


--
-- Name: idx_users_city_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_users_city_id ON public.users USING btree (city_id);


--
-- Name: idx_users_document_number; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_users_document_number ON public.users USING btree (document_number) WHERE (document_number IS NOT NULL);


--
-- Name: idx_users_location; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_users_location ON public.users USING btree (gps_latitude, gps_longitude) WHERE ((gps_latitude IS NOT NULL) AND (gps_longitude IS NOT NULL));


--
-- Name: idx_users_role; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_users_role ON public.users USING btree (role);


--
-- Name: uk_payment_value_type; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uk_payment_value_type ON public.payout_items USING btree (payment_id, value_type);


--
-- Name: uk_users_document_number; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uk_users_document_number ON public.users USING btree (document_number) WHERE (document_number IS NOT NULL);


--
-- Name: client_contracts enforce_single_primary_client_contract; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER enforce_single_primary_client_contract BEFORE INSERT OR UPDATE ON public.client_contracts FOR EACH ROW EXECUTE FUNCTION public.check_primary_client_contract();


--
-- Name: adm_profiles update_adm_profiles_updated_at; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER update_adm_profiles_updated_at BEFORE UPDATE ON public.adm_profiles FOR EACH ROW EXECUTE FUNCTION public.update_updated_at_column();


--
-- Name: TRIGGER update_adm_profiles_updated_at ON adm_profiles; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TRIGGER update_adm_profiles_updated_at ON public.adm_profiles IS 'Auto-update updated_at on row modification';


--
-- Name: courier_profiles update_courier_profiles_updated_at; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER update_courier_profiles_updated_at BEFORE UPDATE ON public.courier_profiles FOR EACH ROW EXECUTE FUNCTION public.update_updated_at_column();


--
-- Name: TRIGGER update_courier_profiles_updated_at ON courier_profiles; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TRIGGER update_courier_profiles_updated_at ON public.courier_profiles IS 'Auto-update updated_at on row modification';


--
-- Name: deliveries update_deliveries_updated_at; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER update_deliveries_updated_at BEFORE UPDATE ON public.deliveries FOR EACH ROW EXECUTE FUNCTION public.update_updated_at_column();


--
-- Name: TRIGGER update_deliveries_updated_at ON deliveries; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TRIGGER update_deliveries_updated_at ON public.deliveries IS 'Auto-update updated_at on row modification';


--
-- Name: evaluations update_evaluations_updated_at; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER update_evaluations_updated_at BEFORE UPDATE ON public.evaluations FOR EACH ROW EXECUTE FUNCTION public.update_updated_at_column();


--
-- Name: TRIGGER update_evaluations_updated_at ON evaluations; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TRIGGER update_evaluations_updated_at ON public.evaluations IS 'Auto-update updated_at on row modification';


--
-- Name: payout_items update_payout_items_updated_at; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER update_payout_items_updated_at BEFORE UPDATE ON public.payout_items FOR EACH ROW EXECUTE FUNCTION public.update_updated_at_column();


--
-- Name: TRIGGER update_payout_items_updated_at ON payout_items; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TRIGGER update_payout_items_updated_at ON public.payout_items IS 'Auto-update updated_at on row modification';


--
-- Name: adm_profiles fk_adm_user; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.adm_profiles
    ADD CONSTRAINT fk_adm_user FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;


--
-- Name: client_contracts fk_client_contract_client; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.client_contracts
    ADD CONSTRAINT fk_client_contract_client FOREIGN KEY (client_id) REFERENCES public.users(id) ON DELETE CASCADE;


--
-- Name: client_contracts fk_client_contract_organization; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.client_contracts
    ADD CONSTRAINT fk_client_contract_organization FOREIGN KEY (organization_id) REFERENCES public.organizations(id) ON DELETE CASCADE;


--
-- Name: courier_profiles fk_courier_user; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.courier_profiles
    ADD CONSTRAINT fk_courier_user FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;


--
-- Name: deliveries fk_deliveries_organizer; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.deliveries
    ADD CONSTRAINT fk_deliveries_organizer FOREIGN KEY (organizer_id) REFERENCES public.users(id);


--
-- Name: deliveries fk_delivery_client; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.deliveries
    ADD CONSTRAINT fk_delivery_client FOREIGN KEY (client_id) REFERENCES public.users(id) ON DELETE RESTRICT;


--
-- Name: deliveries fk_delivery_courier; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.deliveries
    ADD CONSTRAINT fk_delivery_courier FOREIGN KEY (courier_id) REFERENCES public.users(id) ON DELETE RESTRICT;


--
-- Name: deliveries fk_delivery_payment; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.deliveries
    ADD CONSTRAINT fk_delivery_payment FOREIGN KEY (payment_id) REFERENCES public.payments(id) ON DELETE SET NULL;


--
-- Name: CONSTRAINT fk_delivery_payment ON deliveries; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON CONSTRAINT fk_delivery_payment ON public.deliveries IS 'Foreign key para pagamento da entrega';


--
-- Name: employment_contracts fk_employment_courier; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.employment_contracts
    ADD CONSTRAINT fk_employment_courier FOREIGN KEY (courier_id) REFERENCES public.users(id) ON DELETE CASCADE;


--
-- Name: employment_contracts fk_employment_organization; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.employment_contracts
    ADD CONSTRAINT fk_employment_organization FOREIGN KEY (organization_id) REFERENCES public.organizations(id) ON DELETE CASCADE;


--
-- Name: evaluations fk_evaluation_delivery; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.evaluations
    ADD CONSTRAINT fk_evaluation_delivery FOREIGN KEY (delivery_id) REFERENCES public.deliveries(id) ON DELETE CASCADE;


--
-- Name: evaluations fk_evaluation_evaluated; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.evaluations
    ADD CONSTRAINT fk_evaluation_evaluated FOREIGN KEY (evaluated_id) REFERENCES public.users(id) ON DELETE RESTRICT;


--
-- Name: evaluations fk_evaluation_evaluator; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.evaluations
    ADD CONSTRAINT fk_evaluation_evaluator FOREIGN KEY (evaluator_id) REFERENCES public.users(id) ON DELETE RESTRICT;


--
-- Name: payout_items fk_item_payment; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.payout_items
    ADD CONSTRAINT fk_item_payment FOREIGN KEY (payment_id) REFERENCES public.payments(id) ON DELETE RESTRICT;


--
-- Name: organizations fk_organizations_city; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.organizations
    ADD CONSTRAINT fk_organizations_city FOREIGN KEY (city_id) REFERENCES public.cities(id);


--
-- Name: organizations fk_organizations_owner; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.organizations
    ADD CONSTRAINT fk_organizations_owner FOREIGN KEY (owner_id) REFERENCES public.users(id);


--
-- Name: payments fk_payment_delivery; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.payments
    ADD CONSTRAINT fk_payment_delivery FOREIGN KEY (delivery_id) REFERENCES public.deliveries(id) ON DELETE CASCADE;


--
-- Name: payments fk_payment_organization; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.payments
    ADD CONSTRAINT fk_payment_organization FOREIGN KEY (organization_id) REFERENCES public.organizations(id) ON DELETE SET NULL;


--
-- Name: payments fk_payment_payer; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.payments
    ADD CONSTRAINT fk_payment_payer FOREIGN KEY (payer_id) REFERENCES public.users(id) ON DELETE RESTRICT;


--
-- Name: payout_items fk_payout_items_beneficiary; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.payout_items
    ADD CONSTRAINT fk_payout_items_beneficiary FOREIGN KEY (beneficiary_id) REFERENCES public.users(id);


--
-- Name: user_push_tokens fk_user_push_tokens_user_id; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_push_tokens
    ADD CONSTRAINT fk_user_push_tokens_user_id FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;


--
-- Name: users fk_users_city; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT fk_users_city FOREIGN KEY (city_id) REFERENCES public.cities(id);


--
-- Name: users allow_all_users; Type: POLICY; Schema: public; Owner: -
--

CREATE POLICY allow_all_users ON public.users USING (true) WITH CHECK (true);


--
-- PostgreSQL database dump complete
--

