package com.platform.module.file.service;

import com.platform.framework.core.common.exception.BusinessException;
import com.platform.module.file.config.FileStorageProperties;
import com.platform.module.file.exception.FileModuleErrorCode;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

/**
 * 첨부파일을 실제로 디스크에 저장/조회한다. 저장 위치(FileStorageProperties.basePath)는
 * application.yml의 framework.file.storage.base-path로 설정한다.
 *
 * DB(FileItem.filePath)에는 basePath 기준 상대경로만 저장한다 — 예를 들어
 * "TNT_xxx/FGR_xxx/3f2a1b_guide.pdf" 처럼. basePath 자체를 나중에 옮기더라도(로컬 디스크 ->
 * NAS 마운트 경로 등) DB 데이터를 안 건드려도 되게 하기 위함이다.
 *
 * 파일명 충돌 방지: 원본 파일명 앞에 UUID 8자리를 붙인다 — 같은 이름의 파일을 여러 번
 * 올려도 서로 덮어쓰지 않는다.
 */
@Service
public class FileStorageService {

    private final FileStorageProperties properties;

    public FileStorageService(FileStorageProperties properties) {
        this.properties = properties;
    }

    /**
     * subDir(보통 "{tenantId}/{groupId}") 아래에 파일을 저장하고, basePath 기준 상대경로를
     * 반환한다. 디렉터리가 없으면 만든다.
     */
    public String save(MultipartFile file, String subDir) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(FileModuleErrorCode.EMPTY_FILE, "빈 파일은 업로드할 수 없습니다.");
        }

        String originalFilename = StringUtils.hasText(file.getOriginalFilename())
                ? Path.of(file.getOriginalFilename()).getFileName().toString() // 경로 조작 방지 (../ 등 제거)
                : "file";
        String storedFilename = UUID.randomUUID().toString().substring(0, 8) + "_" + originalFilename;
        String relativePath = subDir + "/" + storedFilename;

        Path targetPath = resolve(relativePath);
        try {
            Files.createDirectories(targetPath.getParent());
            try (InputStream in = file.getInputStream()) {
                Files.copy(in, targetPath, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new BusinessException(FileModuleErrorCode.FILE_STORAGE_ERROR,
                    "파일을 저장하는 중 오류가 발생했습니다: " + e.getMessage());
        }

        return relativePath;
    }

    /** relativePath(=FileItem.filePath에 저장된 값)를 실제 디스크 경로로 바꾼다. */
    public Path resolve(String relativePath) {
        return Path.of(properties.getBasePath()).resolve(relativePath).normalize();
    }

    public boolean exists(String relativePath) {
        return Files.exists(resolve(relativePath));
    }
}
