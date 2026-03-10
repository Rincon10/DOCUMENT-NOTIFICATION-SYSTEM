-- ===================================================================
-- DOCUMENT NOTIFICATION SYSTEM - DATABASE INITIALIZATION SCRIPT
-- Author: Ivan Camilo Rincon Saavedra
-- Version: 1.0
-- Date: 2026-02-24
-- ===================================================================

-- ===================================================================
-- 1. DROP EXISTING SCHEMA AND RECREATE
-- ===================================================================

DROP SCHEMA IF EXISTS "document" CASCADE;
CREATE SCHEMA "document";

DROP SCHEMA IF EXISTS customer CASCADE;
CREATE SCHEMA customer;

-- ===================================================================
-- 2. CREATE EXTENSIONS
-- ===================================================================

--CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- ===================================================================
-- 3. CREATE ENUM TYPES
-- ===================================================================

-- Document Status Enum
DROP TYPE IF EXISTS "document".document_status;
CREATE TYPE "document".document_status AS ENUM ('PENDING', 'GENERATED', 'SENT', 'CANCELLED', 'CANCELLING');

-- Document Type Enum (for future use)
DROP TYPE IF EXISTS "document".document_type;
CREATE TYPE "document".document_type AS ENUM ('INVOICE', 'RECEIPT', 'STATEMENT', 'OTHER');

-- Saga Status Enum
DROP TYPE IF EXISTS "document".saga_status;
CREATE TYPE "document".saga_status AS ENUM ('STARTED', 'SUCCESSFUL', 'FAILED', 'COMPENSATED','PROCESSING','COMPENSATING');

-- Outbox Status Enum
DROP TYPE IF EXISTS "document".outbox_status;
CREATE TYPE "document".outbox_status AS ENUM ('STARTED', 'COMPLETED', 'FAILED');

-- Generation Status Enum
DROP TYPE IF EXISTS "document".generation_status;
CREATE TYPE "document".generation_status AS ENUM ('GENERATION_STARTED', 'GENERATION_COMPLETED', 'GENERATION_FAILED');

-- ===================================================================
-- 4. CUSTOMER SERVICE SCHEMA - TABLES
-- ===================================================================

DROP TABLE IF EXISTS customer.customers CASCADE;

CREATE TABLE customer.customers
(
    id         UUID         NOT NULL,
    username   VARCHAR(255) NOT NULL,
    first_name VARCHAR(255) NOT NULL,
    last_name  VARCHAR(255) NOT NULL,
    CONSTRAINT customers_pkey PRIMARY KEY (id),
    CONSTRAINT customers_username_unique UNIQUE (username)
);

-- Create Index for customer queries
CREATE INDEX idx_customer_username ON customer.customers (username);

-- ===================================================================
-- 5. DOCUMENT SERVICE SCHEMA - TABLES
-- ===================================================================

-- Documents Table
DROP TABLE IF EXISTS "document".documents CASCADE;

CREATE TABLE "document".documents
(
    id                     UUID                       NOT NULL,
    customer_id            UUID                       NOT NULL,
    generation_id          UUID,
    file_name              VARCHAR(255),
    file_path              VARCHAR(500),
    period_start_date      DATE                       NOT NULL,
    period_end_date        DATE                       NOT NULL,
    total_late_interest    NUMERIC(19, 2),
    total_regular_interest NUMERIC(19, 2),
    total_amount           NUMERIC(19, 2),
    created_by             VARCHAR(255),
    created_at             TIMESTAMP                  NOT NULL,
    updated_at             TIMESTAMP,
    document_status        "document".document_status NOT NULL,
    failure_messages       TEXT,
    CONSTRAINT documents_pkey PRIMARY KEY (id),
    CONSTRAINT documents_customer_fk FOREIGN KEY (customer_id)
        REFERENCES customer.customers (id) ON DELETE RESTRICT ON UPDATE CASCADE
);

