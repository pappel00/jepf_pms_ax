package com.platform.module.pms.event;

import com.platform.framework.security.access.SystemRoles;
import com.platform.framework.security.tenant.TenantContext;
import com.platform.module.tenant.event.TenantCreatedEvent;
import com.platform.module.user.domain.Role;
import com.platform.module.user.domain.UserGroup;
import com.platform.module.user.domain.UserGroupRole;
import com.platform.module.user.repository.RoleRepository;
import com.platform.module.user.repository.UserGroupRepository;
import com.platform.module.user.repository.UserGroupRoleRepository;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * PMS 권한 기본값 시딩 — 메뉴 접근 Role, 행위 Role, 사용자그룹과 그룹 ↔ Role 매핑(docs/pms/04-권한-매트릭스.md).
 * 새 테넌트가 생기면 채운다(멱등, find-or-create). 페이지 ↔ Role 매핑은 PortalBootstrapListener 가 맡는다.
 * 권한 자체는 프레임워크의 Role/사용자그룹 관리 화면에서 바꾼다. seedDefaults()는 개발용 시더가 직접 호출한다.
 */
@Component
public class PmsAccessBootstrapListener {

    private static final Logger log = LoggerFactory.getLogger(PmsAccessBootstrapListener.class);

    // 메뉴 접근 Role
    public static final String SALES = "ROLE_PMS_SALES";
    public static final String PM = "ROLE_PMS_PM";
    public static final String MEMBER = "ROLE_PMS_MEMBER";
    public static final String PARTNER = "ROLE_PMS_PARTNER";
    public static final String OPS = "ROLE_PMS_OPS";
    public static final String MANAGER = "ROLE_PMS_MANAGER";
    // 행위 Role
    public static final String APPROVE = "ROLE_PMS_APPROVE";
    public static final String COST_VIEW = "ROLE_PMS_COST_VIEW";

    private record RoleDef(String code, String name, String alias, String description) {
    }

    private record GroupDef(String code, String name, List<String> roles) {
    }

    private static final String CATEGORY = "PMS";

    private static final List<RoleDef> ROLES = List.of(
            new RoleDef(SALES, "PMS 영업/기획", "PMS Sales", "고객·RFP·제안서 담당. 영업 단계 메뉴."),
            new RoleDef(PM, "PMS 수행 PM", "PMS PM", "프로젝트 관리자(자격). 관리 범위는 프로젝트 멤버로 판정."),
            new RoleDef(MEMBER, "PMS 수행 인력", "PMS Member", "자사 투입 인력. 진척 입력, 이슈·산출물은 조회."),
            new RoleDef(PARTNER, "PMS 협력사 인력", "PMS Partner", "협력사 인력. 자기 업무·진척만."),
            new RoleDef(OPS, "PMS 운영 담당", "PMS Ops", "운영 단계 프로젝트 담당."),
            new RoleDef(MANAGER, "PMS 센터장/경영", "PMS Manager", "전체 현황 조회와 승인."),
            new RoleDef(APPROVE, "PMS 승인·확정", "PMS Approve", "단계전환·투입 변경 승인, RFP 확정 M/D, 재평가 요청."),
            new RoleDef(COST_VIEW, "PMS 원가 열람", "PMS Cost View", "예상 원가·인건비·단가·이익률 열람."));

    private static final List<GroupDef> GROUPS = List.of(
            new GroupDef("PMS_SALES", "PMS 영업/기획", List.of(SystemRoles.USER, SALES)),
            new GroupDef("PMS_PM", "PMS 수행 PM", List.of(SystemRoles.USER, PM, COST_VIEW)),
            new GroupDef("PMS_MEMBER", "PMS 수행 인력", List.of(SystemRoles.USER, MEMBER)),
            new GroupDef("PMS_PARTNER", "PMS 협력사 인력", List.of(SystemRoles.USER, PARTNER)),
            new GroupDef("PMS_OPS", "PMS 운영 담당", List.of(SystemRoles.USER, OPS)),
            new GroupDef("PMS_MANAGER", "PMS 센터장/경영", List.of(SystemRoles.USER, MANAGER, APPROVE, COST_VIEW)));

    private final RoleRepository roleRepository;
    private final UserGroupRepository userGroupRepository;
    private final UserGroupRoleRepository userGroupRoleRepository;

    public PmsAccessBootstrapListener(RoleRepository roleRepository, UserGroupRepository userGroupRepository,
                                      UserGroupRoleRepository userGroupRoleRepository) {
        this.roleRepository = roleRepository;
        this.userGroupRepository = userGroupRepository;
        this.userGroupRoleRepository = userGroupRoleRepository;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onTenantCreated(TenantCreatedEvent event) {
        TenantContext.setTenantId(event.getTenantId());
        try {
            seedDefaults();
            log.info("[PmsAccessBootstrapListener] companyCode={} PMS Role·사용자그룹 부트스트랩 완료", event.getCompanyCode());
        } finally {
            TenantContext.clear();
        }
    }

    /** 현재 TenantContext 테넌트에 PMS Role·사용자그룹·그룹 Role 을 채운다(이미 있는 행은 건드리지 않음). */
    public void seedDefaults() {
        for (RoleDef r : ROLES) {
            if (!roleRepository.existsByRoleCode(r.code())) {
                roleRepository.saveAndFlush(Role.create(r.code(), r.name(), r.alias(), CATEGORY, r.description()));
            }
        }
        for (GroupDef g : GROUPS) {
            UserGroup group = userGroupRepository.findByGroupCodeAndDeletedYn(g.code(), "N")
                    .orElseGet(() -> userGroupRepository.saveAndFlush(UserGroup.create(g.code(), g.name(), "시스템관리", null, "N")));
            for (String role : g.roles()) {
                if (!userGroupRoleRepository.existsByUserGroupIdAndRoleCode(group.getId(), role)) {
                    userGroupRoleRepository.saveAndFlush(UserGroupRole.create(group.getId(), role));
                }
            }
        }
    }
}
