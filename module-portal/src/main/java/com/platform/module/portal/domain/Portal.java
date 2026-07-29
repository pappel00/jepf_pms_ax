package com.platform.module.portal.domain;

import com.platform.framework.core.common.utils.PrefixedIdGenerator;
import com.platform.framework.security.tenant.TenantAwareEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * 포탈(사이트) 마스터. 사진 속 BackOffice "포탈 관리" 화면 대응.
 * 하나의 회사(테넌트)가 여러 포탈(Admin Portal, User Portal 등)을 가질 수 있다.
 */
@Entity
@Table(name = "prt_portal",
        uniqueConstraints = @UniqueConstraint(name = "uk_portal_tenant_code", columnNames = {"tenant_id", "portal_code"}))
public class Portal extends TenantAwareEntity {

    @Column(name = "portal_code", length = 30, nullable = false)
    private String portalCode;

    @Column(name = "portal_name", length = 200, nullable = false)
    private String portalName;

    /** PC / MOBILE / ALL */
    @Column(name = "device_type", length = 10, nullable = false)
    private String deviceType;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "use_yn", length = 1, nullable = false)
    private String useYn = "Y";

    protected Portal() {
    }

    private Portal(String portalCode, String portalName, String deviceType, String description) {
        super(PrefixedIdGenerator.generate("PRT"));
        this.portalCode = portalCode;
        this.portalName = portalName;
        this.deviceType = deviceType;
        this.description = description;
    }

    public static Portal create(String portalCode, String portalName, String deviceType, String description) {
        return new Portal(portalCode, portalName, deviceType, description);
    }

    public void update(String portalName, String deviceType, String description) {
        this.portalName = portalName;
        this.deviceType = deviceType;
        this.description = description;
    }

    public void activate() {
        this.useYn = "Y";
    }

    public void deactivate() {
        this.useYn = "N";
    }

    public String getPortalCode() {
        return portalCode;
    }

    public String getPortalName() {
        return portalName;
    }

    public String getDeviceType() {
        return deviceType;
    }

    public String getDescription() {
        return description;
    }

    public String getUseYn() {
        return useYn;
    }
}
