package com.platform.module.pms.domain;

import com.platform.framework.core.common.utils.PrefixedIdGenerator;
import com.platform.framework.security.tenant.TenantAwareEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/**
 * 보고서 처리 이력
 * 테이블: pms_report_history (docs/pms/03-ERD.md). 생성된 골격이며, 상태 전이 등 도메인 메서드는 서비스 구현 시 추가한다.
 */
@Entity
@Table(name = "pms_report_history")
public class ReportHistory extends TenantAwareEntity {

    /** 보고서 ID */
    @Column(name = "report_id", length = 64, nullable = false)
    private String reportId;

    /** 처리: 작성/제출/수정/수정 취소/재제출 */
    @Column(name = "action_code", length = 20, nullable = false)
    private String actionCode;

    /** 처리자 */
    @Column(name = "actor_user_id", length = 64, nullable = false)
    private String actorUserId;

    /** 처리 일시 */
    @Column(name = "acted_at", nullable = false)
    private LocalDateTime actedAt;

    /** 내용(수정 사유 포함) */
    @Column(name = "history_text", length = 1000)
    private String historyText;

    protected ReportHistory() {
    }

    private ReportHistory(String id) {
        super(id);
    }

    public static ReportHistory create() {
        return new ReportHistory(PrefixedIdGenerator.generate("RPH"));
    }

    public ReportHistory withReportId(String value) {
        this.reportId = value;
        return this;
    }

    public ReportHistory withActionCode(String value) {
        this.actionCode = value;
        return this;
    }

    public ReportHistory withActorUserId(String value) {
        this.actorUserId = value;
        return this;
    }

    public ReportHistory withActedAt(LocalDateTime value) {
        this.actedAt = value;
        return this;
    }

    public ReportHistory withHistoryText(String value) {
        this.historyText = value;
        return this;
    }

    public String getReportId() {
        return reportId;
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
