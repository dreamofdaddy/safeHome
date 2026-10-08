CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL,
    name VARCHAR(100) NOT NULL,
    phone VARCHAR(30),
    verification_status VARCHAR(30) NOT NULL DEFAULT 'UNVERIFIED',
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uk_users_email UNIQUE (email)
);

CREATE TABLE properties (
    id BIGSERIAL PRIMARY KEY,
    address VARCHAR(500) NOT NULL,
    road_address VARCHAR(500),
    building_name VARCHAR(200),
    building_type VARCHAR(50),
    unit_number VARCHAR(100),
    postal_code VARCHAR(20),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE property_owners (
    id BIGSERIAL PRIMARY KEY,
    property_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    ownership_ratio NUMERIC(5, 2),
    verified_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_property_owners_property
        FOREIGN KEY (property_id)
        REFERENCES properties(id),

    CONSTRAINT fk_property_owners_user
        FOREIGN KEY (user_id)
        REFERENCES users(id),

    CONSTRAINT uk_property_owners_property_user
        UNIQUE (property_id, user_id),

    CONSTRAINT chk_property_owners_ratio
        CHECK (ownership_ratio IS NULL OR
               ownership_ratio > 0 AND ownership_ratio <= 100)
);

CREATE TABLE listings (
    id BIGSERIAL PRIMARY KEY,
    property_id BIGINT NOT NULL,
    seller_user_id BIGINT NOT NULL,
    transaction_type VARCHAR(30) NOT NULL,
    deposit_amount NUMERIC(15, 0),
    monthly_rent NUMERIC(15, 0),
    sale_price NUMERIC(15, 0),
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    listed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expired_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_listings_property
        FOREIGN KEY (property_id)
        REFERENCES properties(id),

    CONSTRAINT fk_listings_seller
        FOREIGN KEY (seller_user_id)
        REFERENCES users(id)
);

CREATE TABLE registry_snapshots (
    id BIGSERIAL PRIMARY KEY,
    property_id BIGINT NOT NULL,
    source VARCHAR(100) NOT NULL,
    observed_at TIMESTAMP NOT NULL,
    document_hash VARCHAR(128),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_registry_snapshots_property
        FOREIGN KEY (property_id)
        REFERENCES properties(id)
);

CREATE TABLE registry_rights (
    id BIGSERIAL PRIMARY KEY,
    registry_snapshot_id BIGINT NOT NULL,
    right_type VARCHAR(50) NOT NULL,
    holder_name VARCHAR(200),
    amount NUMERIC(15, 0),
    priority INTEGER,
    registered_at TIMESTAMP,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_registry_rights_snapshot
        FOREIGN KEY (registry_snapshot_id)
        REFERENCES registry_snapshots(id)
);

CREATE TABLE contracts (
    id BIGSERIAL PRIMARY KEY,
    property_id BIGINT NOT NULL,
    listing_id BIGINT,
    landlord_user_id BIGINT NOT NULL,
    tenant_user_id BIGINT NOT NULL,
    contract_type VARCHAR(30) NOT NULL,
    deposit_amount NUMERIC(15, 0),
    monthly_rent NUMERIC(15, 0),
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
    signed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_contracts_property
        FOREIGN KEY (property_id)
        REFERENCES properties(id),

    CONSTRAINT fk_contracts_listing
        FOREIGN KEY (listing_id)
        REFERENCES listings(id),

    CONSTRAINT fk_contracts_landlord
        FOREIGN KEY (landlord_user_id)
        REFERENCES users(id),

    CONSTRAINT fk_contracts_tenant
        FOREIGN KEY (tenant_user_id)
        REFERENCES users(id),

    CONSTRAINT chk_contracts_period
        CHECK (end_date >= start_date)
);

CREATE TABLE contract_special_terms (
    id BIGSERIAL PRIMARY KEY,
    contract_id BIGINT NOT NULL,
    term_type VARCHAR(50) NOT NULL,
    content TEXT NOT NULL,
    sequence INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_contract_special_terms_contract
        FOREIGN KEY (contract_id)
        REFERENCES contracts(id)
        ON DELETE CASCADE
);

CREATE TABLE risk_analyses (
    id BIGSERIAL PRIMARY KEY,
    property_id BIGINT NOT NULL,
    registry_snapshot_id BIGINT,
    analysis_version VARCHAR(50) NOT NULL,
    risk_level VARCHAR(30) NOT NULL,
    risk_score NUMERIC(5, 2),
    analyzed_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_risk_analyses_property
        FOREIGN KEY (property_id)
        REFERENCES properties(id),

    CONSTRAINT fk_risk_analyses_registry_snapshot
        FOREIGN KEY (registry_snapshot_id)
        REFERENCES registry_snapshots(id),

    CONSTRAINT chk_risk_analyses_score
        CHECK (risk_score IS NULL OR
               risk_score >= 0 AND risk_score <= 100)
);

CREATE TABLE risk_factors (
    id BIGSERIAL PRIMARY KEY,
    risk_analysis_id BIGINT NOT NULL,
    factor_type VARCHAR(50) NOT NULL,
    severity VARCHAR(30) NOT NULL,
    value VARCHAR(500),
    description TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_risk_factors_analysis
        FOREIGN KEY (risk_analysis_id)
        REFERENCES risk_analyses(id)
        ON DELETE CASCADE
);

CREATE INDEX idx_property_owners_property_id
    ON property_owners(property_id);

CREATE INDEX idx_property_owners_user_id
    ON property_owners(user_id);

CREATE INDEX idx_listings_property_id
    ON listings(property_id);

CREATE INDEX idx_listings_seller_user_id
    ON listings(seller_user_id);

CREATE INDEX idx_listings_status
    ON listings(status);

CREATE INDEX idx_registry_snapshots_property_id
    ON registry_snapshots(property_id);

CREATE INDEX idx_registry_snapshots_observed_at
    ON registry_snapshots(observed_at);

CREATE INDEX idx_registry_rights_snapshot_id
    ON registry_rights(registry_snapshot_id);

CREATE INDEX idx_contracts_property_id
    ON contracts(property_id);

CREATE INDEX idx_contracts_landlord_user_id
    ON contracts(landlord_user_id);

CREATE INDEX idx_contracts_tenant_user_id
    ON contracts(tenant_user_id);

CREATE INDEX idx_contract_special_terms_contract_id
    ON contract_special_terms(contract_id);

CREATE INDEX idx_risk_analyses_property_id
    ON risk_analyses(property_id);

CREATE INDEX idx_risk_analyses_analyzed_at
    ON risk_analyses(analyzed_at);

CREATE INDEX idx_risk_factors_analysis_id
    ON risk_factors(risk_analysis_id);