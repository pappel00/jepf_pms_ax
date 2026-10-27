package com.platform.module.pms.domain;

import com.platform.framework.core.common.utils.PrefixedIdGenerator;
import com.platform.framework.security.tenant.TenantAwareEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * 제안서 RFP 항목 대응표
 * 테이블: pms_proposal_mapping (docs/pms/03-ERD.md). 생성된 골격이며, 상태 전이 등 도메인 메서드는 서비스 구현 시 추가한다.
 */
@Entity
@Table(name = "pms_proposal_mapping", uniqueConstraints = {
        @UniqueConstraint(name = "uk_pms_proposal_mapping", columnNames = {"tenant_id", "proposal_id", "rfp_item_id"})
})
public class ProposalMapping extends TenantAwareEntity {

    /** 제안서 ID */
    @Column(name = "proposal_id", length = 64, nullable = false)
    private String proposalId;

    /** RFP 항목 ID */
    @Column(name = "rfp_item_id", length = 64, nullable = false)
    private String rfpItemId;

    /** 제안서 목차(절) */
    @Column(name = "section", length = 100)
    private String section;

    /** 대응 방식: 수용/일부 수용/대안 제시/미수용 */
    @Column(name = "response_code", length = 20)
    private String responseCode;

    /** 대응 요약 */
    @Column(name = "summary", length = 1000)
    private String summary;

    /** 담당자 */
    @Column(name = "owner_user_id", length = 64)
    private String ownerUserId;

    protected ProposalMapping() {
    }

    private ProposalMapping(String id) {
        super(id);
    }

    public static ProposalMapping create() {
        return new ProposalMapping(PrefixedIdGenerator.generate("PRM"));
    }

    public ProposalMapping withProposalId(String value) {
        this.proposalId = value;
        return this;
    }

    public ProposalMapping withRfpItemId(String value) {
        this.rfpItemId = value;
        return this;
    }

    public ProposalMapping withSection(String value) {
        this.section = value;
        return this;
    }

    public ProposalMapping withResponseCode(String value) {
        this.responseCode = value;
        return this;
    }

    public ProposalMapping withSummary(String value) {
        this.summary = value;
        return this;
    }

    public ProposalMapping withOwnerUserId(String value) {
        this.ownerUserId = value;
        return this;
    }

    public String getProposalId() {
        return proposalId;
    }

    public String getRfpItemId() {
        return rfpItemId;
    }

    public String getSection() {
        return section;
    }

    public String getResponseCode() {
        return responseCode;
    }

    public String getSummary() {
        return summary;
    }

    public String getOwnerUserId() {
        return ownerUserId;
    }
}
