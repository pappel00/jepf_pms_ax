package com.platform.sandbox;

import com.platform.framework.security.access.SystemRoles;
import com.platform.framework.security.tenant.TenantContext;
import com.platform.module.code.domain.CodeDetail;
import com.platform.module.code.domain.CodeGroup;
import com.platform.module.code.repository.CodeDetailRepository;
import com.platform.module.code.repository.CodeGroupRepository;
import com.platform.module.notice.event.NoticeCodeBootstrapListener;
import com.platform.module.portal.event.PageCodeBootstrapListener;
import com.platform.module.tenant.domain.Company;
import com.platform.module.tenant.repository.CompanyRepository;
import com.platform.module.user.domain.Role;
import com.platform.module.user.domain.User;
import com.platform.module.user.domain.UserGroup;
import com.platform.module.user.domain.UserGroupRole;
import com.platform.module.user.domain.UserGroupUser;
import com.platform.module.user.event.UserGroupCodeBootstrapListener;
import com.platform.module.user.repository.RoleRepository;
import com.platform.module.user.repository.UserGroupRepository;
import com.platform.module.user.repository.UserGroupRoleRepository;
import com.platform.module.user.repository.UserGroupUserRepository;
import com.platform.module.user.repository.UserRepository;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * ⚠️ 로컬 개발/테스트 전용.
 * 서버 기동 시 샘플 회사(DEMO)와 관리자 계정(admin / admin1234)을 자동으로 만들어,
 * 바로 /api/v1/auth/login 으로 로그인해볼 수 있게 한다.
 *
 * User는 더 이상 역할을 직접 갖지 않는다(Role -> UserGroup -> User 구조, User.java 참고).
 * 그래서 이 시더는 UserGroup 2개를 만든다:
 *  - DEFAULT_USER (defaultYn=Y, ROLE_USER 매핑) — 앞으로 회원가입하는 사용자가 자동으로 배정됨
 *  - ADMIN_GROUP (ROLE_USER/ROLE_ADMIN/ROLE_SYSTEM_ADMIN 매핑) — admin 계정을 여기 소속시켜
 *    Role 기반 인가가 걸린 API들도 바로 테스트할 수 있게 한다.
 *
 * ⚠️ Role 마스터에도 이 세 SystemRole을 시딩한다 (실제로 겪은 문제 — UserGroupRoleService는
 * SystemRoles를 Role 마스터 조회 없이 항상 허용하도록 예외 처리해뒀는데, 그 결과 admin이
 * 실제로 ROLE_ADMIN 등을 갖고 있는데도 GET /api/v1/roles 목록엔 아무 것도 안 보이는 상태가
 * 됐었다. 검증 로직의 예외 처리(ALWAYS_ALLOWED_ROLE_CODES)는 안전장치로 그대로 두고,
 * Role 관리 화면에서 실제로 눈에 보이도록 마스터 행도 만들어준다.
 *
 * ⚠️ 이 시더는 CompanyService.create()(TenantCreatedEvent 발행)를 거치지 않고 Company를 직접
 * 만든다 — 그래서 TenantBootstrapListener/NoticeCodeBootstrapListener/PageCodeBootstrapListener/
 * UserGroupCodeBootstrapListener가 실제 API로 회사를 만들 때 자동으로 해주는 일(Role/UserGroup,
 * 공지 팝업 설정 공통코드, 페이지 용도/디바이스유형 공통코드, 사용자그룹 분류 공통코드 시딩)을
 * 여기서 똑같이 직접 해줘야 한다. 실제 이벤트 리스너들과 값이 어긋나지 않도록 주의할 것 —
 * PageCodeBootstrapListener/UserGroupCodeBootstrapListener는 아예 시딩 메서드(seedDefaults())를
 * 재사용하도록 열어둬서 값이 복사되지 않는다.
 *
 * 로그인 없이 회사를 만들 수 있는 API 자체가 없기 때문에(회사 생성도 인증 필요), 이 시더가
 * 사실상 "최초 부트스트랩 관리자" 역할을 한다. 실제 운영에서는 별도의 시스템 관리자 초기화
 * 절차가 필요하다 — 이 클래스는 로컬 샌드박스 전용이며 운영 배포 시 제거해야 한다.
 *
 * AuthService와 동일한 이유로 @Transactional 을 쓰지 않는다: 회사 조회(테넌트 무관) 이후
 * TenantContext 를 설정하고, 그 다음에 실행되는 조회/저장이 각자 새 트랜잭션(=새 Session)을
 * 열도록 두어야 Hibernate가 올바른 테넌트로 Session을 연다.
 */
@Component
public class DevDataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DevDataSeeder.class);

    private final CompanyRepository companyRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserGroupRepository userGroupRepository;
    private final UserGroupRoleRepository userGroupRoleRepository;
    private final UserGroupUserRepository userGroupUserRepository;
    private final CodeGroupRepository codeGroupRepository;
    private final CodeDetailRepository codeDetailRepository;
    private final PageCodeBootstrapListener pageCodeBootstrapListener;
    private final UserGroupCodeBootstrapListener userGroupCodeBootstrapListener;
    private final PasswordEncoder passwordEncoder;

    public DevDataSeeder(CompanyRepository companyRepository,
                          UserRepository userRepository,
                          RoleRepository roleRepository,
                          UserGroupRepository userGroupRepository,
                          UserGroupRoleRepository userGroupRoleRepository,
                          UserGroupUserRepository userGroupUserRepository,
                          CodeGroupRepository codeGroupRepository,
                          CodeDetailRepository codeDetailRepository,
                          PageCodeBootstrapListener pageCodeBootstrapListener,
                          UserGroupCodeBootstrapListener userGroupCodeBootstrapListener,
                          PasswordEncoder passwordEncoder) {
        this.companyRepository = companyRepository;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.userGroupRepository = userGroupRepository;
        this.userGroupRoleRepository = userGroupRoleRepository;
        this.userGroupUserRepository = userGroupUserRepository;
        this.codeGroupRepository = codeGroupRepository;
        this.codeDetailRepository = codeDetailRepository;
        this.pageCodeBootstrapListener = pageCodeBootstrapListener;
        this.userGroupCodeBootstrapListener = userGroupCodeBootstrapListener;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        Company company = findOrCreateDemoCompany();

        TenantContext.setTenantId(company.getId());
        try {
            createRoleIfAbsent(SystemRoles.USER, "일반 사용자", "User", "시스템",
                    "기본 로그인 사용자 권한. 회원가입 시 기본으로 부여됨.");
            createRoleIfAbsent(SystemRoles.ADMIN, "테넌트 관리자", "Admin", "시스템",
                    "이 테넌트 내 부서/사용자/역할/포탈 등을 관리할 수 있는 권한.");
            createRoleIfAbsent(SystemRoles.SYSTEM_ADMIN, "시스템 관리자", "System Admin", "시스템",
                    "회사(테넌트) 자체를 생성/관리할 수 있는 최상위 권한.");

            UserGroup defaultGroup = findOrCreateGroup("DEFAULT_USER", "기본권한 사용자", "Y");
            addRoleIfAbsent(defaultGroup.getId(), SystemRoles.USER);

            UserGroup adminGroup = findOrCreateGroup("ADMIN_GROUP", "시스템 관리자", "N");
            addRoleIfAbsent(adminGroup.getId(), SystemRoles.USER);
            addRoleIfAbsent(adminGroup.getId(), SystemRoles.ADMIN);
            addRoleIfAbsent(adminGroup.getId(), SystemRoles.SYSTEM_ADMIN);

            createAdminUserIfAbsent(adminGroup.getId());

            seedNoticePopupCodes();

            // 페이지 용도(일반/팝업)/디바이스유형(PC/MOBILE/ALL) 공통코드. 위 seedNoticePopupCodes()와
            // 달리 값을 여기에 복사하지 않고 리스너의 시딩 로직을 그대로 재사용한다.
            pageCodeBootstrapListener.seedDefaults();

            // 사용자그룹 분류(컨텐츠관리/시스템관리/데이터입력관리) 공통코드. 마찬가지로 리스너의
            // 시딩 로직을 재사용한다.
            userGroupCodeBootstrapListener.seedDefaults();
        } finally {
            TenantContext.clear();
        }
    }

    private Company findOrCreateDemoCompany() {
        return companyRepository.findByCompanyCodeAndDeletedYn("DEMO", "N")
                .orElseGet(() -> {
                    Company created = companyRepository.saveAndFlush(
                            Company.create("DEMO", "데모 회사", "로컬 개발용 샘플 회사"));
                    log.info("[DevDataSeeder] 샘플 회사 생성: companyCode=DEMO");
                    return created;
                });
    }

    private void createRoleIfAbsent(String roleCode, String roleName, String roleAlias,
                                     String category, String description) {
        if (!roleRepository.existsByRoleCode(roleCode)) {
            roleRepository.saveAndFlush(Role.create(roleCode, roleName, roleAlias, category, description));
            log.info("[DevDataSeeder] 샘플 역할 생성: roleCode={}", roleCode);
        }
    }

    private UserGroup findOrCreateGroup(String groupCode, String groupName, String defaultYn) {
        return userGroupRepository.findByGroupCodeAndDeletedYn(groupCode, "N")
                .orElseGet(() -> {
                    // "시스템관리" 는 UserGroupCodeBootstrapListener 가 시딩하는 USER_GROUP_CATEGORY
                    // 공통코드의 값 중 하나다 — TenantBootstrapListener와 동일하게 맞췄다.
                    UserGroup created = userGroupRepository.saveAndFlush(
                            UserGroup.create(groupCode, groupName, "시스템관리", null, defaultYn));
                    log.info("[DevDataSeeder] 샘플 사용자그룹 생성: groupCode={}", groupCode);
                    return created;
                });
    }

    private void addRoleIfAbsent(String userGroupId, String roleCode) {
        if (!userGroupRoleRepository.existsByUserGroupIdAndRoleCode(userGroupId, roleCode)) {
            userGroupRoleRepository.saveAndFlush(UserGroupRole.create(userGroupId, roleCode));
        }
    }

    private void createAdminUserIfAbsent(String adminGroupId) {
        User admin = userRepository.findByLoginIdAndDeletedYn("admin", "N")
                .orElseGet(() -> {
                    User created = userRepository.saveAndFlush(User.create(
                            "admin", passwordEncoder.encode("admin1234"), "admin@example.com", "관리자"));
                    log.info("[DevDataSeeder] 샘플 관리자 계정 생성 완료 -> companyCode=DEMO, loginId=admin, password=admin1234");
                    return created;
                });

        if (!userGroupUserRepository.existsByUserGroupIdAndUserId(adminGroupId, admin.getId())) {
            userGroupUserRepository.saveAndFlush(UserGroupUser.create(adminGroupId, admin.getId()));
            log.info("[DevDataSeeder] admin 계정을 ADMIN_GROUP에 배정 완료 (roles=USER/ADMIN/SYSTEM_ADMIN)");
        }
    }

    /**
     * 공지 등록 화면의 팝업 상세 설정(적용유형/팝업빈도/크기/팝업모양/위치) 선택지를 공통코드로
     * 채워둔다 — NoticeCodeBootstrapListener가 실제 POST /api/v1/companies 로 회사를 만들 때
     * 하는 것과 동일한 값이다(이 시더는 이벤트를 거치지 않아서 직접 호출해줘야 한다).
     */
    private void seedNoticePopupCodes() {
        seedCodeGroup(NoticeCodeBootstrapListener.GROUP_APPLY_TYPE, "공지 적용유형", List.of("로그인", "페이지"));
        seedCodeGroup(NoticeCodeBootstrapListener.GROUP_POPUP_FREQUENCY, "공지 팝업빈도", List.of("1회"));
        seedCodeGroup(NoticeCodeBootstrapListener.GROUP_POPUP_SIZE, "공지 팝업크기", List.of("소형", "중형", "대형"));
        seedCodeGroup(NoticeCodeBootstrapListener.GROUP_POPUP_SHAPE, "공지 팝업모양", List.of("레이어팝업", "윈도우팝업"));
        seedCodeGroup(NoticeCodeBootstrapListener.GROUP_POPUP_POSITION, "공지 팝업위치", List.of("중앙", "좌상단", "부모창"));
    }

    private void seedCodeGroup(String groupCode, String groupName, List<String> values) {
        CodeGroup group = codeGroupRepository.findByGroupCodeAndDeletedYn(groupCode, "N")
                .orElseGet(() -> {
                    CodeGroup created = codeGroupRepository.saveAndFlush(CodeGroup.create(groupCode, groupName, null));
                    log.info("[DevDataSeeder] 샘플 공통코드 그룹 생성: groupCode={}", groupCode);
                    return created;
                });

        int sortOrder = 0;
        for (String value : values) {
            if (!codeDetailRepository.existsByGroupIdAndCode(group.getId(), value)) {
                codeDetailRepository.saveAndFlush(CodeDetail.create(group.getId(), value, value, sortOrder, null));
            }
            sortOrder++;
        }
    }
}
