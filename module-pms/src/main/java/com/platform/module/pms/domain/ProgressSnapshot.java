package com.platform.module.pms.domain;

import com.platform.framework.core.common.utils.PrefixedIdGenerator;
import com.platform.framework.security.tenant.TenantAwareEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 주간 진척 스냅샷 (프로젝트 단위)
 * 테이블: pms_progress_snapshot (docs/pms/03-ERD.md). 생성된 골격이며, 상태 전이 등 도메인 메서드는 서비스 구현 시 추가한다.
 */
@Entity
@Table(name = "pms_progress_snapshot", uniqueConstraints = {
        @UniqueConstraint(name = "uk_pms_progress_snapshot", columnNames = {"tenant_id", "project_id", "snapshot_date"})
})
public class ProgressSnapshot extends TenantAwareEntity {

    /** 프로젝트 ID */
    @Column(name = "project_id", length = 64, nullable = false)
    private String projectId;

    /** 해당 주 월요일 */
    @Column(name = "snapshot_date", nullable = false)
    private LocalDate snapshotDate;

    /** 전체 진척(실적) */
    @Column(name = "overall_pct", precision = 5, scale = 1, nullable = false)
    private BigDecimal overallPct;

    /** 목표(계획) */
    @Column(name = "target_pct", precision = 5, scale = 1, nullable = false)
    private BigDecimal targetPct;

    /** 저장자 (자동 저장은 SYSTEM) */
    @Column(name = "saved_by", length = 64)
    private String savedBy;

    protected ProgressSnapshot() {
    }

    private ProgressSnapshot(String id) {
        super(id);
    }

    public static ProgressSnapshot create() {
        return new ProgressSnapshot(PrefixedIdGenerator.generate("PSN"));
    }

    public ProgressSnapshot withProjectId(String value) {
        this.projectId = value;
        return this;
    }

    public ProgressSnapshot withSnapshotDate(LocalDate value) {
        this.snapshotDate = value;
        return this;
    }

    public ProgressSnapshot withOverallPct(BigDecimal value) {
        this.overallPct = value;
        return this;
    }

    public ProgressSnapshot withTargetPct(BigDecimal value) {
        this.targetPct = value;
        return this;
    }

    public ProgressSnapshot withSavedBy(String value) {
        this.savedBy = value;
        return this;
    }

    public String getProjectId() {
        return projectId;
    }

    public LocalDate getSnapshotDate() {
        return snapshotDate;
    }

    public BigDecimal getOverallPct() {
        return overallPct;
    }

    public BigDecimal getTargetPct() {
        return targetPct;
    }

    public String getSavedBy() {
        return savedBy;
    }
}
