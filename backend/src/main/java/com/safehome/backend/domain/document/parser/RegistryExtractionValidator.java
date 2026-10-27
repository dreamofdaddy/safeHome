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
        "MORTGAGE", "LEASEHOLD", "SEIZURE", "PROVISIONAL_SEIZURE", "TRUST", "AUCTION", "OTHER");

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
            String entity = owner.entityType() == null ? "PERSON" : owner.entityType();
            boolean corporation = "CORPORATION".equals(entity);
            if (!Set.of("PERSON","CORPORATION","UNKNOWN").contains(entity)
                    || blank(owner.name()) || owner.name().length() > 200
                    || (corporation ? owner.maskedIdentifier() != null && !owner.maskedIdentifier().matches("[0-9]{6}-[0-9*]{7}")
                        : owner.maskedIdentifier() == null || !owner.maskedIdentifier().matches("[0-9]{6}-\\*{7}")))
                throw new RegistryParseException("REGISTRY_OWNER_UNCERTAIN");
            if (owner.ownershipRatio() != null && (owner.ownershipRatio().compareTo(BigDecimal.ZERO) <= 0
                    || owner.ownershipRatio().compareTo(BigDecimal.valueOf(100)) > 0))
                throw new RegistryParseException("REGISTRY_OWNER_RATIO_INVALID");
            if (owner.ownershipShare()!=null && owner.ownershipShare().length()>100)
                throw new RegistryParseException("REGISTRY_OWNER_RATIO_INVALID");
            if(owner.ownershipType()!=null && !Set.of("OWNER","CO_OWNER","UNKNOWN").contains(owner.ownershipType())
                || owner.priorityNumber()!=null && owner.priorityNumber().length()>40
                || owner.ownerAddress()!=null && owner.ownerAddress().length()>1000
                || owner.acquisitionAmount()!=null && (owner.acquisitionAmount().signum()<0 || owner.acquisitionAmount().precision()-owner.acquisitionAmount().scale()>18))
                throw new RegistryParseException("REGISTRY_OWNER_UNCERTAIN");
            BigDecimal computed=OwnershipShare.percentage(owner.ownershipShare());
            return new VerifiedRegistryDocument.Owner(owner.name().trim(), owner.maskedIdentifier(),
                computed==null?owner.ownershipRatio():computed, parseDate(owner.registeredAt(), false),owner.ownershipShare(),
                owner.ownershipType()==null?"UNKNOWN":owner.ownershipType(),owner.priorityNumber(),owner.ownerAddress(),owner.acquisitionAmount(),corporation ? "CORPORATION" : "PERSON");
        }).toList();

        if (parsed.rights() == null) throw new RegistryParseException("REGISTRY_RIGHTS_UNCERTAIN");
        List<VerifiedRegistryDocument.Right> rights = parsed.rights().stream().map(right -> {
            if (right == null || right.type() == null || !RIGHT_TYPES.contains(right.type())
                    || right.status() == null || !Set.of("ACTIVE", "INACTIVE").contains(right.status())
                    || right.sourceName() != null && (right.sourceName().length() > 200 || right.sourceName().chars().anyMatch(Character::isISOControl)))
                throw new RegistryParseException("REGISTRY_RIGHT_UNSUPPORTED");
            if (right.amount() != null && right.amount().compareTo(BigDecimal.ZERO) < 0)
                throw new RegistryParseException("REGISTRY_RIGHT_AMOUNT_INVALID");
            if (right.priority() != null && right.priority() <= 0)
                throw new RegistryParseException("REGISTRY_RIGHT_PRIORITY_INVALID");
            return new VerifiedRegistryDocument.Right(right.type(), right.holderName(),
                right.amount(), right.priority(), parseDate(right.registeredAt(), false), right.status(), right.sourceName());
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
