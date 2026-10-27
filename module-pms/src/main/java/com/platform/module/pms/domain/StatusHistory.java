package com.platform.module.pms.domain;

import com.platform.framework.core.common.utils.PrefixedIdGenerator;
import com.platform.framework.security.tenant.TenantAwareEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/**
 * 공통 상태 변경 이력 (처리 현황 모달의 지난 처리)
 * 테이블: pms_status_history (docs/pms/03-ERD.md). 생성된 골격이며, 상태 전이 등 도메인 메서드는 서비스 구현 시 추가한다.
 */
@Entity
@Table(name = "pms_status_history")
public class StatusHistory extends TenantAwareEntity {

    /** 대상 유형 (PROJECT/RFP_EVAL/PROPOSAL/OPS_INCIDENT/OPS_OPPORTUNITY/NEXT_OPPORTUNITY/OPS_TASK 등) */
    @Column(name = "target_type_code", length = 30, nullable = false)
    private String targetTypeCode;

    /** 대상 ID */
    @Column(name = "target_id", length = 64, nullable = false)
    private String targetId;

    /** 이전 상태 */
    @Column(name = "from_status", length = 30)
    private String fromStatus;

    /** 변경 상태 */
    @Column(name = "to_status", length = 30, nullable = false)
    private String toStatus;

    /** 처리자 */
    @Column(name = "actor_user_id", length = 64, nullable = false)
    private String actorUserId;

    /** 처리 일시 */
    @Column(name = "acted_at", nullable = false)
    private LocalDateTime actedAt;

    /** 사유·메모 */
    @Column(name = "memo", length = 1000)
    private String memo;

    protected StatusHistory() {
    }

    private StatusHistory(String id) {
        super(id);
    }

    public static StatusHistory create() {
        return new StatusHistory(PrefixedIdGenerator.generate("SHS"));
    }

    public StatusHistory withTargetTypeCode(String value) {
        this.targetTypeCode = value;
        return this;
    }

    public StatusHistory withTargetId(String value) {
        this.targetId = value;
        return this;
    }

    public StatusHistory withFromStatus(String value) {
        this.fromStatus = value;
        return this;
    }

    public StatusHistory withToStatus(String value) {
        this.toStatus = value;
        return this;
    }

    public StatusHistory withActorUserId(String value) {
        this.actorUserId = value;
        return this;
    }

    public StatusHistory withActedAt(LocalDateTime value) {
        this.actedAt = value;
        return this;
    }

    public StatusHistory withMemo(String value) {
        this.memo = value;
        return this;
    }

    public String getTargetTypeCode() {
        return targetTypeCode;
    }

    public String getTargetId() {
        return targetId;
    }

    public String getFromStatus() {
        return fromStatus;
    }

    public String getToStatus() {
        return toStatus;
    }

    public String getActorUserId() {
        return actorUserId;
    }

    public LocalDateTime getActedAt() {
        return actedAt;
    }

    public String getMemo() {
        return memo;
    }
}
