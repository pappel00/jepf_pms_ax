package com.platform.module.tenant.domain;

import com.platform.framework.core.common.entity.BaseEntity;
import com.platform.framework.core.common.utils.PrefixedIdGenerator;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * 멀티테넌시의 기준이 되는 회사(테넌트) 마스터 엔티티.
 * 이 엔티티의 PK 값이 곧 TenantContext.getTenantId() 로 전파되는 tenantId 이다.
 * Company 자신은 테넌트에 "속하는" 데이터가 아니라 테넌트 그 자체를 정의하므로
 * {@link com.platform.framework.security.tenant.TenantAwareEntity} 를 상속하지 않는다.
 */
@Entity
@Table(name = "tnt_company",
        uniqueConstraints = @UniqueConstraint(name = "uk_company_code", columnNames = "company_code"))
public class Company extends BaseEntity {

    @Column(name = "company_code", length = 30, nullable = false)
    private String companyCode;

    @Column(name = "company_name", length = 200, nullable = false)
    private String companyName;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "use_yn", length = 1, nullable = false)
    private String useYn = "Y";

    protected Company() {
    }

    private Company(String companyCode, String companyName, String description) {
        super(PrefixedIdGenerator.generate("TNT"));
        this.companyCode = companyCode;
        this.companyName = companyName;
        this.description = description;
    }

    public static Company create(String companyCode, String companyName, String description) {
        return new Company(companyCode, companyName, description);
    }

    public void update(String companyName, String description) {
        this.companyName = companyName;
        this.description = description;
    }

    public void activate() {
        this.useYn = "Y";
    }

    public void deactivate() {
        this.useYn = "N";
    }

    public String getCompanyCode() {
        return companyCode;
    }

    public String getCompanyName() {
        return companyName;
    }

    public String getDescription() {
        return description;
    }

    public String getUseYn() {
        return useYn;
    }
}
