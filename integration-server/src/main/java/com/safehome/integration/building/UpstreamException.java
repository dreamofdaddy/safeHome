package com.safehome.integration.building;

// Do not retain URI, response body, credentials, or the underlying exception.
public final class UpstreamException extends RuntimeException {
    private final boolean retryable;
    private final XmlFailure xmlFailure;
    public UpstreamException(String code, boolean retryable) {
        this(code, retryable, null);
    }
    public UpstreamException(String code, boolean retryable, XmlFailure xmlFailure) {
        super(code);
        this.retryable = retryable;
        this.xmlFailure = xmlFailure;
    }
    public boolean retryable() { return retryable; }
    public XmlFailure xmlFailure() { return xmlFailure; }
}
