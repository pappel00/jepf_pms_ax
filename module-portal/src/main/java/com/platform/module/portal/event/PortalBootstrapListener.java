package com.platform.module.portal.event;

import com.platform.framework.security.access.SystemRoles;
import com.platform.framework.security.tenant.TenantContext;
import com.platform.module.portal.domain.Application;
import com.platform.module.portal.domain.ApplicationRole;
import com.platform.module.portal.domain.Menu;
import com.platform.module.portal.domain.MenuPage;
import com.platform.module.portal.domain.Page;
import com.platform.module.portal.domain.Portal;
import com.platform.module.portal.repository.ApplicationRepository;
import com.platform.module.portal.repository.ApplicationRoleRepository;
import com.platform.module.portal.repository.MenuPageRepository;
import com.platform.module.portal.repository.MenuRepository;
import com.platform.module.portal.repository.PageRepository;
import com.platform.module.portal.repository.PortalRepository;
import com.platform.module.tenant.event.TenantCreatedEvent;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 새 회사(테넌트)가 생성되면 BackOffice 진입점으로 쓸 기본 Portal/Application들과, 그 하위의
 * 실제 내비게이션(메뉴 트리)까지 자동으로 채워둔다 — TenantBootstrapListener(module-user,
 * Role/UserGroup)/PageCodeBootstrapListener와 동일한 이유/패턴이다:
 *  - Portal(`BO`, "BackOffice") — 관리 화면 전용 포탈.
 *  - Application(`HOME`, "홈") — sortOrder=0, requiredRoleCode 없음(ApplicationRole 매핑을
 *    아예 안 걸어서 로그인한 사용자 누구나 접근 가능 — PageRole/ApplicationRole 공통 기본
 *    정책). 하위에 Page 1개("홈", `/workspace/home`, 기존 `DashboardPage` 재사용)만 둔다.
 *  - Application(`SYS_MGT`, "시스템 관리") — sortOrder=1, requiredRoleCode=ROLE_SYSTEM_ADMIN.
 *    Menu(폴더) 4개(조직 관리/User Management/Portal Management/Data Management) + 그 하위
 *    Page 17개(기존 관리 화면 하나하나) + MenuPage 매핑 — "시스템관리 하위 메뉴의 DB화":
 *    프론트 `Layout.jsx`에 하드코딩돼 있던 사이드바 구성을 그대로 DB로 옮긴 것이다.
 *  - Application(`STD_MGT`, "표준관리") — sortOrder=2, requiredRoleCode 없음(예제라 일단
 *    전체 공개). "시스템 관리 외 다른 어플리케이션 추가" 패턴의 실제 동작 예시 —
 *    `frontend/src/pages/apps/STD_MGT/`에 화면 2개, `App.jsx`에 `/workspace/STD_MGT/...`
 *    라우트 2개가 대응된다.
 *
 * ⚠️ 각 Page의 `pageFile`은 이 React 프론트엔드 안의 실제 라우트 경로다(`/workspace/...` 접두사,
 * `App.jsx`의 `/workspace/*` 라우트 트리 참고) — 별도 렌더러 없이 페이지 클릭 시
 * `navigate(page.pageFile)`만 하면 기존 화면 컴포넌트가 그대로 뜬다. 페이지 단위 `PageRole`
 * 매핑은 걸지 않는다 — 각 어플리케이션 자체가 이미 ApplicationRole로 (있다면) 걸러지므로,
 * 그 안의 개별 페이지까지 다시 막을 필요가 없다고 판단했다(매핑이 없으면 PageRoleService.
 * isAccessible()이 "로그인 사용자 누구나 접근 가능"으로 기본 허용하는데, 이미 상위
 * 어플리케이션에서 한 번 걸러졌기 때문에 결과적으로 이중 게이트가 되지 않는다).
 *
 * ⚠️ ROLE_SYSTEM_ADMIN은 TenantBootstrapListener가 새 테넌트에 자동으로 뿌려주지 않는
 * 최상위 권한이다(테넌트를 새로 만들 수 있는 권한이라 의도적으로 제외됨 — TenantBootstrapListener
 * 참고). 그래서 새로 생긴 테넌트에는 이 매핑이 걸려 있어도 실제로 ROLE_SYSTEM_ADMIN을 가진
 * 사용자가 아직 없어 "시스템 관리" 어플리케이션에 아무도 접근하지 못하는 상태로 시작한다 —
 * 의도된 동작이다(플랫폼 최상위 권한을 모든 테넌트에 자동으로 열어주면 안 되기 때문). 이후
 * SYSTEM_ADMIN 권한을 가진 누군가가 UserGroupUserController/UserGroupRoleController로 해당
 * 테넌트에 시스템 관리자를 지정해줘야 접근 가능해진다. `HOME`은 반대로 처음부터 누구나 접근
 * 가능해야 하는 랜딩 화면이라 ApplicationRole 자체를 만들지 않는다.
 *
 * ApplicationRole.roleCode는 Role 마스터에 실제 행이 있는지 검증하지 않는다(PageRole과 동일한
 * 느슨한 참조) — SystemRoles(ROLE_SYSTEM_ADMIN 등)는 Role 마스터 유무와 무관하게 항상 유효한
 * 프레임워크 레벨 권한이기 때문이다.
 *
 * @TransactionalEventListener(phase = AFTER_COMMIT)을 쓰는 이유는 다른 부트스트랩 리스너들과
 * 동일하다 — 발행하는 트랜잭션(Company 생성)이 아직 열려있는 도중에 실행되면 이미 열린
 * Session을 그대로 재사용하게 되어 TenantContext 설정이 무시되는 문제가 생긴다.
 */
