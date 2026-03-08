-- ===================================================================
-- DOCUMENT NOTIFICATION SYSTEM - GENERATOR SERVICE DATABASE INITIALIZATION SCRIPT
-- Author: Ivan Camilo Rincon Saavedra
-- Version: 1.0
-- Date: 2026-03-07
-- ===================================================================

-- ===================================================================
-- 1. DROP EXISTING SCHEMA AND RECREATE
-- ===================================================================

DROP SCHEMA IF EXISTS generator CASCADE;
CREATE SCHEMA generator;

-- ===================================================================
-- 2. CREATE EXTENSIONS
-- ===================================================================

--CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- ===================================================================
-- 3. CREATE ENUM TYPES
-- ===================================================================

-- Generation Status Enum
DROP TYPE IF EXISTS generator.generation_status;
CREATE TYPE generator.generation_status AS ENUM (
    'GENERATION_COMPLETED',
    'GENERATION_FAILED',
    'GENERATION_CANCELLED'
);

-- Outbox Status Enum
DROP TYPE IF EXISTS generator.outbox_status;
CREATE TYPE generator.outbox_status AS ENUM (
    'STARTED',
    'COMPLETED',
    'FAILED'
);

-- ===================================================================
-- 4. GENERATOR SERVICE SCHEMA - TABLES
-- ===================================================================

-- Document Generation Table
DROP TABLE IF EXISTS generator.document_generation CASCADE;

CREATE TABLE generator.document_generation
(
    id            UUID                       NOT NULL,
    customer_id   UUID                       NOT NULL,
    document_id   UUID                       NOT NULL,
    document_name VARCHAR(100),
    status        generator.generation_status NOT NULL,
    created_at    TIMESTAMP WITH TIME ZONE   NOT NULL,
    CONSTRAINT document_generation_pkey PRIMARY KEY (id)
);

-- Create Indexes for document generation queries
CREATE INDEX idx_document_generation_document_id ON generator.document_generation (document_id);
CREATE INDEX idx_document_generation_customer_id ON generator.document_generation (customer_id);
CREATE INDEX idx_document_generation_status ON generator.document_generation (status);
CREATE INDEX idx_document_generation_created_at ON generator.document_generation (created_at);

-- ===================================================================
-- 5. OUTBOX PATTERN - TABLES
-- ===================================================================

-- Document Outbox Table (for Generator response flow)
DROP TABLE IF EXISTS generator.document_outbox CASCADE;

CREATE TABLE generator.document_outbox
(
    id                UUID                    NOT NULL,
    saga_id           UUID                    NOT NULL,
    created_at        TIMESTAMP WITH TIME ZONE NOT NULL,
    processed_at      TIMESTAMP WITH TIME ZONE,
    type              VARCHAR(255)            NOT NULL,
    payload           TEXT                    NOT NULL,
    generation_status generator.generation_status NOT NULL,
    outbox_status     generator.outbox_status NOT NULL,
    version           INTEGER DEFAULT 0,
    CONSTRAINT document_outbox_pkey PRIMARY KEY (id)
);

-- Create Indexes for outbox polling
CREATE INDEX idx_document_outbox_saga_id ON generator.document_outbox (saga_id);
CREATE INDEX idx_document_outbox_status ON generator.document_outbox (outbox_status);
CREATE INDEX idx_document_outbox_created_at ON generator.document_outbox (created_at);
CREATE INDEX idx_document_outbox_generation_status ON generator.document_outbox (generation_status);
CREATE INDEX idx_document_outbox_type_outbox_status ON generator.document_outbox (type, outbox_status);
CREATE INDEX idx_document_outbox_saga_generation_outbox_status
    ON generator.document_outbox (saga_id, generation_status, outbox_status);


SELECT * FROM generator.document_generation dg;
SELECT * FROM generator.document_generation;