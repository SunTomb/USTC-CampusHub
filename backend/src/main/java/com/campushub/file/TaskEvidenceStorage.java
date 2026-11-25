package com.campushub.file;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;
import java.time.Instant;
import java.nio.file.LinkOption;
import java.util.function.BiConsumer;
import java.util.UUID;
import javax.imageio.ImageIO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class TaskEvidenceStorage {

    public static final long MAX_FILE_BYTES = 5L * 1024 * 1024;
    private static final int SIGNATURE_BYTES = 12;
    private final Path root;

    public TaskEvidenceStorage(@Value("${campushub.file-storage.root:./data/uploads}") String root) {
        this.root = Path.of(root).toAbsolutePath().normalize();
    }

    public StoredTaskEvidence store(MultipartFile file, Long taskId) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("图片文件不能为空");
        }
        if (file.getSize() > MAX_FILE_BYTES) {
            throw new IllegalArgumentException("单张图片不能超过 5 MiB");
        }
        Path taskDirectory = root.resolve("task-evidence").resolve(String.valueOf(taskId)).normalize();
        ensureContained(taskDirectory);
        Path temporary = null;
        String safeName = safeOriginalName(file.getOriginalFilename());
        try {
            Files.createDirectories(taskDirectory);
            temporary = Files.createTempFile(taskDirectory, ".upload-", ".tmp");
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] signature = new byte[SIGNATURE_BYTES];
            int signatureLength = 0;
            long size = 0;
            try (InputStream input = file.getInputStream(); var output = Files.newOutputStream(temporary)) {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = input.read(buffer)) != -1) {
                    size += read;
                    if (size > MAX_FILE_BYTES) {
                        throw new IllegalArgumentException("单张图片不能超过 5 MiB");
                    }
                    if (signatureLength < SIGNATURE_BYTES) {
                        int copy = Math.min(read, SIGNATURE_BYTES - signatureLength);
                        System.arraycopy(buffer, 0, signature, signatureLength, copy);
                        signatureLength += copy;
                    }
                    digest.update(buffer, 0, read);
                    output.write(buffer, 0, read);
                }
            }
            String contentType = detectContentType(signature, signatureLength, file.getContentType());
            validateImagePayload(temporary);
            String storageKey = "task-evidence/" + taskId + "/" + UUID.randomUUID();
            Path destination = root.resolve(storageKey).normalize();
            ensureContained(destination);
            Files.move(temporary, destination, StandardCopyOption.ATOMIC_MOVE);
            temporary = null;
            return new StoredTaskEvidence(storageKey, safeName, contentType, size,
                    HexFormat.of().formatHex(digest.digest()));
        } catch (IllegalArgumentException exception) {
            deleteTemporary(temporary);
            throw exception;
        } catch (IOException | NoSuchAlgorithmException exception) {
            deleteTemporary(temporary);
            throw new IllegalStateException("保存任务图片失败", exception);
        }
    }

    public byte[] read(String storageKey) {
        try {
            return Files.readAllBytes(resolveContained(storageKey));
        } catch (IOException exception) {
            throw new IllegalStateException("读取任务图片失败", exception);
        }
    }

    public void delete(String storageKey) {
        try {
            Files.deleteIfExists(resolveContained(storageKey));
        } catch (IOException exception) {
            throw new IllegalStateException("清理任务图片失败", exception);
        }
    }

    // Inspect only expired, service-owned names; never touch historical uploads or fresh files.
    public void visitStaleFiles(Instant cutoff, BiConsumer<Long,String> visitor) {
        Path evidenceRoot = ensureContained(root.resolve("task-evidence"));
        if (!Files.isDirectory(evidenceRoot, LinkOption.NOFOLLOW_LINKS)) return;
        try (var paths = Files.walk(evidenceRoot, 2)) {
            var iterator = paths.iterator();
            while (iterator.hasNext()) {
                Path path = iterator.next();
                String key = root.relativize(path).toString().replace(java.io.File.separatorChar, '/');
                if (!key.matches("task-evidence/[1-9][0-9]{0,17}/([a-f0-9]{8}(-[a-f0-9]{4}){3}-[a-f0-9]{12}|[.]upload-[0-9]+[.]tmp)")) continue;
                if (!Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) continue;
                try {
                    if (Files.getLastModifiedTime(path, LinkOption.NOFOLLOW_LINKS).toInstant().isBefore(cutoff)) {
                        visitor.accept(Long.valueOf(path.getParent().getFileName().toString()), key);
                    }
                } catch (java.nio.file.NoSuchFileException ignored) { /* Concurrent cleanup. */ }
            }
        } catch (IOException e) { throw new IllegalStateException("扫描任务图片失败", e); }
    }

    long countStoredFiles() {
        try {
            if (!Files.exists(root)) {
                return 0;
            }
            try (var paths = Files.walk(root)) {
                return paths.filter(Files::isRegularFile).count();
            }
        } catch (IOException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private Path resolveContained(String key) {
        if (key == null || key.isBlank() || key.startsWith("/") || key.contains("\\") || key.contains("..")) {
            throw new IllegalArgumentException("非法文件路径");
        }
        return ensureContained(root.resolve(key).normalize());
    }

    private Path ensureContained(Path path) {
        if (!path.startsWith(root)) {
            throw new IllegalArgumentException("非法文件路径");
        }
        for (Path current = path; current != null && current.startsWith(root); current = current.getParent()) {
            if (Files.isSymbolicLink(current)) throw new IllegalArgumentException("非法文件路径");
        }
        return path;
    }

    private String detectContentType(byte[] signature, int length, String suppliedType) {
        String normalized = suppliedType == null ? "" : suppliedType.toLowerCase(Locale.ROOT);
        if (length >= 8 && (signature[0] & 0xff) == 0x89 && signature[1] == 0x50
                && signature[2] == 0x4e && signature[3] == 0x47 && signature[4] == 0x0d
                && signature[5] == 0x0a && signature[6] == 0x1a && signature[7] == 0x0a) {
            requireDeclaredType(normalized, "image/png");
            return "image/png";
        }
        if (length >= 3 && (signature[0] & 0xff) == 0xff && (signature[1] & 0xff) == 0xd8
                && (signature[2] & 0xff) == 0xff) {
            requireDeclaredType(normalized, "image/jpeg");
            return "image/jpeg";
        }
        throw new IllegalArgumentException("仅支持 JPEG 或 PNG 图片格式");
    }

    private void validateImagePayload(Path temporary) {
        try (var input = ImageIO.createImageInputStream(temporary.toFile())) {
            var readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) throw new IllegalArgumentException("图片内容无法解码");
            var reader = readers.next();
            try {
                reader.setInput(input, true, true);
                if ((long) reader.getWidth(0) * reader.getHeight(0) > 16_000_000) throw new IllegalArgumentException("图片像素过多");
                reader.read(0);
            } finally { reader.dispose(); }
        } catch (IOException exception) {
            throw new IllegalArgumentException("图片内容无法解码", exception);
        }
    }

    private void requireDeclaredType(String suppliedType, String expected) {
        if (!suppliedType.isBlank() && !expected.equals(suppliedType)) {
            throw new IllegalArgumentException("图片内容类型与文件签名不匹配");
        }
    }

    private String safeOriginalName(String originalName) {
        String name = originalName == null ? "evidence" : originalName;
        name = name.replace('\\', '/');
        int slash = name.lastIndexOf('/');
        if (slash >= 0) {
            name = name.substring(slash + 1);
        }
        name = name.replaceAll("[^\\p{L}\\p{N}._ -]", "_").trim();
        if (name.isBlank() || ".".equals(name) || "..".equals(name)) {
            return "evidence";
        }
        return name.length() > 255 ? name.substring(0, 255) : name;
    }

    private void deleteTemporary(Path temporary) {
        if (temporary == null) {
            return;
        }
        try {
            Files.deleteIfExists(temporary);
        } catch (IOException ignored) {
            // Best-effort cleanup; the original failure remains authoritative.
        }
    }
}
