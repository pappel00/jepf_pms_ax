package com.platform.module.pms.domain;

import com.platform.framework.core.common.utils.PrefixedIdGenerator;
import com.platform.framework.security.tenant.TenantAwareEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDate;

/**
 * 제안서
 * 테이블: pms_proposal (docs/pms/03-ERD.md). 생성된 골격이며, 상태 전이 등 도메인 메서드는 서비스 구현 시 추가한다.
 */
@Entity
@Table(name = "pms_proposal", uniqueConstraints = {
        @UniqueConstraint(name = "uk_pms_proposal_code", columnNames = {"tenant_id", "proposal_code"})
})
public class Proposal extends TenantAwareEntity {

    /** 제안서 코드 (PROP-연도-###) */
    @Column(name = "proposal_code", length = 30, nullable = false)
    private String proposalCode;

    /** 프로젝트 ID */
    @Column(name = "project_id", length = 64, nullable = false)
    private String projectId;

    /** 대응 RFP ID */
    @Column(name = "rfp_id", length = 64, nullable = false)
    private String rfpId;

    /** 제안서명 */
    @Column(name = "title", length = 300, nullable = false)
    private String title;

    /** 상태: 작성 중/내부 검토/제출 완료/결과 확정 */
    @Column(name = "status_code", length = 30, nullable = false)
    private String statusCode;

    /** 작성 책임자 */
    @Column(name = "owner_user_id", length = 64, nullable = false)
    private String ownerUserId;

    /** 제출 마감일 */
    @Column(name = "due_date")
    private LocalDate dueDate;

    /** 제출일 */
    @Column(name = "submitted_date")
    private LocalDate submittedDate;

    /** 제안 금액 */
    @Column(name = "price")
    private Long price;

    /** 예상 원가 */
    @Column(name = "cost_estimate")
    private Long costEstimate;

    protected Proposal() {
    }

    private Proposal(String id) {
        super(id);
    }

    public static Proposal create() {
        return new Proposal(PrefixedIdGenerator.generate("PRP"));
    }

    public Proposal withProposalCode(String value) {
        this.proposalCode = value;
        return this;
    }

    public Proposal withProjectId(String value) {
        this.projectId = value;
        return this;
    }

    public Proposal withRfpId(String value) {
        this.rfpId = value;
        return this;
    }

    public Proposal withTitle(String value) {
        this.title = value;
        return this;
    }

    public Proposal withStatusCode(String value) {
        this.statusCode = value;
        return this;
    }

    public Proposal withOwnerUserId(String value) {
        this.ownerUserId = value;
        return this;
    }

    public Proposal withDueDate(LocalDate value) {
        this.dueDate = value;
        return this;
    }

    public Proposal withSubmittedDate(LocalDate value) {
        this.submittedDate = value;
        return this;
    }

    public Proposal withPrice(Long value) {
        this.price = value;
        return this;
    }

    public Proposal withCostEstimate(Long value) {
        this.costEstimate = value;
        return this;
    }

    public String getProposalCode() {
        return proposalCode;
    }

    public String getProjectId() {
        return projectId;
    }

    public String getRfpId() {
        return rfpId;
    }

    public String getTitle() {
        return title;
    }

    public String getStatusCode() {
        return statusCode;
    }

    public String getOwnerUserId() {
        return ownerUserId;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public LocalDate getSubmittedDate() {
        return submittedDate;
    }

    public Long getPrice() {
        return price;
    }

    public Long getCostEstimate() {
        return costEstimate;
    }
}
