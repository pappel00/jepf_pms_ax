package com.platform.module.pms.domain;

import com.platform.framework.core.common.utils.PrefixedIdGenerator;
import com.platform.framework.security.tenant.TenantAwareEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;

/**
 * 이슈 ↔ 업무 N:M
 * 테이블: pms_issue_task (docs/pms/03-ERD.md). 생성된 골격이며, 상태 전이 등 도메인 메서드는 서비스 구현 시 추가한다.
 */
@Entity
@Table(name = "pms_issue_task", uniqueConstraints = {
        @UniqueConstraint(name = "uk_pms_issue_task", columnNames = {"tenant_id", "issue_id", "task_id"})
})
public class IssueTask extends TenantAwareEntity {

    /** 이슈 ID */
    @Column(name = "issue_id", length = 64, nullable = false)
    private String issueId;

    /** WBS 업무 ID */
    @Column(name = "task_id", length = 64, nullable = false)
    private String taskId;

    /** 연결 처리자 */
    @Column(name = "linked_by", length = 64)
    private String linkedBy;

    /** 연결 일시 */
    @Column(name = "linked_at")
    private LocalDateTime linkedAt;

    protected IssueTask() {
    }

    private IssueTask(String id) {
        super(id);
    }

    public static IssueTask create() {
        return new IssueTask(PrefixedIdGenerator.generate("ITK"));
    }

    public IssueTask withIssueId(String value) {
        this.issueId = value;
        return this;
    }

    public IssueTask withTaskId(String value) {
        this.taskId = value;
        return this;
    }

    public IssueTask withLinkedBy(String value) {
        this.linkedBy = value;
        return this;
    }

    public IssueTask withLinkedAt(LocalDateTime value) {
        this.linkedAt = value;
        return this;
    }

    public String getIssueId() {
        return issueId;
    }

    public String getTaskId() {
        return taskId;
    }

    public String getLinkedBy() {
        return linkedBy;
    }

    public LocalDateTime getLinkedAt() {
        return linkedAt;
    }
}
