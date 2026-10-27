package com.platform.module.pms.domain;

import com.platform.framework.core.common.utils.PrefixedIdGenerator;
import com.platform.framework.security.tenant.TenantAwareEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * 투입 계획 변경 요청·승인·이력
 * 테이블: pms_team_change (docs/pms/03-ERD.md). 생성된 골격이며, 상태 전이 등 도메인 메서드는 서비스 구현 시 추가한다.
 */
@Entity
@Table(name = "pms_team_change", uniqueConstraints = {
        @UniqueConstraint(name = "uk_pms_team_change_code", columnNames = {"tenant_id", "change_code"})
})
public class TeamChange extends TenantAwareEntity {

    /** 변경 요청 코드 (CHG-연도-###) */
    @Column(name = "change_code", length = 30, nullable = false)
    private String changeCode;

    /** 프로젝트 ID */
    @Column(name = "project_id", length = 64, nullable = false)
    private String projectId;

    /** 상태: 승인 대기/승인/반려/취소 */
    @Column(name = "status_code", length = 20, nullable = false)
    private String statusCode;

    /** 요청자 */
    @Column(name = "requester_user_id", length = 64, nullable = false)
    private String requesterUserId;

    /** 요청 일시 */
    @Column(name = "requested_at", nullable = false)
    private LocalDateTime requestedAt;

    /** 변경 사유 */
    @Column(name = "reason", length = 1000, nullable = false)
    private String reason;

    /** 변경 전 스냅샷(관리자·투입 인력·열람 제한) */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "before_json", columnDefinition = "jsonb", nullable = false)
    private String beforeJson;

    /** 변경 후 스냅샷 */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "after_json", columnDefinition = "jsonb", nullable = false)
    private String afterJson;

    /** 변경 요약 문장 */
    @Column(name = "summary_text", columnDefinition = "TEXT")
    private String summaryText;

    /** 승인 시 과투입 예상 */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "conflict_json", columnDefinition = "jsonb")
    private String conflictJson;

    /** 승인/반려자 */
    @Column(name = "decided_by", length = 64)
    private String decidedBy;

    /** 처리 일시 */
    @Column(name = "decided_at")
    private LocalDateTime decidedAt;

    /** 처리 의견(반려 사유) */
    @Column(name = "decision_comment", length = 1000)
    private String decisionComment;

    protected TeamChange() {
    }

    private TeamChange(String id) {
        super(id);
    }

    public static TeamChange create() {
        return new TeamChange(PrefixedIdGenerator.generate("TCH"));
    }

    public TeamChange withChangeCode(String value) {
        this.changeCode = value;
        return this;
    }

    public TeamChange withProjectId(String value) {
        this.projectId = value;
        return this;
    }

    public TeamChange withStatusCode(String value) {
        this.statusCode = value;
        return this;
    }

    public TeamChange withRequesterUserId(String value) {
        this.requesterUserId = value;
        return this;
    }

    public TeamChange withRequestedAt(LocalDateTime value) {
        this.requestedAt = value;
        return this;
    }

    public TeamChange withReason(String value) {
        this.reason = value;
        return this;
    }

    public TeamChange withBeforeJson(String value) {
        this.beforeJson = value;
        return this;
    }

    public TeamChange withAfterJson(String value) {
        this.afterJson = value;
        return this;
    }

    public TeamChange withSummaryText(String value) {
        this.summaryText = value;
        return this;
    }

    public TeamChange withConflictJson(String value) {
        this.conflictJson = value;
        return this;
    }

    public TeamChange withDecidedBy(String value) {
        this.decidedBy = value;
        return this;
    }

    public TeamChange withDecidedAt(LocalDateTime value) {
        this.decidedAt = value;
        return this;
    }

    public TeamChange withDecisionComment(String value) {
        this.decisionComment = value;
        return this;
    }

    public String getChangeCode() {
        return changeCode;
    }

    public String getProjectId() {
        return projectId;
    }

    public String getStatusCode() {
        return statusCode;
    }

    public String getRequesterUserId() {
        return requesterUserId;
    }

    public LocalDateTime getRequestedAt() {
        return requestedAt;
    }

    public String getReason() {
        return reason;
    }

    public String getBeforeJson() {
        return beforeJson;
    }

    public String getAfterJson() {
        return afterJson;
    }

    public String getSummaryText() {
        return summaryText;
    }

    public String getConflictJson() {
        return conflictJson;
    }

    public String getDecidedBy() {
        return decidedBy;
    }

    public LocalDateTime getDecidedAt() {
        return decidedAt;
    }

    public String getDecisionComment() {
        return decisionComment;
    }
}