-- Create Indexes for documents
CREATE INDEX idx_document_customer_id ON "document".documents (customer_id);
CREATE INDEX idx_document_status ON "document".documents (document_status);
CREATE INDEX idx_document_created_at ON "document".documents (created_at);
CREATE INDEX idx_document_period_dates ON "document".documents (period_start_date, period_end_date);

-- ===================================================================
-- Document Address Table
-- ===================================================================

DROP TABLE IF EXISTS "document".document_address CASCADE;

CREATE TABLE "document".document_address
(
    id           UUID NOT NULL,
    document_id  UUID NOT NULL,
    state        VARCHAR(20),
    postal_code  VARCHAR(20),
    address_line VARCHAR(255),
    city         VARCHAR(100),
    country      VARCHAR(100),
    CONSTRAINT document_address_pkey PRIMARY KEY (id),
    CONSTRAINT document_address_document_fk FOREIGN KEY (document_id)
        REFERENCES "document".documents (id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT document_address_unique UNIQUE (document_id)
);

-- Create Index
CREATE INDEX idx_document_address_document_id ON "document".document_address (document_id);

-- ===================================================================
-- Document Items Table
-- ===================================================================

DROP TABLE IF EXISTS "document".document_items CASCADE;

CREATE TABLE "document".document_items
(
    id               BIGINT NOT NULL,
    document_id      UUID   NOT NULL,
    item_id          UUID   NOT NULL,
    late_interest    NUMERIC(10, 2),
    regular_interest NUMERIC(10, 2),
    sub_total        NUMERIC(10, 2),
    CONSTRAINT document_items_pkey PRIMARY KEY (id, document_id),
    CONSTRAINT document_items_document_fk FOREIGN KEY (document_id)
        REFERENCES "document".documents (id) ON DELETE CASCADE ON UPDATE CASCADE
);

-- Create Index
CREATE INDEX idx_document_items_document_id ON "document".document_items (document_id);
CREATE INDEX idx_document_items_item_id ON "document".document_items (item_id);

-- ===================================================================
-- 6. OUTBOX PATTERN - TABLES
-- ===================================================================

-- Generation Outbox Table (for Saga Pattern)
DROP TABLE IF EXISTS "document".generation_outbox CASCADE;

CREATE TABLE "document".generation_outbox
(
    id              UUID                       NOT NULL,
    saga_id         UUID                       NOT NULL,
    created_at      TIMESTAMP,
    processed_at    TIMESTAMP,
    type            VARCHAR(255)               NOT NULL,
    payload         TEXT                       NOT NULL,
    saga_status     "document".saga_status     NOT NULL,
    document_status "document".document_status NOT NULL,
    outbox_status   "document".outbox_status   NOT NULL,
    version         INTEGER DEFAULT 0,
    CONSTRAINT generation_outbox_pkey PRIMARY KEY (id),
    CONSTRAINT generation_outbox_saga_id_unique UNIQUE (saga_id)
);

-- Create Indexes for outbox polling
CREATE UNIQUE INDEX uk_generation_outbox_type_saga_id_saga_status
    ON "document".generation_outbox (type, saga_id, saga_status);
CREATE INDEX idx_generation_outbox_saga_id ON "document".generation_outbox (saga_id);
CREATE INDEX idx_generation_outbox_status ON "document".generation_outbox (outbox_status);
CREATE INDEX idx_generation_outbox_created_at ON "document".generation_outbox (created_at);
CREATE INDEX idx_generation_outbox_saga_status ON "document".generation_outbox (saga_status);

-- Notification Outbox Table (for Notification Service)
DROP TABLE IF EXISTS "document".notification_outbox CASCADE;

CREATE TABLE "document".notification_outbox
(
    id            UUID                     NOT NULL,
    saga_id       UUID                     NOT NULL,
    document_id   UUID                     NOT NULL,
    created_at    TIMESTAMP                NOT NULL,
    processed_at  TIMESTAMP,
    type          VARCHAR(255)             NOT NULL,
    payload       TEXT                     NOT NULL,
    saga_status   "document".saga_status  NOT NULL,
    outbox_status "document".outbox_status NOT NULL,
    version       INTEGER DEFAULT 0,
    CONSTRAINT notification_outbox_pkey PRIMARY KEY (id),
    CONSTRAINT notification_outbox_document_fk FOREIGN KEY (document_id)
        REFERENCES "document".documents (id) ON DELETE CASCADE ON UPDATE CASCADE
);

-- Create Indexes for notification outbox polling
CREATE UNIQUE INDEX uk_notification_outbox_type_saga_id_saga_status
    ON "document".notification_outbox (type, saga_id, saga_status);
CREATE INDEX idx_notification_outbox_saga_id ON "document".notification_outbox (saga_id);
CREATE INDEX idx_notification_outbox_document_id ON "document".notification_outbox (document_id);
CREATE INDEX idx_notification_outbox_status ON "document".notification_outbox (outbox_status);
CREATE INDEX idx_notification_outbox_created_at ON "document".notification_outbox (created_at);
CREATE INDEX idx_notification_outbox_saga_status ON "document".notification_outbox (saga_status);

-- ===================================================================
-- 7. MATERIALIZED VIEWS FOR CROSS-SCHEMA QUERIES
-- ===================================================================

-- Materialized View for Customer Data in Document Schema
DROP
MATERIALIZED VIEW IF EXISTS "document".customers CASCADE;

CREATE
MATERIALIZED VIEW "document".customers
TABLESPACE pg_default
AS
SELECT c.id,
       c.username,
       c.first_name,
       c.last_name
FROM customer.customers c WITH DATA;

-- Create UNIQUE Index on materialized view for CONCURRENT refresh
CREATE UNIQUE INDEX idx_document_customer_m_view_id_unique ON "document".customers (id);

-- ===================================================================
-- 8. TRIGGERS FOR MATERIALIZED VIEW REFRESH
-- ===================================================================

-- Function to refresh materialized view
DROP FUNCTION IF EXISTS customer.refresh_document_customer_m_view() CASCADE;

CREATE
OR REPLACE FUNCTION customer.refresh_document_customer_m_view()
RETURNS TRIGGER AS $$
BEGIN
    REFRESH
MATERIALIZED VIEW CONCURRENTLY "document".customers;
RETURN NULL;
END;
$$
LANGUAGE plpgsql;

-- Trigger to refresh on changes to customers
DROP TRIGGER IF EXISTS refresh_document_customer_m_view ON customer.customers;

CREATE TRIGGER refresh_document_customer_m_view
    AFTER INSERT OR
UPDATE OR
DELETE
OR TRUNCATE
ON customer.customers
    FOR EACH STATEMENT
    EXECUTE PROCEDURE customer.refresh_document_customer_m_view();

-- ===================================================================
-- 9. INITIAL DATA POPULATION
-- ===================================================================

-- Insert sample customer
INSERT INTO customer.customers (id, username, first_name, last_name)
VALUES ('550e8400-e29b-41d4-a716-446655440000', 'camilo.rincon@gmail.com', 'Camilo',
        'Rincon') ON CONFLICT (id) DO NOTHING;

-- Insert sample customers for testing
INSERT INTO customer.customers (id, username, first_name, last_name)
VALUES ('550e8400-e29b-41d4-a716-446655440001', 'juan.perez@gmail.com', 'Juan', 'Perez'),
       ('550e8400-e29b-41d4-a716-446655440002', 'maria.garcia@gmail.com', 'Maria', 'Garcia'),
       ('550e8400-e29b-41d4-a716-446655440003', 'carlos.lopez@gmail.com', 'Carlos',
        'Lopez') ON CONFLICT (id) DO NOTHING;

-- Refresh the materialized view with initial data
REFRESH
MATERIALIZED VIEW "document".customers;