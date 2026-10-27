package com.platform.module.pms.domain;

import com.platform.framework.core.common.utils.PrefixedIdGenerator;
import com.platform.framework.security.tenant.TenantAwareEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * 멤버 투입 단계
 * 테이블: pms_member_phase (docs/pms/03-ERD.md). 생성된 골격이며, 상태 전이 등 도메인 메서드는 서비스 구현 시 추가한다.
 */
@Entity
@Table(name = "pms_member_phase", uniqueConstraints = {
        @UniqueConstraint(name = "uk_pms_member_phase", columnNames = {"tenant_id", "member_id", "phase_code"})
})
public class MemberPhase extends TenantAwareEntity {

    /** 프로젝트 멤버 ID */
    @Column(name = "member_id", length = 64, nullable = false)
    private String memberId;

    /** 단계 (분석/설계/개발/테스트/운영) */
    @Column(name = "phase_code", length = 20, nullable = false)
    private String phaseCode;

    protected MemberPhase() {
    }

    private MemberPhase(String id) {
        super(id);
    }

    public static MemberPhase create() {
        return new MemberPhase(PrefixedIdGenerator.generate("MPH"));
    }

    public MemberPhase withMemberId(String value) {
        this.memberId = value;
        return this;
    }

    public MemberPhase withPhaseCode(String value) {
        this.phaseCode = value;
        return this;
    }

    public String getMemberId() {
        return memberId;
    }

    public String getPhaseCode() {
        return phaseCode;
    }
}
