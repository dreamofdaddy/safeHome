-- New uploads are stored in a private filesystem directory. Old encrypted DB PDFs remain readable.
ALTER TABLE registry_documents
    ALTER COLUMN encrypted_content DROP NOT NULL,
    ADD COLUMN storage_key VARCHAR(64);

ALTER TABLE registry_documents
    ADD CONSTRAINT chk_registry_documents_storage
    CHECK ((encrypted_content IS NOT NULL AND storage_key IS NULL)
        OR (encrypted_content IS NULL AND storage_key IS NOT NULL));

CREATE UNIQUE INDEX uk_registry_documents_storage_key
    ON registry_documents(storage_key) WHERE storage_key IS NOT NULL;
