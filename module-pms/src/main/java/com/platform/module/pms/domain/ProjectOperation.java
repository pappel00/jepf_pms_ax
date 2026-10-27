package com.platform.module.pms.domain;

import com.platform.framework.core.common.utils.PrefixedIdGenerator;
import com.platform.framework.security.tenant.TenantAwareEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDate;

/**
 * 운영 정보 (프로젝트 1:0..1)
 * 테이블: pms_project_operation (docs/pms/03-ERD.md). 생성된 골격이며, 상태 전이 등 도메인 메서드는 서비스 구현 시 추가한다.
 */
@Entity
@Table(name = "pms_project_operation", uniqueConstraints = {
        @UniqueConstraint(name = "uk_pms_project_operation", columnNames = {"tenant_id", "project_id"})
})
public class ProjectOperation extends TenantAwareEntity {

    /** 프로젝트 ID */
    @Column(name = "project_id", length = 64, nullable = false)
    private String projectId;

    /** 운영 형태: 하자보수/유상 유지보수/하자보수+유지보수 */
    @Column(name = "op_type_code", length = 30, nullable = false)
    private String opTypeCode;

    /** 운영 시작일 */
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    /** 운영 종료일 */
    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    /** 계약금액 */
    @Column(name = "contract_amount", precision = 18, scale = 0)
    private Long contractAmount;

    /** SLA 등급: 기본/표준/프리미엄 */
    @Column(name = "sla_level_code", length = 20)
    private String slaLevelCode;

    /** SLA 응답 목표 시간 */
    @Column(name = "sla_respond_hours")
    private Integer slaRespondHours;

    /** SLA 해결 목표 시간 */
    @Column(name = "sla_resolve_hours")
    private Integer slaResolveHours;

    /** 서비스 시간(표시용) */
    @Column(name = "sla_service_hours", length = 100)
    private String slaServiceHours;

    /** 생성한 운영 전환 ID */
    @Column(name = "transition_id", length = 64)
    private String transitionId;

    protected ProjectOperation() {
    }

    private ProjectOperation(String id) {
        super(id);
    }

    public static ProjectOperation create() {
        return new ProjectOperation(PrefixedIdGenerator.generate("POP"));
    }

    public ProjectOperation withProjectId(String value) {
        this.projectId = value;
        return this;
    }

    public ProjectOperation withOpTypeCode(String value) {
        this.opTypeCode = value;
        return this;
    }

    public ProjectOperation withStartDate(LocalDate value) {
        this.startDate = value;
        return this;
    }

    public ProjectOperation withEndDate(LocalDate value) {
        this.endDate = value;
        return this;
    }

    public ProjectOperation withContractAmount(Long value) {
        this.contractAmount = value;
        return this;
    }

    public ProjectOperation withSlaLevelCode(String value) {
        this.slaLevelCode = value;
        return this;
    }

    public ProjectOperation withSlaRespondHours(Integer value) {
        this.slaRespondHours = value;
        return this;
    }

    public ProjectOperation withSlaResolveHours(Integer value) {
        this.slaResolveHours = value;
        return this;
    }

    public ProjectOperation withSlaServiceHours(String value) {
        this.slaServiceHours = value;
        return this;
    }

    public ProjectOperation withTransitionId(String value) {
        this.transitionId = value;
        return this;
    }

    public String getProjectId() {
        return projectId;
    }

    public String getOpTypeCode() {
        return opTypeCode;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public Long getContractAmount() {
        return contractAmount;
    }

    public String getSlaLevelCode() {
        return slaLevelCode;
    }

    public Integer getSlaRespondHours() {
        return slaRespondHours;
    }

    public Integer getSlaResolveHours() {
        return slaResolveHours;
    }

    public String getSlaServiceHours() {
        return slaServiceHours;
    }

    public String getTransitionId() {
        return transitionId;
    }
}
