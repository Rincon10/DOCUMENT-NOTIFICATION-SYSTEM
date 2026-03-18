-- ===================================================================
-- MIGRATION: Add document_type column to documents table
-- Author: Ivan Camilo Rincon Saavedra
-- Version: 1.1
-- Date: 2026-03-17
-- ===================================================================

-- Add document_type column if it doesn't exist
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'document'
        AND table_name = 'documents'
        AND column_name = 'document_type'
    ) THEN
        ALTER TABLE "document".documents
        ADD COLUMN document_type "document".document_type NOT NULL DEFAULT 'PDF';

        RAISE NOTICE 'Column document_type added to documents table';
    ELSE
        RAISE NOTICE 'Column document_type already exists in documents table';
    END IF;
END $$;

-- Update existing records to have a valid document_type if null
UPDATE "document".documents
SET document_type = 'PDF'
WHERE document_type IS NULL;
