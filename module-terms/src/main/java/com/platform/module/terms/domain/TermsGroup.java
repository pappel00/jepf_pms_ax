package com.platform.module.terms.domain;

import com.platform.framework.core.common.utils.PrefixedIdGenerator;
import com.platform.framework.security.tenant.TenantAwareEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * 이용약관(약관기본). 사진 속 BackOffice "Data Management > 이용약관 관리" 화면의 상위 대응.
 * 예: termsCode="SERVICE_TERMS"(서비스 이용약관), termsCode="PRIVACY_TERMS"(개인정보 수집·이용) —
 * 이 아래에 TermsVersion으로 시점별 개정 이력이 달린다.
 *
 * 약관은 코드/DB와 별개의 module-code(공통코드)와 이름이 비슷해 보이지만 서로 다른 개념이다 —
 * TermsGroup은 "이 테넌트에 어떤 약관 항목이 있는지"를, TermsVersion은 "그 약관이 시점별로
 * 어떻게 개정되어 왔는지"를 관리한다. 실제 서비스 화면에서 사용자가 동의한 시점의 버전을
 * 나중에 증빙해야 하는 경우가 많아서(예: 개인정보 관련 분쟁), 버전 이력 관리가 핵심이다.
 *
 * ⚠️ 이용약관은 포탈별로 관리된다 — 같은 서비스라도 웹/모바일 등 포탈(채널)마다 서로 다른
 * 약관 세트를 가질 수 있어야 한다는 요구사항에 따라 portalId를 필수로 갖는다. 다른 참조
 * 컬럼들과 동일하게 module-portal에 대한 진짜 JPA 연관관계가 아니라 느슨한 문자열 참조다
 * (module-terms는 module-portal에 의존하지 않는다). 같은 termsCode라도 포탈이 다르면
 * 별개의 약관으로 취급한다 — 유니크 제약이 (tenant_id, terms_code)가 아니라
 * (tenant_id, portal_id, terms_code)인 이유다.
 */
@Entity
@Table(name = "cmn_terms_group",
        uniqueConstraints = @UniqueConstraint(name = "uk_terms_group_tenant_portal_code",
                columnNames = {"tenant_id", "portal_id", "terms_code"}))
public class TermsGroup extends TenantAwareEntity {

    @Column(name = "portal_id", length = 64, nullable = false)
    private String portalId;

    @Column(name = "terms_code", length = 50, nullable = false)
    private String termsCode;

    @Column(name = "terms_name", length = 200, nullable = false)
    private String termsName;

    /** 필수/선택 — 사용자 동의 화면에서 필수 체크박스로 보여줄지 결정하는 용도. */
    @Column(name = "terms_type", length = 20, nullable = false)
    private String termsType;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "use_yn", length = 1, nullable = false)
    private String useYn = "Y";

    protected TermsGroup() {
    }

    private TermsGroup(String portalId, String termsCode, String termsName, String termsType, String description) {
        super(PrefixedIdGenerator.generate("TGR"));
        this.portalId = portalId;
        this.termsCode = termsCode;
        this.termsName = termsName;
        this.termsType = termsType;
        this.description = description;
    }

    public static TermsGroup create(String portalId, String termsCode, String termsName,
                                     String termsType, String description) {
        return new TermsGroup(portalId, termsCode, termsName, termsType, description);
    }

    public void update(String termsName, String termsType, String description) {
        this.termsName = termsName;
        this.termsType = termsType;
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

    public String getTermsCode() {
        return termsCode;
    }

    public String getTermsName() {
        return termsName;
    }

    public String getTermsType() {
        return termsType;
    }

    public String getDescription() {
        return description;
    }

    public String getUseYn() {
        return useYn;
    }
}
