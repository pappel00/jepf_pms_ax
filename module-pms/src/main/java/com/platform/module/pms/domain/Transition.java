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
 * 단계전환 요청·승인·롤백 (수행/운영 공통)
 * 테이블: pms_transition (docs/pms/03-ERD.md). 생성된 골격이며, 상태 전이 등 도메인 메서드는 서비스 구현 시 추가한다.
 */
@Entity
@Table(name = "pms_transition", uniqueConstraints = {
        @UniqueConstraint(name = "uk_pms_transition_code", columnNames = {"tenant_id", "transition_code"})
})
public class Transition extends TenantAwareEntity {

    /** 전환 코드 (TRN-연도-###) */
    @Column(name = "transition_code", length = 30, nullable = false)
    private String transitionCode;

    /** 프로젝트 ID */
    @Column(name = "project_id", length = 64, nullable = false)
    private String projectId;

    /** 구분: EXEC(수행단계전환)/OPS(운영단계전환) */
    @Column(name = "kind_code", length = 10, nullable = false)
    private String kindCode;

    /** 상태: 승인 대기/승인/반려/취소/롤백 */
    @Column(name = "status_code", length = 20, nullable = false)
    private String statusCode;

    /** 요청자 */
    @Column(name = "requester_user_id", length = 64, nullable = false)
    private String requesterUserId;

    /** 요청 일시 */
    @Column(name = "requested_at", nullable = false)
    private LocalDateTime requestedAt;

    /** 승인/반려자 */
    @Column(name = "approver_user_id", length = 64)
    private String approverUserId;

    /** 처리 일시 */
    @Column(name = "decided_at")
    private LocalDateTime decidedAt;

    /** 처리 의견(반려 사유) */
    @Column(name = "decision_comment", length = 1000)
    private String decisionComment;

    /** 실행 파라미터(단계, 선택 RFP 항목, 배정 방식, 멤버 스냅샷 등) */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "selection_json", columnDefinition = "jsonb", nullable = false)
    private String selectionJson;

    /** 승인 화면 요약 */
    @Column(name = "summary", length = 500)
    private String summary;

    /** 대응 RFP ID */
    @Column(name = "rfp_id", length = 64)
    private String rfpId;

    /** 선택 RFP 항목 수 */
    @Column(name = "item_count")
    private Integer itemCount;

    /** 롤백 처리자 */
    @Column(name = "rolled_back_by", length = 64)
    private String rolledBackBy;

    /** 롤백 일시 */
    @Column(name = "rolled_back_at")
    private LocalDateTime rolledBackAt;

    /** 롤백 사유 */
    @Column(name = "rollback_reason", length = 1000)
    private String rollbackReason;

    protected Transition() {
    }

    private Transition(String id) {
        super(id);
    }

    public static Transition create() {
        return new Transition(PrefixedIdGenerator.generate("TRN"));
    }

    public Transition withTransitionCode(String value) {
        this.transitionCode = value;
        return this;
    }

    public Transition withProjectId(String value) {
        this.projectId = value;
        return this;
    }

    public Transition withKindCode(String value) {
        this.kindCode = value;
        return this;
    }

    public Transition withStatusCode(String value) {
        this.statusCode = value;
        return this;
    }

    public Transition withRequesterUserId(String value) {
        this.requesterUserId = value;
        return this;
    }

    public Transition withRequestedAt(LocalDateTime value) {
        this.requestedAt = value;
        return this;
    }

    public Transition withApproverUserId(String value) {
        this.approverUserId = value;
        return this;
    }

    public Transition withDecidedAt(LocalDateTime value) {
        this.decidedAt = value;
        return this;
    }

    public Transition withDecisionComment(String value) {
        this.decisionComment = value;
        return this;
    }

    public Transition withSelectionJson(String value) {
        this.selectionJson = value;
        return this;
    }

    public Transition withSummary(String value) {
        this.summary = value;
        return this;
    }

    public Transition withRfpId(String value) {
        this.rfpId = value;
        return this;
    }

    public Transition withItemCount(Integer value) {
        this.itemCount = value;
        return this;
    }

    public Transition withRolledBackBy(String value) {
        this.rolledBackBy = value;
        return this;
    }

    public Transition withRolledBackAt(LocalDateTime value) {
        this.rolledBackAt = value;
        return this;
    }

    public Transition withRollbackReason(String value) {
        this.rollbackReason = value;
        return this;
    }

    public String getTransitionCode() {
        return transitionCode;
    }

    public String getProjectId() {
        return projectId;
    }

    public String getKindCode() {
        return kindCode;
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

    public String getApproverUserId() {
        return approverUserId;
    }

    public LocalDateTime getDecidedAt() {
        return decidedAt;
    }

    public String getDecisionComment() {
        return decisionComment;
    }

    public String getSelectionJson() {
        return selectionJson;
    }

    public String getSummary() {
        return summary;
    }

    public String getRfpId() {
        return rfpId;
    }

    public Integer getItemCount() {
        return itemCount;
    }

    public String getRolledBackBy() {
        return rolledBackBy;
    }

    public LocalDateTime getRolledBackAt() {
        return rolledBackAt;
    }

    public String getRollbackReason() {
        return rollbackReason;
    }
}
