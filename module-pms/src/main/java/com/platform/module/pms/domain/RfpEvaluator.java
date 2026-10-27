package com.platform.module.pms.domain;

import com.platform.framework.core.common.utils.PrefixedIdGenerator;
import com.platform.framework.security.tenant.TenantAwareEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;

/**
 * RFP 평가자 지정 + 제출 상태
 * 테이블: pms_rfp_evaluator (docs/pms/03-ERD.md). 생성된 골격이며, 상태 전이 등 도메인 메서드는 서비스 구현 시 추가한다.
 */
@Entity
@Table(name = "pms_rfp_evaluator", uniqueConstraints = {
        @UniqueConstraint(name = "uk_pms_rfp_evaluator", columnNames = {"tenant_id", "rfp_id", "evaluator_user_id"})
})
public class RfpEvaluator extends TenantAwareEntity {

    /** RFP ID */
    @Column(name = "rfp_id", length = 64, nullable = false)
    private String rfpId;

    /** 평가자(수행 조직 PM) */
    @Column(name = "evaluator_user_id", length = 64, nullable = false)
    private String evaluatorUserId;

    /** 제출 여부 (제출 후 수정 불가, 재평가 요청으로 해제) */
    @Column(name = "submitted_yn", length = 1, nullable = false)
    private String submittedYn = "N";

    /** 제출 일시 */
    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    protected RfpEvaluator() {
    }

    private RfpEvaluator(String id) {
        super(id);
    }

    public static RfpEvaluator create() {
        return new RfpEvaluator(PrefixedIdGenerator.generate("RFE"));
    }

    public RfpEvaluator withRfpId(String value) {
        this.rfpId = value;
        return this;
    }

    public RfpEvaluator withEvaluatorUserId(String value) {
        this.evaluatorUserId = value;
        return this;
    }

    public RfpEvaluator withSubmittedYn(String value) {
        this.submittedYn = value;
        return this;
    }

    public RfpEvaluator withSubmittedAt(LocalDateTime value) {
        this.submittedAt = value;
        return this;
    }

    public String getRfpId() {
        return rfpId;
    }

    public String getEvaluatorUserId() {
        return evaluatorUserId;
    }

    public String getSubmittedYn() {
        return submittedYn;
    }

    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }
}
