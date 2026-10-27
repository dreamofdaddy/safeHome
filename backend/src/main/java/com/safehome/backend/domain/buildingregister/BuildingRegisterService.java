package com.safehome.backend.domain.buildingregister;

import static com.safehome.backend.domain.buildingregister.BuildingRegisterModels.*;
import com.safehome.backend.common.exception.*;
import com.safehome.backend.domain.document.PrivatePdfStorage;
import com.safehome.backend.domain.listing.*;
import java.security.MessageDigest;
import java.util.*;
import org.apache.pdfbox.Loader;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.*;
import org.springframework.web.multipart.MultipartFile;

@Service
public class BuildingRegisterService {
    private final ListingRepository listings;private final BuildingRegisterStore store;
    private final PrivatePdfStorage storage;private final BuildingRegisterWorker worker;
    public BuildingRegisterService(ListingRepository listings,BuildingRegisterStore store,PrivatePdfStorage storage,BuildingRegisterWorker worker) {
        this.listings=listings;this.store=store;this.storage=storage;this.worker=worker;
    }
    private Listing owned(long userId,long listingId,boolean lock) {
        Listing l=(lock?listings.findByIdForUpdate(listingId):listings.findById(listingId))
                .orElseThrow(()->new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        if(!Objects.equals(l.getRegisteredByUserId(),userId)) throw new BusinessException(ErrorCode.FORBIDDEN);
        return l;
    }
    @Transactional(readOnly=true) public Envelope current(long userId,long listingId) {
        owned(userId,listingId,false);var history=store.history(listingId);
        var summaries=history.stream().map(r->new Summary(r.id(),r.status(),!store.documentCurrent(r),r.createdAt())).toList();
        return new Envelope(history.isEmpty()?null:response(history.getFirst()),summaries);
    }
    @Transactional public Review uploadAndAnalyze(long userId,long listingId,List<MultipartFile> files,boolean consent,UUID requestId) {
        var listing=owned(userId,listingId,true);
        if(!consent || requestId==null)throw new BusinessException(ErrorCode.BUILDING_DOCUMENT_CONSENT);
        var existing=store.byRequest(listingId,requestId);
        if(existing!=null)return response(existing);
        if("REGISTRY_PROCESSING".equals(listing.getStatus()) || store.hasRunningWork(listingId))throw new BusinessException(ErrorCode.BUILDING_DOCUMENT_STATE);
        var uploaded=upload(userId,listingId,files);
        store.bindRequest(uploaded.id(),requestId);
        return start(userId,listingId,uploaded.id(),true);
    }
    @Transactional public Review upload(long userId,long listingId,List<MultipartFile> files) {
        var listing=owned(userId,listingId,true);
        if(files==null || files.isEmpty() || files.size()>5) throw new BusinessException(ErrorCode.BUILDING_DOCUMENT_INVALID);
        long total=0;int pages=0;
        var bytes=new ArrayList<byte[]>();var counts=new ArrayList<Integer>();var names=new ArrayList<String>();
        try {
            for(var f:files) {
                total+=f.getSize();
                if(f.isEmpty() || total>10L*1024*1024 || f.getOriginalFilename()==null
                        || !f.getOriginalFilename().toLowerCase(Locale.ROOT).endsWith(".pdf"))
                    throw new BusinessException(ErrorCode.BUILDING_DOCUMENT_INVALID);
                byte[] pdf=f.getBytes();
                if(pdf.length<5 || pdf[0]!='%' || pdf[1]!='P' || pdf[2]!='D' || pdf[3]!='F' || pdf[4]!='-')
                    throw new BusinessException(ErrorCode.BUILDING_DOCUMENT_INVALID);
                try(var document=Loader.loadPDF(pdf)) {
                    int count=document.getNumberOfPages();pages+=count;
                    if(document.isEncrypted() || count<1 || pages>50) throw new BusinessException(ErrorCode.BUILDING_DOCUMENT_INVALID);
                    counts.add(count);
                }
                bytes.add(pdf);
                String name=f.getOriginalFilename().replaceAll("[\\p{Cntrl}/\\\\]","_");
                names.add(name.length()>200?name.substring(name.length()-200):name);
            }
        } catch(BusinessException e) { throw e; }
        catch(Exception e) { throw new BusinessException(ErrorCode.BUILDING_DOCUMENT_INVALID); }
        var refs=new ArrayList<FileRef>();
        // Register cleanup before the first write; partially stored bundles are removed on rollback.
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCompletion(int status) {
                if(status!=STATUS_COMMITTED) for(var ref:refs) storage.delete(ref.storageKey());
            }
        });
        for(int i=0;i<bytes.size();i++) {
            byte[] pdf=bytes.get(i);String hash;
            try { hash=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(pdf)); }
            catch(Exception e) { throw new IllegalStateException("Digest unavailable"); }
            refs.add(new FileRef(names.get(i),pdf.length,counts.get(i),hash,storage.store(pdf)));
        }
        long id=store.create(listing,List.copyOf(refs));return response(store.get(id));
    }
    @Transactional public Review start(long userId,long listingId,long reviewId,boolean consent) {
        owned(userId,listingId,true);
        var row=store.get(reviewId);
        if(row==null || row.listingId()!=listingId) throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        if(!consent) throw new BusinessException(ErrorCode.BUILDING_DOCUMENT_CONSENT);
        if(BuildingRegisterPolicy.requiresNewFiles(row.errorCode()) || !store.current(row) || !store.start(reviewId)) throw new BusinessException(ErrorCode.BUILDING_DOCUMENT_STATE);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCommit() { try { worker.process(reviewId); }catch(RuntimeException e){store.fail(reviewId,"QUEUE_BUSY");} }
        });
        return response(store.get(reviewId));
    }
    @Transactional(readOnly=true) public Review review(long userId,long listingId,long reviewId) {
        owned(userId,listingId,false);var row=store.get(reviewId);
        if(row==null || row.listingId()!=listingId) throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        return response(row);
    }
    @Transactional(readOnly=true) public byte[] download(long userId,long listingId,long reviewId,int index) {
        owned(userId,listingId,false);var row=store.get(reviewId);
        if(row==null || row.listingId()!=listingId || index<1 || index>row.files().size())
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        var file=row.files().get(index-1);byte[] pdf=storage.read(file.storageKey());
        try { if(!HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(pdf)).equals(file.sha256()))
                throw new BusinessException(ErrorCode.BUILDING_DOCUMENT_INVALID); }
        catch(BusinessException e) {throw e;}catch(Exception e){throw new IllegalStateException("Digest unavailable");}
        return pdf;
    }
    private Review response(Stored row) {
        var files=new ArrayList<FileInfo>();for(int i=0;i<row.files().size();i++) {
            var f=row.files().get(i);files.add(new FileInfo(i+1,f.name(),f.sizeBytes(),f.pages()));
        }
        return new Review(row.id(),row.status(),BuildingRegisterPolicy.requiredType(row.context().buildingType()),List.copyOf(files),row.result(),row.errorCode(),row.attemptCount(),
                row.provider(),row.model(),!store.documentCurrent(row),row.createdAt(),row.processedAt());
    }
    @EventListener(ApplicationReadyEvent.class) public void recover() { store.recover(); }
}
