package com.platform.module.pms.domain;

import com.platform.framework.core.common.utils.PrefixedIdGenerator;
import com.platform.framework.security.tenant.TenantAwareEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDate;

/**
 * 이슈 (프로젝트 소속, 업무 연결은 pms_issue_task)
 * 테이블: pms_issue (docs/pms/03-ERD.md). 생성된 골격이며, 상태 전이 등 도메인 메서드는 서비스 구현 시 추가한다.
 */
@Entity
@Table(name = "pms_issue", uniqueConstraints = {
        @UniqueConstraint(name = "uk_pms_issue_code", columnNames = {"tenant_id", "issue_code"})
})
public class Issue extends TenantAwareEntity {

    /** 이슈 코드 (ISS-###) */
    @Column(name = "issue_code", length = 30, nullable = false)
    private String issueCode;

    /** 프로젝트 ID */
    @Column(name = "project_id", length = 64, nullable = false)
    private String projectId;

    /** 제목 */
    @Column(name = "title", length = 300, nullable = false)
    private String title;

    /** 유형 */
    @Column(name = "type_code", length = 30, nullable = false)
    private String typeCode;

    /** 우선순위: 긴급/높음/보통/낮음 */
    @Column(name = "priority_code", length = 10, nullable = false)
    private String priorityCode;

    /** 상태: 오픈/진행 중/해결/종결 */
    @Column(name = "status_code", length = 20, nullable = false)
    private String statusCode;

    /** 보고자 */
    @Column(name = "reporter_user_id", length = 64, nullable = false)
    private String reporterUserId;

    /** 담당자 */
    @Column(name = "assignee_user_id", length = 64, nullable = false)
    private String assigneeUserId;

    /** 등록일 */
    @Column(name = "registered_date")
    private LocalDate registeredDate;

    /** 조치 기한 */
    @Column(name = "due_date")
    private LocalDate dueDate;

    /** 해결일 */
    @Column(name = "resolved_date")
    private LocalDate resolvedDate;

    /** 일정 영향(일) */
    @Column(name = "impact_days", nullable = false)
    private Integer impactDays = 0;

    /** 이슈 내용 */
    @Column(name = "content", columnDefinition = "TEXT", nullable = false)
    private String content;

    /** 조치 내용 */
    @Column(name = "resolution", columnDefinition = "TEXT")
    private String resolution;

    protected Issue() {
    }

    private Issue(String id) {
        super(id);
    }

    public static Issue create() {
        return new Issue(PrefixedIdGenerator.generate("ISU"));
    }

    public Issue withIssueCode(String value) {
        this.issueCode = value;
        return this;
    }

    public Issue withProjectId(String value) {
        this.projectId = value;
        return this;
    }

    public Issue withTitle(String value) {
        this.title = value;
        return this;
    }

    public Issue withTypeCode(String value) {
        this.typeCode = value;
        return this;
    }

    public Issue withPriorityCode(String value) {
        this.priorityCode = value;
        return this;
    }

    public Issue withStatusCode(String value) {
        this.statusCode = value;
        return this;
    }

    public Issue withReporterUserId(String value) {
        this.reporterUserId = value;
        return this;
    }

    public Issue withAssigneeUserId(String value) {
        this.assigneeUserId = value;
        return this;
    }

    public Issue withRegisteredDate(LocalDate value) {
        this.registeredDate = value;
        return this;
    }

    public Issue withDueDate(LocalDate value) {
        this.dueDate = value;
        return this;
    }

    public Issue withResolvedDate(LocalDate value) {
        this.resolvedDate = value;
        return this;
    }

    public Issue withImpactDays(Integer value) {
        this.impactDays = value;
        return this;
    }

    public Issue withContent(String value) {
        this.content = value;
        return this;
    }

    public Issue withResolution(String value) {
        this.resolution = value;
        return this;
    }

    public String getIssueCode() {
        return issueCode;
    }

    public String getProjectId() {
        return projectId;
    }

    public String getTitle() {
        return title;
    }

    public String getTypeCode() {
        return typeCode;
    }

    public String getPriorityCode() {
        return priorityCode;
    }

    public String getStatusCode() {
        return statusCode;
    }

    public String getReporterUserId() {
        return reporterUserId;
    }

    public String getAssigneeUserId() {
        return assigneeUserId;
    }

    public LocalDate getRegisteredDate() {
        return registeredDate;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public LocalDate getResolvedDate() {
        return resolvedDate;
    }

    public Integer getImpactDays() {
        return impactDays;
    }

    public String getContent() {
        return content;
    }

    public String getResolution() {
        return resolution;
    }
}
