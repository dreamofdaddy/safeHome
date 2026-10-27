ALTER TABLE registry_documents ADD COLUMN analysis_request_id UUID;
ALTER TABLE registry_documents ADD COLUMN analysis_consented_at TIMESTAMP;
CREATE UNIQUE INDEX uq_registry_analysis_request ON registry_documents(listing_id,analysis_request_id) WHERE analysis_request_id IS NOT NULL;
ALTER TABLE property_owners ADD COLUMN ownership_type VARCHAR(20) NOT NULL DEFAULT 'UNKNOWN' CHECK(ownership_type IN ('OWNER','CO_OWNER','UNKNOWN'));
ALTER TABLE property_owners ADD COLUMN registry_priority_number VARCHAR(40);
ALTER TABLE property_owners ADD COLUMN owner_address VARCHAR(1000);
ALTER TABLE property_owners ADD COLUMN acquisition_amount NUMERIC(20,2) CHECK(acquisition_amount>=0);
