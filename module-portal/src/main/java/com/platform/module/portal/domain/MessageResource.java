package com.platform.module.portal.domain;

import com.platform.framework.core.common.utils.PrefixedIdGenerator;
import com.platform.framework.security.tenant.TenantAwareEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * 다국어 메시지자원. 사진 속 BackOffice "메시지자원 관리" 화면 대응.
 * 같은 messageResourceCode 가 언어별로 여러 행을 가진다 (예: MSG_ALT_CHK_UNIT_DUP 의 ko/en 값이 각각 별도 행).
 * Page.messageResourceCode 가 이 코드를 참조해서, 프론트가 현재 로그인 사용자의 언어로 페이지 표시명 등을
 * 해석할 수 있게 한다 (다국어 지원의 핵심 연결고리).
 */
@Entity
@Table(name = "prt_message_resource",
        uniqueConstraints = @UniqueConstraint(name = "uk_msgres_tenant_code_lang",
                columnNames = {"tenant_id", "message_resource_code", "language_id"}))
public class MessageResource extends TenantAwareEntity {

    @Column(name = "message_resource_code", length = 100, nullable = false)
    private String messageResourceCode;

    /** Alert / Text */
    @Column(name = "resource_type", length = 20, nullable = false)
    private String resourceType;

    /** ko / en 등 */
    @Column(name = "language_id", length = 10, nullable = false)
    private String languageId;

    // 컬럼명 주의: "value"는 H2의 예약어라 그대로 쓰면 "create table" DDL이 문법 오류로 실패한다
    // (실제로 겪은 버그). 자바 필드/게터 이름(value/getValue())은 그대로 두고 실제 컬럼명만 바꿨다.
    @Column(name = "message_value", length = 2000, nullable = false)
    private String value;

    @Column(name = "default_value", length = 2000)
    private String defaultValue;

    @Column(name = "use_yn", length = 1, nullable = false)
    private String useYn = "Y";

    protected MessageResource() {
    }

    private MessageResource(String messageResourceCode, String resourceType, String languageId,
                             String value, String defaultValue) {
        super(PrefixedIdGenerator.generate("MSG"));
        this.messageResourceCode = messageResourceCode;
        this.resourceType = resourceType;
        this.languageId = languageId;
        this.value = value;
        this.defaultValue = defaultValue;
    }

    public static MessageResource create(String messageResourceCode, String resourceType, String languageId,
                                          String value, String defaultValue) {
        return new MessageResource(messageResourceCode, resourceType, languageId, value, defaultValue);
    }

    public void update(String value, String defaultValue) {
        this.value = value;
        this.defaultValue = defaultValue;
    }

    public void activate() {
        this.useYn = "Y";
    }

    public void deactivate() {
        this.useYn = "N";
    }

    public String getMessageResourceCode() {
        return messageResourceCode;
    }

    public String getResourceType() {
        return resourceType;
    }

    public String getLanguageId() {
        return languageId;
    }

    public String getValue() {
        return value;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public String getUseYn() {
        return useYn;
    }
}
