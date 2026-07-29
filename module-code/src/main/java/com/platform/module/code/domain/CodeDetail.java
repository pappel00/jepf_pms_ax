package com.platform.module.code.domain;

import com.platform.framework.core.common.utils.PrefixedIdGenerator;
import com.platform.framework.security.tenant.TenantAwareEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * 코드상세. CodeGroup 하나에 여러 개가 속한다 (예: DEVICE_TYPE 그룹 아래 PC/MOBILE/ALL).
 * groupId 는 다른 참조 컬럼들과 동일하게 단순 문자열 참조다(진짜 JPA 연관관계 아님).
 */
@Entity
@Table(name = "cmn_code_detail",
        uniqueConstraints = @UniqueConstraint(name = "uk_code_detail_tenant_group_code",
                columnNames = {"tenant_id", "group_id", "code"}))
public class CodeDetail extends TenantAwareEntity {

    @Column(name = "group_id", length = 64, nullable = false)
    private String groupId;

    @Column(name = "code", length = 50, nullable = false)
    private String code;

    @Column(name = "code_name", length = 200, nullable = false)
    private String codeName;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "use_yn", length = 1, nullable = false)
    private String useYn = "Y";

    protected CodeDetail() {
    }

    private CodeDetail(String groupId, String code, String codeName, int sortOrder, String description) {
        super(PrefixedIdGenerator.generate("CDT"));
        this.groupId = groupId;
        this.code = code;
        this.codeName = codeName;
        this.sortOrder = sortOrder;
        this.description = description;
    }

    public static CodeDetail create(String groupId, String code, String codeName, int sortOrder, String description) {
        return new CodeDetail(groupId, code, codeName, sortOrder, description);
    }

    public void update(String codeName, int sortOrder, String description) {
        this.codeName = codeName;
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

    public String getCode() {
        return code;
    }

    public String getCodeName() {
        return codeName;
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
