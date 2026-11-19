package com.campushub.file;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;
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
            validateImagePayload(temporary, contentType, size);
            String storageKey = "task-evidence/" + taskId + "/" + UUID.randomUUID();
            Path destination = root.resolve(storageKey).normalize();
            ensureContained(destination);
            try {
                Files.move(temporary, destination, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(temporary, destination, StandardCopyOption.REPLACE_EXISTING);
            }
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
        if (length >= 12 && signature[0] == 'R' && signature[1] == 'I' && signature[2] == 'F'
                && signature[3] == 'F' && signature[8] == 'W' && signature[9] == 'E'
                && signature[10] == 'B' && signature[11] == 'P') {
            requireDeclaredType(normalized, "image/webp");
            return "image/webp";
        }
        throw new IllegalArgumentException("仅支持 JPEG、PNG 或 WebP 图片格式");
    }

    private void validateImagePayload(Path temporary, String contentType, long size) {
        try {
            if ("image/webp".equals(contentType)) {
                validateWebp(Files.readAllBytes(temporary), size);
                return;
            }
            if (ImageIO.read(temporary.toFile()) == null) {
                throw new IllegalArgumentException("图片内容无法解码");
            }
        } catch (IOException exception) {
            throw new IllegalArgumentException("图片内容无法解码", exception);
        }
    }

    private void validateWebp(byte[] bytes, long size) {
        if (bytes.length != size || bytes.length < 20
                || bytes[0] != 'R' || bytes[1] != 'I' || bytes[2] != 'F' || bytes[3] != 'F'
                || bytes[8] != 'W' || bytes[9] != 'E' || bytes[10] != 'B' || bytes[11] != 'P') {
            throw new IllegalArgumentException("图片内容无法解码");
        }
        long riffSize = littleEndianInt(bytes, 4);
        if (riffSize != bytes.length - 8) {
            throw new IllegalArgumentException("图片内容无法解码");
        }
        int offset = 12;
        boolean imageChunk = false;
        while (offset + 8 <= bytes.length) {
            int chunkSize = (int) littleEndianInt(bytes, offset + 4);
            if (chunkSize < 0 || offset + 8L + chunkSize > bytes.length) {
                throw new IllegalArgumentException("图片内容无法解码");
            }
            String chunkType = new String(bytes, offset, 4, java.nio.charset.StandardCharsets.US_ASCII);
            if (("VP8 ".equals(chunkType) || "VP8L".equals(chunkType) || "VP8X".equals(chunkType))
                    && chunkSize > 0) {
                imageChunk = true;
            }
            offset += 8 + chunkSize + (chunkSize & 1);
        }
        if (offset != bytes.length || !imageChunk) {
            throw new IllegalArgumentException("图片内容无法解码");
        }
    }

    private long littleEndianInt(byte[] bytes, int offset) {
        return (bytes[offset] & 0xffL)
                | ((bytes[offset + 1] & 0xffL) << 8)
                | ((bytes[offset + 2] & 0xffL) << 16)
                | ((bytes[offset + 3] & 0xffL) << 24);
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
