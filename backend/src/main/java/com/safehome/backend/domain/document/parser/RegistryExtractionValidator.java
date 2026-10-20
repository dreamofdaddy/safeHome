package com.safehome.backend.domain.document.parser;

import com.safehome.backend.domain.listing.Listing;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Set;

@Component
public class RegistryExtractionValidator {
    private static final Set<String> RIGHT_TYPES = Set.of(
        "MORTGAGE", "LEASEHOLD", "SEIZURE", "PROVISIONAL_SEIZURE", "OTHER");

    public VerifiedRegistryDocument validate(ParsedRegistryDocument parsed, Listing listing) {
        if (parsed == null || parsed.registryPropertyNumber() == null
                || !parsed.registryPropertyNumber().matches("[0-9]{4}-[0-9]{4}-[0-9]{6}"))
            throw new RegistryParseException("REGISTRY_NUMBER_UNCERTAIN");
        if ((blank(parsed.address()) && blank(parsed.roadAddress()))
                || listing == null || blank(listing.getFullAddress())
                || (!sameAddress(listing.getFullAddress(), parsed.address())
                    && !sameAddress(listing.getFullAddress(), parsed.roadAddress())))
            throw new RegistryParseException("REGISTRY_ADDRESS_MISMATCH");
        if (parsed.owners() == null || parsed.owners().isEmpty())
            throw new RegistryParseException("REGISTRY_OWNER_UNCERTAIN");
        LocalDateTime observedAt = parseDate(parsed.observedAt(), true);
        if (observedAt.isAfter(LocalDateTime.now().plusDays(1)))
            throw new RegistryParseException("REGISTRY_DATE_INVALID");

        List<VerifiedRegistryDocument.Owner> owners = parsed.owners().stream().map(owner -> {
            if (blank(owner.name()) || owner.name().length() > 200
                    || owner.maskedIdentifier() == null
                    || !owner.maskedIdentifier().matches("[0-9]{6}-\\*{7}"))
                throw new RegistryParseException("REGISTRY_OWNER_UNCERTAIN");
            if (owner.ownershipRatio() != null && (owner.ownershipRatio().compareTo(BigDecimal.ZERO) <= 0
                    || owner.ownershipRatio().compareTo(BigDecimal.valueOf(100)) > 0))
                throw new RegistryParseException("REGISTRY_OWNER_RATIO_INVALID");
            return new VerifiedRegistryDocument.Owner(owner.name().trim(), owner.maskedIdentifier(),
                owner.ownershipRatio(), parseDate(owner.registeredAt(), false));
        }).toList();

        if (parsed.rights() == null) throw new RegistryParseException("REGISTRY_RIGHTS_UNCERTAIN");
        List<VerifiedRegistryDocument.Right> rights = parsed.rights().stream().map(right -> {
            if (right.type() == null || !RIGHT_TYPES.contains(right.type())
                    || !Set.of("ACTIVE", "INACTIVE").contains(right.status())
                    || ("OTHER".equals(right.type()) && "ACTIVE".equals(right.status())))
                throw new RegistryParseException("REGISTRY_RIGHT_UNSUPPORTED");
            if (right.amount() != null && right.amount().compareTo(BigDecimal.ZERO) < 0)
                throw new RegistryParseException("REGISTRY_RIGHT_AMOUNT_INVALID");
            if (right.priority() != null && right.priority() <= 0)
                throw new RegistryParseException("REGISTRY_RIGHT_PRIORITY_INVALID");
            return new VerifiedRegistryDocument.Right(right.type(), right.holderName(),
                right.amount(), right.priority(), parseDate(right.registeredAt(), false), right.status());
        }).toList();
        String primaryAddress = blank(parsed.address()) ? parsed.roadAddress().trim() : parsed.address().trim();
        return new VerifiedRegistryDocument(parsed.registryPropertyNumber(), primaryAddress,
            parsed.roadAddress(), parsed.buildingName(), parsed.unitNumber(), observedAt, owners, rights);
    }

    private LocalDateTime parseDate(String value, boolean required) {
        if (blank(value)) {
            if (required) throw new RegistryParseException("REGISTRY_DATE_INVALID");
            return null;
        }
        try { return LocalDateTime.parse(value); }
        catch (DateTimeParseException ex) { throw new RegistryParseException("REGISTRY_DATE_INVALID"); }
    }

    private boolean blank(String value) { return value == null || value.isBlank(); }

    private boolean sameAddress(String left, String right) {
        return RegistryAddressMatcher.same(left, right);
    }
}
