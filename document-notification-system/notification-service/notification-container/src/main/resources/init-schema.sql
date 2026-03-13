-- ===================================================================
-- DOCUMENT NOTIFICATION SYSTEM - NOTIFICATION SERVICE DATABASE INITIALIZATION SCRIPT
-- Author: Ivan Camilo Rincon Saavedra
-- Version: 1.0
-- ===================================================================

-- ===================================================================
-- 1. DROP EXISTING SCHEMA AND RECREATE
-- ===================================================================

DROP SCHEMA IF EXISTS notification CASCADE;
CREATE SCHEMA notification;

-- ===================================================================
-- 2. CREATE ENUM TYPES
-- ===================================================================

-- Notification Status Enum
DROP TYPE IF EXISTS notification.notification_status;
CREATE TYPE notification.notification_status AS ENUM (
    'NOTIFICATION_PENDING',
    'NOTIFICATION_SENT',
    'NOTIFICATION_CANCELLED',
    'NOTIFICATION_FAILED'
);

-- Outbox Status Enum
DROP TYPE IF EXISTS notification.outbox_status;
CREATE TYPE notification.outbox_status AS ENUM (
    'STARTED',
    'COMPLETED',
    'FAILED'
);

-- ===================================================================
-- 3. NOTIFICATION SERVICE SCHEMA - TABLES
-- ===================================================================

-- Document Notification Table
DROP TABLE IF EXISTS notification.document_notification CASCADE;

CREATE TABLE notification.document_notification
(
    id           UUID                            NOT NULL,
    customer_id  UUID                            NOT NULL,
    document_id  UUID                            NOT NULL,
    recipient_id VARCHAR(255),
    subject      VARCHAR(500),
    status       notification.notification_status NOT NULL,
    created_at   TIMESTAMP WITH TIME ZONE        NOT NULL,
    CONSTRAINT document_notification_pkey PRIMARY KEY (id)
);

-- Create Indexes for document notification queries
CREATE INDEX idx_document_notification_document_id ON notification.document_notification (document_id);
CREATE INDEX idx_document_notification_customer_id ON notification.document_notification (customer_id);
CREATE INDEX idx_document_notification_status ON notification.document_notification (status);
CREATE INDEX idx_document_notification_created_at ON notification.document_notification (created_at);

-- ===================================================================
-- 4. OUTBOX PATTERN - TABLES
-- ===================================================================

-- Document Outbox Table (for Notification response flow)
DROP TABLE IF EXISTS notification.document_outbox CASCADE;

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
    CONSTRAINT document_outbox_pkey PRIMARY KEY (id)
);

-- Create Indexes for outbox polling
CREATE INDEX idx_document_outbox_saga_id ON notification.document_outbox (saga_id);
CREATE INDEX idx_document_outbox_status ON notification.document_outbox (outbox_status);
CREATE INDEX idx_document_outbox_created_at ON notification.document_outbox (created_at);
CREATE INDEX idx_document_outbox_notification_status ON notification.document_outbox (notification_status);
CREATE INDEX idx_document_outbox_type_outbox_status ON notification.document_outbox (type, outbox_status);
CREATE INDEX idx_document_outbox_saga_notification_outbox_status
    ON notification.document_outbox (saga_id, notification_status, outbox_status);
CREATE UNIQUE INDEX document_outbox_type_saga_id_notification_status_outbox_status
    ON notification.document_outbox (type, saga_id, notification_status, outbox_status);


SELECT *
FROM notification.document_notification dn;
SELECT *
FROM notification.document_outbox do2;
