package com.platform.module.pms.domain;

import com.platform.framework.core.common.utils.PrefixedIdGenerator;
import com.platform.framework.security.tenant.TenantAwareEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 차기 사업 기회 (수행 단계)
 * 테이블: pms_next_opportunity (docs/pms/03-ERD.md). 생성된 골격이며, 상태 전이 등 도메인 메서드는 서비스 구현 시 추가한다.
 */
@Entity
@Table(name = "pms_next_opportunity", uniqueConstraints = {
        @UniqueConstraint(name = "uk_pms_next_opp_code", columnNames = {"tenant_id", "opp_code"})
})
public class NextOpportunity extends TenantAwareEntity {

    /** 기회 코드 (NB-###) */
    @Column(name = "opp_code", length = 30, nullable = false)
    private String oppCode;

    /** 프로젝트 ID */
    @Column(name = "project_id", length = 64, nullable = false)
    private String projectId;

    /** 건명 */
    @Column(name = "title", length = 300, nullable = false)
    private String title;

    /** 기회 유형 */
    @Column(name = "kind_code", length = 50, nullable = false)
    private String kindCode;

    /** 발생 경로: 고객 요청/현장 발견/수행 이슈/영업 제안/기타 */
    @Column(name = "source_code", length = 30)
    private String sourceCode;

    /** 요구 내용·배경 */
    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    /** 요청자(고객사 담당자) ID */
    @Column(name = "requester_contact_id", length = 64)
    private String requesterContactId;

    /** 의사결정자(고객사 담당자) ID (미확인이면 null) */
    @Column(name = "decision_maker_contact_id", length = 64)
    private String decisionMakerContactId;

    /** 고객 예산: 확보/검토 중/미정 */
    @Column(name = "budget_code", length = 20)
    private String budgetCode;

    /** 예상 규모(M/M) */
    @Column(name = "est_mm", precision = 6, scale = 2)
    private BigDecimal estMm;

    /** 예상 금액 */
    @Column(name = "amount")
    private Long amount;

    /** 예상 시기 (YYYY-Qn) */
    @Column(name = "quarter", length = 10)
    private String quarter;

    /** 단계: 발굴/검토 중/제안 준비/제안 완료/보류·실패/이관 완료 */
    @Column(name = "stage_code", length = 20, nullable = false)
    private String stageCode;

    /** 우선순위 */
    @Column(name = "priority_code", length = 10)
    private String priorityCode;

    /** 영업 담당자 */
    @Column(name = "owner_user_id", length = 64)
    private String ownerUserId;

    /** 연결 수행 이슈 ID */
    @Column(name = "issue_id", length = 64)
    private String issueId;

    /** 다음 할 일 */
    @Column(name = "next_action", length = 500)
    private String nextAction;

    /** 다음 할 일 예정일 */
    @Column(name = "next_date")
    private LocalDate nextDate;

    /** 현재 범위 외 사유 */
    @Column(name = "scope_reason", length = 500)
    private String scopeReason;

    /** 비고 */
    @Column(name = "memo", length = 1000)
    private String memo;

    /** 이관된 운영 영업 기회 ID */
    @Column(name = "ops_opportunity_id", length = 64)
    private String opsOpportunityId;

    protected NextOpportunity() {
    }

    private NextOpportunity(String id) {
        super(id);
    }

    public static NextOpportunity create() {
        return new NextOpportunity(PrefixedIdGenerator.generate("NOP"));
    }

    public NextOpportunity withOppCode(String value) {
        this.oppCode = value;
        return this;
    }

    public NextOpportunity withProjectId(String value) {
        this.projectId = value;
        return this;
    }

    public NextOpportunity withTitle(String value) {
        this.title = value;
        return this;
    }

    public NextOpportunity withKindCode(String value) {
        this.kindCode = value;
        return this;
    }

    public NextOpportunity withSourceCode(String value) {
        this.sourceCode = value;
        return this;
    }

    public NextOpportunity withContent(String value) {
        this.content = value;
        return this;
    }

    public NextOpportunity withRequesterContactId(String value) {
        this.requesterContactId = value;
        return this;
    }

    public NextOpportunity withDecisionMakerContactId(String value) {
        this.decisionMakerContactId = value;
        return this;
    }

    public NextOpportunity withBudgetCode(String value) {
        this.budgetCode = value;
        return this;
    }

    public NextOpportunity withEstMm(BigDecimal value) {
        this.estMm = value;
        return this;
    }

    public NextOpportunity withAmount(Long value) {
        this.amount = value;
        return this;
    }

    public NextOpportunity withQuarter(String value) {
        this.quarter = value;
        return this;
    }

    public NextOpportunity withStageCode(String value) {
        this.stageCode = value;
        return this;
    }

    public NextOpportunity withPriorityCode(String value) {
        this.priorityCode = value;
        return this;
    }

    public NextOpportunity withOwnerUserId(String value) {
        this.ownerUserId = value;
        return this;
    }

    public NextOpportunity withIssueId(String value) {
        this.issueId = value;
        return this;
    }

    public NextOpportunity withNextAction(String value) {
        this.nextAction = value;
        return this;
    }

    public NextOpportunity withNextDate(LocalDate value) {
        this.nextDate = value;
        return this;
    }

    public NextOpportunity withScopeReason(String value) {
        this.scopeReason = value;
        return this;
    }

    public NextOpportunity withMemo(String value) {
        this.memo = value;
        return this;
    }

    public NextOpportunity withOpsOpportunityId(String value) {
        this.opsOpportunityId = value;
        return this;
    }

    public String getOppCode() {
        return oppCode;
    }

    public String getProjectId() {
        return projectId;
    }

    public String getTitle() {
        return title;
    }

    public String getKindCode() {
        return kindCode;
    }

    public String getSourceCode() {
        return sourceCode;
    }

    public String getContent() {
        return content;
    }

    public String getRequesterContactId() {
        return requesterContactId;
    }

    public String getDecisionMakerContactId() {
        return decisionMakerContactId;
    }

    public String getBudgetCode() {
        return budgetCode;
    }

    public BigDecimal getEstMm() {
        return estMm;
    }

    public Long getAmount() {
        return amount;
    }

    public String getQuarter() {
        return quarter;
    }

    public String getStageCode() {
        return stageCode;
    }

    public String getPriorityCode() {
        return priorityCode;
    }

    public String getOwnerUserId() {
        return ownerUserId;
    }

    public String getIssueId() {
        return issueId;
    }

    public String getNextAction() {
        return nextAction;
    }

    public LocalDate getNextDate() {
        return nextDate;
    }

    public String getScopeReason() {
        return scopeReason;
    }

    public String getMemo() {
        return memo;
    }

    public String getOpsOpportunityId() {
        return opsOpportunityId;
    }
}
