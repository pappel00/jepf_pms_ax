package com.platform.module.portal.domain;

import com.platform.framework.core.common.utils.PrefixedIdGenerator;
import com.platform.framework.security.tenant.TenantAwareEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * 페이지 마스터. 사진 속 BackOffice "페이지 관리" 화면 대응.
 * pageFile 은 실제 프론트엔드 라우트/파일 경로(예: /ES/pages/A/PGE_ESA_00001)를 담는다.
 * messageResourceCode 는 MessageResource.messageResourceCode 를 참조하는 단순 문자열 컬럼으로
 * (다른 참조 컬럼들과 동일한 패턴 — 진짜 JPA 연관관계 아님), 프론트가 현재 로그인 사용자의
 * 언어로 이 페이지의 표시명을 다국어 해석할 때 쓴다. 없으면 pageName을 그대로 쓰면 된다.
 */
@Entity
@Table(name = "prt_page",
        uniqueConstraints = @UniqueConstraint(name = "uk_page_tenant_code", columnNames = {"tenant_id", "page_code"}))
public class Page extends TenantAwareEntity {

    @Column(name = "page_code", length = 30, nullable = false)
    private String pageCode;

    @Column(name = "page_name", length = 200, nullable = false)
    private String pageName;

    /** 일반 / 팝업 등 */
    @Column(name = "page_type", length = 20, nullable = false)
    private String pageType;

    /** PC / MOBILE / ALL */
    @Column(name = "device_type", length = 10, nullable = false)
    private String deviceType;

    @Column(name = "page_file", length = 500)
    private String pageFile;

    @Column(name = "message_resource_code", length = 100)
    private String messageResourceCode;

    @Column(name = "use_yn", length = 1, nullable = false)
    private String useYn = "Y";

    protected Page() {
    }

    private Page(String pageCode, String pageName, String pageType, String deviceType, String pageFile,
                 String messageResourceCode) {
        super(PrefixedIdGenerator.generate("PGE"));
        this.pageCode = pageCode;
        this.pageName = pageName;
        this.pageType = pageType;
        this.deviceType = deviceType;
        this.pageFile = pageFile;
        this.messageResourceCode = messageResourceCode;
    }

    public static Page create(String pageCode, String pageName, String pageType, String deviceType,
                               String pageFile, String messageResourceCode) {
        return new Page(pageCode, pageName, pageType, deviceType, pageFile, messageResourceCode);
    }

    public void update(String pageName, String pageType, String deviceType, String pageFile,
                        String messageResourceCode) {
        this.pageName = pageName;
        this.pageType = pageType;
        this.deviceType = deviceType;
        this.pageFile = pageFile;
        this.messageResourceCode = messageResourceCode;
    }

    public void activate() {
        this.useYn = "Y";
    }

    public void deactivate() {
        this.useYn = "N";
    }

    public String getPageCode() {
        return pageCode;
    }

    public String getPageName() {
        return pageName;
    }

    public String getPageType() {
        return pageType;
    }

    public String getDeviceType() {
        return deviceType;
    }

    public String getPageFile() {
        return pageFile;
    }

    public String getMessageResourceCode() {
        return messageResourceCode;
    }

    public String getUseYn() {
        return useYn;
    }
}
