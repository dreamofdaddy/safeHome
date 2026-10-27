package com.safehome.backend.domain.document;

import com.safehome.backend.domain.document.parser.RegistryDocumentParser;
import com.safehome.backend.domain.document.parser.RegistryParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class RegistryAnalysisWorker {
    private static final Logger log = LoggerFactory.getLogger(RegistryAnalysisWorker.class);
    private final RegistryDocumentRepository documents;
    private final RegistryDocumentService documentService;
    private final RegistryDocumentParser parser;
    private final DocumentAnalysisPersistenceService persistence;
    private final DocumentAnalysisFailureService failure;

    public RegistryAnalysisWorker(RegistryDocumentRepository documents,
            RegistryDocumentService documentService, RegistryDocumentParser parser,
            DocumentAnalysisPersistenceService persistence, DocumentAnalysisFailureService failure) {
        this.documents = documents; this.documentService = documentService;
        this.parser = parser; this.persistence = persistence; this.failure = failure;
    }

    @Async("registryAnalysisExecutor")
    public void process(Long documentId) {
        long started = System.nanoTime();
        String stage = "LOAD_DOCUMENT";
        RegistryDocument document = null;
        try {
            document = documents.findById(documentId).orElseThrow();
            if (!"PROCESSING".equals(document.getStatus())) return;
            stage = "READ_PRIVATE_PDF";
            byte[] pdf = documentService.download(document.getUploadedByUserId(), documentId);
            stage = "PARSE_PDF";
            var parsed = parser.parse(pdf);
            stage = "VALIDATE_AND_PERSIST";
            persistence.complete(documentId, parsed, parser.provider(), parser.model());
        } catch (Exception ex) {
            String code = ex instanceof RegistryParseException parse ? parse.code() : "ANALYSIS_FAILED";
            log.warn("Registry document analysis failed: documentId={}, code={}, exception={}, listingId={}, stage={}, durationMs={}, provider={}, model={}, sizeBytes={}, sha256={}, cause={}",
                documentId, code, ex.getClass().getSimpleName(), document == null ? null : document.getListingId(),
                stage, (System.nanoTime() - started) / 1_000_000, parser.provider(), parser.model(),
                document == null ? null : document.getSizeBytes(), document == null ? null : document.getSha256(),
                ex.getCause() == null ? null : ex.getCause().getClass().getSimpleName());
            failure.fail(documentId, code);
        }
    }
}
