package com.platform.module.pms.domain;

import com.platform.framework.core.common.utils.PrefixedIdGenerator;
import com.platform.framework.security.tenant.TenantAwareEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;

/**
 * RFP 항목 평가 (항목 × 평가자)
 * 테이블: pms_rfp_eval (docs/pms/03-ERD.md). 생성된 골격이며, 상태 전이 등 도메인 메서드는 서비스 구현 시 추가한다.
 */
@Entity
@Table(name = "pms_rfp_eval", uniqueConstraints = {
        @UniqueConstraint(name = "uk_pms_rfp_eval", columnNames = {"tenant_id", "rfp_item_id", "evaluator_user_id"})
})
public class RfpEval extends TenantAwareEntity {

    /** RFP 항목 ID */
    @Column(name = "rfp_item_id", length = 64, nullable = false)
    private String rfpItemId;

    /** 평가자 */
    @Column(name = "evaluator_user_id", length = 64, nullable = false)
    private String evaluatorUserId;

    /** 기능/비기능 */
    @Column(name = "kind_code", length = 10)
    private String kindCode;

    /** 구현 여부: 구현/일부 구현/미구현/협의 필요 */
    @Column(name = "impl_code", length = 20)
    private String implCode;

    /** 난이도: 하/중/상 */
    @Column(name = "difficulty_code", length = 10)
    private String difficultyCode;

    /** 구현 시 예상 M/D */
    @Column(name = "md", precision = 6, scale = 2)
    private BigDecimal md;

    /** 의견 */
    @Column(name = "comment_text", length = 1000)
    private String commentText;

    protected RfpEval() {
    }

    private RfpEval(String id) {
        super(id);
    }

    public static RfpEval create() {
        return new RfpEval(PrefixedIdGenerator.generate("REV"));
    }

    public RfpEval withRfpItemId(String value) {
        this.rfpItemId = value;
        return this;
    }

    public RfpEval withEvaluatorUserId(String value) {
        this.evaluatorUserId = value;
        return this;
    }

    public RfpEval withKindCode(String value) {
        this.kindCode = value;
        return this;
    }

    public RfpEval withImplCode(String value) {
        this.implCode = value;
        return this;
    }

    public RfpEval withDifficultyCode(String value) {
        this.difficultyCode = value;
        return this;
    }

    public RfpEval withMd(BigDecimal value) {
        this.md = value;
        return this;
    }

    public RfpEval withCommentText(String value) {
        this.commentText = value;
        return this;
    }

    public String getRfpItemId() {
        return rfpItemId;
    }

    public String getEvaluatorUserId() {
        return evaluatorUserId;
    }

    public String getKindCode() {
        return kindCode;
    }

    public String getImplCode() {
        return implCode;
    }

    public String getDifficultyCode() {
        return difficultyCode;
    }

    public BigDecimal getMd() {
        return md;
    }

    public String getCommentText() {
        return commentText;
    }
}
