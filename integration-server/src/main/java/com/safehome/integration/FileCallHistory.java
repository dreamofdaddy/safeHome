package com.safehome.integration;

import com.safehome.integration.building.CallHistory;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.json.JsonMapper;

/** Daily UTC JSONL, forced to local storage before returning. Paths and exceptions are never logged. */
public final class FileCallHistory implements CallHistory.Sink {
    private static final Logger log = LoggerFactory.getLogger(FileCallHistory.class);
    private final Path directory;
    private final JsonMapper mapper = JsonMapper.builder().build();
    private final AtomicLong failures = new AtomicLong();

    public FileCallHistory(Path directory) {
        this.directory = directory.toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.directory);
            Path probe = Files.createTempFile(this.directory, ".write-check-", ".tmp");
            Files.delete(probe);
        } catch (IOException | RuntimeException ignored) {
            throw new IllegalStateException("Call history directory is not writable");
        }
    }
    @Override public synchronized void record(CallHistory.Event event) {
        try {
            String day = event.timestamp().atOffset(ZoneOffset.UTC).toLocalDate().toString();
            Path file = directory.resolve("building-calls-" + day + ".jsonl");
            byte[] bytes = (mapper.writeValueAsString(event) + "\n").getBytes(java.nio.charset.StandardCharsets.UTF_8);
            try (var channel = FileChannel.open(file, StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.APPEND);
                 var lock = channel.lock()) {
                var buffer = ByteBuffer.wrap(bytes);
                while (buffer.hasRemaining()) channel.write(buffer);
                channel.force(false);
            }
        } catch (IOException | RuntimeException ignored) {
            failures.incrementAndGet();
            log.warn("BUILDING_CALL_HISTORY_WRITE_FAILED");
        }
    }
    public long writeFailures() { return failures.get(); }
}
