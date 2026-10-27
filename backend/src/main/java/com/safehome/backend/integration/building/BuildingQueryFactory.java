package com.safehome.backend.integration.building;

import com.safehome.backend.domain.document.parser.VerifiedRegistryDocument;
import com.safehome.backend.domain.listing.AddressSearchEvidence;
import com.safehome.backend.domain.listing.Listing;
import java.text.Normalizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/** Builds a query only from selected cadastral evidence and explicit unit labels. */
@Component
public class BuildingQueryFactory {
    private static final Pattern LOT = Pattern.compile("(?:^|\\s)(산\\s*)?([0-9]{1,4})(?:-([0-9]{1,4}))?\\s*$");
    private static final Pattern DONG = Pattern.compile("(?<![0-9A-Za-z가-힣])(?:제)?0*([1-9][0-9]*)동");
    private static final Pattern FLOOR = Pattern.compile("(지하|지상|B)?(?:제)?0*([1-9][0-9]*)층|지층");
    private static final Pattern UNIT = Pattern.compile("(?:제)?0*([0-9]+)호");
    private static final Pattern BARE_UNIT = Pattern.compile("(?:제)?0*([0-9]+)(?:호)?");

    public enum Status { READY, INPUT_INCOMPLETE, INPUT_CONFLICT }
    public record Parts(String rawDong, String normalizedDong, String rawFloor,
                        BuildingQuery.Ground ground, Integer floorNumber,
                        String rawUnit, String normalizedUnit, boolean repeatedLabel) {}
    public record Result(Status status, String reason, BuildingQuery query,
                         Parts listingParts, Parts registryParts) {}

    public Result build(Listing listing, VerifiedRegistryDocument registry) {
        if (listing == null || registry == null) return result(Status.INPUT_INCOMPLETE, "MISSING_SOURCE", null, null);
        Parts fromListing = parse(listing.getDetailAddress(), false);
        Parts fromRegistry = combine(parse(registry.address(), false), parse(registry.unitNumber(), true));
        return buildParts(listing, fromListing, fromRegistry);
    }

    public Result buildListing(Listing listing) {
        if (listing == null) return result(Status.INPUT_INCOMPLETE, "MISSING_SOURCE", null, null);
        return buildParts(listing, parse(listing.getDetailAddress(), false), parse(null, false));
    }

    private Result buildParts(Listing listing, Parts fromListing, Parts fromRegistry) {
        if (fromListing.repeatedLabel() || fromRegistry == null || fromRegistry.repeatedLabel())
            return result(Status.INPUT_CONFLICT, "REPEATED_OR_CONFLICTING_LABEL", fromListing, fromRegistry);
        AddressSearchEvidence evidence = listing.getAddressSearchEvidence();
        if (evidence == null || !"N".equals(evidence.noSelected()) || evidence.jibunAddress() == null)
            return result(Status.INPUT_INCOMPLETE, "CONFIRMED_JIBUN_REQUIRED", fromListing, fromRegistry);
        if (!evidence.selectedAddress().equals(listing.getAddress())
                || !evidence.bcode().matches("[0-9]{10}")
                || !evidence.bcode().startsWith(evidence.sigunguCode()))
            return result(Status.INPUT_CONFLICT, "ADDRESS_EVIDENCE_CONFLICT", fromListing, fromRegistry);
        Matcher lot = LOT.matcher(compactSpaces(evidence.jibunAddress()));
        if (!lot.find()) return result(Status.INPUT_INCOMPLETE, "LOT_REQUIRED", fromListing, fromRegistry);
        if (different(fromListing.normalizedDong(), fromRegistry.normalizedDong())
                || different(fromListing.normalizedUnit(), fromRegistry.normalizedUnit())
                || (fromListing.ground() != null && fromRegistry.ground() != null
                    && fromListing.ground() != fromRegistry.ground())
                || different(fromListing.floorNumber(), fromRegistry.floorNumber()))
            return result(Status.INPUT_CONFLICT, "UNIT_LABEL_CONFLICT", fromListing, fromRegistry);
        String dong = choose(fromListing.normalizedDong(), fromRegistry.normalizedDong());
        String unit = choose(fromListing.normalizedUnit(), fromRegistry.normalizedUnit());
        BuildingQuery.Ground ground = fromListing.ground() != null ? fromListing.ground() : fromRegistry.ground();
        Integer floor = choose(fromListing.floorNumber(), fromRegistry.floorNumber());
        if (ground == null || floor == null || unit == null)
            return result(Status.INPUT_INCOMPLETE, "EXPLICIT_FLOOR_AND_UNIT_REQUIRED", fromListing, fromRegistry);
        try {
            return new Result(Status.READY, null,
                    new BuildingQuery(evidence.sigunguCode(), evidence.bcode().substring(5),
                            lot.group(1) == null ? "0" : "1", pad(lot.group(2)),
                            pad(lot.group(3) == null ? "0" : lot.group(3)), dong, ground, floor, unit),
                    fromListing, fromRegistry);
        } catch (IllegalArgumentException ex) {
            return result(Status.INPUT_INCOMPLETE, "INVALID_QUERY_FIELD", fromListing, fromRegistry);
        }
    }

