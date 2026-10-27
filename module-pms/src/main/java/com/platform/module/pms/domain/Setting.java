package com.platform.module.pms.domain;

import com.platform.framework.core.common.utils.PrefixedIdGenerator;
import com.platform.framework.security.tenant.TenantAwareEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * PMS 설정 (키-값, 테넌트 단위)
 * 테이블: pms_setting (docs/pms/03-ERD.md). 생성된 골격이며, 상태 전이 등 도메인 메서드는 서비스 구현 시 추가한다.
 */
@Entity
@Table(name = "pms_setting", uniqueConstraints = {
        @UniqueConstraint(name = "uk_pms_setting_key", columnNames = {"tenant_id", "setting_key"})
})
public class Setting extends TenantAwareEntity {

    /** 설정 키 (예: staffing.over_limit_pct) */
    @Column(name = "setting_key", length = 100, nullable = false)
    private String settingKey;

    /** 설정 값 */
    @Column(name = "setting_value", length = 500)
    private String settingValue;

    protected Setting() {
    }

    private Setting(String id) {
        super(id);
    }

    public static Setting create() {
        return new Setting(PrefixedIdGenerator.generate("SET"));
    }

    public Setting withSettingKey(String value) {
        this.settingKey = value;
        return this;
    }

    public Setting withSettingValue(String value) {
        this.settingValue = value;
        return this;
    }

    public String getSettingKey() {
        return settingKey;
    }

    public String getSettingValue() {
        return settingValue;
    }
}
