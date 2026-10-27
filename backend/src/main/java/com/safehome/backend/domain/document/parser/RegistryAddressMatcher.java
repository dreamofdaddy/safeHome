package com.safehome.backend.domain.document.parser;

import java.text.Normalizer;
import java.util.regex.Pattern;

/** Compare equivalent notation without erasing lot numbers, units or basement markers. */
public final class RegistryAddressMatcher {
    private static final Pattern FLOOR = Pattern.compile("(?<![0-9])(?:제)?(?:지하)?[0-9]+층");
    private RegistryAddressMatcher() {}
    public static boolean same(String left, String right) {
        if (left == null || right == null || left.isBlank() || right.isBlank()) return false;
        String a = normalize(left), b = normalize(right);
        if (a.equals(b)) return true;
        if (a.contains("지하") || b.contains("지하")) return false;
        // A floor may be omitted when the same room number identifies the unit.
        if (!a.matches(".*[0-9]+호.*") || !b.matches(".*[0-9]+호.*")) return false;
        String originalA = Normalizer.normalize(left, Normalizer.Form.NFKC);
        String originalB = Normalizer.normalize(right, Normalizer.Form.NFKC);
        boolean af = FLOOR.matcher(originalA).find(), bf = FLOOR.matcher(originalB).find();
        if (af == bf) return false;
        // Preserve the boundary between lot/building numbers and a separate floor token.
        return normalize(FLOOR.matcher(originalA).replaceAll(""))
            .equals(normalize(FLOOR.matcher(originalB).replaceAll("")));
    }
    private static final String[][] REGION_NAMES = {
        {"서울특별시", "서울"}, {"부산광역시", "부산"}, {"대구광역시", "대구"},
        {"인천광역시", "인천"}, {"광주광역시", "광주"}, {"대전광역시", "대전"},
        {"울산광역시", "울산"}, {"세종특별자치시", "세종"}, {"경기도", "경기"},
        {"강원특별자치도", "강원도", "강원"}, {"충청북도", "충북"}, {"충청남도", "충남"},
        {"전북특별자치도", "전라북도", "전북"}, {"전라남도", "전남"},
        {"경상북도", "경북"}, {"경상남도", "경남"}, {"제주특별자치도", "제주도", "제주"}
    };
    // Only normalize a separate leading administrative-name token, never a building/street name.
    private static String normalizeRegion(String value) {
        String address = value.strip();
        for (String[] names : REGION_NAMES) for (String name : names) {
            if (address.startsWith(name) && (address.length() == name.length()
                    || Character.isWhitespace(address.charAt(name.length())))) {
                return names[0] + address.substring(name.length());
            }
        }
        return address;
    }


    static String normalize(String value) {
        if (value == null) return null;
        return normalizeRegion(Normalizer.normalize(value, Normalizer.Form.NFKC))
            .replaceAll("제(?=[0-9]+(?:동|층|호))", "")
            .replaceAll("[^\\p{IsHangul}A-Za-z0-9-]", "");
    }
}
