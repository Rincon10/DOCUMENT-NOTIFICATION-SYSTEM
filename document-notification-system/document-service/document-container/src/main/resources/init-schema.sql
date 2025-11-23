DROP SCHEMA IF EXISTS "document" CASCADE;

CREATE SCHEMA "document";

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

DROP TYPE IF EXISTS order_status;
CREATE TYPE order_status AS ENUM ('PENDING', 'GENERATED', 'SENT', 'CANCELLED', 'CANCELLING');

DROP TABLE IF EXISTS "document".documents CASCADE;

CREATE TABLE documents (
    id UUID,
    customer_id UUID,
    account_id UUID,
    file_name VARCHAR(255),
    file_path VARCHAR(500),

    period_start_date DATE NOT NULL,
    period_end_date DATE NOT NULL,

    total_late_interest NUMERIC(19,2),
    total_regular_interest NUMERIC(19,2),
    total_amount NUMERIC(19,2),

    created_by VARCHAR(255),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,

    document_status order_status NOT NULL,
    failure_messages character varying COLLATE pg_catalog."default",
    CONSTRAINT documents_pkey PRIMARY KEY (id)
);