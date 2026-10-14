package com.safehome.backend.domain.document;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PrivatePdfStorageTest {
    @TempDir Path root;

    @Test
    void storesByKoreanCalendarDayAndReadsLegacyFlatKeys() throws Exception {
        PrivatePdfStorage storage = new PrivatePdfStorage(root.toString());
        byte[] pdf = "%PDF-test".getBytes();
        String key = storage.store(pdf);
        assertThat(key).startsWith(LocalDate.now(ZoneId.of("Asia/Seoul")) + "/");
        assertThat(Files.readAllBytes(root.resolve(key))).isEqualTo(pdf);
        assertThat(storage.read(key)).isEqualTo(pdf);

        String legacyKey = UUID.randomUUID() + ".pdf";
        Files.write(root.resolve(legacyKey), pdf);
        assertThat(storage.read(legacyKey)).isEqualTo(pdf);
        storage.delete(key);
        assertThat(root.resolve(key)).doesNotExist();
    }
}
