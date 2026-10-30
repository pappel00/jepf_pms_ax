package com.platform.module.pms.domain;

import com.platform.framework.core.common.utils.PrefixedIdGenerator;
import com.platform.framework.security.tenant.TenantAwareEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * 알림 발송 이력 — 메신저(Mattermost 등)로 보낸 알림과 건너뛴/실패한 사유를 남긴다.
 * 테이블: pms_notify_log (docs/pms/03-ERD.md).
 */
@Entity
@Table(name = "pms_notify_log")
public class NotifyLog extends TenantAwareEntity {

    /** 받는 사람(usr_user.id) */
    @Column(name = "user_id", length = 64, nullable = false)
    private String userId;

    /** 채널: Mattermost/텔레그램/이메일/미설정 */
    @Column(name = "channel_code", length = 20, nullable = false)
    private String channelCode;

    /** 알림 종류: TEST, TASK_DUE, ISSUE_ASSIGNED 등 */
    @Column(name = "kind_code", length = 30, nullable = false)
    private String kindCode;

    @Column(name = "message", length = 2000)
    private String message;

    /** SENT / FAILED / SKIPPED */
    @Column(name = "status_code", length = 20, nullable = false)
    private String statusCode;

    /** 실패·건너뜀 사유 */
    @Column(name = "error_message", length = 500)
    private String errorMessage;

    protected NotifyLog() {
    }

    private NotifyLog(String id) {
        super(id);
    }

    public static NotifyLog create() {
        return new NotifyLog(PrefixedIdGenerator.generate("NLG"));
    }

    public NotifyLog withUserId(String value) {
        this.userId = value;
        return this;
    }

    public NotifyLog withChannelCode(String value) {
        this.channelCode = value;
        return this;
    }

    public NotifyLog withKindCode(String value) {
        this.kindCode = value;
        return this;
    }

    public NotifyLog withMessage(String value) {
        this.message = value == null || value.length() <= 2000 ? value : value.substring(0, 2000);
        return this;
    }

    public NotifyLog withStatusCode(String value) {
        this.statusCode = value;
        return this;
    }

    public NotifyLog withErrorMessage(String value) {
        this.errorMessage = value == null || value.length() <= 500 ? value : value.substring(0, 500);
        return this;
    }

    public String getUserId() {
        return userId;
    }

    public String getChannelCode() {
        return channelCode;
    }

    public String getKindCode() {
        return kindCode;
    }

    public String getMessage() {
        return message;
    }

    public String getStatusCode() {
        return statusCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}
