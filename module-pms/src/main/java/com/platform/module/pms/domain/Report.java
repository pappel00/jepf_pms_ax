package com.platform.module.pms.domain;

import com.platform.framework.core.common.utils.PrefixedIdGenerator;
import com.platform.framework.security.tenant.TenantAwareEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDate;

/**
 * 주간/월간 보고서 (프로젝트 × 유형 × 기간 1건)
 * 테이블: pms_report (docs/pms/03-ERD.md). 생성된 골격이며, 상태 전이 등 도메인 메서드는 서비스 구현 시 추가한다.
 */
@Entity
@Table(name = "pms_report", uniqueConstraints = {
        @UniqueConstraint(name = "uk_pms_report_period", columnNames = {"tenant_id", "project_id", "report_type_code", "period_start"})
})
public class Report extends TenantAwareEntity {

    /** 프로젝트 ID */
    @Column(name = "project_id", length = 64, nullable = false)
    private String projectId;

    /** 유형: WEEK/MONTH */
    @Column(name = "report_type_code", length = 10, nullable = false)
    private String reportTypeCode;

    /** 기간 시작일 */
    @Column(name = "period_start", nullable = false)
    private LocalDate periodStart;

    /** 기간 종료일 */
    @Column(name = "period_end", nullable = false)
    private LocalDate periodEnd;

    /** 상태: 작성 중/제출 완료/수정 중 */
    @Column(name = "status_code", length = 20, nullable = false)
    private String statusCode;

    /** 특이사항·요청사항 */
    @Column(name = "memo", columnDefinition = "TEXT")
    private String memo;

    /** 제출 횟수(재제출마다 +1) */
    @Column(name = "submit_version", nullable = false)
    private Integer submitVersion = 0;

    /** 작성 담당(프로젝트 관리자) */
    @Column(name = "author_user_id", length = 64, nullable = false)
    private String authorUserId;

    protected Report() {
    }

    private Report(String id) {
        super(id);
    }

    public static Report create() {
        return new Report(PrefixedIdGenerator.generate("RPT"));
    }

    public Report withProjectId(String value) {
        this.projectId = value;
        return this;
    }

    public Report withReportTypeCode(String value) {
        this.reportTypeCode = value;
        return this;
    }

    public Report withPeriodStart(LocalDate value) {
        this.periodStart = value;
        return this;
    }

    public Report withPeriodEnd(LocalDate value) {
        this.periodEnd = value;
        return this;
    }

    public Report withStatusCode(String value) {
        this.statusCode = value;
        return this;
    }

    public Report withMemo(String value) {
        this.memo = value;
        return this;
    }

    public Report withSubmitVersion(Integer value) {
        this.submitVersion = value;
        return this;
    }

    public Report withAuthorUserId(String value) {
        this.authorUserId = value;
        return this;
    }

    public String getProjectId() {
        return projectId;
    }

    public String getReportTypeCode() {
        return reportTypeCode;
    }

    public LocalDate getPeriodStart() {
        return periodStart;
    }

    public LocalDate getPeriodEnd() {
        return periodEnd;
    }

    public String getStatusCode() {
        return statusCode;
    }

    public String getMemo() {
        return memo;
    }

    public Integer getSubmitVersion() {
        return submitVersion;
    }

    public String getAuthorUserId() {
        return authorUserId;
    }
}
