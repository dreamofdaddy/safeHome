package com.safehome.backend.domain.document;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.safehome.backend.domain.document.parser.*;
import com.safehome.backend.domain.listing.*;
import com.safehome.backend.domain.property.*;
import com.safehome.backend.domain.registry.*;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import java.math.BigDecimal;
import java.util.Optional;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;

class RegistryDiagnosticsFlowTest {
    private RegistryDocument processing() {
        var doc=new RegistryDocument(1L,null,54L,"synthetic.pdf",100,"synthetic-hash","synthetic-key");
        doc.startProcessing();return doc;
    }
    @Test void capturesRejectedExtractionBeforeTransactionFailureAndNeverPersistsItAsSuccessful() {
        var documents=mock(RegistryDocumentRepository.class);var listings=mock(ListingRepository.class);
        var properties=mock(PropertyRepository.class);var snapshots=mock(RegistrySnapshotRepository.class);
        var validator=mock(RegistryExtractionValidator.class);var diagnostics=mock(RegistryValidationDiagnostics.class);
        var listing=Listing.pendingAnalysis(1L,"서울 테스트로 1",null,"VILLA","202호","JEONSE",BigDecimal.ONE,null,null,null,null);
        var parsed=new ParsedRegistryDocument(null,null,null,null,null,null,null,null);
        when(documents.findById(54L)).thenReturn(Optional.of(processing()));
        when(listings.findByIdForUpdate(54L)).thenReturn(Optional.of(listing));
        when(validator.validate(parsed,listing)).thenThrow(new RegistryParseException("REGISTRY_ADDRESS_MISMATCH"));
        var service=new DocumentAnalysisPersistenceService(documents,listings,properties,snapshots,
            mock(PropertyOwnerRepository.class),mock(RegistryRightRepository.class),mock(OwnerIdentityService.class),
            validator,diagnostics);
        assertThatThrownBy(()->service.complete(54L,parsed,"MOCK","test")).hasMessage("REGISTRY_ADDRESS_MISMATCH");
        verify(diagnostics).record(54L,listing,parsed,"REGISTRY_ADDRESS_MISMATCH");
        verifyNoInteractions(properties,snapshots);
    }
    @Test void parserFailureLogHasStageAndEvidenceMetadataButNeverRawExceptionMessages() {
        var documents=mock(RegistryDocumentRepository.class);var service=mock(RegistryDocumentService.class);
        var parser=mock(RegistryDocumentParser.class);var persistence=mock(DocumentAnalysisPersistenceService.class);
        var failure=mock(DocumentAnalysisFailureService.class);
        when(documents.findById(54L)).thenReturn(Optional.of(processing()));
        when(service.download(1L,54L)).thenReturn(new byte[]{1});
        when(parser.provider()).thenReturn("MOCK");when(parser.model()).thenReturn("test");
        when(parser.parse(any())).thenThrow(new IllegalArgumentException("PRIVATE_RAW_ERROR_SENTINEL"));
        var logger=(Logger)LoggerFactory.getLogger(RegistryAnalysisWorker.class);
        var appender=new ListAppender<ILoggingEvent>();appender.start();logger.addAppender(appender);
        try {
            new RegistryAnalysisWorker(documents,service,parser,persistence,failure).process(54L);
            String message=appender.list.getFirst().getFormattedMessage();
            assertThat(message).contains("documentId=54","listingId=54","stage=PARSE_PDF","durationMs=", "provider=MOCK","sizeBytes=100","sha256=synthetic-hash");
            assertThat(message).doesNotContain("PRIVATE_RAW_ERROR_SENTINEL");
            verify(failure).fail(54L,"ANALYSIS_FAILED");verifyNoInteractions(persistence);
        } finally {logger.detachAppender(appender);appender.stop();}
    }
}
