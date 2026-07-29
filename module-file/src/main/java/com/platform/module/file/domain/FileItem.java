package com.platform.module.file.domain;

import com.platform.framework.core.common.utils.PrefixedIdGenerator;
import com.platform.framework.security.tenant.TenantAwareEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * 첨부항목. FileGroup 하나에 여러 개가 속한다 (예: NOTICE_ATTACH 그룹 아래 공지사항 첨부파일들).
 * groupId 는 다른 참조 컬럼들과 동일하게 단순 문자열 참조다(진짜 JPA 연관관계 아님).
 *
 * 실제 파일 바이트는 다루지 않고 메타데이터만 저장한다(FileGroup 클래스 주석 참고) —
 * filePath 는 실제 스토리지 연동 전까지는 관리자가 직접 입력하는 경로/URL 문자열이다.
 */
@Entity
@Table(name = "cmn_file_item")
public class FileItem extends TenantAwareEntity {

    @Column(name = "group_id", length = 64, nullable = false)
    private String groupId;

    @Column(name = "file_name", length = 300, nullable = false)
    private String fileName;

    @Column(name = "file_path", length = 1000, nullable = false)
    private String filePath;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "use_yn", length = 1, nullable = false)
    private String useYn = "Y";

    protected FileItem() {
    }

    private FileItem(String groupId, String fileName, String filePath, Long fileSize,
                      int sortOrder, String description) {
        super(PrefixedIdGenerator.generate("FIT"));
        this.groupId = groupId;
        this.fileName = fileName;
        this.filePath = filePath;
        this.fileSize = fileSize;
        this.sortOrder = sortOrder;
        this.description = description;
    }

    public static FileItem create(String groupId, String fileName, String filePath, Long fileSize,
                                   int sortOrder, String description) {
        return new FileItem(groupId, fileName, filePath, fileSize, sortOrder, description);
    }

    public void update(String fileName, String filePath, Long fileSize, int sortOrder, String description) {
        this.fileName = fileName;
        this.filePath = filePath;
        this.fileSize = fileSize;
        this.sortOrder = sortOrder;
        this.description = description;
    }

    public void activate() {
        this.useYn = "Y";
    }

    public void deactivate() {
        this.useYn = "N";
    }

    public String getGroupId() {
        return groupId;
    }

    public String getFileName() {
        return fileName;
    }

    public String getFilePath() {
        return filePath;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public String getDescription() {
        return description;
    }

    public String getUseYn() {
        return useYn;
    }
}
