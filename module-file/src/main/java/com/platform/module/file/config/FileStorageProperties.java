package com.platform.module.file.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 첨부파일이 실제로 저장되는 위치를 설정한다 (application.yml의
 * framework.file.storage.base-path). 기본값은 로컬 개발용 상대 경로다 — 실제 운영
 * 배포에서는 반드시 절대 경로(또는 마운트된 볼륨 경로)로 재정의해야 한다.
 *
 * 이 경로는 DB에는 절대 저장되지 않는다 — FileItem.filePath에는 이 base-path 기준
 * 상대경로만 저장해서(FileStorageService 참고), 나중에 base-path를 옮기더라도(예: 로컬
 * 디스크 -> NAS 마운트 경로 변경) DB 데이터를 건드릴 필요가 없게 했다.
 */
@ConfigurationProperties(prefix = "framework.file.storage")
public class FileStorageProperties {

    private String basePath = "./storage/files";

    public String getBasePath() {
        return basePath;
    }

    public void setBasePath(String basePath) {
        this.basePath = basePath;
    }
}
