-- ===================================================================
-- DOCUMENT NOTIFICATION SYSTEM - CONSOLIDATED DATABASE INIT FOR DOCKER
-- Creates all schemas required by every microservice in a single pass.
-- Executed once when the PostgreSQL container is first created.
-- ===================================================================

-- ===================================================================
-- EXTENSIONS
-- ===================================================================
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- ===================================================================
-- 1. CUSTOMER SCHEMA
-- ===================================================================
DROP SCHEMA IF EXISTS customer CASCADE;
CREATE SCHEMA customer;

CREATE TABLE customer.customers
(
    id         UUID         NOT NULL,
    username   VARCHAR(255) NOT NULL,
    first_name VARCHAR(255) NOT NULL,
    last_name  VARCHAR(255) NOT NULL,
    CONSTRAINT customers_pkey PRIMARY KEY (id),
    CONSTRAINT customers_username_unique UNIQUE (username)
);

CREATE INDEX idx_customer_username ON customer.customers (username);

-- Materialized view used by customer-service itself
DROP MATERIALIZED VIEW IF EXISTS customer.document_customer_m_view;
CREATE MATERIALIZED VIEW customer.document_customer_m_view
TABLESPACE pg_default AS
SELECT id, username, first_name, last_name
FROM customer.customers WITH DATA;

CREATE OR REPLACE FUNCTION customer.refresh_document_customer_m_view()
RETURNS TRIGGER AS $$
BEGIN
    REFRESH MATERIALIZED VIEW customer.document_customer_m_view;
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS refresh_document_customer_m_view ON customer.customers;
CREATE TRIGGER refresh_document_customer_m_view
    AFTER INSERT OR UPDATE OR DELETE OR TRUNCATE
    ON customer.customers
    FOR EACH STATEMENT
    EXECUTE PROCEDURE customer.refresh_document_customer_m_view();

-- Seed data
INSERT INTO customer.customers (id, username, first_name, last_name) VALUES
    ('550e8400-e29b-41d4-a716-446655440000', 'camilo.rincon@gmail.com', 'Camilo', 'Rincon'),
    ('550e8400-e29b-41d4-a716-446655440001', 'proyectobizagig3@gmail.com', 'Camilo', 'Rincon'),
    ('550e8400-e29b-41d4-a716-446655440002', 'maria.garcia@gmail.com', 'Maria', 'Garcia'),
    ('550e8400-e29b-41d4-a716-446655440003', 'carlos.lopez@gmail.com', 'Carlos', 'Lopez')
ON CONFLICT (id) DO NOTHING;

-- ===================================================================
-- 2. DOCUMENT SCHEMA
-- ===================================================================
DROP SCHEMA IF EXISTS "document" CASCADE;
CREATE SCHEMA "document";

-- Enum types
CREATE TYPE "document".document_status AS ENUM ('PENDING', 'GENERATED', 'SENT', 'CANCELLED', 'CANCELLING');
CREATE TYPE "document".document_type AS ENUM ('PDF', 'HTML', 'XML');
CREATE TYPE "document".saga_status AS ENUM ('STARTED', 'SUCCESSFUL', 'FAILED', 'COMPENSATED', 'PROCESSING', 'COMPENSATING');
CREATE TYPE "document".outbox_status AS ENUM ('STARTED', 'COMPLETED', 'FAILED');
CREATE TYPE "document".generation_status AS ENUM ('GENERATION_STARTED', 'GENERATION_COMPLETED', 'GENERATION_FAILED');

-- Documents table
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
    document_type          "document".document_type   NOT NULL,
    failure_messages       TEXT,
    CONSTRAINT documents_pkey PRIMARY KEY (id),
    CONSTRAINT documents_customer_fk FOREIGN KEY (customer_id)
        REFERENCES customer.customers (id) ON DELETE RESTRICT ON UPDATE CASCADE
);

CREATE INDEX idx_document_customer_id ON "document".documents (customer_id);
CREATE INDEX idx_document_status ON "document".documents (document_status);
CREATE INDEX idx_document_created_at ON "document".documents (created_at);
CREATE INDEX idx_document_period_dates ON "document".documents (period_start_date, period_end_date);

-- Document address
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

CREATE INDEX idx_document_address_document_id ON "document".document_address (document_id);

-- Document items
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

