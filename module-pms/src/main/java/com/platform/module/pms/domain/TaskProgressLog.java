package com.platform.module.pms.domain;

import com.platform.framework.core.common.utils.PrefixedIdGenerator;
import com.platform.framework.security.tenant.TenantAwareEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 업무 진척 입력 이력
 * 테이블: pms_task_progress_log (docs/pms/03-ERD.md). 생성된 골격이며, 상태 전이 등 도메인 메서드는 서비스 구현 시 추가한다.
 */
@Entity
@Table(name = "pms_task_progress_log")
public class TaskProgressLog extends TenantAwareEntity {

    /** WBS 업무 ID */
    @Column(name = "task_id", length = 64, nullable = false)
    private String taskId;

    /** 입력 진척률 */
    @Column(name = "progress_pct", precision = 5, scale = 1, nullable = false)
    private BigDecimal progressPct;

    /** 입력자 */
    @Column(name = "input_by_user_id", length = 64, nullable = false)
    private String inputByUserId;

    /** 입력 일시 */
    @Column(name = "input_at", nullable = false)
    private LocalDateTime inputAt;

    /** 입력 채널: WEB/TELEGRAM/MATTERMOST/ADMIN_PROXY */
    @Column(name = "channel_code", length = 20, nullable = false)
    private String channelCode;

    /** 메모 */
    @Column(name = "note", length = 500)
    private String note;

    protected TaskProgressLog() {
    }

    private TaskProgressLog(String id) {
        super(id);
    }

    public static TaskProgressLog create() {
        return new TaskProgressLog(PrefixedIdGenerator.generate("TPL"));
    }

    public TaskProgressLog withTaskId(String value) {
        this.taskId = value;
        return this;
    }

    public TaskProgressLog withProgressPct(BigDecimal value) {
        this.progressPct = value;
        return this;
    }

    public TaskProgressLog withInputByUserId(String value) {
        this.inputByUserId = value;
        return this;
    }

    public TaskProgressLog withInputAt(LocalDateTime value) {
        this.inputAt = value;
        return this;
    }

    public TaskProgressLog withChannelCode(String value) {
        this.channelCode = value;
        return this;
    }

    public TaskProgressLog withNote(String value) {
        this.note = value;
        return this;
    }

    public String getTaskId() {
        return taskId;
    }

    public BigDecimal getProgressPct() {
        return progressPct;
    }

    public String getInputByUserId() {
        return inputByUserId;
    }

    public LocalDateTime getInputAt() {
        return inputAt;
    }

    public String getChannelCode() {
        return channelCode;
    }

    public String getNote() {
        return note;
    }
}