@Component
public class PortalBootstrapListener {

    private static final Logger log = LoggerFactory.getLogger(PortalBootstrapListener.class);

    public static final String PORTAL_CODE_BACK_OFFICE = "BO";
    public static final String PORTAL_NAME_BACK_OFFICE = "BackOffice";
    public static final String PORTAL_DEVICE_TYPE = "PC";

    public static final String APPLICATION_CODE_HOME = "HOME";
    public static final String APPLICATION_CODE_SYSTEM_MANAGEMENT = "SYS_MGT";
    public static final String APPLICATION_CODE_STANDARD_MANAGEMENT = "STD_MGT";
    public static final String APPLICATION_CODE_PMS = "PMS";

    /** PageCodeBootstrapListener가 시딩하는 PAGE_TYPE/PAGE_DEVICE_TYPE 공통코드의 값이다. */
    private static final String PAGE_TYPE_GENERAL = "일반";
    private static final String PAGE_DEVICE_TYPE_PC = "PC";

    private record PageSeed(String pageCode, String pageName, String route) {
    }

    private record FolderSeed(String menuCode, String menuName, List<PageSeed> pages) {
    }

    /**
     * 어플리케이션 하나의 시딩 정의. requiredRoleCode가 null이면 ApplicationRole을 아예 안
     * 만든다 — 즉 로그인한 사용자 누구나 접근 가능하다(HOME이 이 경우).
     */
    private record AppSeed(String applicationCode, String applicationName, int sortOrder,
                            String requiredRoleCode, List<FolderSeed> menuTree) {
    }

