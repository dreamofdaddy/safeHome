package com.safehome.integration.building;

// Do not retain URI, response body, credentials, or the underlying exception.
public final class UpstreamException extends RuntimeException {
    private final boolean retryable;
    public UpstreamException(String code, boolean retryable) {
        super(code);
        this.retryable = retryable;
    }
    public boolean retryable() { return retryable; }
}
