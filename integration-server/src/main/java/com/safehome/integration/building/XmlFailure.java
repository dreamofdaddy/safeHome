package com.safehome.integration.building;

import java.security.MessageDigest;
import java.util.HexFormat;
import org.xml.sax.SAXParseException;

/** Parser metadata only; never retain exception text, response bytes, or text excerpts. */
public record XmlFailure(Kind kind, int line, int column, int responseBytes, String responseSha256) {
    public enum Kind { ENTITY_REFERENCE, INVALID_CHARACTER, ENCODING, PROLOG, TRUNCATED, DOCTYPE_FORBIDDEN, OTHER_SYNTAX }
    public XmlFailure {
        if (kind == null || line < 0 || column < 0 || responseBytes < 0
                || responseSha256 == null || !responseSha256.matches("[a-f0-9]{64}"))
            throw new IllegalArgumentException("Invalid XML diagnostic");
    }
    static XmlFailure from(byte[] bytes, SAXParseException error) {
        String message = error.getMessage() == null ? "" : error.getMessage();
        Kind kind = message.contains("DOCTYPE") ? Kind.DOCTYPE_FORBIDDEN
                : message.contains("entity name") || message.contains("reference to entity")
                    || (message.contains("엔티티") && message.contains("참조")) ? Kind.ENTITY_REFERENCE
                : message.contains("invalid XML character") ? Kind.INVALID_CHARACTER
                : message.contains("UTF-8") || message.contains("encoding") ? Kind.ENCODING
                : message.contains("prolog") || message.contains("프롤로그") ? Kind.PROLOG
                : message.contains("same entity") || message.contains("must be terminated") ? Kind.TRUNCATED
                : Kind.OTHER_SYNTAX;
        try {
            return new XmlFailure(kind, Math.max(0, error.getLineNumber()), Math.max(0, error.getColumnNumber()),
                    bytes.length, HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)));
        } catch (java.security.NoSuchAlgorithmException impossible) { throw new IllegalStateException("Digest unavailable"); }
    }
}
