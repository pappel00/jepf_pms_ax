package com.platform.module.pms.domain;

import com.platform.framework.core.common.utils.PrefixedIdGenerator;
import com.platform.framework.security.tenant.TenantAwareEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * 제안서 제출 체크리스트
 * 테이블: pms_proposal_checklist (docs/pms/03-ERD.md). 생성된 골격이며, 상태 전이 등 도메인 메서드는 서비스 구현 시 추가한다.
 */
@Entity
@Table(name = "pms_proposal_checklist")
public class ProposalChecklist extends TenantAwareEntity {

    /** 제안서 ID */
    @Column(name = "proposal_id", length = 64, nullable = false)
    private String proposalId;

    /** 제출 서류명 */
    @Column(name = "item_name", length = 200, nullable = false)
    private String itemName;

    /** 준비 완료 여부 */
    @Column(name = "done_yn", length = 1, nullable = false)
    private String doneYn = "N";

    /** 정렬 */
    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;

    protected ProposalChecklist() {
    }

    private ProposalChecklist(String id) {
        super(id);
    }

    public static ProposalChecklist create() {
        return new ProposalChecklist(PrefixedIdGenerator.generate("PRC"));
    }

    public ProposalChecklist withProposalId(String value) {
        this.proposalId = value;
        return this;
    }

    public ProposalChecklist withItemName(String value) {
        this.itemName = value;
        return this;
    }

    public ProposalChecklist withDoneYn(String value) {
        this.doneYn = value;
        return this;
    }

    public ProposalChecklist withSortOrder(Integer value) {
        this.sortOrder = value;
        return this;
    }

    public String getProposalId() {
        return proposalId;
    }

    public String getItemName() {
        return itemName;
    }

    public String getDoneYn() {
        return doneYn;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }
}