    /**
     * 프론트 `Layout.jsx`의 `NAV_SECTIONS`를 그대로 옮긴 것이다 — 두 곳이 어긋나지 않도록
     * `Layout.jsx`를 `/workspace/*` 셸로 교체할 때 이 목록도 같이 정리해야 한다.
     * 새 어플리케이션을 추가할 때는 이 목록에 `AppSeed` 하나만 더 넣으면 된다.
     */
    private static final List<AppSeed> APPLICATIONS = List.of(
            new AppSeed(APPLICATION_CODE_HOME, "홈", 0, null, List.of(
                    new FolderSeed("HOME_MENU", "홈", List.of(
                            new PageSeed("HOME_PAGE", "홈", "/workspace/home"))))),
            new AppSeed(APPLICATION_CODE_SYSTEM_MANAGEMENT, "시스템 관리", 1, SystemRoles.SYSTEM_ADMIN, List.of(
                    new FolderSeed("SYS_MENU_ORG", "조직 관리", List.of(
                            new PageSeed("SYS_PAGE_COMPANIES", "회사 관리", "/workspace/companies"),
                            new PageSeed("SYS_PAGE_DEPARTMENTS", "부서 관리", "/workspace/departments"))),
                    new FolderSeed("SYS_MENU_USER", "User Management", List.of(
                            new PageSeed("SYS_PAGE_USERS", "사용자 관리", "/workspace/users"),
                            new PageSeed("SYS_PAGE_USER_GROUPS", "사용자그룹 관리", "/workspace/user-groups"),
                            new PageSeed("SYS_PAGE_ROLES", "Role 관리", "/workspace/roles"))),
                    new FolderSeed("SYS_MENU_PORTAL", "Portal Management", List.of(
                            new PageSeed("SYS_PAGE_PAGES", "페이지 관리", "/workspace/pages"),
                            new PageSeed("SYS_PAGE_PORTALS", "포탈 관리", "/workspace/portals"),
                            new PageSeed("SYS_PAGE_APPLICATIONS", "어플리케이션 관리", "/workspace/applications"),
                            new PageSeed("SYS_PAGE_MENUS", "메뉴 관리", "/workspace/menus"),
                            new PageSeed("SYS_PAGE_DATA_SERVICES", "데이터서비스 관리", "/workspace/data-services"),
                            new PageSeed("SYS_PAGE_MESSAGE_RESOURCES", "메시지자원 관리", "/workspace/message-resources"))),
                    new FolderSeed("SYS_MENU_DATA", "Data Management", List.of(
                            new PageSeed("SYS_PAGE_CODES", "코드 관리", "/workspace/codes"),
                            new PageSeed("SYS_PAGE_FILES", "첨부파일 그룹 관리", "/workspace/files"),
                            new PageSeed("SYS_PAGE_TERMS", "이용약관 관리", "/workspace/terms"),
                            new PageSeed("SYS_PAGE_PRIVACY_POLICY", "개인정보처리방침 관리", "/workspace/privacy-policy"),
                            new PageSeed("SYS_PAGE_NOTICES", "공지/동의서 관리", "/workspace/notices"),
                            new PageSeed("SYS_PAGE_FORMS", "표준양식 관리", "/workspace/forms"))))),
            // "시스템 관리 외 다른 어플리케이션 추가" 패턴의 실제 예시(frontend/src/pages/apps/STD_MGT/
            // 참고). requiredRoleCode를 null로 둬서 일단 전체 로그인 사용자에게 열어뒀다 — 실제
            // 화면으로 굳히면 SYS_MGT처럼 필요한 Role을 지정하면 된다.
            new AppSeed(APPLICATION_CODE_STANDARD_MANAGEMENT, "표준관리", 2, null, List.of(
                    new FolderSeed("STD_MENU_SAMPLE", "예제 메뉴", List.of(
                            new PageSeed("STD_PAGE_SAMPLE1", "예제 화면 1", "/workspace/STD_MGT/sample1"),
                            new PageSeed("STD_PAGE_SAMPLE2", "예제 화면 2", "/workspace/STD_MGT/sample2"))))),
            // PMS 목업 — 화면설계 검토용(frontend/src/pages/apps/PMS/, 더미 데이터). 요구사항
            // 문서(docs/pms) 6절에 따라 BO 포탈 안의 어플리케이션으로 먼저 진행한다. 로그인한
            // 사용자 누구나 접근 가능(requiredRoleCode null) — 정식 구현 시 프로젝트 멤버 권한으로 교체.
            new AppSeed(APPLICATION_CODE_PMS, "PMS", 3, null, List.of(
                    // 고객사 관리 — 발주사/담당자 마스터. 프로젝트 관리에서 선택해 입력한다
                    new FolderSeed("PMS_MENU_CUSTOMER", "고객사 관리", List.of(
                            new PageSeed("PMS_PAGE_CUSTOMERS", "고객사 관리", "/workspace/PMS/customers"),
                            new PageSeed("PMS_PAGE_CUSTOMER_CONTACTS", "고객사 담당자 관리", "/workspace/PMS/customer-contacts"))),
                    new FolderSeed("PMS_MENU_PROJECT", "영업단계", List.of(
                            new PageSeed("PMS_PAGE_SALES_NOTES", "영업 노트", "/workspace/PMS/sales-notes"),
                            new PageSeed("PMS_PAGE_PROJECTS", "프로젝트 관리", "/workspace/PMS/projects"),
                            new PageSeed("PMS_PAGE_RFP", "RFP 관리", "/workspace/PMS/rfp"),
                            new PageSeed("PMS_PAGE_RFP_EVAL", "RFP 평가", "/workspace/PMS/rfp-eval"),
                            new PageSeed("PMS_PAGE_RFP_EVAL_ADMIN", "RFP 평가 관리", "/workspace/PMS/rfp-eval-admin"),
                            new PageSeed("PMS_PAGE_PROPOSALS", "제안서 관리", "/workspace/PMS/proposals"))),
                    // 영업단계(수주) → 수행 단계 사이: 수주 확정 프로젝트를 수행 단계 기초데이터와 함께 전환
                    new FolderSeed("PMS_MENU_TRANSITION", "단계전환", List.of(
                            new PageSeed("PMS_PAGE_PHASE_TRANSITION", "수행단계전환", "/workspace/PMS/phase-transition"),
                            new PageSeed("PMS_PAGE_OPS_TRANSITION", "운영단계전환", "/workspace/PMS/ops-transition"))),
                    new FolderSeed("PMS_MENU_WORK", "수행 단계", List.of(
                            new PageSeed("PMS_PAGE_EXEC_PROJECT", "프로젝트 관리", "/workspace/PMS/exec-project"),
                            new PageSeed("PMS_PAGE_DOC_TAILORING", "문서 테일러링 관리", "/workspace/PMS/doc-tailoring"),
                            new PageSeed("PMS_PAGE_WBS", "WBS / 진척 관리", "/workspace/PMS/wbs"),
                            new PageSeed("PMS_PAGE_ISSUES", "이슈 관리", "/workspace/PMS/issues"),
                            new PageSeed("PMS_PAGE_REPORT_WEEKLY", "주간 보고", "/workspace/PMS/report-weekly"),
                            new PageSeed("PMS_PAGE_REPORT_MONTHLY", "월간 보고", "/workspace/PMS/report-monthly"),
                            new PageSeed("PMS_PAGE_DASHBOARD", "내 업무 대시보드", "/workspace/PMS/dashboard"))),
                    // 운영 단계 — 운영단계전환을 마친(운영 중) 프로젝트의 운영 정보·운영 인력·인수 현황 관리
                    new FolderSeed("PMS_MENU_OPS", "운영 단계", List.of(
                            new PageSeed("PMS_PAGE_OPS_PROJECT", "프로젝트 관리", "/workspace/PMS/ops-project"))),
                    // 인력관리 — 프로젝트의 투입 인력(프로젝트 멤버)을 인력 기준으로 모아 투입 계획·실적을 관리
                    new FolderSeed("PMS_MENU_HR", "인력관리", List.of(
                            new PageSeed("PMS_PAGE_STAFFING", "투입 관리", "/workspace/PMS/staffing"),
                            new PageSeed("PMS_PAGE_RATES", "단가 관리", "/workspace/PMS/rates")))))
    );

