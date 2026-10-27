package com.platform.module.pms.domain;

import com.platform.framework.core.common.utils.PrefixedIdGenerator;
import com.platform.framework.security.tenant.TenantAwareEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/**
 * 이슈 처리 이력
 * 테이블: pms_issue_history (docs/pms/03-ERD.md). 생성된 골격이며, 상태 전이 등 도메인 메서드는 서비스 구현 시 추가한다.
 */
@Entity
@Table(name = "pms_issue_history")
public class IssueHistory extends TenantAwareEntity {

    /** 이슈 ID */
    @Column(name = "issue_id", length = 64, nullable = false)
    private String issueId;

    /** 처리: 등록/상태 변경/업무 연결/업무 연결 해제/메모 */
    @Column(name = "action_code", length = 30, nullable = false)
    private String actionCode;

    /** 처리자 */
    @Column(name = "actor_user_id", length = 64, nullable = false)
    private String actorUserId;

    /** 처리 일시 */
    @Column(name = "acted_at", nullable = false)
    private LocalDateTime actedAt;

    /** 내용 */
    @Column(name = "history_text", length = 1000)
    private String historyText;

    protected IssueHistory() {
    }

    private IssueHistory(String id) {
        super(id);
    }

    public static IssueHistory create() {
        return new IssueHistory(PrefixedIdGenerator.generate("IHS"));
    }

    public IssueHistory withIssueId(String value) {
        this.issueId = value;
        return this;
    }

    public IssueHistory withActionCode(String value) {
        this.actionCode = value;
        return this;
    }

    public IssueHistory withActorUserId(String value) {
        this.actorUserId = value;
        return this;
    }

    public IssueHistory withActedAt(LocalDateTime value) {
        this.actedAt = value;
        return this;
    }

    public IssueHistory withHistoryText(String value) {
        this.historyText = value;
        return this;
    }

    public String getIssueId() {
        return issueId;
    }

    public String getActionCode() {
        return actionCode;
    }

    public String getActorUserId() {
        return actorUserId;
    }

    public LocalDateTime getActedAt() {
        return actedAt;
    }

    public String getHistoryText() {
        return historyText;
    }
}
