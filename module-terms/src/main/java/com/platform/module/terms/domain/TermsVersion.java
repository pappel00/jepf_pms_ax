package com.platform.module.terms.domain;

import com.platform.framework.core.common.utils.PrefixedIdGenerator;
import com.platform.framework.security.tenant.TenantAwareEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import java.time.LocalDate;

/**
 * 이용약관 버전(개정 이력). TermsGroup 하나에 여러 개가 속한다 — 시행일자 순으로 개정 이력을
 * 전부 남긴다. groupId 는 다른 참조 컬럼들과 동일하게 단순 문자열 참조다.
 *
 * ⚠️ 필드명을 versionNo로 지었다 — BaseEntity가 이미 낙관적 락용 @Version 필드(Long, getVersion())를
 * 갖고 있어서, 약관의 "버전"(문자열, 예: "1.0")을 그냥 version이라고 지으면 getVersion()이
 * BaseEntity의 것과 시그니처(반환 타입)가 달라 오버라이드 충돌로 컴파일 에러가 난다
 * (실제로 겪음 — "getVersion() in TermsVersion cannot override getVersion() in BaseEntity:
 * return type String is not compatible with Long").
 */
@Entity
@Table(name = "cmn_terms_version")
public class TermsVersion extends TenantAwareEntity {

    @Column(name = "group_id", length = 64, nullable = false)
    private String groupId;

    @Column(name = "version_no", length = 30, nullable = false)
    private String versionNo;

    @Lob
    @Column(name = "content", nullable = false)
    private String content;

    @Column(name = "effective_date", nullable = false)
    private LocalDate effectiveDate;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "use_yn", length = 1, nullable = false)
    private String useYn = "Y";

    protected TermsVersion() {
    }

    private TermsVersion(String groupId, String versionNo, String content, LocalDate effectiveDate, String description) {
        super(PrefixedIdGenerator.generate("TVR"));
        this.groupId = groupId;
        this.versionNo = versionNo;
        this.content = content;
        this.effectiveDate = effectiveDate;
        this.description = description;
    }

    public static TermsVersion create(String groupId, String versionNo, String content,
                                       LocalDate effectiveDate, String description) {
        return new TermsVersion(groupId, versionNo, content, effectiveDate, description);
    }

    public void update(String versionNo, String content, LocalDate effectiveDate, String description) {
        this.versionNo = versionNo;
        this.content = content;
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

    public String getContent() {
        return content;
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
