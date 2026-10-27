package com.platform.module.pms.domain;

import com.platform.framework.core.common.utils.PrefixedIdGenerator;
import com.platform.framework.security.tenant.TenantAwareEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * 운영 전환 인수·이관 항목
 * 테이블: pms_handover_item (docs/pms/03-ERD.md). 생성된 골격이며, 상태 전이 등 도메인 메서드는 서비스 구현 시 추가한다.
 */
@Entity
@Table(name = "pms_handover_item")
public class HandoverItem extends TenantAwareEntity {

    /** 프로젝트 ID */
    @Column(name = "project_id", length = 64, nullable = false)
    private String projectId;

    /** 운영 전환 ID */
    @Column(name = "transition_id", length = 64)
    private String transitionId;

    /** 유형: FEATURE/DOC/ISSUE/NEXT_OPP */
    @Column(name = "item_type_code", length = 20, nullable = false)
    private String itemTypeCode;

    /** 기능·산출물·이슈·차기 기회 ID */
    @Column(name = "ref_id", length = 64)
    private String refId;

    /** 항목명 */
    @Column(name = "item_name", length = 300, nullable = false)
    private String itemName;

    protected HandoverItem() {
    }

    private HandoverItem(String id) {
        super(id);
    }

    public static HandoverItem create() {
        return new HandoverItem(PrefixedIdGenerator.generate("HOI"));
    }

    public HandoverItem withProjectId(String value) {
        this.projectId = value;
        return this;
    }

    public HandoverItem withTransitionId(String value) {
        this.transitionId = value;
        return this;
    }

    public HandoverItem withItemTypeCode(String value) {
        this.itemTypeCode = value;
        return this;
    }

    public HandoverItem withRefId(String value) {
        this.refId = value;
        return this;
    }

    public HandoverItem withItemName(String value) {
        this.itemName = value;
        return this;
    }

    public String getProjectId() {
        return projectId;
    }

    public String getTransitionId() {
        return transitionId;
    }

    public String getItemTypeCode() {
        return itemTypeCode;
    }

    public String getRefId() {
        return refId;
    }

    public String getItemName() {
        return itemName;
    }
}
