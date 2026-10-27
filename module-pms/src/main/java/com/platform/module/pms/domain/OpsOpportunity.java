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
 * 운영 영업 기회
 * 테이블: pms_ops_opportunity (docs/pms/03-ERD.md). 생성된 골격이며, 상태 전이 등 도메인 메서드는 서비스 구현 시 추가한다.
 */
@Entity
@Table(name = "pms_ops_opportunity", uniqueConstraints = {
        @UniqueConstraint(name = "uk_pms_ops_opp_code", columnNames = {"tenant_id", "opp_code"})
})
public class OpsOpportunity extends TenantAwareEntity {

    /** 영업 기회 코드 (OS-###) */
    @Column(name = "opp_code", length = 30, nullable = false)
    private String oppCode;

    /** 프로젝트 ID */
    @Column(name = "project_id", length = 64, nullable = false)
    private String projectId;

    /** 유형: 재계약·갱신/추가 개발/유상 개선/증설·확장/교육·컨설팅/신규 제안 */
    @Column(name = "kind_code", length = 30, nullable = false)
    private String kindCode;

    /** 건명 */
    @Column(name = "title", length = 300, nullable = false)
    private String title;

    /** 단계: 발굴/제안/협상/수주/실패·보류 */
    @Column(name = "stage_code", length = 20, nullable = false)
    private String stageCode;

    /** 예상 금액 */
    @Column(name = "amount", precision = 18, scale = 0)
    private Long amount;

    /** 확률도(%) */
    @Column(name = "probability_pct", precision = 5, scale = 2)
    private BigDecimal probabilityPct;

    /** 담당자 */
    @Column(name = "owner_user_id", length = 64)
    private String ownerUserId;

    /** 예상 계약일 */
    @Column(name = "expected_date")
    private LocalDate expectedDate;

    /** 다음 할 일 */
    @Column(name = "next_action", length = 500)
    private String nextAction;

    /** 다음 할 일 예정일 */
    @Column(name = "next_date")
    private LocalDate nextDate;

    /** 메모 */
    @Column(name = "memo", length = 1000)
    private String memo;

    /** 출처 운영 이슈 ID */
    @Column(name = "source_incident_id", length = 64)
    private String sourceIncidentId;

    /** 출처 차기 사업 기회 ID */
    @Column(name = "source_next_opp_id", length = 64)
    private String sourceNextOppId;

    /** 고객사 담당자 ID */
    @Column(name = "contact_id", length = 64)
    private String contactId;

    protected OpsOpportunity() {
    }

    private OpsOpportunity(String id) {
        super(id);
    }

    public static OpsOpportunity create() {
        return new OpsOpportunity(PrefixedIdGenerator.generate("OOP"));
    }

    public OpsOpportunity withOppCode(String value) {
        this.oppCode = value;
        return this;
    }

    public OpsOpportunity withProjectId(String value) {
        this.projectId = value;
        return this;
    }

    public OpsOpportunity withKindCode(String value) {
        this.kindCode = value;
        return this;
    }

    public OpsOpportunity withTitle(String value) {
        this.title = value;
        return this;
    }

    public OpsOpportunity withStageCode(String value) {
        this.stageCode = value;
        return this;
    }

    public OpsOpportunity withAmount(Long value) {
        this.amount = value;
        return this;
    }

    public OpsOpportunity withProbabilityPct(BigDecimal value) {
        this.probabilityPct = value;
        return this;
    }

    public OpsOpportunity withOwnerUserId(String value) {
        this.ownerUserId = value;
        return this;
    }

    public OpsOpportunity withExpectedDate(LocalDate value) {
        this.expectedDate = value;
        return this;
    }

    public OpsOpportunity withNextAction(String value) {
        this.nextAction = value;
        return this;
    }

    public OpsOpportunity withNextDate(LocalDate value) {
        this.nextDate = value;
        return this;
    }

    public OpsOpportunity withMemo(String value) {
        this.memo = value;
        return this;
    }

    public OpsOpportunity withSourceIncidentId(String value) {
        this.sourceIncidentId = value;
        return this;
    }

    public OpsOpportunity withSourceNextOppId(String value) {
        this.sourceNextOppId = value;
        return this;
    }

    public OpsOpportunity withContactId(String value) {
        this.contactId = value;
        return this;
    }

    public String getOppCode() {
        return oppCode;
    }

    public String getProjectId() {
        return projectId;
    }

    public String getKindCode() {
        return kindCode;
    }

    public String getTitle() {
        return title;
    }

    public String getStageCode() {
        return stageCode;
    }

    public Long getAmount() {
        return amount;
    }

    public BigDecimal getProbabilityPct() {
        return probabilityPct;
    }

    public String getOwnerUserId() {
        return ownerUserId;
    }

    public LocalDate getExpectedDate() {
        return expectedDate;
    }

    public String getNextAction() {
        return nextAction;
    }

    public LocalDate getNextDate() {
        return nextDate;
    }

    public String getMemo() {
        return memo;
    }

    public String getSourceIncidentId() {
        return sourceIncidentId;
    }

    public String getSourceNextOppId() {
        return sourceNextOppId;
    }

    public String getContactId() {
        return contactId;
    }
}
