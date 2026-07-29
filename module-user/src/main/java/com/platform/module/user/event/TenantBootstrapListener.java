package com.platform.module.user.event;

import com.platform.framework.security.access.SystemRoles;
import com.platform.framework.security.tenant.TenantContext;
import com.platform.module.tenant.event.TenantCreatedEvent;
import com.platform.module.user.domain.Role;
import com.platform.module.user.domain.UserGroup;
import com.platform.module.user.domain.UserGroupRole;
import com.platform.module.user.repository.RoleRepository;
import com.platform.module.user.repository.UserGroupRepository;
import com.platform.module.user.repository.UserGroupRoleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 새 회사(테넌트)가 생성되면 그 테넌트가 곧바로 쓸 수 있도록 기초 데이터를 부트스트랩한다:
 *  - Role 마스터에 ROLE_USER/ROLE_ADMIN 시딩 (ROLE_SYSTEM_ADMIN은 일부러 제외 — 테넌트를
 *    새로 만들 수 있는 최상위 권한은 자동으로 뿌려주지 않는다, 별도의 플랫폼 부트스트랩
 *    절차로만 부여되어야 한다)
 *  - DEFAULT_USER 그룹(defaultYn=Y, ROLE_USER 매핑) — 이 테넌트에서 누군가 회원가입하면
 *    AuthService.register() 가 자동으로 이 그룹에 넣어준다
 *  - ADMIN_GROUP 그룹(ROLE_USER + ROLE_ADMIN 매핑) — 아직 소속 사용자는 없다. 이 테넌트의
 *    첫 사용자가 회원가입한 뒤, SYSTEM_ADMIN 권한을 가진 누군가가 UserGroupUserController로
 *    그 사용자를 이 그룹에 넣어줘야 그 테넌트의 관리자가 된다 (흔한 B2B 온보딩 흐름과 동일).
 *
 * @TransactionalEventListener(phase = AFTER_COMMIT) 을 쓰는 이유: CompanyService.create() 의
 * 트랜잭션이 아직 열려있는 도중에 이 리스너가 실행되면, 이미 열린 Session을 그대로 재사용하게
 * 되어 TenantContext 설정이 무시되는 문제가 생긴다 (open-in-view 버그와 같은 유형). 발행하는
 * 트랜잭션이 커밋된 "이후"에 실행되도록 해서, 각 리포지토리 호출이 항상 새 Session을 열게 한다
 * (AuthService/DevDataSeeder와 동일한 이유로 이 리스너도 @Transactional을 쓰지 않는다).
 */
@Component
public class TenantBootstrapListener {

    private static final Logger log = LoggerFactory.getLogger(TenantBootstrapListener.class);

    private final RoleRepository roleRepository;
    private final UserGroupRepository userGroupRepository;
    private final UserGroupRoleRepository userGroupRoleRepository;

    public TenantBootstrapListener(RoleRepository roleRepository,
                                    UserGroupRepository userGroupRepository,
                                    UserGroupRoleRepository userGroupRoleRepository) {
        this.roleRepository = roleRepository;
        this.userGroupRepository = userGroupRepository;
        this.userGroupRoleRepository = userGroupRoleRepository;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onTenantCreated(TenantCreatedEvent event) {
        TenantContext.setTenantId(event.getTenantId());
        try {
            createRoleIfAbsent(SystemRoles.USER, "일반 사용자", "User", "시스템",
                    "기본 로그인 사용자 권한. 회원가입 시 기본으로 부여됨.");
            createRoleIfAbsent(SystemRoles.ADMIN, "테넌트 관리자", "Admin", "시스템",
                    "이 테넌트 내 부서/사용자/역할/포탈 등을 관리할 수 있는 권한.");

            UserGroup defaultGroup = createGroupIfAbsent("DEFAULT_USER", "기본권한 사용자", "Y");
            addRoleIfAbsent(defaultGroup.getId(), SystemRoles.USER);

            UserGroup adminGroup = createGroupIfAbsent("ADMIN_GROUP", "테넌트 관리자", "N");
            addRoleIfAbsent(adminGroup.getId(), SystemRoles.USER);
            addRoleIfAbsent(adminGroup.getId(), SystemRoles.ADMIN);

            log.info("[TenantBootstrapListener] companyCode={} 테넌트 기초 데이터 부트스트랩 완료 "
                    + "(Role: ROLE_USER/ROLE_ADMIN, UserGroup: DEFAULT_USER/ADMIN_GROUP)", event.getCompanyCode());
        } finally {
            TenantContext.clear();
        }
    }

    private void createRoleIfAbsent(String roleCode, String roleName, String roleAlias,
                                     String category, String description) {
        if (!roleRepository.existsByRoleCode(roleCode)) {
            roleRepository.saveAndFlush(Role.create(roleCode, roleName, roleAlias, category, description));
        }
    }

    private UserGroup createGroupIfAbsent(String groupCode, String groupName, String defaultYn) {
        return userGroupRepository.findByGroupCodeAndDeletedYn(groupCode, "N")
                .orElseGet(() -> userGroupRepository.saveAndFlush(
                        // "시스템관리" 는 UserGroupCodeBootstrapListener 가 시딩하는
                        // USER_GROUP_CATEGORY 공통코드의 값 중 하나다 — 하드코딩된 임의 문자열이
                        // 아니라 실제 코드 관리 화면에 존재하는 값으로 맞췄다.
                        UserGroup.create(groupCode, groupName, "시스템관리", null, defaultYn)));
    }

    private void addRoleIfAbsent(String userGroupId, String roleCode) {
        if (!userGroupRoleRepository.existsByUserGroupIdAndRoleCode(userGroupId, roleCode)) {
            userGroupRoleRepository.saveAndFlush(UserGroupRole.create(userGroupId, roleCode));
        }
    }
}
