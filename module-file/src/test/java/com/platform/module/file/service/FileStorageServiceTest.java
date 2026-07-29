package com.platform.module.file.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.platform.framework.core.common.exception.BusinessException;
import com.platform.module.file.config.FileStorageProperties;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

class FileStorageServiceTest {

    private Path tempDir;
    private FileStorageService service;

    @BeforeEach
    void setUp() throws IOException {
        tempDir = Files.createTempDirectory("jepf-file-storage-test");
        FileStorageProperties properties = new FileStorageProperties();
        properties.setBasePath(tempDir.toString());
        service = new FileStorageService(properties);
    }

    @AfterEach
    void cleanUp() throws IOException {
        try (var stream = Files.walk(tempDir)) {
            stream.sorted((a, b) -> b.compareTo(a)).forEach(p -> p.toFile().delete());
        }
    }

    @Test
    void saveWritesFileUnderBasePathAndReturnsRelativePath() throws IOException {
        var file = new MockMultipartFile("file", "guide.pdf", "application/pdf", "hello world".getBytes());

        String relativePath = service.save(file, "TNT_1/FGR_1");

        assertThat(relativePath).startsWith("TNT_1/FGR_1/").endsWith("_guide.pdf");
        assertThat(service.exists(relativePath)).isTrue();
        assertThat(Files.readString(service.resolve(relativePath))).isEqualTo("hello world");
    }

    @Test
    void saveGeneratesDifferentPathsForSameFilenameToAvoidCollision() {
        var file1 = new MockMultipartFile("file", "guide.pdf", "application/pdf", "v1".getBytes());
        var file2 = new MockMultipartFile("file", "guide.pdf", "application/pdf", "v2".getBytes());

        String path1 = service.save(file1, "TNT_1/FGR_1");
        String path2 = service.save(file2, "TNT_1/FGR_1");

        assertThat(path1).isNotEqualTo(path2);
        assertThat(service.exists(path1)).isTrue();
        assertThat(service.exists(path2)).isTrue();
    }

    @Test
    void saveFailsWhenFileIsEmpty() {
        var emptyFile = new MockMultipartFile("file", "empty.txt", "text/plain", new byte[0]);

        assertThatThrownBy(() -> service.save(emptyFile, "TNT_1/FGR_1"))
                .isInstanceOf(BusinessException.class);
    }
}
