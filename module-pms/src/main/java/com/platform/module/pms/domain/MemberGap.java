package com.platform.module.pms.domain;

import com.platform.framework.core.common.utils.PrefixedIdGenerator;
import com.platform.framework.security.tenant.TenantAwareEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalDate;

/**
 * 멤버 투입 공백 구간 (중간에 투입이 비는 기간)
 * 테이블: pms_member_gap (docs/pms/03-ERD.md). 생성된 골격이며, 상태 전이 등 도메인 메서드는 서비스 구현 시 추가한다.
 */
@Entity
@Table(name = "pms_member_gap")
public class MemberGap extends TenantAwareEntity {

    /** 프로젝트 멤버 ID */
    @Column(name = "member_id", length = 64, nullable = false)
    private String memberId;

    /** 공백 시작일 */
    @Column(name = "from_date", nullable = false)
    private LocalDate fromDate;

    /** 공백 종료일 */
    @Column(name = "to_date", nullable = false)
    private LocalDate toDate;

    protected MemberGap() {
    }

    private MemberGap(String id) {
        super(id);
    }

    public static MemberGap create() {
        return new MemberGap(PrefixedIdGenerator.generate("MGP"));
    }

    public MemberGap withMemberId(String value) {
        this.memberId = value;
        return this;
    }

    public MemberGap withFromDate(LocalDate value) {
        this.fromDate = value;
        return this;
    }

    public MemberGap withToDate(LocalDate value) {
        this.toDate = value;
        return this;
    }

    public String getMemberId() {
        return memberId;
    }

    public LocalDate getFromDate() {
        return fromDate;
    }

    public LocalDate getToDate() {
        return toDate;
    }
}
