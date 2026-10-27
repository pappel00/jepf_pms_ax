package com.platform.module.pms.domain;

import com.platform.framework.core.common.utils.PrefixedIdGenerator;
import com.platform.framework.security.tenant.TenantAwareEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 보고서 제출 버전
 * 테이블: pms_report_version (docs/pms/03-ERD.md). 생성된 골격이며, 상태 전이 등 도메인 메서드는 서비스 구현 시 추가한다.
 */
@Entity
@Table(name = "pms_report_version", uniqueConstraints = {
        @UniqueConstraint(name = "uk_pms_report_version", columnNames = {"tenant_id", "report_id", "version_no"})
})
public class ReportVersion extends TenantAwareEntity {

    /** 보고서 ID */
    @Column(name = "report_id", length = 64, nullable = false)
    private String reportId;

    /** 제출 버전 번호 */
    @Column(name = "version_no", nullable = false)
    private Integer versionNo = 0;

    /** 제출자 */
    @Column(name = "submitted_by", length = 64, nullable = false)
    private String submittedBy;

    /** 제출 일시 */
    @Column(name = "submitted_at", nullable = false)
    private LocalDateTime submittedAt;

    /** 제출 시점 전체 진척 */
    @Column(name = "overall_pct", precision = 5, scale = 1)
    private BigDecimal overallPct;

    /** 제출 시점 목표 */
    @Column(name = "target_pct", precision = 5, scale = 1)
    private BigDecimal targetPct;

    /** 제출본 특이사항 스냅샷 */
    @Column(name = "memo", columnDefinition = "TEXT")
    private String memo;

    protected ReportVersion() {
    }

    private ReportVersion(String id) {
        super(id);
    }

    public static ReportVersion create() {
        return new ReportVersion(PrefixedIdGenerator.generate("RPV"));
    }

    public ReportVersion withReportId(String value) {
        this.reportId = value;
        return this;
    }

    public ReportVersion withVersionNo(Integer value) {
        this.versionNo = value;
        return this;
    }

    public ReportVersion withSubmittedBy(String value) {
        this.submittedBy = value;
        return this;
    }

    public ReportVersion withSubmittedAt(LocalDateTime value) {
        this.submittedAt = value;
        return this;
    }

    public ReportVersion withOverallPct(BigDecimal value) {
        this.overallPct = value;
        return this;
    }

    public ReportVersion withTargetPct(BigDecimal value) {
        this.targetPct = value;
        return this;
    }

    public ReportVersion withMemo(String value) {
        this.memo = value;
        return this;
    }

    public String getReportId() {
        return reportId;
    }

    public Integer getVersionNo() {
        return versionNo;
    }

    public String getSubmittedBy() {
        return submittedBy;
    }

    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }

    public BigDecimal getOverallPct() {
        return overallPct;
    }

    public BigDecimal getTargetPct() {
        return targetPct;
    }

    public String getMemo() {
        return memo;
    }
}
