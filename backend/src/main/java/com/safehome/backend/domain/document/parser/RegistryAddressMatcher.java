package com.safehome.backend.domain.document.parser;

import java.text.Normalizer;
import java.util.regex.Pattern;

/** Compare equivalent notation without erasing lot numbers, units or basement markers. */
public final class RegistryAddressMatcher {
    private static final Pattern FLOOR = Pattern.compile("(?<![0-9])(?:지하)?[0-9]+층");
    private RegistryAddressMatcher() {}
    public static boolean same(String left, String right) {
        if (left == null || right == null || left.isBlank() || right.isBlank()) return false;
        String a = normalize(left), b = normalize(right);
        if (a.equals(b)) return true;
        if (a.contains("지하") || b.contains("지하")) return false;
        // A floor may be omitted when the same room number identifies the unit.
        if (!a.matches(".*[0-9]+호.*") || !b.matches(".*[0-9]+호.*")) return false;
        boolean af = FLOOR.matcher(a).find(), bf = FLOOR.matcher(b).find();
        if (af == bf) return false;
        return FLOOR.matcher(a).replaceAll("").equals(FLOOR.matcher(b).replaceAll(""));
    }
    private static String normalize(String value) {
        return Normalizer.normalize(value, Normalizer.Form.NFKC)
            .replaceAll("제(?=[0-9]+(?:동|층|호))", "")
            .replaceAll("[^\\p{IsHangul}A-Za-z0-9-]", "");
    }
}
