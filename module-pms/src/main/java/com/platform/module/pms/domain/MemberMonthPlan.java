package com.platform.module.pms.domain;

import com.platform.framework.core.common.utils.PrefixedIdGenerator;
import com.platform.framework.security.tenant.TenantAwareEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;

/**
 * 멤버 월별 투입률 조정 (직접 수정한 달만 저장)
 * 테이블: pms_member_month_plan (docs/pms/03-ERD.md). 생성된 골격이며, 상태 전이 등 도메인 메서드는 서비스 구현 시 추가한다.
 */
@Entity
@Table(name = "pms_member_month_plan", uniqueConstraints = {
        @UniqueConstraint(name = "uk_pms_member_month_plan", columnNames = {"tenant_id", "member_id", "plan_month"})
})
public class MemberMonthPlan extends TenantAwareEntity {

    /** 프로젝트 멤버 ID */
    @Column(name = "member_id", length = 64, nullable = false)
    private String memberId;

    /** 월 (YYYY-MM) */
    @Column(name = "plan_month", length = 7, nullable = false)
    private String planMonth;

    /** 그 달 투입률(%) */
    @Column(name = "rate_pct", precision = 5, scale = 1, nullable = false)
    private BigDecimal ratePct;

    protected MemberMonthPlan() {
    }

    private MemberMonthPlan(String id) {
        super(id);
    }

    public static MemberMonthPlan create() {
        return new MemberMonthPlan(PrefixedIdGenerator.generate("MMP"));
    }

    public MemberMonthPlan withMemberId(String value) {
        this.memberId = value;
        return this;
    }

    public MemberMonthPlan withPlanMonth(String value) {
        this.planMonth = value;
        return this;
    }

    public MemberMonthPlan withRatePct(BigDecimal value) {
        this.ratePct = value;
        return this;
    }

    public String getMemberId() {
        return memberId;
    }

    public String getPlanMonth() {
        return planMonth;
    }

    public BigDecimal getRatePct() {
        return ratePct;
    }
}
