package com.platform.module.portal.event;

import com.platform.framework.security.access.SystemRoles;
import com.platform.framework.security.tenant.TenantContext;
import com.platform.module.portal.domain.Application;
import com.platform.module.portal.domain.ApplicationRole;
import com.platform.module.portal.domain.Portal;
import com.platform.module.portal.repository.ApplicationRepository;
import com.platform.module.portal.repository.ApplicationRoleRepository;
import com.platform.module.portal.repository.PortalRepository;
import com.platform.module.tenant.event.TenantCreatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 새 회사(테넌트)가 생성되면 BackOffice 진입점으로 쓸 기본 Portal/Application을 자동으로
 * 채워둔다 — TenantBootstrapListener(module-user, Role/UserGroup)/PageCodeBootstrapListener/
 * UserGroupCodeBootstrapListener와 동일한 이유/패턴이다:
 *  - Portal(`BO`, "BackOffice") — 관리 화면 전용 포탈.
 *  - Application(`SYS_MGT`, "시스템 관리", BO 포탈 소속) — 시스템 관리 화면 진입점.
 *  - ApplicationRole(SYS_MGT ↔ ROLE_SYSTEM_ADMIN) — 시스템 관리자만 접근 가능하도록 기본 매핑.
 *
 * ⚠️ ROLE_SYSTEM_ADMIN은 TenantBootstrapListener가 새 테넌트에 자동으로 뿌려주지 않는
 * 최상위 권한이다(테넌트를 새로 만들 수 있는 권한이라 의도적으로 제외됨 — TenantBootstrapListener
 * 참고). 그래서 새로 생긴 테넌트에는 이 매핑이 걸려 있어도 실제로 ROLE_SYSTEM_ADMIN을 가진
 * 사용자가 아직 없어 "시스템 관리" 어플리케이션에 아무도 접근하지 못하는 상태로 시작한다 —
 * 의도된 동작이다(플랫폼 최상위 권한을 모든 테넌트에 자동으로 열어주면 안 되기 때문). 이후
 * SYSTEM_ADMIN 권한을 가진 누군가가 UserGroupUserController/UserGroupRoleController로 해당
 * 테넌트에 시스템 관리자를 지정해줘야 접근 가능해진다.
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

    public static final String APPLICATION_CODE_SYSTEM_MANAGEMENT = "SYS_MGT";
    public static final String APPLICATION_NAME_SYSTEM_MANAGEMENT = "시스템 관리";

    private final PortalRepository portalRepository;
    private final ApplicationRepository applicationRepository;
    private final ApplicationRoleRepository applicationRoleRepository;

    public PortalBootstrapListener(PortalRepository portalRepository,
                                    ApplicationRepository applicationRepository,
                                    ApplicationRoleRepository applicationRoleRepository) {
        this.portalRepository = portalRepository;
        this.applicationRepository = applicationRepository;
        this.applicationRoleRepository = applicationRoleRepository;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onTenantCreated(TenantCreatedEvent event) {
        TenantContext.setTenantId(event.getTenantId());
        try {
            seedDefaults();
            log.info("[PortalBootstrapListener] companyCode={} 기본 포탈/어플리케이션 부트스트랩 완료",
                    event.getCompanyCode());
        } finally {
            TenantContext.clear();
        }
    }

    /**
     * 현재 TenantContext에 설정된 테넌트에 기본 Portal/Application/ApplicationRole을 채운다
     * (멱등). public인 이유는 PageCodeBootstrapListener와 동일하다 — app-sandbox의
     * DevDataSeeder가 이 메서드를 그대로 재사용해서 값이 두 곳에 복사되지 않게 한다.
     */
    public void seedDefaults() {
        Portal portal = portalRepository.findByPortalCodeAndDeletedYn(PORTAL_CODE_BACK_OFFICE, "N")
                .orElseGet(() -> portalRepository.saveAndFlush(
                        Portal.create(PORTAL_CODE_BACK_OFFICE, PORTAL_NAME_BACK_OFFICE, PORTAL_DEVICE_TYPE,
                                "관리자용 BackOffice 포탈")));

        Application application = applicationRepository
                .findByApplicationCodeAndDeletedYn(APPLICATION_CODE_SYSTEM_MANAGEMENT, "N")
                .orElseGet(() -> applicationRepository.saveAndFlush(
                        Application.create(APPLICATION_CODE_SYSTEM_MANAGEMENT, APPLICATION_NAME_SYSTEM_MANAGEMENT,
                                portal.getId())));

        if (!applicationRoleRepository.existsByApplicationIdAndRoleCode(application.getId(), SystemRoles.SYSTEM_ADMIN)) {
            applicationRoleRepository.saveAndFlush(ApplicationRole.create(application.getId(), SystemRoles.SYSTEM_ADMIN));
        }
    }
}
