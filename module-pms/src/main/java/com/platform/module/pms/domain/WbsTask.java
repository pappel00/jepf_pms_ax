package com.platform.module.pms.domain;

import com.platform.framework.core.common.utils.PrefixedIdGenerator;
import com.platform.framework.security.tenant.TenantAwareEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * WBS 업무 (자기참조 트리: 단계 → 분류 → 작업)
 * 테이블: pms_wbs_task (docs/pms/03-ERD.md). 생성된 골격이며, 상태 전이 등 도메인 메서드는 서비스 구현 시 추가한다.
 */
@Entity
@Table(name = "pms_wbs_task")
public class WbsTask extends TenantAwareEntity {

    /** 프로젝트 ID */
    @Column(name = "project_id", length = 64, nullable = false)
    private String projectId;

    /** 상위 노드 ID (단계=null) */
    @Column(name = "parent_id", length = 64)
    private String parentId;

    /** 노드 유형: PHASE/CATEGORY/LEAF */
    @Column(name = "node_type_code", length = 10, nullable = false)
    private String nodeTypeCode;

    /** 단계 (사업관리/분석/설계/개발/테스트/이행/준공·안정화) */
    @Column(name = "phase_code", length = 20, nullable = false)
    private String phaseCode;

    /** 분류 */
    @Column(name = "category_name", length = 100)
    private String categoryName;

    /** 업무명 */
    @Column(name = "task_name", length = 300, nullable = false)
    private String taskName;

    /** 연결 RFP 항목 ID */
    @Column(name = "rfp_item_id", length = 64)
    private String rfpItemId;

    /** 요구사항 코드 표시값 */
    @Column(name = "req_code", length = 30)
    private String reqCode;

    /** 기능명 */
    @Column(name = "feature_name", length = 300)
    private String featureName;

    /** 담당자 (업무당 1명) */
    @Column(name = "assignee_user_id", length = 64)
    private String assigneeUserId;

    /** 계획 시작일 */
    @Column(name = "start_date")
    private LocalDate startDate;

    /** 계획 종료일 */
    @Column(name = "end_date")
    private LocalDate endDate;

    /** 가중치 */
    @Column(name = "weight", precision = 8, scale = 2, nullable = false)
    private BigDecimal weight;

    /** 진척률 (리프만 저장) */
    @Column(name = "progress_pct", precision = 5, scale = 1)
    private BigDecimal progressPct;

    /** 선행 업무 ID (finish-to-start) */
    @Column(name = "pred_task_id", length = 64)
    private String predTaskId;

    /** 업무 복사 원본 ID */
    @Column(name = "copied_from_id", length = 64)
    private String copiedFromId;

    /** 정렬 */
    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;

    protected WbsTask() {
    }

    private WbsTask(String id) {
        super(id);
    }

    public static WbsTask create() {
        return new WbsTask(PrefixedIdGenerator.generate("WBT"));
    }

    public WbsTask withProjectId(String value) {
        this.projectId = value;
        return this;
    }

    public WbsTask withParentId(String value) {
        this.parentId = value;
        return this;
    }

    public WbsTask withNodeTypeCode(String value) {
        this.nodeTypeCode = value;
        return this;
    }

    public WbsTask withPhaseCode(String value) {
        this.phaseCode = value;
        return this;
    }

    public WbsTask withCategoryName(String value) {
        this.categoryName = value;
        return this;
    }

    public WbsTask withTaskName(String value) {
        this.taskName = value;
        return this;
    }

    public WbsTask withRfpItemId(String value) {
        this.rfpItemId = value;
        return this;
    }

    public WbsTask withReqCode(String value) {
        this.reqCode = value;
        return this;
    }

    public WbsTask withFeatureName(String value) {
        this.featureName = value;
        return this;
    }

    public WbsTask withAssigneeUserId(String value) {
        this.assigneeUserId = value;
        return this;
    }

    public WbsTask withStartDate(LocalDate value) {
        this.startDate = value;
        return this;
    }

    public WbsTask withEndDate(LocalDate value) {
        this.endDate = value;
        return this;
    }

    public WbsTask withWeight(BigDecimal value) {
        this.weight = value;
        return this;
    }

    public WbsTask withProgressPct(BigDecimal value) {
        this.progressPct = value;
        return this;
    }

    public WbsTask withPredTaskId(String value) {
        this.predTaskId = value;
        return this;
    }

    public WbsTask withCopiedFromId(String value) {
        this.copiedFromId = value;
        return this;
    }

    public WbsTask withSortOrder(Integer value) {
        this.sortOrder = value;
        return this;
    }

    public String getProjectId() {
        return projectId;
    }

    public String getParentId() {
        return parentId;
    }

    public String getNodeTypeCode() {
        return nodeTypeCode;
    }

    public String getPhaseCode() {
        return phaseCode;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public String getTaskName() {
        return taskName;
    }

    public String getRfpItemId() {
        return rfpItemId;
    }

    public String getReqCode() {
        return reqCode;
    }

    public String getFeatureName() {
        return featureName;
    }

    public String getAssigneeUserId() {
        return assigneeUserId;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public BigDecimal getWeight() {
        return weight;
    }

    public BigDecimal getProgressPct() {
        return progressPct;
    }

    public String getPredTaskId() {
        return predTaskId;
    }

    public String getCopiedFromId() {
        return copiedFromId;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }
}
