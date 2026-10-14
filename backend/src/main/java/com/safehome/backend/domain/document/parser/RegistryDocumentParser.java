package com.safehome.backend.domain.document.parser;

public interface RegistryDocumentParser {
    ParsedRegistryDocument parse(byte[] pdf);
    String provider();
    String model();
}
