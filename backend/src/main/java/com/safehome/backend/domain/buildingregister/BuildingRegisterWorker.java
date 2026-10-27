package com.safehome.backend.domain.buildingregister;

import com.safehome.backend.domain.document.PrivatePdfStorage;
import com.safehome.backend.domain.document.parser.RegistryParseException;
import java.security.MessageDigest;
import java.util.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class BuildingRegisterWorker {
    private final BuildingRegisterStore store; private final PrivatePdfStorage storage;
    private final BuildingRegisterParser parser; private final BuildingRegisterEvaluator evaluator;
    public BuildingRegisterWorker(BuildingRegisterStore store,PrivatePdfStorage storage,
            BuildingRegisterParser parser,BuildingRegisterEvaluator evaluator) {
        this.store=store;this.storage=storage;this.parser=parser;this.evaluator=evaluator;
    }
    @Async("buildingRegisterExecutor")
    public void process(long id) {
        try {
            var row=store.get(id); if(row==null || !"PROCESSING".equals(row.status())) return;
            if(!store.current(row)) { store.fail(id,"SOURCE_CHANGED");return; }
            var pdfs=new ArrayList<byte[]>();
            for(var file:row.files()) {
                byte[] pdf=storage.read(file.storageKey());
                String hash=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(pdf));
                if(!hash.equals(file.sha256())) throw new RegistryParseException("FILE_INTEGRITY_FAILED");
                pdfs.add(pdf);
            }
            var result=evaluator.evaluate(row.context(),parser.parse(pdfs),row.files());
            store.complete(id,result,parser.provider(),parser.model());
        } catch(Exception e) {
            String code=e instanceof RegistryParseException p?p.code():"PARSER_REQUEST_FAILED";
            if(!Set.of("PARSER_NOT_CONFIGURED","PARSER_AUTH_FAILED","PARSER_RATE_LIMITED","PARSER_PROVIDER_ERROR",
                    "PARSER_INCOMPLETE","PARSER_EMPTY_RESULT","PARSER_INTERRUPTED","PARSER_REQUEST_FAILED",
                    "PARSER_INVALID_OUTPUT","PARSER_INVALID_SOURCE","PARSER_SOURCE_MISMATCH","NOT_BUILDING_REGISTER","FILE_INTEGRITY_FAILED",
                    "EXCLUSIVE_REGISTER_REQUIRED","GENERAL_REGISTER_REQUIRED","PRIMARY_REGISTER_REQUIRED").contains(code)) code="PARSER_REQUEST_FAILED";
            store.fail(id,code);
        }
    }
}