CREATE INDEX idx_document_items_document_id ON "document".document_items (document_id);
CREATE INDEX idx_document_items_item_id ON "document".document_items (item_id);

-- Generation outbox
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

CREATE UNIQUE INDEX uk_generation_outbox_type_saga_id_saga_status
    ON "document".generation_outbox (type, saga_id, saga_status);
CREATE INDEX idx_generation_outbox_saga_id ON "document".generation_outbox (saga_id);
CREATE INDEX idx_generation_outbox_status ON "document".generation_outbox (outbox_status);
CREATE INDEX idx_generation_outbox_created_at ON "document".generation_outbox (created_at);
CREATE INDEX idx_generation_outbox_saga_status ON "document".generation_outbox (saga_status);

-- Notification outbox
CREATE TABLE "document".notification_outbox
(
    id              UUID                       NOT NULL,
    saga_id         UUID,
    document_id     UUID,
    created_at      TIMESTAMP,
    processed_at    TIMESTAMP,
    type            VARCHAR(255),
    payload         TEXT,
    saga_status     "document".saga_status,
    document_status "document".document_status,
    outbox_status   "document".outbox_status,
    version         INTEGER DEFAULT 0,
    CONSTRAINT notification_outbox_pkey PRIMARY KEY (id),
    CONSTRAINT notification_outbox_document_fk FOREIGN KEY (document_id)
        REFERENCES "document".documents (id) ON DELETE CASCADE ON UPDATE CASCADE
);

CREATE UNIQUE INDEX uk_notification_outbox_type_saga_id_saga_status
    ON "document".notification_outbox (type, saga_id, saga_status);
CREATE INDEX idx_notification_outbox_saga_id ON "document".notification_outbox (saga_id);
CREATE INDEX idx_notification_outbox_document_id ON "document".notification_outbox (document_id);
CREATE INDEX idx_notification_outbox_status ON "document".notification_outbox (outbox_status);
CREATE INDEX idx_notification_outbox_created_at ON "document".notification_outbox (created_at);
CREATE INDEX idx_notification_outbox_saga_status ON "document".notification_outbox (saga_status);

-- Materialized view for cross-schema customer data
CREATE MATERIALIZED VIEW "document".customers
TABLESPACE pg_default AS
SELECT c.id, c.username, c.first_name, c.last_name
FROM customer.customers c WITH DATA;

CREATE UNIQUE INDEX idx_document_customer_m_view_id_unique ON "document".customers (id);

-- Trigger to refresh document.customers when customer.customers changes
CREATE OR REPLACE FUNCTION customer.refresh_document_customer_m_view_doc()
RETURNS TRIGGER AS $$
BEGIN
    REFRESH MATERIALIZED VIEW CONCURRENTLY "document".customers;
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS refresh_document_customer_m_view_doc ON customer.customers;
CREATE TRIGGER refresh_document_customer_m_view_doc
    AFTER INSERT OR UPDATE OR DELETE OR TRUNCATE
    ON customer.customers
    FOR EACH STATEMENT
    EXECUTE PROCEDURE customer.refresh_document_customer_m_view_doc();

-- ===================================================================
-- 3. GENERATOR SCHEMA
-- ===================================================================
DROP SCHEMA IF EXISTS generator CASCADE;
CREATE SCHEMA generator;

CREATE TYPE generator.generation_status AS ENUM (
    'GENERATION_COMPLETED', 'GENERATION_FAILED', 'GENERATION_CANCELLED'
);

CREATE TYPE generator.outbox_status AS ENUM ('STARTED', 'COMPLETED', 'FAILED');

CREATE TABLE generator.document_generation
(
    id            UUID                        NOT NULL,
    customer_id   UUID                        NOT NULL,
    document_id   UUID                        NOT NULL,
    document_name VARCHAR(100),
    status        generator.generation_status NOT NULL,
    created_at    TIMESTAMP WITH TIME ZONE    NOT NULL,
    CONSTRAINT document_generation_pkey PRIMARY KEY (id)
);

CREATE INDEX idx_document_generation_document_id ON generator.document_generation (document_id);
CREATE INDEX idx_document_generation_customer_id ON generator.document_generation (customer_id);
CREATE INDEX idx_document_generation_status ON generator.document_generation (status);
CREATE INDEX idx_document_generation_created_at ON generator.document_generation (created_at);

