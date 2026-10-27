package com.safehome.backend.domain.buildingregister;

import static com.safehome.backend.domain.buildingregister.BuildingRegisterModels.*;
import com.safehome.backend.domain.document.parser.RegistryAddressMatcher;
import com.safehome.backend.domain.document.parser.RegistryParseException;
import java.text.Normalizer;
import java.time.LocalDate;
import java.util.*;
import org.springframework.stereotype.Component;

@Component
public class BuildingRegisterEvaluator {
    public Result evaluate(Context context, Extraction extracted, List<FileRef> files) {
        if(extracted==null || !extracted.buildingRegisterDocument()) throw new RegistryParseException("NOT_BUILDING_REGISTER");
        if(extracted.entries()==null || extracted.entries().isEmpty() || extracted.entries().size()>100)
            throw new RegistryParseException("PARSER_INVALID_OUTPUT");
        BuildingRegisterExtractionValidator.validateCoverage(extracted,files.size());
        var entries=new ArrayList<Entry>(); var checks=new ArrayList<Check>();
        boolean marked=false, review=false;
        String expectedDong=dong(context.detailAddress());
        String expectedUnit=unit(context.detailAddress());
        if(expectedUnit==null) expectedUnit=unit(context.registryUnit());
        for(var e:extracted.entries()) {
            if(e==null || e.fileIndex()<1 || e.fileIndex()>files.size() || e.page()<1
                    || e.page()>files.get(e.fileIndex()-1).pages()
                    || e.documentType()==null || !Set.of("GENERAL","TITLE","EXCLUSIVE","SUMMARY","UNKNOWN").contains(e.documentType())
                    || e.violationStatus()==null || !Set.of("MARKED","NO_MARK_VISIBLE","UNKNOWN").contains(e.violationStatus())
                    || e.evidence()==null || e.evidence().size()>30
                    || (e.areaSquareMeters()!=null && (e.areaSquareMeters().signum()<0 || e.areaSquareMeters().precision()>15)))
                throw new RegistryParseException("PARSER_INVALID_OUTPUT");
            String issued=clean(e.issuedAt(),10);
            if(issued!=null) try { if(LocalDate.parse(issued).isAfter(LocalDate.now().plusDays(1))) throw new IllegalArgumentException(); }
                catch(Exception ex) { throw new RegistryParseException("PARSER_INVALID_OUTPUT"); }
            var evidence=new ArrayList<Evidence>();
            for(var note:e.evidence()) {
                if(note==null || note.page()<1 || note.page()>files.get(e.fileIndex()-1).pages()
                        || note.kind()==null || !Set.of("VIOLATION","USE_CHANGE","AREA_CHANGE","OTHER").contains(note.kind()))
                    throw new RegistryParseException("PARSER_INVALID_OUTPUT");
                String text=clean(note.text(),500);
                if(text!=null) evidence.add(new Evidence(note.page(),note.kind(),text));
            }
            String violation=e.violationStatus();
            if("MARKED".equals(violation) && evidence.stream().noneMatch(n->"VIOLATION".equals(n.kind()) && n.text().contains("위반")))
                violation="UNKNOWN";
            var entry=new Entry(e.fileIndex(),e.page(),e.documentType(),issued,clean(e.jibunAddress(),500),
                    clean(e.roadAddress(),500),clean(e.buildingName(),200),clean(e.dongName(),100),clean(e.floorLabel(),100),
                    clean(e.unitName(),100),clean(e.purpose(),200),e.areaSquareMeters(),violation,List.copyOf(evidence));
            entries.add(entry); marked |= "MARKED".equals(violation);
            String listingAddress=address(context.address(),entry);
            String registryAddress=context.registrySnapshotId()==null ? "NOT_AVAILABLE"
                    : sameFull(context.registryAddress(),entry) || sameFull(context.registryRoadAddress(),entry) ? "MATCHED" : "REVIEW_REQUIRED";
            String comparedUnit="EXCLUSIVE".equals(entry.documentType()) ? compareUnit(expectedUnit,entry.unitName()) : "NOT_APPLICABLE";
            String apiUnit="COMPLETED".equals(context.apiExecutionStatus()) && "MATCHED".equals(context.apiResolutionStatus())
                    && "EXCLUSIVE".equals(entry.documentType()) ? compareUnit(context.apiUnit(),entry.unitName()) : "NOT_AVAILABLE";
            String purpose=purpose(context.buildingType(),entry.purpose());
            String comparedDong=expectedDong==null ? "NOT_AVAILABLE" : dong(entry.dongName())==null ? "NOT_AVAILABLE"
                    : expectedDong.equals(dong(entry.dongName())) ? "MATCHED" : "DIFFERENT";
            checks.add(new Check(entries.size(),listingAddress,registryAddress,comparedUnit,apiUnit,purpose,comparedDong));
            review |= !"MATCHED".equals(listingAddress) || "REVIEW_REQUIRED".equals(registryAddress)
                    || "DIFFERENT".equals(comparedUnit) || "NOT_AVAILABLE".equals(comparedUnit)
                    || "DIFFERENT".equals(apiUnit) || "REVIEW_REQUIRED".equals(purpose)
                    || (expectedDong!=null && !"MATCHED".equals(comparedDong))
                    || "UNKNOWN".equals(violation) || "UNKNOWN".equals(entry.documentType()) || !evidence.isEmpty();
        }
        // Several sections may legitimately represent the title and an exclusive unit. Do not erase disagreements.
        if(new HashSet<>(entries.stream().filter(e->"EXCLUSIVE".equals(e.documentType()))
                .map(e->normalize(e.dongName())+":"+(unit(e.unitName())==null?normalize(e.unitName()):unit(e.unitName()))).toList()).size()>1) review=true;
        BuildingRegisterPolicy.validate(context.buildingType(),entries);
        return new Result(List.copyOf(entries),List.copyOf(checks),marked,
                marked ? "VIOLATION_MENTIONED" : review ? "REVIEW_REQUIRED" : "DOCUMENTS_COMPARED",
                context.registrySnapshotId(),context.apiIdentificationId());
    }
    private String clean(String value,int max) {
        if(value==null || value.isBlank()) return null;
        if(value.length()>max) throw new RegistryParseException("PARSER_INVALID_OUTPUT");
        return value.strip().replaceAll("(?<![0-9])[0-9]{6}[- ]?[1-8][0-9]{6}(?![0-9])","[개인정보 생략]");
    }
    private String address(String expected,Entry e) {
        if(e.jibunAddress()==null && e.roadAddress()==null) return "NOT_AVAILABLE";
        return sameUnitAddress(expected,e.jibunAddress()) || sameUnitAddress(expected,e.roadAddress())
                ? "MATCHED" : "REVIEW_REQUIRED";
    }
    private boolean sameFull(String expected,Entry e) {
        if(sameUnitAddress(expected,e.jibunAddress()) || sameUnitAddress(expected,e.roadAddress())) return true;
        for(String base:Arrays.asList(e.jibunAddress(),e.roadAddress())) {
            if(base==null) continue;
            String details=String.join(" ",Arrays.asList(e.buildingName(),e.dongName(),e.floorLabel(),e.unitName())
                    .stream().filter(Objects::nonNull).toList());
            if(sameUnitAddress(expected,base+" "+details)) return true;
        }
        return false;
    }
    private boolean sameUnitAddress(String left,String right) {
        if(left==null || right==null) return false;
        return RegistryAddressMatcher.same(left.replaceAll("(?:제)?0*([1-9][0-9]*|0)호","$1호"),
                right.replaceAll("(?:제)?0*([1-9][0-9]*|0)호","$1호"));
    }
    private String dong(String value) {
        if(value==null) return null;
        var matcher=java.util.regex.Pattern.compile("(?:제)?0*([0-9]+|[A-Za-z가-힣])동").matcher(Normalizer.normalize(value,Normalizer.Form.NFKC));
        if(!matcher.find()) return null;
        String found=matcher.group(1).toUpperCase(Locale.ROOT);
        return matcher.find()?null:found;
    }
    private String compareUnit(String expected,String observed) {
        String left=unit(expected),right=unit(observed);
        if(left==null || right==null) return "NOT_AVAILABLE";
        return left.equals(right) ? "MATCHED" : "DIFFERENT";
    }
    private String unit(String value) {
        if(value==null) return null;
        String normalized=Normalizer.normalize(value,Normalizer.Form.NFKC).strip();
        var matcher=java.util.regex.Pattern.compile("(?:제)?0*([0-9]+)호").matcher(normalized);
        if(matcher.find()) { String found=matcher.group(1);return matcher.find()?null:found; }
        return normalized.matches("(?:제)?[0-9]+") ? normalized.replaceFirst("^제","").replaceFirst("^0+(?!$)","") : null;
    }
    private String normalize(String value) { return value==null?"":Normalizer.normalize(value,Normalizer.Form.NFKC).replaceAll("제(?=[0-9])|\\s",""); }
    private String purpose(String type,String text) {
        if(text==null) return "NOT_AVAILABLE";
        String compact=text.replaceAll("\\s","");
        List<String> words=switch(type==null?"":type) {
            case "APARTMENT" -> List.of("아파트"); case "OFFICETEL" -> List.of("오피스텔");
            case "VILLA" -> List.of("다세대","연립","빌라"); case "DETACHED_HOUSE" -> List.of("단독주택");
            case "MULTI_FAMILY" -> List.of("다가구"); case "MULTI_UNIT" -> List.of("다세대"); default -> List.of();
        };
        return words.isEmpty()?"NOT_AVAILABLE":words.stream().anyMatch(compact::contains)?"MATCHED":"REVIEW_REQUIRED";
    }
}
