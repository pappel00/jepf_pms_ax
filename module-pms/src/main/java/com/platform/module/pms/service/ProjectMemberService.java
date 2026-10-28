package com.platform.module.pms.service;

import com.platform.framework.core.common.exception.InvalidRequestException;
import com.platform.framework.core.common.exception.TenantException;
import com.platform.framework.core.common.response.ErrorDetail;
import com.platform.module.pms.domain.MemberGap;
import com.platform.module.pms.domain.MemberMonthPlan;
import com.platform.module.pms.domain.MemberPhase;
import com.platform.module.pms.domain.Project;
import com.platform.module.pms.domain.ProjectMember;
import com.platform.module.pms.domain.UserProfile;
import com.platform.module.pms.dto.TeamDto;
import com.platform.module.pms.exception.PmsErrorCode;
import com.platform.module.pms.repository.MemberGapRepository;
import com.platform.module.pms.repository.MemberMonthPlanRepository;
import com.platform.module.pms.repository.MemberPhaseRepository;
import com.platform.module.pms.repository.ProjectMemberRepository;
import com.platform.module.pms.repository.ProjectRepository;
import com.platform.module.pms.repository.UserProfileRepository;
import com.platform.module.user.domain.User;
import com.platform.module.user.repository.UserRepository;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 프로젝트 관리자·투입 인력(팀). 수행(EXEC)과 운영(OPS)을 같은 테이블로 두고 kind 로 구분한다.
 * 팀 변경은 직접 수정하지 않고 단계전환(최초 지정)·투입 계획 변경 요청(승인) 흐름에서만 replaceTeam 으로 통째로 바꾼다.
 *
 * 관리자도 멤버 행 1개를 갖는다(manager_yn=Y). 관리자가 투입 인력에 없으면 role_code=MANAGER_ONLY 인 행으로 둔다.
 * 교체는 물리 삭제 후 재생성이다 — (프로젝트, 구분, 사용자) 유니크 제약이 삭제된 행까지 포함하기 때문이며,
 * 변경 이력은 투입 계획 변경 요청(before/after 스냅샷)에 남는다.
 */
@Service
@Transactional(readOnly = true)
public class ProjectMemberService {

    public static final String KIND_EXEC = "EXEC";
    static final String MANAGER_ONLY = "MANAGER_ONLY";

    private final ProjectAccess access;
    private final ProjectMemberRepository memberRepository;
    private final MemberPhaseRepository phaseRepository;
    private final MemberGapRepository gapRepository;
    private final MemberMonthPlanRepository monthPlanRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final UserProfileRepository profileRepository;

    public ProjectMemberService(ProjectAccess access, ProjectMemberRepository memberRepository, MemberPhaseRepository phaseRepository,
                                MemberGapRepository gapRepository, MemberMonthPlanRepository monthPlanRepository,
                                ProjectRepository projectRepository, UserRepository userRepository,
                                UserProfileRepository profileRepository) {
        this.access = access;
        this.memberRepository = memberRepository;
        this.phaseRepository = phaseRepository;
        this.gapRepository = gapRepository;
        this.monthPlanRepository = monthPlanRepository;
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
    }

    // ---------- 조회 ----------

    public TeamDto.TeamResponse getTeam(String projectId, String kind) {
        Project project = projectRepository.findByIdAndDeletedYn(projectId, "N")
                .orElseThrow(() -> new TenantException(PmsErrorCode.PROJECT_NOT_FOUND, "존재하지 않는 프로젝트입니다: " + projectId));
        List<ProjectMember> rows = memberRepository.findAllByProjectIdAndKindCodeOrderByCreatedDateAsc(projectId, kind);
        access.requireAccess(projectId);
        return buildTeams(List.of(project), rows).getOrDefault(projectId, emptyTeam(project));
    }

    /** 구분(kind)별 모든 프로젝트의 팀 — 프로젝트 목록 화면이 한 번에 읽는다. 팀이 있는 프로젝트만 돌려준다. */
    public List<TeamDto.TeamResponse> listTeams(String kind) {
        List<ProjectMember> rows = memberRepository.findAllByKindCodeOrderByCreatedDateAsc(kind);
        Set<String> projectIds = new HashSet<>();
        rows.forEach(r -> projectIds.add(r.getProjectId()));
        List<Project> projects = projectRepository.findAllById(projectIds).stream().filter(p -> "N".equals(p.getDeletedYn())).toList();
        return access.filter(new ArrayList<>(buildTeams(projects, rows).values()), TeamDto.TeamResponse::projectId);
    }

