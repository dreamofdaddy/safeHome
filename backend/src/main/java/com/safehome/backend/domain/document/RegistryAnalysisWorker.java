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
        try {
            RegistryDocument document = documents.findById(documentId).orElseThrow();
            if (!"PROCESSING".equals(document.getStatus())) return;
            byte[] pdf = documentService.download(document.getUploadedByUserId(), documentId);
            persistence.complete(documentId, parser.parse(pdf), parser.provider(), parser.model());
        } catch (Exception ex) {
            String code = ex instanceof RegistryParseException parse ? parse.code() : "ANALYSIS_FAILED";
            log.warn("Registry document analysis failed: documentId={}, code={}, exception={}",
                documentId, code, ex.getClass().getSimpleName());
            failure.fail(documentId, code);
        }
    }
}