CREATE TABLE generator.document_outbox
(
    id                UUID                        NOT NULL,
    saga_id           UUID                        NOT NULL,
    created_at        TIMESTAMP WITH TIME ZONE    NOT NULL,
    processed_at      TIMESTAMP WITH TIME ZONE,
    type              VARCHAR(255)                NOT NULL,
    payload           TEXT                        NOT NULL,
    generation_status generator.generation_status NOT NULL,
    outbox_status     generator.outbox_status     NOT NULL,
    version           INTEGER DEFAULT 0,
    CONSTRAINT document_outbox_pkey PRIMARY KEY (id)
);

CREATE INDEX idx_gen_document_outbox_saga_id ON generator.document_outbox (saga_id);
CREATE INDEX idx_gen_document_outbox_status ON generator.document_outbox (outbox_status);
CREATE INDEX idx_gen_document_outbox_created_at ON generator.document_outbox (created_at);
CREATE INDEX idx_gen_document_outbox_generation_status ON generator.document_outbox (generation_status);
CREATE INDEX idx_gen_document_outbox_type_outbox_status ON generator.document_outbox (type, outbox_status);
CREATE INDEX idx_gen_document_outbox_saga_gen_outbox_status
    ON generator.document_outbox (saga_id, generation_status, outbox_status);
CREATE UNIQUE INDEX gen_document_outbox_type_saga_id_gen_status_outbox_status
    ON generator.document_outbox (type, saga_id, generation_status, outbox_status);

-- ===================================================================
-- 4. NOTIFICATION SCHEMA
-- ===================================================================
DROP SCHEMA IF EXISTS notification CASCADE;
CREATE SCHEMA notification;

CREATE TYPE notification.notification_status AS ENUM (
    'NOTIFICATION_PENDING', 'NOTIFICATION_SENT', 'NOTIFICATION_CANCELLED', 'NOTIFICATION_FAILED'
);

CREATE TYPE notification.outbox_status AS ENUM ('STARTED', 'COMPLETED', 'FAILED');

CREATE TABLE notification.document_notification
(
    id           UUID                             NOT NULL,
    customer_id  UUID                             NOT NULL,
    document_id  UUID                             NOT NULL,
    recipient_id VARCHAR(255),
    subject      VARCHAR(500),
    status       notification.notification_status NOT NULL,
    created_at   TIMESTAMP WITH TIME ZONE         NOT NULL,
    CONSTRAINT document_notification_pkey PRIMARY KEY (id)
);

CREATE INDEX idx_document_notification_document_id ON notification.document_notification (document_id);
CREATE INDEX idx_document_notification_customer_id ON notification.document_notification (customer_id);
CREATE INDEX idx_document_notification_status ON notification.document_notification (status);
CREATE INDEX idx_document_notification_created_at ON notification.document_notification (created_at);

CREATE TABLE notification.document_outbox
(
    id                  UUID                             NOT NULL,
    saga_id             UUID                             NOT NULL,
    created_at          TIMESTAMP WITH TIME ZONE         NOT NULL,
    processed_at        TIMESTAMP WITH TIME ZONE,
    type                VARCHAR(255)                     NOT NULL,
    payload             TEXT                             NOT NULL,
    notification_status notification.notification_status NOT NULL,
    outbox_status       notification.outbox_status       NOT NULL,
    version             INTEGER DEFAULT 0,
    CONSTRAINT notif_document_outbox_pkey PRIMARY KEY (id)
);

CREATE INDEX idx_notif_document_outbox_saga_id ON notification.document_outbox (saga_id);
CREATE INDEX idx_notif_document_outbox_status ON notification.document_outbox (outbox_status);
CREATE INDEX idx_notif_document_outbox_created_at ON notification.document_outbox (created_at);
CREATE INDEX idx_notif_document_outbox_notification_status ON notification.document_outbox (notification_status);
CREATE INDEX idx_notif_document_outbox_type_outbox_status ON notification.document_outbox (type, outbox_status);
CREATE INDEX idx_notif_document_outbox_saga_notif_outbox_status
    ON notification.document_outbox (saga_id, notification_status, outbox_status);
CREATE UNIQUE INDEX notif_document_outbox_type_saga_id_notif_status_outbox_status
    ON notification.document_outbox (type, saga_id, notification_status, outbox_status);

-- ===================================================================
-- DONE - All 4 schemas ready: customer, document, generator, notification
-- ===================================================================
