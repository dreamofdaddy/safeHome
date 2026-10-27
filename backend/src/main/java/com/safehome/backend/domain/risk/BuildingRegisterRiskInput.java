package com.safehome.backend.domain.risk;

import com.safehome.backend.domain.buildingregister.BuildingRegisterPolicy;
import com.safehome.backend.domain.buildingregister.BuildingRegisterStore;
import com.safehome.backend.domain.listing.Listing;
import com.safehome.backend.domain.risk.analysis.RiskRuleResult;
import org.springframework.stereotype.Component;
import java.util.List;

/** Latest successfully analysed PDF facts, independent of subsequent API comparisons. */
@Component
public class BuildingRegisterRiskInput {
    private final BuildingRegisterStore store;
    public BuildingRegisterRiskInput(BuildingRegisterStore store){this.store=store;}
    public record Input(String fingerprint,List<RiskRuleResult> factors) {}
    public Input read(Listing listing) {
        if(listing==null || listing.getId()==null)return new Input("building-violation-v1:none",List.of());
        var review=store.latestParsed(listing.getId());
        if(review==null || review.result()==null || !store.sourceMatches(review)
                || !BuildingRegisterPolicy.hasRequired(listing.getBuildingType(),review.result().entries()))
            return new Input("building-violation-v1:none",List.of());
        var marked=review.result().entries().stream().filter(e->"MARKED".equals(e.violationStatus())).toList();
        String fingerprint="building-violation-v1:"+review.id()+":"+!marked.isEmpty();
        if(marked.isEmpty())return new Input(fingerprint,List.of());
        String source=marked.stream().map(e->"파일 "+e.fileIndex()+" · "+e.page()+"페이지").distinct().collect(java.util.stream.Collectors.joining(", "));
        return new Input(fingerprint,List.of(RiskRuleResult.of("BUILDING_VIOLATION","HIGH",
            "건축물대장 문서 #"+review.id()+" · "+source,
            "첨부 건축물대장에서 위반건축물 표시가 확인되었습니다. 해당 호실 또는 건물의 위반 대상·내용과 시정 여부를 확인해 주세요. 표제부·총괄표제부 기재는 해당 호실과의 관련성을 추가로 확인해야 합니다.")));
    }
}
