package com.platform.module.pms.domain;

import com.platform.framework.core.common.utils.PrefixedIdGenerator;
import com.platform.framework.security.tenant.TenantAwareEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDate;

/**
 * 운영 업무
 * 테이블: pms_ops_task (docs/pms/03-ERD.md). 생성된 골격이며, 상태 전이 등 도메인 메서드는 서비스 구현 시 추가한다.
 */
@Entity
@Table(name = "pms_ops_task", uniqueConstraints = {
        @UniqueConstraint(name = "uk_pms_ops_task_code", columnNames = {"tenant_id", "task_code"})
})
public class OpsTask extends TenantAwareEntity {

    /** 운영 업무 코드 (OT-###) */
    @Column(name = "task_code", length = 30, nullable = false)
    private String taskCode;

    /** 프로젝트 ID */
    @Column(name = "project_id", length = 64, nullable = false)
    private String projectId;

    /** 업무명 */
    @Column(name = "title", length = 300, nullable = false)
    private String title;

    /** 구분: 정기 점검/패치·배포/장애 조치/문의 대응/요청 작업/보고/기타 */
    @Column(name = "category_code", length = 30, nullable = false)
    private String categoryCode;

    /** 담당자 */
    @Column(name = "assignee_user_id", length = 64)
    private String assigneeUserId;

    /** 기한 */
    @Column(name = "due_date")
    private LocalDate dueDate;

    /** 상태: 대기/진행 중/완료/보류 */
    @Column(name = "status_code", length = 20, nullable = false)
    private String statusCode;

    /** 우선순위 */
    @Column(name = "priority_code", length = 10, nullable = false)
    private String priorityCode;

    /** 반복: 없음/매주/매월/분기 */
    @Column(name = "repeat_code", length = 10, nullable = false)
    private String repeatCode;

    /** 반복 종료일 (비우면 운영 종료일까지) */
    @Column(name = "repeat_end_date")
    private LocalDate repeatEndDate;

    /** 반복으로 만들어진 이전 업무 ID */
    @Column(name = "prev_task_id", length = 64)
    private String prevTaskId;

    /** 자동 생성 여부 */
    @Column(name = "auto_created_yn", length = 1, nullable = false)
    private String autoCreatedYn = "N";

    /** 메모 */
    @Column(name = "memo", length = 1000)
    private String memo;

    /** 연결 운영 이슈 ID */
    @Column(name = "incident_id", length = 64)
    private String incidentId;

    protected OpsTask() {
    }

    private OpsTask(String id) {
        super(id);
    }

    public static OpsTask create() {
        return new OpsTask(PrefixedIdGenerator.generate("OTK"));
    }

    public OpsTask withTaskCode(String value) {
        this.taskCode = value;
        return this;
    }

    public OpsTask withProjectId(String value) {
        this.projectId = value;
        return this;
    }

    public OpsTask withTitle(String value) {
        this.title = value;
        return this;
    }

    public OpsTask withCategoryCode(String value) {
        this.categoryCode = value;
        return this;
    }

    public OpsTask withAssigneeUserId(String value) {
        this.assigneeUserId = value;
        return this;
    }

    public OpsTask withDueDate(LocalDate value) {
        this.dueDate = value;
        return this;
    }

    public OpsTask withStatusCode(String value) {
        this.statusCode = value;
        return this;
    }

    public OpsTask withPriorityCode(String value) {
        this.priorityCode = value;
        return this;
    }

    public OpsTask withRepeatCode(String value) {
        this.repeatCode = value;
        return this;
    }

    public OpsTask withRepeatEndDate(LocalDate value) {
        this.repeatEndDate = value;
        return this;
    }

    public OpsTask withPrevTaskId(String value) {
        this.prevTaskId = value;
        return this;
    }

    public OpsTask withAutoCreatedYn(String value) {
        this.autoCreatedYn = value;
        return this;
    }

    public OpsTask withMemo(String value) {
        this.memo = value;
        return this;
    }

    public OpsTask withIncidentId(String value) {
        this.incidentId = value;
        return this;
    }

    public String getTaskCode() {
        return taskCode;
    }

    public String getProjectId() {
        return projectId;
    }

    public String getTitle() {
        return title;
    }

    public String getCategoryCode() {
        return categoryCode;
    }

    public String getAssigneeUserId() {
        return assigneeUserId;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public String getStatusCode() {
        return statusCode;
    }

    public String getPriorityCode() {
        return priorityCode;
    }

    public String getRepeatCode() {
        return repeatCode;
    }

    public LocalDate getRepeatEndDate() {
        return repeatEndDate;
    }

    public String getPrevTaskId() {
        return prevTaskId;
    }

    public String getAutoCreatedYn() {
        return autoCreatedYn;
    }

    public String getMemo() {
        return memo;
    }

    public String getIncidentId() {
        return incidentId;
    }
}
