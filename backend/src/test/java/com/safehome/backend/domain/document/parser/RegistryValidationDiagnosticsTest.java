package com.safehome.backend.domain.document.parser;

import com.safehome.backend.domain.listing.Listing;
import java.math.BigDecimal;
import java.nio.file.*;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.jackson.databind.json.JsonMapper;
import static org.assertj.core.api.Assertions.*;

class RegistryValidationDiagnosticsTest {
    @TempDir Path root;
    private final JsonMapper mapper = JsonMapper.builder().build();
    private Listing listing() {
        return Listing.pendingAnalysis(1L, "서울 테스트로 1", "테스트주택", "VILLA", "제202호", "JEONSE",
            BigDecimal.ONE, null, null, null, null);
    }
    private ParsedRegistryDocument parsed() {
        return new ParsedRegistryDocument("1413-2026-000001", "서울 테스트동 2 제203호", null,
            "테스트주택", "203호", "2026-10-23T00:00:00",
            List.of(new ParsedRegistryDocument.Owner("PRIVATE_OWNER_SENTINEL", "800101-*******", BigDecimal.valueOf(100), null)),
            List.of(new ParsedRegistryDocument.Right("MORTGAGE", "PRIVATE_HOLDER_SENTINEL", BigDecimal.TEN, 1, null, "ACTIVE")));
    }
    private Path report() throws Exception {
        try (var files=Files.list(root.resolve("diagnostics/registry-validation"))) {
            return files.findFirst().orElseThrow();
        }
    }
    @Test void recordsOnlyAddressEvidenceAndNormalizedComparisonWithoutOwnerOrRightsContents() throws Exception {
        var diagnostics = new RegistryValidationDiagnostics(mapper, root.toString());
        diagnostics.record(54L, listing(), parsed(), "REGISTRY_ADDRESS_MISMATCH");
        String json = Files.readString(report());
        var tree = mapper.readTree(json);
        assertThat(tree.path("documentId").asLong()).isEqualTo(54L);
        assertThat(tree.path("registered").path("fullAddress").asString()).isEqualTo("서울 테스트로 1 제202호");
        assertThat(tree.path("comparison").path("normalizedRegistered").asString()).isEqualTo("서울특별시테스트로1202호");
        assertThat(tree.path("comparison").path("lotMatches").asBoolean()).isFalse();
        assertThat(tree.path("extracted").path("roadAddress").isNull()).isTrue();
        assertThat(json).doesNotContain("PRIVATE_OWNER_SENTINEL", "PRIVATE_HOLDER_SENTINEL", "800101", "1413-2026-000001");
    }
    @Test void recordsSafeUnsupportedRightReasonWithoutItsSourceTextOrHolder() throws Exception {
        var base=parsed();
        var input=new ParsedRegistryDocument(base.registryPropertyNumber(),base.address(),base.roadAddress(),base.buildingName(),base.unitNumber(),base.observedAt(),base.owners(),
            List.of(new ParsedRegistryDocument.Right("OTHER","PRIVATE_HOLDER_SENTINEL",BigDecimal.TEN,1,null,"ACTIVE","PRIVATE_SOURCE_SENTINEL 가처분")));
        new RegistryValidationDiagnostics(mapper,root.toString()).record(54L,listing(),input,"REGISTRY_RIGHT_UNSUPPORTED");
        var json=Files.readString(report());var check=mapper.readTree(json).path("facts").path("rightChecks").get(0);
        assertThat(check.path("rejectionReason").asString()).isEqualTo("NONE");
        assertThat(check.path("sourceCategory").asString()).isEqualTo("가처분");
        assertThat(json).doesNotContain("PRIVATE_HOLDER_SENTINEL","PRIVATE_SOURCE_SENTINEL","PRIVATE_OWNER_SENTINEL","800101");
    }
    @Test void retainsSeparateReportsForDifferentAttempts() throws Exception {
        var diagnostics = new RegistryValidationDiagnostics(mapper, root.toString());
        diagnostics.record(54L, listing(), parsed(), "REGISTRY_ADDRESS_MISMATCH");
        diagnostics.record(54L, listing(), parsed(), "REGISTRY_ADDRESS_MISMATCH");
        try (var files=Files.list(root.resolve("diagnostics/registry-validation"))) {assertThat(files.count()).isEqualTo(2);}
    }
    @Test void otherValidationFailuresKeepOnlySafeCountsAndPresenceFacts() throws Exception {
        new RegistryValidationDiagnostics(mapper, root.toString()).record(54L, listing(), parsed(), "REGISTRY_OWNER_UNCERTAIN");
        var tree=mapper.readTree(Files.readString(report()));
        assertThat(tree.has("registered")).isFalse();
        assertThat(tree.has("extracted")).isFalse();
        assertThat(tree.path("facts").path("ownerCount").asInt()).isEqualTo(1);
    }
    @Test void inabilityToWriteEvidenceNeverMasksTheOriginalFailure() throws Exception {
        Path file=root.resolve("not-a-directory");Files.writeString(file,"test");
        var diagnostics = new RegistryValidationDiagnostics(mapper, file.toString());
        assertThatCode(()->diagnostics.record(54L, listing(), parsed(), "REGISTRY_ADDRESS_MISMATCH")).doesNotThrowAnyException();
    }
    @Test void missingExtractionIsDiagnosableWithoutInventingAnAddress() throws Exception {
        new RegistryValidationDiagnostics(mapper, root.toString()).record(54L, listing(), null, "REGISTRY_ADDRESS_MISMATCH");
        var tree=mapper.readTree(Files.readString(report()));
        assertThat(tree.path("facts").path("extractionPresent").asBoolean()).isFalse();
        assertThat(tree.path("extracted").path("lotAddress").isNull()).isTrue();
    }
    @Test void readsOnlyMatchingListingAndDocumentAddressFieldsAndToleratesMissingEvidence() throws Exception {
        var diagnostics=new RegistryValidationDiagnostics(mapper,root.toString());var item=listing();
        org.springframework.test.util.ReflectionTestUtils.setField(item,"id",57L);
        diagnostics.record(77L,item,parsed(),"REGISTRY_ADDRESS_MISMATCH");
        var evidence=diagnostics.addressMismatch(77L,57L);
        assertThat(evidence).isNotNull();assertThat(evidence.registeredAddress()).isEqualTo(item.getFullAddress());
        assertThat(evidence.lotAddress()).isEqualTo(parsed().address());
        assertThat(diagnostics.addressMismatch(77L,58L)).isNull();
        assertThat(diagnostics.addressMismatch(78L,57L)).isNull();
        assertThat(mapper.writeValueAsString(evidence)).doesNotContain("PRIVATE_OWNER_SENTINEL","directory","800101","ownerCount");
    }
}
