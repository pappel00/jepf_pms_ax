package com.platform.module.form.domain;

import com.platform.framework.core.common.utils.PrefixedIdGenerator;
import com.platform.framework.security.tenant.TenantAwareEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * 표준양식(양식기본). 사진 속 BackOffice "Data Management > 표준양식 관리" 화면의 상위 대응.
 * module-terms(이용약관)/module-privacy(개인정보처리방침)와 완전히 같은 구조다 — 표준양식도
 * 시점별 개정 이력을 추적해야 한다는 요구사항에 따라 2단(양식기본+버전) 구조로 만들었다.
 *
 * category(분류)는 약관/개인정보처리방침에는 없던 필드다 — 표준양식은 보통 인사/총무/재무
 * 같은 부서·업무 분류가 있는 경우가 많아서 추가했다.
 *
 * ⚠️ 이용약관/개인정보처리방침과 동일하게 포탈별로 관리된다 — portalId가 필수다. 다른
 * 참조 컬럼들과 동일하게 진짜 FK가 아니라 느슨한 문자열 참조다(module-form은 module-portal에
 * 의존하지 않는다). 같은 formCode라도 포탈이 다르면 별개의 양식으로 취급한다.
 */
@Entity
@Table(name = "cmn_form_group",
        uniqueConstraints = @UniqueConstraint(name = "uk_form_group_tenant_portal_code",
                columnNames = {"tenant_id", "portal_id", "form_code"}))
public class FormGroup extends TenantAwareEntity {

    @Column(name = "portal_id", length = 64, nullable = false)
    private String portalId;

    @Column(name = "form_code", length = 50, nullable = false)
    private String formCode;

    @Column(name = "form_name", length = 200, nullable = false)
    private String formName;

    @Column(name = "category", length = 50)
    private String category;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "use_yn", length = 1, nullable = false)
    private String useYn = "Y";

    protected FormGroup() {
    }

    private FormGroup(String portalId, String formCode, String formName, String category, String description) {
        super(PrefixedIdGenerator.generate("FMG"));
        this.portalId = portalId;
        this.formCode = formCode;
        this.formName = formName;
        this.category = category;
        this.description = description;
    }

    public static FormGroup create(String portalId, String formCode, String formName,
                                    String category, String description) {
        return new FormGroup(portalId, formCode, formName, category, description);
    }

    public void update(String formName, String category, String description) {
        this.formName = formName;
        this.category = category;
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

    public String getFormCode() {
        return formCode;
    }

    public String getFormName() {
        return formName;
    }

    public String getCategory() {
        return category;
    }

    public String getDescription() {
        return description;
    }

    public String getUseYn() {
        return useYn;
    }
}
