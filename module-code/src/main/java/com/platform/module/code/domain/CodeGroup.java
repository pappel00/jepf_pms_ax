package com.platform.module.code.domain;

import com.platform.framework.core.common.utils.PrefixedIdGenerator;
import com.platform.framework.security.tenant.TenantAwareEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * 코드그룹(코드기본). 사진 속 BackOffice "Data Management > 코드 관리" 화면의 상위(코드기본) 대응.
 * 예: groupCode="DEVICE_TYPE", groupName="디바이스 유형" — 이 그룹 아래에 CodeDetail로
 * "PC"/"MOBILE" 같은 실제 코드 값들이 달린다.
 */
@Entity
@Table(name = "cmn_code_group",
        uniqueConstraints = @UniqueConstraint(name = "uk_code_group_tenant_code", columnNames = {"tenant_id", "group_code"}))
public class CodeGroup extends TenantAwareEntity {

    @Column(name = "group_code", length = 50, nullable = false)
    private String groupCode;

    @Column(name = "group_name", length = 200, nullable = false)
    private String groupName;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "use_yn", length = 1, nullable = false)
    private String useYn = "Y";

    protected CodeGroup() {
    }

    private CodeGroup(String groupCode, String groupName, String description) {
        super(PrefixedIdGenerator.generate("CGR"));
        this.groupCode = groupCode;
        this.groupName = groupName;
        this.description = description;
    }

    public static CodeGroup create(String groupCode, String groupName, String description) {
        return new CodeGroup(groupCode, groupName, description);
    }

    public void update(String groupName, String description) {
        this.groupName = groupName;
        this.description = description;
    }

    public void activate() {
        this.useYn = "Y";
    }

    public void deactivate() {
        this.useYn = "N";
    }

    public String getGroupCode() {
        return groupCode;
    }

    public String getGroupName() {
        return groupName;
    }

    public String getDescription() {
        return description;
    }

    public String getUseYn() {
        return useYn;
    }
}
