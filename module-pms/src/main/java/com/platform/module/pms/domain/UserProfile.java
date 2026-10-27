package com.platform.module.pms.domain;

import com.platform.framework.core.common.utils.PrefixedIdGenerator;
import com.platform.framework.security.tenant.TenantAwareEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * PMS 사용자 확장 프로필 (usr_user 1:1)
 * 테이블: pms_user_profile (docs/pms/03-ERD.md). 생성된 골격이며, 상태 전이 등 도메인 메서드는 서비스 구현 시 추가한다.
 */
@Entity
@Table(name = "pms_user_profile", uniqueConstraints = {
        @UniqueConstraint(name = "uk_pms_user_profile", columnNames = {"tenant_id", "user_id"})
})
public class UserProfile extends TenantAwareEntity {

    /** usr_user.id */
    @Column(name = "user_id", length = 64, nullable = false)
    private String userId;

    /** 소속 구분: 자사/협력사 */
    @Column(name = "org_type_code", length = 20, nullable = false)
    private String orgTypeCode;

    /** 협력사 소속회사명 */
    @Column(name = "partner_company", length = 200)
    private String partnerCompany;

    /** 알림 채널: Mattermost/텔레그램/이메일/미설정 */
    @Column(name = "notify_channel_code", length = 20)
    private String notifyChannelCode;

    /** 텔레그램 chat ID */
    @Column(name = "telegram_chat_id", length = 100)
    private String telegramChatId;

    /** Mattermost 사용자 ID */
    @Column(name = "mattermost_user_id", length = 100)
    private String mattermostUserId;

    /** 인력 등급: 특급/고급/중급/초급 */
    @Column(name = "job_grade_code", length = 20)
    private String jobGradeCode;

    protected UserProfile() {
    }

    private UserProfile(String id) {
        super(id);
    }

    public static UserProfile create() {
        return new UserProfile(PrefixedIdGenerator.generate("UPF"));
    }

    public UserProfile withUserId(String value) {
        this.userId = value;
        return this;
    }

    public UserProfile withOrgTypeCode(String value) {
        this.orgTypeCode = value;
        return this;
    }

    public UserProfile withPartnerCompany(String value) {
        this.partnerCompany = value;
        return this;
    }

    public UserProfile withNotifyChannelCode(String value) {
        this.notifyChannelCode = value;
        return this;
    }

    public UserProfile withTelegramChatId(String value) {
        this.telegramChatId = value;
        return this;
    }

    public UserProfile withMattermostUserId(String value) {
        this.mattermostUserId = value;
        return this;
    }

    public UserProfile withJobGradeCode(String value) {
        this.jobGradeCode = value;
        return this;
    }

    public String getUserId() {
        return userId;
    }

    public String getOrgTypeCode() {
        return orgTypeCode;
    }

    public String getPartnerCompany() {
        return partnerCompany;
    }

    public String getNotifyChannelCode() {
        return notifyChannelCode;
    }

    public String getTelegramChatId() {
        return telegramChatId;
    }

    public String getMattermostUserId() {
        return mattermostUserId;
    }

    public String getJobGradeCode() {
        return jobGradeCode;
    }
}
