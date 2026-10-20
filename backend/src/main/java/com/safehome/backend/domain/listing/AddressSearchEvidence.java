package com.safehome.backend.domain.listing;

import com.safehome.backend.common.exception.BusinessException;
import com.safehome.backend.common.exception.ErrorCode;

/** Client supplied postcode selection; validated for internal consistency, not provider authenticity. */
public record AddressSearchEvidence(String selectedType, String selectedAddress,
                                    String jibunAddress, String bcode, String sigunguCode,
                                    String noSelected) {
    public AddressSearchEvidence validatedFor(String address) {
        if (!("R".equals(selectedType) || "J".equals(selectedType))
                || selectedAddress == null || selectedAddress.isBlank() || selectedAddress.length() > 500
                || address == null || !selectedAddress.strip().equals(address.strip())
                || bcode == null || !bcode.matches("[0-9]{10}")
                || sigunguCode == null || !sigunguCode.matches("[0-9]{5}")
                || !bcode.startsWith(sigunguCode)
                || !("Y".equals(noSelected) || "N".equals(noSelected))
                || (jibunAddress != null && jibunAddress.length() > 500)
                || ("J".equals(selectedType) && !selectedAddress.strip().equals(strip(jibunAddress)))) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        return new AddressSearchEvidence(selectedType, selectedAddress.strip(), strip(jibunAddress),
                bcode, sigunguCode, noSelected);
    }

    private static String strip(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }
}
