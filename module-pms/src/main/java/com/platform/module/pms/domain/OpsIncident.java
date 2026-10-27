package com.platform.module.pms.domain;

import com.platform.framework.core.common.utils.PrefixedIdGenerator;
import com.platform.framework.security.tenant.TenantAwareEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;

/**
 * 운영 이슈 (장애·문의·개선 요청)
 * 테이블: pms_ops_incident (docs/pms/03-ERD.md). 생성된 골격이며, 상태 전이 등 도메인 메서드는 서비스 구현 시 추가한다.
 */
@Entity
@Table(name = "pms_ops_incident", uniqueConstraints = {
        @UniqueConstraint(name = "uk_pms_ops_incident_code", columnNames = {"tenant_id", "incident_code"})
})
public class OpsIncident extends TenantAwareEntity {

    /** 운영 이슈 코드 (OI-###) */
    @Column(name = "incident_code", length = 30, nullable = false)
    private String incidentCode;

    /** 프로젝트 ID */
    @Column(name = "project_id", length = 64, nullable = false)
    private String projectId;

    /** 유형: 장애/문의/개선 요청/기타 */
    @Column(name = "type_code", length = 20, nullable = false)
    private String typeCode;

    /** 제목 */
    @Column(name = "title", length = 300, nullable = false)
    private String title;

    /** 내용 */
    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    /** 우선순위 */
    @Column(name = "priority_code", length = 10, nullable = false)
    private String priorityCode;

    /** 상태: 접수/분석/조치 중/해결/종결 */
    @Column(name = "status_code", length = 20, nullable = false)
    private String statusCode;

    /** 접수자(고객 측 포함) */
    @Column(name = "reporter_name", length = 100)
    private String reporterName;

    /** 담당자 */
    @Column(name = "assignee_user_id", length = 64)
    private String assigneeUserId;

    /** 접수 일시 */
    @Column(name = "received_at", nullable = false)
    private LocalDateTime receivedAt;

    /** 응답 일시 */
    @Column(name = "responded_at")
    private LocalDateTime respondedAt;

    /** 해결 일시 */
    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    /** 조치 내용 */
    @Column(name = "action_text", columnDefinition = "TEXT")
    private String actionText;

    /** 이 이슈에서 만든 운영 영업 기회 ID */
    @Column(name = "opportunity_id", length = 64)
    private String opportunityId;

    protected OpsIncident() {
    }

    private OpsIncident(String id) {
        super(id);
    }

    public static OpsIncident create() {
        return new OpsIncident(PrefixedIdGenerator.generate("OIN"));
    }

    public OpsIncident withIncidentCode(String value) {
        this.incidentCode = value;
        return this;
    }

    public OpsIncident withProjectId(String value) {
        this.projectId = value;
        return this;
    }

    public OpsIncident withTypeCode(String value) {
        this.typeCode = value;
        return this;
    }

    public OpsIncident withTitle(String value) {
        this.title = value;
        return this;
    }

    public OpsIncident withContent(String value) {
        this.content = value;
        return this;
    }

    public OpsIncident withPriorityCode(String value) {
        this.priorityCode = value;
        return this;
    }

    public OpsIncident withStatusCode(String value) {
        this.statusCode = value;
        return this;
    }

    public OpsIncident withReporterName(String value) {
        this.reporterName = value;
        return this;
    }

    public OpsIncident withAssigneeUserId(String value) {
        this.assigneeUserId = value;
        return this;
    }

    public OpsIncident withReceivedAt(LocalDateTime value) {
        this.receivedAt = value;
        return this;
    }

    public OpsIncident withRespondedAt(LocalDateTime value) {
        this.respondedAt = value;
        return this;
    }

    public OpsIncident withResolvedAt(LocalDateTime value) {
        this.resolvedAt = value;
        return this;
    }

    public OpsIncident withActionText(String value) {
        this.actionText = value;
        return this;
    }

    public OpsIncident withOpportunityId(String value) {
        this.opportunityId = value;
        return this;
    }

    public String getIncidentCode() {
        return incidentCode;
    }

    public String getProjectId() {
        return projectId;
    }

    public String getTypeCode() {
        return typeCode;
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public String getPriorityCode() {
        return priorityCode;
    }

    public String getStatusCode() {
        return statusCode;
    }

    public String getReporterName() {
        return reporterName;
    }

    public String getAssigneeUserId() {
        return assigneeUserId;
    }

    public LocalDateTime getReceivedAt() {
        return receivedAt;
    }

    public LocalDateTime getRespondedAt() {
        return respondedAt;
    }

    public LocalDateTime getResolvedAt() {
        return resolvedAt;
    }

    public String getActionText() {
        return actionText;
    }

    public String getOpportunityId() {
        return opportunityId;
    }
}
