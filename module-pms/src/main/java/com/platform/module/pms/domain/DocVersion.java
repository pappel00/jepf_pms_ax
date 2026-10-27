package com.platform.module.pms.domain;

import com.platform.framework.core.common.utils.PrefixedIdGenerator;
import com.platform.framework.security.tenant.TenantAwareEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDate;

/**
 * 산출물 버전
 * 테이블: pms_doc_version (docs/pms/03-ERD.md). 생성된 골격이며, 상태 전이 등 도메인 메서드는 서비스 구현 시 추가한다.
 */
@Entity
@Table(name = "pms_doc_version", uniqueConstraints = {
        @UniqueConstraint(name = "uk_pms_doc_version", columnNames = {"tenant_id", "tailoring_id", "ver"})
})
public class DocVersion extends TenantAwareEntity {

    /** 문서 테일러링 ID */
    @Column(name = "tailoring_id", length = 64, nullable = false)
    private String tailoringId;

    /** 버전 (v1.0, v1.1 ...) */
    @Column(name = "ver", length = 20, nullable = false)
    private String ver;

    /** 등록 일자 */
    @Column(name = "version_date", nullable = false)
    private LocalDate versionDate;

    /** 등록자 */
    @Column(name = "author_user_id", length = 64, nullable = false)
    private String authorUserId;

    /** 파일 항목 (cmn_file_item.id) */
    @Column(name = "file_item_id", length = 64)
    private String fileItemId;

    /** 파일명 */
    @Column(name = "file_name", length = 300)
    private String fileName;

    /** 파일 크기 표시 */
    @Column(name = "file_size", length = 30)
    private String fileSize;

    /** 비고 */
    @Column(name = "note", length = 500)
    private String note;

    protected DocVersion() {
    }

    private DocVersion(String id) {
        super(id);
    }

    public static DocVersion create() {
        return new DocVersion(PrefixedIdGenerator.generate("DVR"));
    }

    public DocVersion withTailoringId(String value) {
        this.tailoringId = value;
        return this;
    }

    public DocVersion withVer(String value) {
        this.ver = value;
        return this;
    }

    public DocVersion withVersionDate(LocalDate value) {
        this.versionDate = value;
        return this;
    }

    public DocVersion withAuthorUserId(String value) {
        this.authorUserId = value;
        return this;
    }

    public DocVersion withFileItemId(String value) {
        this.fileItemId = value;
        return this;
    }

    public DocVersion withFileName(String value) {
        this.fileName = value;
        return this;
    }

    public DocVersion withFileSize(String value) {
        this.fileSize = value;
        return this;
    }

    public DocVersion withNote(String value) {
        this.note = value;
        return this;
    }

    public String getTailoringId() {
        return tailoringId;
    }

    public String getVer() {
        return ver;
    }

    public LocalDate getVersionDate() {
        return versionDate;
    }

    public String getAuthorUserId() {
        return authorUserId;
    }

    public String getFileItemId() {
        return fileItemId;
    }

    public String getFileName() {
        return fileName;
    }

    public String getFileSize() {
        return fileSize;
    }

    public String getNote() {
        return note;
    }
}
