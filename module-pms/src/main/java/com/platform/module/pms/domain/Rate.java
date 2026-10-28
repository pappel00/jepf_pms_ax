package com.platform.module.pms.domain;

import com.platform.framework.core.common.utils.PrefixedIdGenerator;
import com.platform.framework.security.tenant.TenantAwareEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalDate;

/**
 * 직무 × 등급별 M/M 단가
 * 테이블: pms_rate (docs/pms/03-ERD.md). 생성된 골격이며, 상태 전이 등 도메인 메서드는 서비스 구현 시 추가한다.
 */
@Entity
@Table(name = "pms_rate")
public class Rate extends TenantAwareEntity {

    /** 직무 */
    @Column(name = "job_code", length = 30, nullable = false)
    private String jobCode;

    /** 등급: 특급/고급/중급/초급 */
    @Column(name = "grade_code", length = 20, nullable = false)
    private String gradeCode;

    /** M/M 단가(원) */
    @Column(name = "unit_price", nullable = false)
    private Long unitPrice;

    /** 적용 시작일 */
    @Column(name = "from_date", nullable = false)
    private LocalDate fromDate;

    /** 적용 종료일 (비우면 계속) */
    @Column(name = "to_date")
    private LocalDate toDate;

    protected Rate() {
    }

    private Rate(String id) {
        super(id);
    }

    public static Rate create() {
        return new Rate(PrefixedIdGenerator.generate("RTE"));
    }

    public Rate withJobCode(String value) {
        this.jobCode = value;
        return this;
    }

    public Rate withGradeCode(String value) {
        this.gradeCode = value;
        return this;
    }

    public Rate withUnitPrice(Long value) {
        this.unitPrice = value;
        return this;
    }

    public Rate withFromDate(LocalDate value) {
        this.fromDate = value;
        return this;
    }

    public Rate withToDate(LocalDate value) {
        this.toDate = value;
        return this;
    }

    public String getJobCode() {
        return jobCode;
    }

    public String getGradeCode() {
        return gradeCode;
    }

    public Long getUnitPrice() {
        return unitPrice;
    }

    public LocalDate getFromDate() {
        return fromDate;
    }

    public LocalDate getToDate() {
        return toDate;
    }
}
