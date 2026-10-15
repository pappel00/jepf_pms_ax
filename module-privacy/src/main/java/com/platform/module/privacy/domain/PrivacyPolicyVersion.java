package com.platform.module.privacy.domain;

import com.platform.framework.core.common.utils.PrefixedIdGenerator;
import com.platform.framework.security.tenant.TenantAwareEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalDate;

/**
 * 개인정보처리방침 버전(개정 이력). PrivacyPolicyGroup 하나에 여러 개가 속한다 — 시행일자
 * 순으로 개정 이력을 전부 남긴다. groupId 는 다른 참조 컬럼들과 동일하게 단순 문자열 참조다.
 *
 * ⚠️ 필드명이 version이 아니라 versionNo인 이유는 TermsVersion과 동일하다 — BaseEntity가 이미
 * 낙관적 락용 @Version 필드(Long, getVersion())를 갖고 있어서, "버전"(문자열, 예: "1.0")을
 * 그냥 version이라고 지으면 getVersion()이 BaseEntity의 것과 반환 타입이 달라 오버라이드
 * 충돌로 컴파일 자체가 안 된다(module-terms에서 실제로 겪었던 문제, TermsVersion 참고).
 */
@Entity
@Table(name = "cmn_privacy_policy_version")
public class PrivacyPolicyVersion extends TenantAwareEntity {

    @Column(name = "group_id", length = 64, nullable = false)
    private String groupId;

    @Column(name = "version_no", length = 30, nullable = false)
    private String versionNo;

    // @Lob을 쓰지 않는다: PostgreSQL에서 @Lob String은 oid(Large Object)로 매핑돼 TEXT 컬럼과 어긋난다.
    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "effective_date", nullable = false)
    private LocalDate effectiveDate;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "use_yn", length = 1, nullable = false)
    private String useYn = "Y";

    protected PrivacyPolicyVersion() {
    }

    private PrivacyPolicyVersion(String groupId, String versionNo, String content,
                                  LocalDate effectiveDate, String description) {
        super(PrefixedIdGenerator.generate("PPV"));
        this.groupId = groupId;
        this.versionNo = versionNo;
        this.content = content;
        this.effectiveDate = effectiveDate;
        this.description = description;
    }

    public static PrivacyPolicyVersion create(String groupId, String versionNo, String content,
                                               LocalDate effectiveDate, String description) {
        return new PrivacyPolicyVersion(groupId, versionNo, content, effectiveDate, description);
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
