package com.safehome.backend.domain.buildingregister;

import static com.safehome.backend.domain.buildingregister.BuildingRegisterModels.*;
import com.safehome.backend.domain.document.parser.RegistryParseException;
import java.util.List;
import java.util.Set;

/** The certificate requirement applies only to the optional PDF review, never to safety analysis. */
public final class BuildingRegisterPolicy {
    private BuildingRegisterPolicy() {}
    public static String requiredType(String buildingType) {
        return switch(buildingType==null?"":buildingType) {
            case "APARTMENT","OFFICETEL","VILLA","MULTI_UNIT" -> "EXCLUSIVE";
            case "HOUSE","DETACHED_HOUSE","MULTI_FAMILY" -> "GENERAL";
            default -> "EXCLUSIVE_OR_GENERAL";
        };
    }
    public static boolean hasRequired(String buildingType,List<Entry> entries) {
        String type=requiredType(buildingType);
        return entries.stream().anyMatch(e->type.equals(e.documentType())
                || ("EXCLUSIVE_OR_GENERAL".equals(type) && Set.of("EXCLUSIVE","GENERAL").contains(e.documentType())));
    }
    public static boolean requiresNewFiles(String errorCode) {
        return errorCode!=null && Set.of("EXCLUSIVE_REGISTER_REQUIRED","GENERAL_REGISTER_REQUIRED","PRIMARY_REGISTER_REQUIRED").contains(errorCode);
    }
    public static void validate(String buildingType,List<Entry> entries) {
        if(hasRequired(buildingType,entries)) return;
        throw new RegistryParseException(switch(requiredType(buildingType)) {
            case "EXCLUSIVE" -> "EXCLUSIVE_REGISTER_REQUIRED";
            case "GENERAL" -> "GENERAL_REGISTER_REQUIRED";
            default -> "PRIMARY_REGISTER_REQUIRED";
        });
    }
}
