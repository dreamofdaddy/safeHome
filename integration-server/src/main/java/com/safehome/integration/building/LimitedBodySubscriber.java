package com.safehome.integration.building;

import java.io.IOException;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Flow;

/** Bounds retained bytes before forwarding chunks, including unknown-length HTTP bodies. */
final class LimitedBodySubscriber implements HttpResponse.BodySubscriber<byte[]> {
    private final HttpResponse.BodySubscriber<byte[]> delegate = HttpResponse.BodySubscribers.ofByteArray();
    private final int limit;
    private final long declaredLength;
    private Flow.Subscription subscription;
    private long received;
    private boolean finished;

    LimitedBodySubscriber(int limit, long declaredLength) {
        this.limit = limit;
        this.declaredLength = declaredLength;
    }
    @Override public CompletionStage<byte[]> getBody() { return delegate.getBody(); }
    @Override public void onSubscribe(Flow.Subscription subscription) {
        this.subscription = subscription;
        if (declaredLength > limit) {
            reject();
        } else delegate.onSubscribe(subscription);
    }
    @Override public void onNext(List<ByteBuffer> buffers) {
        if (finished) return;
        long size = 0;
        for (ByteBuffer buffer : buffers) size += buffer.remaining();
        if (size > limit - received) {
            reject();
            return;
        }
        received += size;
        delegate.onNext(buffers);
    }
    private void reject() {
        finished = true;
        subscription.cancel();
        delegate.onError(new ResponseTooLargeException());
    }
    @Override public void onError(Throwable error) {
        if (!finished) { finished = true; delegate.onError(error); }
    }
    @Override public void onComplete() {
        if (!finished) { finished = true; delegate.onComplete(); }
    }
    static final class ResponseTooLargeException extends IOException {
        ResponseTooLargeException() { super("RESPONSE_SIZE"); }
    }
}