    private final PortalRepository portalRepository;
    private final ApplicationRepository applicationRepository;
    private final ApplicationRoleRepository applicationRoleRepository;
    private final MenuRepository menuRepository;
    private final PageRepository pageRepository;
    private final MenuPageRepository menuPageRepository;

    public PortalBootstrapListener(PortalRepository portalRepository,
                                    ApplicationRepository applicationRepository,
                                    ApplicationRoleRepository applicationRoleRepository,
                                    MenuRepository menuRepository,
                                    PageRepository pageRepository,
                                    MenuPageRepository menuPageRepository) {
        this.portalRepository = portalRepository;
        this.applicationRepository = applicationRepository;
        this.applicationRoleRepository = applicationRoleRepository;
        this.menuRepository = menuRepository;
        this.pageRepository = pageRepository;
        this.menuPageRepository = menuPageRepository;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onTenantCreated(TenantCreatedEvent event) {
        TenantContext.setTenantId(event.getTenantId());
        try {
            seedDefaults();
            log.info("[PortalBootstrapListener] companyCode={} 기본 포탈/어플리케이션/메뉴 부트스트랩 완료",
                    event.getCompanyCode());
        } finally {
            TenantContext.clear();
        }
    }

    /**
     * 현재 TenantContext에 설정된 테넌트에 기본 Portal/Application/ApplicationRole/Menu/Page/
     * MenuPage를 채운다(멱등, find-or-create — 이미 있는 행의 값은 덮어쓰지 않는다). public인
     * 이유는 PageCodeBootstrapListener와 동일하다 — app-sandbox의 DevDataSeeder가 이 메서드를
     * 그대로 재사용해서 값이 두 곳에 복사되지 않게 한다.
     *
     * ⚠️ find-or-create이라 이미 부트스트랩된 테넌트(예: 로컬 DEMO)의 기존 행은 이 메서드를
     * 다시 실행해도 sortOrder 등이 갱신되지 않는다 — SYS_MGT의 sortOrder를 0에서 1로 바꾼
     * 것처럼가 기존 값 자체를 바꾸는 변경은 이미 시딩된 환경에서는 수동 UPDATE가 필요하다.
     */
    public void seedDefaults() {
        Portal portal = portalRepository.findByPortalCodeAndDeletedYn(PORTAL_CODE_BACK_OFFICE, "N")
                .orElseGet(() -> portalRepository.saveAndFlush(
                        Portal.create(PORTAL_CODE_BACK_OFFICE, PORTAL_NAME_BACK_OFFICE, PORTAL_DEVICE_TYPE,
                                "관리자용 BackOffice 포탈")));

        for (AppSeed appSeed : APPLICATIONS) {
            seedApplication(portal, appSeed);
        }
    }

    private void seedApplication(Portal portal, AppSeed appSeed) {
        Application application = applicationRepository
                .findByApplicationCodeAndDeletedYn(appSeed.applicationCode(), "N")
                .orElseGet(() -> applicationRepository.saveAndFlush(
                        Application.create(appSeed.applicationCode(), appSeed.applicationName(),
                                portal.getId(), appSeed.sortOrder())));

        if (appSeed.requiredRoleCode() != null
                && !applicationRoleRepository.existsByApplicationIdAndRoleCode(application.getId(), appSeed.requiredRoleCode())) {
            applicationRoleRepository.saveAndFlush(
                    ApplicationRole.create(application.getId(), appSeed.requiredRoleCode()));
        }

        int folderSortOrder = 0;
        for (FolderSeed folderSeed : appSeed.menuTree()) {
            int currentFolderSortOrder = folderSortOrder;
            Menu folder = menuRepository.findByMenuCodeAndDeletedYn(folderSeed.menuCode(), "N")
                    .orElseGet(() -> menuRepository.saveAndFlush(
                            Menu.create(folderSeed.menuCode(), folderSeed.menuName(), application.getId(),
                                    null, currentFolderSortOrder, null)));

            int pageSortOrder = 0;
            for (PageSeed pageSeed : folderSeed.pages()) {
                Page page = pageRepository.findByPageCodeAndDeletedYn(pageSeed.pageCode(), "N")
                        .orElseGet(() -> pageRepository.saveAndFlush(
                                Page.create(pageSeed.pageCode(), pageSeed.pageName(), PAGE_TYPE_GENERAL,
                                        PAGE_DEVICE_TYPE_PC, pageSeed.route(), null)));

                if (!menuPageRepository.existsByMenuIdAndPageId(folder.getId(), page.getId())) {
                    menuPageRepository.saveAndFlush(MenuPage.create(folder.getId(), page.getId(), pageSortOrder));
                }
                pageSortOrder++;
            }
            folderSortOrder++;
        }
    }
}
