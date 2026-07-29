package com.platform.module.privacy.domain;

import com.platform.framework.core.common.utils.PrefixedIdGenerator;
import com.platform.framework.security.tenant.TenantAwareEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * 개인정보처리방침(방침기본). 사진 속 BackOffice "Data Management > 개인정보처리방침 관리"
 * 화면의 상위 대응. module-terms(이용약관)와 완전히 같은 구조다 — 개인정보처리방침도 시점별
 * 개정 이력을 추적해야 한다(수집 당시 어떤 버전이 적용됐는지 나중에 증빙해야 하는 경우가
 * 많다는 게 이용약관과 같은 이유). 별도 모듈로 분리한 이유는 사진 속 화면 자체가 "이용약관
 * 관리"와 "개인정보처리방침 관리"로 나뉘어 있어서다 — 관리자 입장에서 서로 다른 메뉴/화면으로
 * 다루고 싶어할 수 있기 때문에 데이터도 분리했다(약관과 방침을 같은 목록에서 섞어 보고 싶지
 * 않을 거라는 가정).
 *
 * ⚠️ 이용약관과 동일하게 포탈별로 관리된다 — portalId가 필수다. 다른 참조 컬럼들과 동일하게
 * 진짜 FK가 아니라 느슨한 문자열 참조다(module-privacy는 module-portal에 의존하지 않는다).
 * 같은 policyCode라도 포탈이 다르면 별개의 방침으로 취급한다.
 */
@Entity
@Table(name = "cmn_privacy_policy_group",
        uniqueConstraints = @UniqueConstraint(name = "uk_privacy_group_tenant_portal_code",
                columnNames = {"tenant_id", "portal_id", "policy_code"}))
public class PrivacyPolicyGroup extends TenantAwareEntity {

    @Column(name = "portal_id", length = 64, nullable = false)
    private String portalId;

    @Column(name = "policy_code", length = 50, nullable = false)
    private String policyCode;

    @Column(name = "policy_name", length = 200, nullable = false)
    private String policyName;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "use_yn", length = 1, nullable = false)
    private String useYn = "Y";

    protected PrivacyPolicyGroup() {
    }

    private PrivacyPolicyGroup(String portalId, String policyCode, String policyName, String description) {
        super(PrefixedIdGenerator.generate("PPG"));
        this.portalId = portalId;
        this.policyCode = policyCode;
        this.policyName = policyName;
        this.description = description;
    }

    public static PrivacyPolicyGroup create(String portalId, String policyCode, String policyName, String description) {
        return new PrivacyPolicyGroup(portalId, policyCode, policyName, description);
    }

    public void update(String policyName, String description) {
        this.policyName = policyName;
        this.description = description;
    }

    public void activate() {
        this.useYn = "Y";
    }

    public void deactivate() {
        this.useYn = "N";
    }

    public String getPortalId() {
        return portalId;
    }

    public String getPolicyCode() {
        return policyCode;
    }

    public String getPolicyName() {
        return policyName;
    }

    public String getDescription() {
        return description;
    }

    public String getUseYn() {
        return useYn;
    }
}
