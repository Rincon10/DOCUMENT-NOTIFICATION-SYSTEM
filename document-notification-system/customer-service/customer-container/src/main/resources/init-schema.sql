DROP SCHEMA IF EXISTS customer CASCADE;

CREATE SCHEMA customer;

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE customer.customers
(
    id uuid NOT NULL,
    username character varying COLLATE pg_catalog."default" NOT NULL,
    first_name character varying COLLATE pg_catalog."default" NOT NULL,
    last_name character varying COLLATE pg_catalog."default" NOT NULL,
    CONSTRAINT customers_pkey PRIMARY KEY (id)
);
DROP MATERIALIZED VIEW IF EXISTS customer.document_customer_m_view;

CREATE MATERIALIZED VIEW customer.document_customer_m_view
TABLESPACE pg_default
AS
 SELECT id,
    username,
    first_name,
    last_name
   FROM customer.customers
WITH DATA;

refresh materialized VIEW customer.document_customer_m_view;

DROP function IF EXISTS customer.refresh_document_customer_m_view;

CREATE OR replace function customer.refresh_document_customer_m_view()
returns trigger
AS '
BEGIN
    refresh materialized VIEW customer.document_customer_m_view;
    return null;
END;
'  LANGUAGE plpgsql;

DROP trigger IF EXISTS refresh_document_customer_m_view ON customer.customers;

CREATE trigger refresh_document_customer_m_view
after INSERT OR UPDATE OR DELETE OR truncate
ON customer.customers FOR each statement
EXECUTE PROCEDURE customer.refresh_document_customer_m_view();
