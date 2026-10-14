package com.safehome.backend.domain.document;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class PrivatePdfStorage {
    private static final Pattern KEY = Pattern.compile("(?:[0-9]{4}-[0-9]{2}-[0-9]{2}/)?[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.pdf");
    private final Path root;

    public PrivatePdfStorage(@Value("${safehome.document.storage-root:./var/registry-documents}") String root) {
        this.root = Path.of(root).toAbsolutePath().normalize();
    }

    public String store(byte[] pdf) {
        try {
            String key = LocalDate.now(ZoneId.of("Asia/Seoul")) + "/" + UUID.randomUUID() + ".pdf";
            Path target = path(key);
            Files.createDirectories(target.getParent());
            Files.write(target, pdf, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
            return key;
        } catch (IOException ex) {
            throw new IllegalStateException("등기부 PDF 저장에 실패했습니다.", ex);
        }
    }

    public byte[] read(String key) {
        try {
            return Files.readAllBytes(path(key));
        } catch (IOException ex) {
            throw new IllegalStateException("등기부 PDF를 읽을 수 없습니다.", ex);
        }
    }

    public void delete(String key) {
        try { Files.deleteIfExists(path(key)); }
        catch (IOException ex) { throw new IllegalStateException("등기부 PDF 정리에 실패했습니다.", ex); }
    }

    private Path path(String key) {
        if (key == null || !KEY.matcher(key).matches())
            throw new IllegalArgumentException("유효하지 않은 PDF 저장 키입니다.");
        Path path = root.resolve(key).normalize();
        if (!path.startsWith(root)) throw new IllegalArgumentException("유효하지 않은 PDF 저장 키입니다.");
        return path;
    }
}
