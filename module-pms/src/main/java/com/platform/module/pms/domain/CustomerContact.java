package com.platform.module.pms.domain;

import com.platform.framework.core.common.utils.PrefixedIdGenerator;
import com.platform.framework.security.tenant.TenantAwareEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * 고객사 담당자 마스터 (로그인 계정과 연결하지 않음)
 * 테이블: pms_customer_contact (docs/pms/03-ERD.md). 생성된 골격이며, 상태 전이 등 도메인 메서드는 서비스 구현 시 추가한다.
 */
@Entity
@Table(name = "pms_customer_contact", uniqueConstraints = {
        @UniqueConstraint(name = "uk_pms_contact_code", columnNames = {"tenant_id", "contact_code"})
})
public class CustomerContact extends TenantAwareEntity {

    /** 담당자 코드 (CTC-###) */
    @Column(name = "contact_code", length = 30, nullable = false)
    private String contactCode;

    /** 고객사 ID */
    @Column(name = "customer_id", length = 64, nullable = false)
    private String customerId;

    /** 담당자명 */
    @Column(name = "contact_name", length = 100, nullable = false)
    private String contactName;

    /** 직책 */
    @Column(name = "job_title", length = 100)
    private String jobTitle;

    /** 부서 */
    @Column(name = "dept_name", length = 100)
    private String deptName;

    /** 연락처 */
    @Column(name = "phone", length = 50)
    private String phone;

    /** 이메일 */
    @Column(name = "email", length = 200)
    private String email;

    /** 비고 */
    @Column(name = "memo", length = 1000)
    private String memo;

    /** 사용 여부 (퇴사·이동 시 N) */
    @Column(name = "use_yn", length = 1, nullable = false)
    private String useYn = "Y";

    protected CustomerContact() {
    }

    private CustomerContact(String id) {
        super(id);
    }

    public static CustomerContact create() {
        return new CustomerContact(PrefixedIdGenerator.generate("CTC"));
    }

    public CustomerContact withContactCode(String value) {
        this.contactCode = value;
        return this;
    }

    public CustomerContact withCustomerId(String value) {
        this.customerId = value;
        return this;
    }

    public CustomerContact withContactName(String value) {
        this.contactName = value;
        return this;
    }

    public CustomerContact withJobTitle(String value) {
        this.jobTitle = value;
        return this;
    }

    public CustomerContact withDeptName(String value) {
        this.deptName = value;
        return this;
    }

    public CustomerContact withPhone(String value) {
        this.phone = value;
        return this;
    }

    public CustomerContact withEmail(String value) {
        this.email = value;
        return this;
    }

    public CustomerContact withMemo(String value) {
        this.memo = value;
        return this;
    }

    public CustomerContact withUseYn(String value) {
        this.useYn = value;
        return this;
    }

    public String getContactCode() {
        return contactCode;
    }

    public String getCustomerId() {
        return customerId;
    }

    public String getContactName() {
        return contactName;
    }

    public String getJobTitle() {
        return jobTitle;
    }

    public String getDeptName() {
        return deptName;
    }

    public String getPhone() {
        return phone;
    }

    public String getEmail() {
        return email;
    }

    public String getMemo() {
        return memo;
    }

    public String getUseYn() {
        return useYn;
    }
}
