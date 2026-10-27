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
 * 프로젝트 관리자·투입 인력 (수행/운영)
 * 테이블: pms_project_member (docs/pms/03-ERD.md). 생성된 골격이며, 상태 전이 등 도메인 메서드는 서비스 구현 시 추가한다.
 */
@Entity
@Table(name = "pms_project_member", uniqueConstraints = {
        @UniqueConstraint(name = "uk_pms_project_member", columnNames = {"tenant_id", "project_id", "kind_code", "user_id"})
})
public class ProjectMember extends TenantAwareEntity {

    /** 프로젝트 ID */
    @Column(name = "project_id", length = 64, nullable = false)
    private String projectId;

    /** 구분: EXEC(수행)/OPS(운영) */
    @Column(name = "kind_code", length = 10, nullable = false)
    private String kindCode;

    /** usr_user.id */
    @Column(name = "user_id", length = 64, nullable = false)
    private String userId;

    /** 프로젝트 관리자 여부 */
    @Column(name = "manager_yn", length = 1, nullable = false)
    private String managerYn = "N";

    /** 관리자 역할 PM/PL/PMO(운영 PM) */
    @Column(name = "manager_role_code", length = 20)
    private String managerRoleCode;

    /** 프로젝트 내 역할 */
    @Column(name = "role_code", length = 30, nullable = false)
    private String roleCode;

    /** 투입 M/M */
    @Column(name = "mm", precision = 6, scale = 2)
    private BigDecimal mm;

    /** 월 투입 비율(%) — null이면 M/M ÷ 기간 방식 */
    @Column(name = "rate_pct", precision = 5, scale = 1)
    private BigDecimal ratePct;

    /** 투입 시작일 */
    @Column(name = "from_date")
    private LocalDate fromDate;

    /** 투입 종료일 */
    @Column(name = "to_date")
    private LocalDate toDate;

    /** 종료일 자동 계산 여부 */
    @Column(name = "auto_end_yn", length = 1, nullable = false)
    private String autoEndYn = "N";

    /** 접근 권한: 편집/열람 */
    @Column(name = "access_level_code", length = 10, nullable = false)
    private String accessLevelCode;

    /** 알림 채널 */
    @Column(name = "notify_channel_code", length = 20)
    private String notifyChannelCode;

    protected ProjectMember() {
    }

    private ProjectMember(String id) {
        super(id);
    }

    public static ProjectMember create() {
        return new ProjectMember(PrefixedIdGenerator.generate("PMB"));
    }

    public ProjectMember withProjectId(String value) {
        this.projectId = value;
        return this;
    }

    public ProjectMember withKindCode(String value) {
        this.kindCode = value;
        return this;
    }

    public ProjectMember withUserId(String value) {
        this.userId = value;
        return this;
    }

    public ProjectMember withManagerYn(String value) {
        this.managerYn = value;
        return this;
    }

    public ProjectMember withManagerRoleCode(String value) {
        this.managerRoleCode = value;
        return this;
    }

    public ProjectMember withRoleCode(String value) {
        this.roleCode = value;
        return this;
    }

    public ProjectMember withMm(BigDecimal value) {
        this.mm = value;
        return this;
    }

    public ProjectMember withRatePct(BigDecimal value) {
        this.ratePct = value;
        return this;
    }

    public ProjectMember withFromDate(LocalDate value) {
        this.fromDate = value;
        return this;
    }

    public ProjectMember withToDate(LocalDate value) {
        this.toDate = value;
        return this;
    }

    public ProjectMember withAutoEndYn(String value) {
        this.autoEndYn = value;
        return this;
    }

    public ProjectMember withAccessLevelCode(String value) {
        this.accessLevelCode = value;
        return this;
    }

    public ProjectMember withNotifyChannelCode(String value) {
        this.notifyChannelCode = value;
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

    public String getManagerYn() {
        return managerYn;
    }

    public String getManagerRoleCode() {
        return managerRoleCode;
    }

    public String getRoleCode() {
        return roleCode;
    }

    public BigDecimal getMm() {
        return mm;
    }

    public BigDecimal getRatePct() {
        return ratePct;
    }

    public LocalDate getFromDate() {
        return fromDate;
    }

    public LocalDate getToDate() {
        return toDate;
    }

    public String getAutoEndYn() {
        return autoEndYn;
    }

    public String getAccessLevelCode() {
        return accessLevelCode;
    }

    public String getNotifyChannelCode() {
        return notifyChannelCode;
    }
}
