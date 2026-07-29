package com.platform.module.tenant.domain;

import com.platform.framework.core.common.utils.PrefixedIdGenerator;
import com.platform.framework.security.tenant.TenantAwareEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.springframework.util.StringUtils;

/**
 * 테넌트(Company) 하위의 부서 엔티티.
 * TenantAwareEntity 를 상속하므로, 조회/저장 시 Hibernate @TenantId 에 의해
 * 자동으로 현재 테넌트(TenantContext)로 필터링/스탬핑된다 — 별도 WHERE 조건 불필요.
 *
 * parentDepartmentId 는 저장 시점에 빈 문자열("")을 null 로 정규화한다 (module-portal의
 * Menu.parentMenuId 에서 실제로 겪은 버그와 동일한 문제를 예방하기 위함 — 트리/계층 구조를
 * 다루는 참조 컬럼은 "값 없음"이 항상 null 로 통일되어 있어야 null 체크 로직이 안전하다).
 */
@Entity
@Table(name = "tnt_department")
public class Department extends TenantAwareEntity {

    @Column(name = "department_code", length = 30, nullable = false)
    private String departmentCode;

    @Column(name = "department_name", length = 200, nullable = false)
    private String departmentName;

    @Column(name = "parent_department_id", length = 64)
    private String parentDepartmentId;

    @Column(name = "use_yn", length = 1, nullable = false)
    private String useYn = "Y";

    protected Department() {
    }

    private Department(String departmentCode, String departmentName, String parentDepartmentId) {
        super(PrefixedIdGenerator.generate("DEPT"));
        this.departmentCode = departmentCode;
        this.departmentName = departmentName;
        this.parentDepartmentId = normalize(parentDepartmentId);
    }

    public static Department create(String departmentCode, String departmentName, String parentDepartmentId) {
        return new Department(departmentCode, departmentName, parentDepartmentId);
    }

    public void update(String departmentName, String parentDepartmentId) {
        this.departmentName = departmentName;
        this.parentDepartmentId = normalize(parentDepartmentId);
    }

    private static String normalize(String value) {
        return StringUtils.hasText(value) ? value : null;
    }

    public void activate() {
        this.useYn = "Y";
    }

    public void deactivate() {
        this.useYn = "N";
    }

    public String getDepartmentCode() {
        return departmentCode;
    }

    public String getDepartmentName() {
        return departmentName;
    }

    public String getParentDepartmentId() {
        return parentDepartmentId;
    }

    public String getUseYn() {
        return useYn;
    }
}
