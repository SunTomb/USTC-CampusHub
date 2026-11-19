package com.campushub.file;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

class TaskEvidenceStorageTest {

    @TempDir
    Path root;

    @Test
    void storesValidPngWithOpaqueContainedKeyAndDigest() throws Exception {
        byte[] png = new byte[] {(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a, 0x01, 0x02};
        TaskEvidenceStorage storage = new TaskEvidenceStorage(root.toString());

        StoredTaskEvidence stored = storage.store(
                new MockMultipartFile("file", "../../pickup.png", "image/png", png), 17L);

        assertThat(stored.storageKey()).startsWith("task-evidence/17/");
        assertThat(stored.storageKey()).doesNotContain("..");
        assertThat(stored.storageKey()).doesNotContain(root.toString());
        assertThat(stored.originalName()).isEqualTo("pickup.png");
        assertThat(stored.contentType()).isEqualTo("image/png");
        assertThat(stored.sizeBytes()).isEqualTo(png.length);
        assertThat(Files.readAllBytes(root.resolve(stored.storageKey()))).containsExactly(png);
        assertThat(stored.sha256()).hasSize(64);
    }

    @Test
    void rejectsContentWithUnsupportedMagicSignature() {
        TaskEvidenceStorage storage = new TaskEvidenceStorage(root.toString());

        assertThatThrownBy(() -> storage.store(
                new MockMultipartFile("file", "note.png", "image/png", "not an image".getBytes()), 17L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("图片格式");
        assertThat(storage.countStoredFiles()).isZero();
    }

    @Test
    void rejectsFilesLargerThanFiveMiBBeforePersisting() {
        TaskEvidenceStorage storage = new TaskEvidenceStorage(root.toString());
        byte[] oversized = new byte[5 * 1024 * 1024 + 1];

        assertThatThrownBy(() -> storage.store(
                new MockMultipartFile("file", "large.jpg", "image/jpeg", oversized), 17L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("5 MiB");
        assertThat(storage.countStoredFiles()).isZero();
    }
    @Test
    void rejectsPathTraversalOnReadAndDelete() {
        TaskEvidenceStorage storage = new TaskEvidenceStorage(root.toString());

        assertThatThrownBy(() -> storage.read("../outside"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("非法文件路径");
        assertThatThrownBy(() -> storage.delete("task-evidence/../outside"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("非法文件路径");
    }
}
