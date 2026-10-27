package com.safehome.backend.domain.document;

import com.safehome.backend.common.exception.BusinessException;
import com.safehome.backend.common.exception.ErrorCode;
import com.safehome.backend.domain.document.dto.*;
import com.safehome.backend.domain.listing.ListingService;
import com.safehome.backend.domain.listing.dto.*;
import org.apache.pdfbox.Loader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;

@Service
public class RegistryDocumentService {
    private static final long MAX_SIZE = 10 * 1024 * 1024;
    private final RegistryDocumentRepository documents;
    private final ListingService listingService;
    private final PrivatePdfStorage storage;
    private final JdbcTemplate jdbc;
    private final String encryptionKey;

    public RegistryDocumentService(RegistryDocumentRepository documents, ListingService listingService,
            PrivatePdfStorage storage, JdbcTemplate jdbc,
            @Value("${safehome.document.encryption-key:}") String encryptionKey) {
        this.documents = documents;
        this.listingService = listingService;
        this.storage = storage;
        this.jdbc = jdbc;
        this.encryptionKey = encryptionKey;
    }

    @Transactional
    public ListingRegistrationResponse register(Long userId, ListingRegistrationRequest request,
                                                MultipartFile registryPdf) {
        byte[] bytes = validatePdf(registryPdf);
        ListingResponse listing = listingService.createPendingAnalysis(userId, request);

        String filename = registryPdf.getOriginalFilename();
        if (filename == null || filename.isBlank()) filename = "registry.pdf";
        filename = filename.replace('/', '_').replace('\\', '_').replace('\r', '_').replace('\n', '_');
        if (filename.length() > 255) filename = filename.substring(filename.length() - 255);
        String storageKey = storage.store(bytes);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status != STATUS_COMMITTED) storage.delete(storageKey);
            }
        });
        RegistryDocument document = documents.save(new RegistryDocument(userId, null,
            listing.id(), filename, bytes.length, sha256(bytes), storageKey));
        return new ListingRegistrationResponse(listing, RegistryDocumentResponse.from(document), "등기부 업로드 완료");
    }

    @Transactional
    public RegistryDocument attach(Long userId,Long listingId,MultipartFile file,java.util.UUID requestId) {
        listingService.assertOwner(listingId,userId);
        String name=file==null?null:file.getOriginalFilename();
        String mime=file==null?null:file.getContentType();
        if(name==null || !name.toLowerCase(java.util.Locale.ROOT).endsWith(".pdf")
            || (mime!=null && !mime.isBlank() && !java.util.Set.of("application/pdf","application/octet-stream").contains(mime)))
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        byte[] bytes=validatePdf(file);
        String key=storage.store(bytes);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization(){
            @Override public void afterCompletion(int status){if(status!=STATUS_COMMITTED)storage.delete(key);}
        });
        name=name.replace('/', '_').replace('\\', '_').replace('\r', '_').replace('\n', '_');
        if(name.length()>255)name=name.substring(name.length()-255);
        RegistryDocument doc=new RegistryDocument(userId,null,listingId,name,bytes.length,sha256(bytes),key);
        doc.consentToAnalysis(requestId);
        return documents.saveAndFlush(doc);
    }

    @Transactional(readOnly = true)
    public RegistryDocumentResponse get(Long userId, Long id) { return RegistryDocumentResponse.from(owned(userId, id)); }

    @Transactional(readOnly = true)
    public List<RegistryDocumentResponse> mine(Long userId) {
        return documents.findByUploadedByUserIdOrderByCreatedAtDesc(userId).stream()
            .filter(d->d.getListingId()==null || listingService.isActiveOwned(d.getListingId(),userId))
            .map(RegistryDocumentResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public byte[] download(Long userId, Long id) {
        RegistryDocument document = owned(userId, id);
        byte[] pdf;
        if (document.getStorageKey() != null) {
            pdf = storage.read(document.getStorageKey());
        } else {
            if (encryptionKey.length() < 32) throw new IllegalStateException("기존 등기부 암호화 키가 설정되지 않았습니다.");
            pdf = jdbc.queryForObject("SELECT pgp_sym_decrypt_bytea(?, ?)", byte[].class,
                document.getEncryptedContent(), encryptionKey);
        }
        if (pdf == null || !sha256(pdf).equals(document.getSha256()))
            throw new IllegalStateException("등기부 PDF 무결성 검증에 실패했습니다.");
        return pdf;
    }

    private RegistryDocument owned(Long userId, Long id) {
        RegistryDocument doc = documents.findById(id)
            .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        if (!doc.getUploadedByUserId().equals(userId)) throw new BusinessException(ErrorCode.FORBIDDEN);
        if(doc.getListingId()!=null) listingService.assertOwner(doc.getListingId(),userId);
        return doc;
    }

    private byte[] validatePdf(MultipartFile file) {
        if (file == null || file.isEmpty() || file.getSize() > MAX_SIZE)
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        try {
            byte[] bytes = file.getBytes();
            if (bytes.length < 5 || bytes[0] != '%' || bytes[1] != 'P' || bytes[2] != 'D'
                    || bytes[3] != 'F' || bytes[4] != '-')
                throw new BusinessException(ErrorCode.INVALID_REQUEST);
            try (var pdf = Loader.loadPDF(bytes)) {
                if (pdf.isEncrypted() || pdf.getNumberOfPages() < 1 || pdf.getNumberOfPages() > 50)
                    throw new BusinessException(ErrorCode.INVALID_REQUEST);
            }
            return bytes;
        } catch (IOException | RuntimeException ex) {
            if (ex instanceof BusinessException business) throw business;
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
    }

    private String sha256(byte[] bytes) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)); }
        catch (Exception e) { throw new IllegalStateException(e); }
    }
}
