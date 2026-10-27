package com.safehome.backend.domain.document.parser;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.regex.Pattern;

public final class OwnershipShare {
    private static final Pattern KOREAN = Pattern.compile("^(?:지분)?([0-9]+)분의([0-9]+)$");
    private static final Pattern FRACTION = Pattern.compile("^([0-9]+)/([0-9]+)$");
    private OwnershipShare() {}
    public static BigDecimal percentage(String original) {
        if(original==null || original.isBlank()) return null;
        String value=original.replaceAll("\\s","");
        var korean=KOREAN.matcher(value);var fraction=FRACTION.matcher(value);
        BigDecimal numerator,denominator;
        if(korean.matches()) {denominator=new BigDecimal(korean.group(1));numerator=new BigDecimal(korean.group(2));}
        else if(fraction.matches()) {numerator=new BigDecimal(fraction.group(1));denominator=new BigDecimal(fraction.group(2));}
        else return null;
        if(denominator.signum()<=0 || numerator.signum()<=0 || numerator.compareTo(denominator)>0)
            throw new RegistryParseException("REGISTRY_OWNER_RATIO_INVALID");
        return numerator.multiply(BigDecimal.valueOf(100)).divide(denominator,8,RoundingMode.HALF_UP);
    }
}
