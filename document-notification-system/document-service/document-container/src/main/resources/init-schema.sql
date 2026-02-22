DROP SCHEMA IF EXISTS "document" CASCADE;

CREATE SCHEMA "document";

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

DROP TYPE IF EXISTS "document".document_status;
CREATE TYPE "document".document_status AS ENUM ('PENDING', 'GENERATED', 'SENT', 'CANCELLED', 'CANCELLING');

DROP TABLE IF EXISTS "document".documents CASCADE;

CREATE TABLE "document".documents (
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

                                      document_status "document".document_status NOT NULL,
                                      failure_messages character varying COLLATE pg_catalog."default",
                                      CONSTRAINT documents_pkey PRIMARY KEY (id)
);


DROP TABLE IF EXISTS "document".document_items CASCADE;

CREATE TABLE "document".document_items
(
    id bigint NOT NULL,
    document_id uuid NOT NULL,
    item_id uuid NOT NULL,
    lateInterest numeric(10,2) NOT NULL,
    regularInterest integer NOT NULL,
    sub_total numeric(10,2) NOT NULL,
    CONSTRAINT document_items_pkey PRIMARY KEY (id, document_id)
);

DROP TABLE IF EXISTS "document".customers CASCADE;

CREATE TABLE "document".customers
(
    id uuid NOT NULL,
    username character varying COLLATE pg_catalog."default" NOT NULL,
    first_name character varying COLLATE pg_catalog."default" NOT NULL,
    last_name character varying COLLATE pg_catalog."default" NOT NULL,
    CONSTRAINT customers_pkey PRIMARY KEY (id)
);

INSERT INTO "document".customers
VALUES('550e8400-e29b-41d4-a716-446655440000','camilo.rincon@gmail.com','camilo','rincon')