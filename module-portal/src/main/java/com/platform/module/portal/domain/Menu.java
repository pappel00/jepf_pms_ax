package com.platform.module.portal.domain;

import com.platform.framework.core.common.utils.PrefixedIdGenerator;
import com.platform.framework.security.tenant.TenantAwareEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.springframework.util.StringUtils;

/**
 * 메뉴(트리 구조, 사진 속 화면에서는 "폴더"라고 부른다). 사진 속 BackOffice "메뉴 관리" 화면 대응.
 *
 * ⚠️ 메뉴(폴더)와 페이지는 N:N 관계다 — 하나의 폴더 아래에 여러 페이지를 놓을 수 있고, 같은
 * 페이지를 여러 폴더에 동시에 놓을 수도 있다(예: 자주찾는 메뉴 폴더 + 원래 소속 폴더 둘 다).
 * 예전엔 Menu가 pageId를 직접 갖는 1:N 구조였는데, 실제로 겪은 요구사항 변경으로 중간에
 * MenuPage 매핑 테이블을 두는 구조로 바꿨다(MenuPage 클래스 참고, Page의 다른 N:M
 * 매핑들(PageRole 등)과 동일한 패턴).
 *
 * linkUrl 은 페이지 매핑과 별개로 외부 링크를 걸 때 쓴다(사진 속 화면의 "링크" 탭에 대응).
 *
 * parentMenuId/linkUrl 은 저장 시점에 빈 문자열("")을 null 로 정규화한다. 그렇지 않으면
 * MenuService.getTree() 의 "parentMenuId == null 이면 루트" 같은 null 체크가 빈 문자열에는
 * 걸리지 않아 트리 조립이 깨진다 (실제로 겪은 버그 — parentMenuId가 ""로 저장된 최상위 메뉴들이
 * 전부 트리에서 누락됨).
 */
@Entity
@Table(name = "prt_menu",
        uniqueConstraints = @UniqueConstraint(name = "uk_menu_tenant_code", columnNames = {"tenant_id", "menu_code"}))
public class Menu extends TenantAwareEntity {

    @Column(name = "menu_code", length = 30, nullable = false)
    private String menuCode;

    @Column(name = "menu_name", length = 200, nullable = false)
    private String menuName;

    @Column(name = "application_id", length = 64, nullable = false)
    private String applicationId;

    @Column(name = "parent_menu_id", length = 64)
    private String parentMenuId;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "link_url", length = 500)
    private String linkUrl;

    @Column(name = "use_yn", length = 1, nullable = false)
    private String useYn = "Y";

    protected Menu() {
    }

    private Menu(String menuCode, String menuName, String applicationId, String parentMenuId,
                 int sortOrder, String linkUrl) {
        super(PrefixedIdGenerator.generate("MNU"));
        this.menuCode = menuCode;
        this.menuName = menuName;
        this.applicationId = applicationId;
        this.parentMenuId = normalize(parentMenuId);
        this.sortOrder = sortOrder;
        this.linkUrl = normalize(linkUrl);
    }

    public static Menu create(String menuCode, String menuName, String applicationId, String parentMenuId,
                               int sortOrder, String linkUrl) {
        return new Menu(menuCode, menuName, applicationId, parentMenuId, sortOrder, linkUrl);
    }

    public void update(String menuName, int sortOrder, String linkUrl) {
        this.menuName = menuName;
        this.sortOrder = sortOrder;
        this.linkUrl = normalize(linkUrl);
    }

    /** 빈 문자열/공백 문자열은 null 로 취급한다 (참조 컬럼의 "값 없음"은 항상 null 로 통일). */
    private static String normalize(String value) {
        return StringUtils.hasText(value) ? value : null;
    }

    public void activate() {
        this.useYn = "Y";
    }

    public void deactivate() {
        this.useYn = "N";
    }

    public String getMenuCode() {
        return menuCode;
    }

    public String getMenuName() {
        return menuName;
    }

    public String getApplicationId() {
        return applicationId;
    }

    public String getParentMenuId() {
        return parentMenuId;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public String getLinkUrl() {
        return linkUrl;
    }

    public String getUseYn() {
        return useYn;
    }
}
