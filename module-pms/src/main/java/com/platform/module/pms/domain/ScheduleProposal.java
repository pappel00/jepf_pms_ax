package com.platform.module.pms.domain;

import com.platform.framework.core.common.utils.PrefixedIdGenerator;
import com.platform.framework.security.tenant.TenantAwareEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;

/**
 * 이슈 해결 시 일정 조정 제안
 * 테이블: pms_schedule_proposal (docs/pms/03-ERD.md). 생성된 골격이며, 상태 전이 등 도메인 메서드는 서비스 구현 시 추가한다.
 */
@Entity
@Table(name = "pms_schedule_proposal", uniqueConstraints = {
        @UniqueConstraint(name = "uk_pms_schedule_proposal_code", columnNames = {"tenant_id", "proposal_code"})
})
public class ScheduleProposal extends TenantAwareEntity {

    /** 제안 코드 (SCP-연도-###) */
    @Column(name = "proposal_code", length = 30, nullable = false)
    private String proposalCode;

    /** 프로젝트 ID */
    @Column(name = "project_id", length = 64, nullable = false)
    private String projectId;

    /** 제안을 만든 이슈 ID */
    @Column(name = "issue_id", length = 64, nullable = false)
    private String issueId;

    /** 상태: 제안/승인/반려/무시 */
    @Column(name = "status_code", length = 20, nullable = false)
    private String statusCode;

    /** 조정 영업일 수 */
    @Column(name = "shift_days", nullable = false)
    private Integer shiftDays = 0;

    /** 제안 근거 */
    @Column(name = "basis_text", length = 500)
    private String basisText;

    /** 처리자 */
    @Column(name = "decided_by", length = 64)
    private String decidedBy;

    /** 처리 일시 */
    @Column(name = "decided_at")
    private LocalDateTime decidedAt;

    /** 처리 의견(반려 사유) */
    @Column(name = "decision_comment", length = 1000)
    private String decisionComment;

    protected ScheduleProposal() {
    }

    private ScheduleProposal(String id) {
        super(id);
    }

    public static ScheduleProposal create() {
        return new ScheduleProposal(PrefixedIdGenerator.generate("SCP"));
    }

    public ScheduleProposal withProposalCode(String value) {
        this.proposalCode = value;
        return this;
    }

    public ScheduleProposal withProjectId(String value) {
        this.projectId = value;
        return this;
    }

    public ScheduleProposal withIssueId(String value) {
        this.issueId = value;
        return this;
    }

    public ScheduleProposal withStatusCode(String value) {
        this.statusCode = value;
        return this;
    }

    public ScheduleProposal withShiftDays(Integer value) {
        this.shiftDays = value;
        return this;
    }

    public ScheduleProposal withBasisText(String value) {
        this.basisText = value;
        return this;
    }

    public ScheduleProposal withDecidedBy(String value) {
        this.decidedBy = value;
        return this;
    }

    public ScheduleProposal withDecidedAt(LocalDateTime value) {
        this.decidedAt = value;
        return this;
    }

    public ScheduleProposal withDecisionComment(String value) {
        this.decisionComment = value;
        return this;
    }

    public String getProposalCode() {
        return proposalCode;
    }

    public String getProjectId() {
        return projectId;
    }

    public String getIssueId() {
        return issueId;
    }

    public String getStatusCode() {
        return statusCode;
    }

    public Integer getShiftDays() {
        return shiftDays;
    }

    public String getBasisText() {
        return basisText;
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