    private Parts parse(String raw, boolean bareUnit) {
        if (raw == null || raw.isBlank()) return new Parts(null, null, null, null, null, null, null, false);
        String text = Normalizer.normalize(raw, Normalizer.Form.NFKC);
        Matcher dong = DONG.matcher(text), floor = FLOOR.matcher(text), unit = UNIT.matcher(text);
        String rawDong = dong.find() ? dong.group() : null;
        String normalizedDong = rawDong == null ? null : numeric(dong.group(1));
        String rawFloor = floor.find() ? floor.group() : null;
        String rawUnit = unit.find() ? unit.group() : null;
        if (rawUnit == null && bareUnit) {
            Matcher plain = BARE_UNIT.matcher(text.strip());
            if (plain.matches()) rawUnit = plain.group();
        }
        BuildingQuery.Ground ground = null;
        Integer floorNumber = null;
        if (rawFloor != null) {
            ground = rawFloor.startsWith("지하") || rawFloor.startsWith("B") || rawFloor.equals("지층")
                    ? BuildingQuery.Ground.BELOW_GROUND : BuildingQuery.Ground.ABOVE_GROUND;
            if (!rawFloor.equals("지층")) {
                try { floorNumber = Integer.parseInt(floor.group(2)); }
                catch (NumberFormatException ignored) { floorNumber = null; }
            }
        }
        boolean repeated = (rawDong != null && dong.find()) || (rawFloor != null && floor.find())
                || (rawUnit != null && unit.find());
        return new Parts(rawDong, normalizedDong, rawFloor,
                ground, floorNumber, rawUnit, rawUnit == null ? null : numericUnit(rawUnit), repeated);
    }

    private Parts combine(Parts left, Parts right) {
        if (different(left.normalizedDong(), right.normalizedDong())
                || different(left.normalizedUnit(), right.normalizedUnit())
                || (left.ground() != null && right.ground() != null && left.ground() != right.ground())
                || different(left.floorNumber(), right.floorNumber())) return null;
        return new Parts(choose(left.rawDong(), right.rawDong()),
                choose(left.normalizedDong(), right.normalizedDong()),
                choose(left.rawFloor(), right.rawFloor()),
                left.ground() != null ? left.ground() : right.ground(),
                choose(left.floorNumber(), right.floorNumber()),
                choose(left.rawUnit(), right.rawUnit()),
                choose(left.normalizedUnit(), right.normalizedUnit()),
                left.repeatedLabel() || right.repeatedLabel());
    }
    private static String numericUnit(String raw) {
        Matcher matcher = BARE_UNIT.matcher(raw);
        return matcher.matches() ? numeric(matcher.group(1)) : null;
    }
    private static String numeric(String value) {
        return value.replaceFirst("^0+(?!$)", "");
    }
    private static String pad(String value) { return "0".repeat(4 - value.length()) + value; }
    private static String compactSpaces(String value) {
        return Normalizer.normalize(value, Normalizer.Form.NFKC).strip().replaceAll("\\s+", " ");
    }
    private static <T> T choose(T left, T right) { return left != null ? left : right; }
    private static <T> boolean different(T left, T right) {
        return left != null && right != null && !left.equals(right);
    }
    private static Result result(Status status, String reason, Parts listing, Parts registry) {
        return new Result(status, reason, null, listing, registry);
    }
}
