package com.platform.module.pms.domain;

import com.platform.framework.core.common.utils.PrefixedIdGenerator;
import com.platform.framework.security.tenant.TenantAwareEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;

/**
 * 월별 실적 M/M
 * 테이블: pms_actual_mm (docs/pms/03-ERD.md). 생성된 골격이며, 상태 전이 등 도메인 메서드는 서비스 구현 시 추가한다.
 */
@Entity
@Table(name = "pms_actual_mm", uniqueConstraints = {
        @UniqueConstraint(name = "uk_pms_actual_mm", columnNames = {"tenant_id", "project_id", "kind_code", "user_id", "actual_month"})
})
public class ActualMm extends TenantAwareEntity {

    /** 프로젝트 ID */
    @Column(name = "project_id", length = 64, nullable = false)
    private String projectId;

    /** 구분: EXEC/OPS */
    @Column(name = "kind_code", length = 10, nullable = false)
    private String kindCode;

    /** 인력 (usr_user.id) */
    @Column(name = "user_id", length = 64, nullable = false)
    private String userId;

    /** 월 (YYYY-MM) */
    @Column(name = "actual_month", length = 7, nullable = false)
    private String actualMonth;

    /** 실적 M/M */
    @Column(name = "actual_mm", precision = 6, scale = 2, nullable = false)
    private BigDecimal actualMm;

    /** 입력자(프로젝트 관리자) */
    @Column(name = "input_by", length = 64)
    private String inputBy;

    protected ActualMm() {
    }

    private ActualMm(String id) {
        super(id);
    }

    public static ActualMm create() {
        return new ActualMm(PrefixedIdGenerator.generate("AMM"));
    }

    public ActualMm withProjectId(String value) {
        this.projectId = value;
        return this;
    }

    public ActualMm withKindCode(String value) {
        this.kindCode = value;
        return this;
    }

    public ActualMm withUserId(String value) {
        this.userId = value;
        return this;
    }

    public ActualMm withActualMonth(String value) {
        this.actualMonth = value;
        return this;
    }

    public ActualMm withActualMm(BigDecimal value) {
        this.actualMm = value;
        return this;
    }

    public ActualMm withInputBy(String value) {
        this.inputBy = value;
        return this;
    }

    public String getProjectId() {
        return projectId;
    }

    public String getKindCode() {
        return kindCode;
    }

    public String getUserId() {
        return userId;
    }

    public String getActualMonth() {
        return actualMonth;
    }

    public BigDecimal getActualMm() {
        return actualMm;
    }

    public String getInputBy() {
        return inputBy;
    }
}