    /** 현재 팀을 요청 형태(Spec)로 — 투입 계획 변경 요청의 before 스냅샷용. */
    public TeamDto.Spec currentSpec(String projectId, String kind) {
        TeamDto.TeamResponse team = getTeam(projectId, kind);
        if (team.manager() == null) {
            return null;
        }
        List<TeamDto.Member> members = team.team().stream().map(m -> new TeamDto.Member(m.userId(), m.roleCode(), m.phases(), m.mm(),
                m.ratePct(), m.fromDate(), m.toDate(), m.autoEnd(), m.accessLevel(), m.notifyChannel(), m.gaps(), m.monthPlan())).toList();
        return new TeamDto.Spec(new TeamDto.Manager(team.manager().userId(), team.manager().roleCode()), members, team.restricted());
    }

    // ---------- 변경 ----------

    /** 팀 전체 교체(관리자 + 투입 인력). 사용자 존재·중복을 검증한다. */
    @Transactional
    public void replaceTeam(String projectId, String kind, TeamDto.Spec spec) {
        validate(spec);
        deleteTeam(projectId, kind);

        Map<String, ProjectMember> byUser = new LinkedHashMap<>();
        for (TeamDto.Member m : spec.team()) {
            ProjectMember row = ProjectMember.create().withProjectId(projectId).withKindCode(kind).withUserId(m.userId())
                    .withManagerYn("N").withRoleCode(m.roleCode()).withMm(m.mm()).withRatePct(m.ratePct())
                    .withFromDate(m.fromDate()).withToDate(m.toDate()).withAutoEndYn(Boolean.TRUE.equals(m.autoEnd()) ? "Y" : "N")
                    .withAccessLevelCode(m.accessLevel() == null || m.accessLevel().isBlank() ? "편집" : m.accessLevel())
                    .withNotifyChannelCode(PmsSupport.blankToNull(m.notifyChannel()));
            byUser.put(m.userId(), row);
        }
        ProjectMember managerRow = byUser.get(spec.manager().userId());
        if (managerRow == null) {
            managerRow = ProjectMember.create().withProjectId(projectId).withKindCode(kind).withUserId(spec.manager().userId())
                    .withManagerYn("Y").withRoleCode(MANAGER_ONLY).withAutoEndYn("N").withAccessLevelCode("편집");
            byUser.put(spec.manager().userId(), managerRow);
        }
        managerRow.withManagerYn("Y").withManagerRoleCode(spec.manager().roleCode());

        Map<String, TeamDto.Member> specByUser = new HashMap<>();
        spec.team().forEach(m -> specByUser.put(m.userId(), m));
        for (ProjectMember row : byUser.values()) {
            ProjectMember saved = memberRepository.saveAndFlush(row);
            TeamDto.Member m = specByUser.get(saved.getUserId());
            if (m == null) {
                continue;
            }
            if (m.phases() != null) {
                new java.util.LinkedHashSet<>(m.phases()).forEach(ph ->
                        phaseRepository.save(MemberPhase.create().withMemberId(saved.getId()).withPhaseCode(ph)));
            }
            if (m.gaps() != null) {
                m.gaps().stream().filter(g -> g.fromDate() != null && g.toDate() != null).forEach(g ->
                        gapRepository.save(MemberGap.create().withMemberId(saved.getId()).withFromDate(g.fromDate()).withToDate(g.toDate())));
            }
            if (m.monthPlan() != null) {
                m.monthPlan().forEach((month, pct) -> monthPlanRepository.save(
                        MemberMonthPlan.create().withMemberId(saved.getId()).withPlanMonth(month).withRatePct(pct)));
            }
        }
    }

    /** 팀 삭제(롤백·교체용). */
    @Transactional
    public void deleteTeam(String projectId, String kind) {
        List<ProjectMember> old = memberRepository.findAllByProjectIdAndKindCodeOrderByCreatedDateAsc(projectId, kind);
        if (old.isEmpty()) {
            return;
        }
        List<String> ids = old.stream().map(ProjectMember::getId).toList();
        phaseRepository.deleteAllByMemberIdIn(ids);
        gapRepository.deleteAllByMemberIdIn(ids);
        monthPlanRepository.deleteAllByMemberIdIn(ids);
        memberRepository.deleteAll(old);
        memberRepository.flush();
    }

    // ---------- 내부 ----------

    private void validate(TeamDto.Spec spec) {
        List<ErrorDetail> errors = new ArrayList<>();
        Set<String> ids = new HashSet<>();
        ids.add(spec.manager().userId());
        Set<String> seen = new HashSet<>();
        for (TeamDto.Member m : spec.team()) {
            ids.add(m.userId());
            if (!seen.add(m.userId())) {
                errors.add(new ErrorDetail("team", m.userId(), "같은 사용자를 투입 인력에 두 번 지정할 수 없습니다."));
            }
            if (m.fromDate() != null && m.toDate() != null && m.toDate().isBefore(m.fromDate())) {
                errors.add(new ErrorDetail("team", m.userId(), "투입 기간의 종료일이 시작일보다 빠릅니다."));
            }
        }
        Set<String> found = userRepository.findAllById(ids).stream().filter(u -> "N".equals(u.getDeletedYn()))
                .map(User::getId).collect(Collectors.toSet());
        for (String id : ids) {
            if (!found.contains(id)) {
                errors.add(new ErrorDetail("team", id, "존재하지 않는 사용자입니다."));
            }
        }
        if (!errors.isEmpty()) {
            throw new InvalidRequestException(PmsErrorCode.PROJECT_INVALID, "프로젝트 관리자·투입 인력 입력값을 확인하세요.", errors);
        }
    }

