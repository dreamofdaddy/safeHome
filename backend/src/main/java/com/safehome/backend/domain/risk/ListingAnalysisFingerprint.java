package com.safehome.backend.domain.risk;

import com.safehome.backend.domain.listing.Listing;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

final class ListingAnalysisFingerprint {
    private ListingAnalysisFingerprint() {}
    static String of(Listing listing) {
        if(listing==null)return null;
        Object[] fields={listing.getId(),listing.getAddress(),listing.getDetailAddress(),listing.getBuildingName(),
            listing.getBuildingType(),listing.getTransactionType(),listing.getDepositAmount(),listing.getMonthlyRent(),
            listing.getSalePrice(),listing.getListedAt(),listing.getExpiredAt(),listing.getAddressSearchEvidence()};
        var text=new StringBuilder();
        for(Object field:fields){String value=field==null?"":field.toString();text.append(value.length()).append(':').append(value);}
        try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(text.toString().getBytes(StandardCharsets.UTF_8)));}
        catch(Exception ex){throw new IllegalStateException("Cannot fingerprint listing input",ex);}
    }
}
