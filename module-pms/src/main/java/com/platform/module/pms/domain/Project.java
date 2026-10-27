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
 * 프로젝트(사업) — 영업부터 운영까지 1건
 * 테이블: pms_project (docs/pms/03-ERD.md). 생성된 골격이며, 상태 전이 등 도메인 메서드는 서비스 구현 시 추가한다.
 */
@Entity
@Table(name = "pms_project", uniqueConstraints = {
        @UniqueConstraint(name = "uk_pms_project_code", columnNames = {"tenant_id", "project_code"})
})
public class Project extends TenantAwareEntity {

    /** 프로젝트 코드 (PRJ-연도-일련번호) */
    @Column(name = "project_code", length = 30, nullable = false)
    private String projectCode;

    /** 프로젝트명 */
    @Column(name = "project_name", length = 300, nullable = false)
    private String projectName;

    /** 상태 (공통코드 PMS_PROJECT_STATUS) */
    @Column(name = "status_code", length = 30, nullable = false)
    private String statusCode;

    /** 수주 확률도(%) */
    @Column(name = "probability_pct", precision = 5, scale = 2, nullable = false)
    private BigDecimal probabilityPct;

    /** 발주사(고객사) ID */
    @Column(name = "customer_id", length = 64, nullable = false)
    private String customerId;

    /** 발주사명 (복사 저장) */
    @Column(name = "client_name", length = 200, nullable = false)
    private String clientName;

    /** 발주사 담당자 ID */
    @Column(name = "contact_id", length = 64)
    private String contactId;

    /** 담당자명 (복사 저장) */
    @Column(name = "contact_name", length = 100)
    private String contactName;

    /** 담당자 직책 (복사 저장) */
    @Column(name = "contact_title", length = 100)
    private String contactTitle;

    /** 담당자 연락처 (복사 저장) */
    @Column(name = "contact_phone", length = 50)
    private String contactPhone;

    /** 담당자 이메일 (복사 저장) */
    @Column(name = "contact_email", length = 200)
    private String contactEmail;

    /** 예정 시작일 */
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    /** 예정 종료일 */
    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    /** 발주금액(확정 전 추정치, 원화) */
    @Column(name = "amount", precision = 18, scale = 0, nullable = false)
    private Long amount;

    /** 발주금액 부가세 포함 여부 */
    @Column(name = "vat_included_yn", length = 1, nullable = false)
    private String vatIncludedYn = "N";

    /** 영업 담당자 (usr_user.id) */
    @Column(name = "sales_user_id", length = 64, nullable = false)
    private String salesUserId;

    /** 수행 PM (usr_user.id) */
    @Column(name = "pm_user_id", length = 64)
    private String pmUserId;

    /** 사업 구분 (PMS_BUSINESS_TYPE) */
    @Column(name = "business_type_code", length = 30)
    private String businessTypeCode;

    /** 계약/입찰 방식 (PMS_BID_TYPE) */
    @Column(name = "bid_type_code", length = 30)
    private String bidTypeCode;

    /** 공고번호 */
    @Column(name = "notice_no", length = 50)
    private String noticeNo;

    /** 제안 마감일 */
    @Column(name = "due_date")
    private LocalDate dueDate;

    /** 발표/평가일 */
    @Column(name = "present_date")
    private LocalDate presentDate;

    /** 경쟁사 */
    @Column(name = "competitors", length = 500)
    private String competitors;

    /** 예상 원가 */
    @Column(name = "cost_estimate", precision = 18, scale = 0)
    private Long costEstimate;

    /** 예상 원가 출처: MANUAL/RATE_CALC */
    @Column(name = "cost_source_type", length = 20)
    private String costSourceType;

    /** 예상 원가 출처 설명 */
    @Column(name = "cost_source_note", length = 300)
    private String costSourceNote;

    /** 실패/보류 사유 코드 (PMS_FAIL_REASON) */
    @Column(name = "fail_reason_code", length = 50)
    private String failReasonCode;

    /** 실패/보류 사유 상세 */
    @Column(name = "fail_reason_detail", length = 1000)
    private String failReasonDetail;

    /** 수행단계전환일 */
    @Column(name = "exec_start_date")
    private LocalDate execStartDate;

    /** 수행 종료일(준공/검수) */
    @Column(name = "exec_end_date")
    private LocalDate execEndDate;

    /** 수행 종료 비고 */
    @Column(name = "exec_end_memo", length = 1000)
    private String execEndMemo;

    /** 멤버 외 열람 제한 */
    @Column(name = "view_restricted_yn", length = 1, nullable = false)
    private String viewRestrictedYn = "N";

    /** 비고 */
    @Column(name = "memo", length = 1000)
    private String memo;

    protected Project() {
    }

    private Project(String id) {
        super(id);
    }

    public static Project create() {
        return new Project(PrefixedIdGenerator.generate("PRJ"));
    }

    public Project withProjectCode(String value) {
        this.projectCode = value;
        return this;
    }

