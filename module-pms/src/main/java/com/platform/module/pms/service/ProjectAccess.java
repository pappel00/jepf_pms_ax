package com.platform.module.pms.service;

import com.platform.framework.core.common.exception.BusinessException;
import com.platform.module.pms.domain.Project;
import com.platform.module.pms.domain.ProjectMember;
import com.platform.module.pms.exception.PmsErrorCode;
import com.platform.module.pms.repository.ProjectMemberRepository;
import com.platform.module.pms.repository.ProjectRepository;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import org.springframework.stereotype.Component;

/**
 * 프로젝트 단위 접근 판정(docs/pms/04-권한-매트릭스.md의 ◐ 참여 프로젝트 · ○ 본인 건).
 *  - 전체 열람: 시스템 호출(인증 없음), ADMIN/SYSTEM_ADMIN, 센터장(PMS_MANAGER·PMS_APPROVE).
 *  - 영업(PMS_SALES): 모든 프로젝트를 보지만 「멤버 외 열람 제한」 프로젝트는 멤버·영업 담당일 때만.
 *  - 그 외(PM·수행 인력·협력사·운영): 내가 멤버(수행/운영 팀)이거나 영업 담당·수행 PM 인 프로젝트만(PM 은 수주 단계 프로젝트도).
 *  - 쓰기(이슈·산출물 등): 프로젝트 관리자(관리자 멤버 또는 수행 PM) 또는 ADMIN/SYSTEM_ADMIN 만. 수행 인력은 조회와 본인 업무 진척 입력만 한다.
 */
@Component
public class ProjectAccess {

    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository memberRepository;

    public ProjectAccess(ProjectRepository projectRepository, ProjectMemberRepository memberRepository) {
        this.projectRepository = projectRepository;
        this.memberRepository = memberRepository;
    }

    /** 모든 프로젝트를 볼 수 있는 사용자인가(시스템 호출·관리자·센터장). */
    public boolean seesAll() {
        return PmsSupport.isSystemCall() || PmsSupport.hasAnyAuthority("ROLE_ADMIN", "ROLE_SYSTEM_ADMIN", "ROLE_PMS_MANAGER", "ROLE_PMS_APPROVE");
    }

    private boolean isAdmin() {
        return PmsSupport.isSystemCall() || PmsSupport.hasAnyAuthority("ROLE_ADMIN", "ROLE_SYSTEM_ADMIN");
    }

    /** 접근 가능한 프로젝트 ID 집합. null 이면 제한 없음(전체). */
    public Set<String> accessibleIds() {
        if (seesAll()) {
            return null;
        }
        String me = PmsSupport.currentUserId();
        boolean sales = PmsSupport.hasAnyAuthority("ROLE_PMS_SALES");
        boolean pm = PmsSupport.hasAnyAuthority("ROLE_PMS_PM");
        Set<String> memberOf = new HashSet<>();
        for (ProjectMember m : memberRepository.findAllByUserIdAndDeletedYn(me, "N")) {
            memberOf.add(m.getProjectId());
        }
        Set<String> out = new HashSet<>();
        for (Project p : projectRepository.findAll()) {
            if (!"N".equals(p.getDeletedYn())) {
                continue;
            }
            boolean mine = memberOf.contains(p.getId()) || me.equals(p.getSalesUserId()) || me.equals(p.getPmUserId());
            boolean restricted = "Y".equals(p.getViewRestrictedYn());
            // 수행 PM 후보는 수주 단계 프로젝트(수행단계전환 대상)를 볼 수 있다.
            boolean candidate = pm && "수주".equals(p.getStatusCode());
            if (mine || (sales && !restricted) || (candidate && !restricted)) {
                out.add(p.getId());
            }
        }
        return out;
    }

    /** 내가 참여한(멤버·영업 담당·수행 PM) 프로젝트 ID 집합. null 이면 제한 없음. 영업·PM 이라도 수행 단계 데이터(WBS·이슈·산출물 등)는 참여한 프로젝트만 본다. */
    public Set<String> myProjectIds() {
        if (seesAll()) {
            return null;
        }
        String me = PmsSupport.currentUserId();
        Set<String> out = new HashSet<>();
        for (ProjectMember m : memberRepository.findAllByUserIdAndDeletedYn(me, "N")) {
            out.add(m.getProjectId());
        }
        for (Project p : projectRepository.findAll()) {
            if ("N".equals(p.getDeletedYn()) && (me.equals(p.getSalesUserId()) || me.equals(p.getPmUserId()))) {
                out.add(p.getId());
            }
        }
        return out;
    }

    /** 참여한 프로젝트의 수행 단계 데이터만 남긴다(전체 열람이면 그대로). */
    public <T> List<T> filterMine(List<T> list, Function<T, String> projectIdOf) {
        Set<String> ids = myProjectIds();
        return ids == null ? list : list.stream().filter(x -> ids.contains(projectIdOf.apply(x))).toList();
    }

    /** 프로젝트 ID 로 목록을 거른다(전체 열람이면 그대로). */
    public <T> List<T> filter(List<T> list, Function<T, String> projectIdOf) {
        Set<String> ids = accessibleIds();
        return ids == null ? list : list.stream().filter(x -> ids.contains(projectIdOf.apply(x))).toList();
    }

    /** 접근할 수 없는 프로젝트면 거부한다. */
    public void requireAccess(String projectId) {
        Set<String> ids = accessibleIds();
        if (ids != null && !ids.contains(projectId)) {
            throw new BusinessException(PmsErrorCode.PROJECT_FORBIDDEN, "참여 중인 프로젝트만 접근할 수 있습니다.");
        }
    }

    /** 내가 이 프로젝트의 관리자(관리자 멤버·수행 PM)인가. */
    public boolean isManager(String projectId) {
        if (isAdmin()) {
            return true;
        }
        String me = PmsSupport.currentUserId();
        Project p = projectRepository.findById(projectId).orElse(null);
        if (p != null && me.equals(p.getPmUserId())) {
            return true;
        }
        return memberRepository.findAllByUserIdAndDeletedYn(me, "N").stream()
                .anyMatch(m -> projectId.equals(m.getProjectId()) && "Y".equals(m.getManagerYn()));
    }

    /** 프로젝트 관리자(또는 ADMIN)만 허용 — 이슈·산출물처럼 관리자만 등록·수정하는 일에 쓴다. */
    public void requireManager(String projectId, String what) {
        if (!isManager(projectId)) {
            throw new BusinessException(PmsErrorCode.PROJECT_FORBIDDEN, what + "은(는) 프로젝트 관리자만 할 수 있습니다.");
        }
    }
}
