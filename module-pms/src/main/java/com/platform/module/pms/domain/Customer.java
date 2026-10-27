package com.platform.module.pms.domain;

import com.platform.framework.core.common.utils.PrefixedIdGenerator;
import com.platform.framework.security.tenant.TenantAwareEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * 고객사 마스터
 * 테이블: pms_customer (docs/pms/03-ERD.md). 생성된 골격이며, 상태 전이 등 도메인 메서드는 서비스 구현 시 추가한다.
 */
@Entity
@Table(name = "pms_customer", uniqueConstraints = {
        @UniqueConstraint(name = "uk_pms_customer_code", columnNames = {"tenant_id", "customer_code"})
})
public class Customer extends TenantAwareEntity {

    /** 고객사 코드 (CUS-###) */
    @Column(name = "customer_code", length = 30, nullable = false)
    private String customerCode;

    /** 고객사명 */
    @Column(name = "customer_name", length = 200, nullable = false)
    private String customerName;

    /** 업종 (공통코드 PMS_INDUSTRY) */
    @Column(name = "industry_code", length = 30)
    private String industryCode;

    /** 거래 상태: 거래 중/잠재/거래 종료 */
    @Column(name = "deal_status_code", length = 30, nullable = false)
    private String dealStatusCode;

    /** 사업자번호 */
    @Column(name = "biz_no", length = 20)
    private String bizNo;

    /** 대표 전화 */
    @Column(name = "phone", length = 50)
    private String phone;

    /** 주소 */
    @Column(name = "address", length = 300)
    private String address;

    /** 홈페이지 */
    @Column(name = "homepage", length = 300)
    private String homepage;

    /** 비고 */
    @Column(name = "memo", length = 1000)
    private String memo;

    protected Customer() {
    }

    private Customer(String id) {
        super(id);
    }

    public static Customer create() {
        return new Customer(PrefixedIdGenerator.generate("CUS"));
    }

    public Customer withCustomerCode(String value) {
        this.customerCode = value;
        return this;
    }

    public Customer withCustomerName(String value) {
        this.customerName = value;
        return this;
    }

    public Customer withIndustryCode(String value) {
        this.industryCode = value;
        return this;
    }

    public Customer withDealStatusCode(String value) {
        this.dealStatusCode = value;
        return this;
    }

    public Customer withBizNo(String value) {
        this.bizNo = value;
        return this;
    }

    public Customer withPhone(String value) {
        this.phone = value;
        return this;
    }

    public Customer withAddress(String value) {
        this.address = value;
        return this;
    }

    public Customer withHomepage(String value) {
        this.homepage = value;
        return this;
    }

    public Customer withMemo(String value) {
        this.memo = value;
        return this;
    }

    public String getCustomerCode() {
        return customerCode;
    }

    public String getCustomerName() {
        return customerName;
    }

    public String getIndustryCode() {
        return industryCode;
    }

    public String getDealStatusCode() {
        return dealStatusCode;
    }

    public String getBizNo() {
        return bizNo;
    }

    public String getPhone() {
        return phone;
    }

    public String getAddress() {
        return address;
    }

    public String getHomepage() {
        return homepage;
    }

    public String getMemo() {
        return memo;
    }
}