    public Project withProjectName(String value) {
        this.projectName = value;
        return this;
    }

    public Project withStatusCode(String value) {
        this.statusCode = value;
        return this;
    }

    public Project withProbabilityPct(BigDecimal value) {
        this.probabilityPct = value;
        return this;
    }

    public Project withCustomerId(String value) {
        this.customerId = value;
        return this;
    }

    public Project withClientName(String value) {
        this.clientName = value;
        return this;
    }

    public Project withContactId(String value) {
        this.contactId = value;
        return this;
    }

    public Project withContactName(String value) {
        this.contactName = value;
        return this;
    }

    public Project withContactTitle(String value) {
        this.contactTitle = value;
        return this;
    }

    public Project withContactPhone(String value) {
        this.contactPhone = value;
        return this;
    }

    public Project withContactEmail(String value) {
        this.contactEmail = value;
        return this;
    }

    public Project withStartDate(LocalDate value) {
        this.startDate = value;
        return this;
    }

    public Project withEndDate(LocalDate value) {
        this.endDate = value;
        return this;
    }

    public Project withAmount(Long value) {
        this.amount = value;
        return this;
    }

    public Project withVatIncludedYn(String value) {
        this.vatIncludedYn = value;
        return this;
    }

    public Project withSalesUserId(String value) {
        this.salesUserId = value;
        return this;
    }

    public Project withPmUserId(String value) {
        this.pmUserId = value;
        return this;
    }

    public Project withBusinessTypeCode(String value) {
        this.businessTypeCode = value;
        return this;
    }

    public Project withBidTypeCode(String value) {
        this.bidTypeCode = value;
        return this;
    }

    public Project withNoticeNo(String value) {
        this.noticeNo = value;
        return this;
    }

    public Project withDueDate(LocalDate value) {
        this.dueDate = value;
        return this;
    }

    public Project withPresentDate(LocalDate value) {
        this.presentDate = value;
        return this;
    }

    public Project withCompetitors(String value) {
        this.competitors = value;
        return this;
    }

    public Project withCostEstimate(Long value) {
        this.costEstimate = value;
        return this;
    }

    public Project withCostSourceType(String value) {
        this.costSourceType = value;
        return this;
    }

    public Project withCostSourceNote(String value) {
        this.costSourceNote = value;
        return this;
    }

    public Project withFailReasonCode(String value) {
        this.failReasonCode = value;
        return this;
    }

    public Project withFailReasonDetail(String value) {
        this.failReasonDetail = value;
        return this;
    }

    public Project withExecStartDate(LocalDate value) {
        this.execStartDate = value;
        return this;
    }

    public Project withExecEndDate(LocalDate value) {
        this.execEndDate = value;
        return this;
    }

    public Project withExecEndMemo(String value) {
        this.execEndMemo = value;
        return this;
    }

    public Project withViewRestrictedYn(String value) {
        this.viewRestrictedYn = value;
        return this;
    }

    public Project withMemo(String value) {
        this.memo = value;
        return this;
    }

    public String getProjectCode() {
        return projectCode;
    }

    public String getProjectName() {
        return projectName;
    }

    public String getStatusCode() {
        return statusCode;
    }

    public BigDecimal getProbabilityPct() {
        return probabilityPct;
    }

    public String getCustomerId() {
        return customerId;
    }

    public String getClientName() {
        return clientName;
    }

    public String getContactId() {
        return contactId;
    }

    public String getContactName() {
        return contactName;
    }

    public String getContactTitle() {
        return contactTitle;
    }

    public String getContactPhone() {
        return contactPhone;
    }

    public String getContactEmail() {
        return contactEmail;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public Long getAmount() {
        return amount;
    }

    public String getVatIncludedYn() {
        return vatIncludedYn;
    }

    public String getSalesUserId() {
        return salesUserId;
    }

    public String getPmUserId() {
        return pmUserId;
    }

    public String getBusinessTypeCode() {
        return businessTypeCode;
    }

    public String getBidTypeCode() {
        return bidTypeCode;
    }

    public String getNoticeNo() {
        return noticeNo;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public LocalDate getPresentDate() {
        return presentDate;
    }

    public String getCompetitors() {
        return competitors;
    }

    public Long getCostEstimate() {
        return costEstimate;
    }

    public String getCostSourceType() {
        return costSourceType;
    }

    public String getCostSourceNote() {
        return costSourceNote;
    }

    public String getFailReasonCode() {
        return failReasonCode;
    }

    public String getFailReasonDetail() {
        return failReasonDetail;
    }

    public LocalDate getExecStartDate() {
        return execStartDate;
    }

    public LocalDate getExecEndDate() {
        return execEndDate;
    }

    public String getExecEndMemo() {
        return execEndMemo;
    }

    public String getViewRestrictedYn() {
        return viewRestrictedYn;
    }

    public String getMemo() {
        return memo;
    }
}
