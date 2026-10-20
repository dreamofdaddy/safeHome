-- Listings store exactly one user-selected address, either lot-number or road-name format.
UPDATE listings
SET listing_address = COALESCE(
    NULLIF(BTRIM(listing_address), ''),
    NULLIF(BTRIM(road_address), '')
);

UPDATE listings AS listing
SET listing_address = COALESCE(
    NULLIF(BTRIM(property.address), ''),
    NULLIF(BTRIM(property.road_address), '')
)
FROM properties AS property
WHERE listing.property_id = property.id
  AND (listing.listing_address IS NULL OR BTRIM(listing.listing_address) = '');

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM listings WHERE listing_address IS NULL OR BTRIM(listing_address) = '') THEN
        RAISE EXCEPTION 'Cannot consolidate listings with a missing address';
    END IF;
END $$;

ALTER TABLE listings RENAME COLUMN listing_address TO address;
ALTER TABLE listings ALTER COLUMN address SET NOT NULL;
ALTER TABLE listings DROP COLUMN road_address;
ALTER TABLE listings DROP COLUMN postal_code;

COMMENT ON COLUMN listings.address IS '사용자가 선택한 지번 또는 도로명 매물주소';