    private TeamDto.TeamResponse emptyTeam(Project p) {
        return new TeamDto.TeamResponse(p.getId(), p.getProjectCode(), null, List.of(), "Y".equals(p.getViewRestrictedYn()));
    }

    private Map<String, TeamDto.TeamResponse> buildTeams(List<Project> projects, List<ProjectMember> rows) {
        Map<String, Project> projectById = new HashMap<>();
        projects.forEach(p -> projectById.put(p.getId(), p));
        List<String> memberIds = rows.stream().map(ProjectMember::getId).toList();

        Map<String, List<String>> phases = new HashMap<>();
        Map<String, List<TeamDto.Gap>> gaps = new HashMap<>();
        Map<String, Map<String, BigDecimal>> months = new HashMap<>();
        if (!memberIds.isEmpty()) {
            phaseRepository.findAllByMemberIdIn(memberIds).forEach(ph ->
                    phases.computeIfAbsent(ph.getMemberId(), k -> new ArrayList<>()).add(ph.getPhaseCode()));
            gapRepository.findAllByMemberIdIn(memberIds).forEach(g ->
                    gaps.computeIfAbsent(g.getMemberId(), k -> new ArrayList<>()).add(new TeamDto.Gap(g.getFromDate(), g.getToDate())));
            monthPlanRepository.findAllByMemberIdIn(memberIds).forEach(mp ->
                    months.computeIfAbsent(mp.getMemberId(), k -> new LinkedHashMap<>()).put(mp.getPlanMonth(), mp.getRatePct()));
        }
        Set<String> userIds = rows.stream().map(ProjectMember::getUserId).collect(Collectors.toSet());
        Map<String, String> names = new HashMap<>();
        userRepository.findAllById(userIds).forEach(u -> names.put(u.getId(), u.getUserName()));
        Map<String, UserProfile> profiles = new HashMap<>();
        profileRepository.findAllByDeletedYn("N").forEach(p -> profiles.put(p.getUserId(), p));

        // 수행 단계 순서(분석→설계→개발→테스트→운영)대로 정렬해서 내려준다.
        List<String> order = List.of("분석", "설계", "개발", "테스트", "운영");
        Map<String, TeamDto.ManagerResponse> managers = new HashMap<>();
        Map<String, List<TeamDto.MemberResponse>> teams = new LinkedHashMap<>();
        for (ProjectMember r : rows) {
            if ("Y".equals(r.getManagerYn())) {
                managers.put(r.getProjectId(), new TeamDto.ManagerResponse(r.getUserId(), names.get(r.getUserId()), r.getManagerRoleCode()));
            }
            if (MANAGER_ONLY.equals(r.getRoleCode())) {
                continue;
            }
            UserProfile profile = profiles.get(r.getUserId());
            List<String> ph = new ArrayList<>(phases.getOrDefault(r.getId(), List.of()));
            ph.sort((a, b) -> Integer.compare(order.indexOf(a), order.indexOf(b)));
            teams.computeIfAbsent(r.getProjectId(), k -> new ArrayList<>()).add(new TeamDto.MemberResponse(r.getUserId(),
                    names.get(r.getUserId()), profile == null ? "자사" : profile.getOrgTypeCode(),
                    profile == null ? null : profile.getPartnerCompany(), r.getRoleCode(), ph, r.getMm(), r.getRatePct(),
                    r.getFromDate(), r.getToDate(), "Y".equals(r.getAutoEndYn()), r.getAccessLevelCode(), r.getNotifyChannelCode(),
                    gaps.getOrDefault(r.getId(), List.of()), months.getOrDefault(r.getId(), Map.of())));
        }
        Map<String, TeamDto.TeamResponse> out = new LinkedHashMap<>();
        for (Project p : projects) {
            if (!managers.containsKey(p.getId()) && !teams.containsKey(p.getId())) {
                continue;
            }
            out.put(p.getId(), new TeamDto.TeamResponse(p.getId(), p.getProjectCode(), managers.get(p.getId()),
                    teams.getOrDefault(p.getId(), List.of()), "Y".equals(p.getViewRestrictedYn())));
        }
        return out;
    }
}
