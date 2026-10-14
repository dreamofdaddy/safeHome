package com.safehome.backend.domain.document.parser;

public class RegistryParseException extends RuntimeException {
    private final String code;

    public RegistryParseException(String code) {
        super(code);
        this.code = code;
    }

    public String code() { return code; }
}
