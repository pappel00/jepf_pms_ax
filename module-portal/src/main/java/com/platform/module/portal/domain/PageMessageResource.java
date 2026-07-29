package com.platform.module.portal.domain;

import com.platform.framework.core.common.utils.PrefixedIdGenerator;
import com.platform.framework.security.tenant.TenantAwareEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * Page - MessageResource 매핑(N:M). 사진 속 페이지 관리 화면의 "메시지 자원" 탭 대응.
 * MessageResource 는 (code, languageId) 조합마다 별도 행이지만, 이 매핑은 언어와 무관하게
 * messageResourceCode 단위로 "이 페이지가 이 메시지자원 코드를 쓴다"만 기록한다 — 실제 언어별
 * 값 해석은 MessageResourceController.resolve() 가 담당한다.
 */
@Entity
@Table(name = "prt_page_message_resource",
        uniqueConstraints = @UniqueConstraint(name = "uk_pmr_tenant_page_code",
                columnNames = {"tenant_id", "page_id", "message_resource_code"}))
public class PageMessageResource extends TenantAwareEntity {

    @Column(name = "page_id", length = 64, nullable = false)
    private String pageId;

    @Column(name = "message_resource_code", length = 100, nullable = false)
    private String messageResourceCode;

    protected PageMessageResource() {
    }

    private PageMessageResource(String pageId, String messageResourceCode) {
        super(PrefixedIdGenerator.generate("PMR"));
        this.pageId = pageId;
        this.messageResourceCode = messageResourceCode;
    }

    public static PageMessageResource create(String pageId, String messageResourceCode) {
        return new PageMessageResource(pageId, messageResourceCode);
    }

    public String getPageId() {
        return pageId;
    }

    public String getMessageResourceCode() {
        return messageResourceCode;
    }
}
