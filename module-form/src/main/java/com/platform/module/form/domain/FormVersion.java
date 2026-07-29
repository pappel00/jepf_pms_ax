package com.platform.module.form.domain;

import com.platform.framework.core.common.utils.PrefixedIdGenerator;
import com.platform.framework.security.tenant.TenantAwareEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalDate;

/**
 * 표준양식 버전(개정 이력). FormGroup 하나에 여러 개가 속한다 — 시행일자 순으로 개정 이력을
 * 전부 남긴다. groupId 는 다른 참조 컬럼들과 동일하게 단순 문자열 참조다.
 *
 * ⚠️ 텍스트 내용(content)이 아니라 **실제 엑셀/워드 파일**로 관리한다 — 관리자가 표준양식
 * 파일(.xlsx/.docx 등)을 그대로 업로드해서 버전별로 보관하는 구조다. 실제 파일 저장은
 * module-file의 FileStorageService를 그대로 재사용한다(같은 저장 방식을 두 번 만들 이유가
 * 없다) — filePath에는 FileStorageProperties.basePath 기준 상대경로만 저장된다.
 *
 * ⚠️ 필드명이 version이 아니라 versionNo인 이유는 TermsVersion/PrivacyPolicyVersion과
 * 동일하다 — BaseEntity가 이미 낙관적 락용 @Version 필드(Long, getVersion())를 갖고 있어서,
 * "버전"(문자열, 예: "1.0")을 그냥 version이라고 지으면 getVersion()이 BaseEntity의 것과
 * 반환 타입이 달라 오버라이드 충돌로 컴파일 자체가 안 된다(module-terms에서 실제로 겪었던 문제).
 */
@Entity
@Table(name = "cmn_form_version")
public class FormVersion extends TenantAwareEntity {

    @Column(name = "group_id", length = 64, nullable = false)
    private String groupId;

    @Column(name = "version_no", length = 30, nullable = false)
    private String versionNo;

    @Column(name = "file_name", length = 300, nullable = false)
    private String fileName;

    @Column(name = "file_path", length = 1000, nullable = false)
    private String filePath;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "effective_date", nullable = false)
    private LocalDate effectiveDate;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "use_yn", length = 1, nullable = false)
    private String useYn = "Y";

    protected FormVersion() {
    }

    private FormVersion(String groupId, String versionNo, String fileName, String filePath, Long fileSize,
                         LocalDate effectiveDate, String description) {
        super(PrefixedIdGenerator.generate("FMV"));
        this.groupId = groupId;
        this.versionNo = versionNo;
        this.fileName = fileName;
        this.filePath = filePath;
        this.fileSize = fileSize;
        this.effectiveDate = effectiveDate;
        this.description = description;
    }

    public static FormVersion create(String groupId, String versionNo, String fileName, String filePath,
                                      Long fileSize, LocalDate effectiveDate, String description) {
        return new FormVersion(groupId, versionNo, fileName, filePath, fileSize, effectiveDate, description);
    }

    public void update(String versionNo, LocalDate effectiveDate, String description) {
        this.versionNo = versionNo;
        this.effectiveDate = effectiveDate;
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

    public String getVersionNo() {
        return versionNo;
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

    public LocalDate getEffectiveDate() {
        return effectiveDate;
    }

    public String getDescription() {
        return description;
    }

    public String getUseYn() {
        return useYn;
    }
}
