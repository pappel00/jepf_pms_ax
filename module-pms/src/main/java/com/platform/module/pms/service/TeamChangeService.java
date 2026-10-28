package com.platform.module.pms.service;

import com.platform.framework.core.common.exception.BusinessException;
import com.platform.framework.core.common.exception.TenantException;
import com.platform.module.pms.domain.Project;
import com.platform.module.pms.domain.TeamChange;
import com.platform.module.pms.dto.TeamChangeDto;
import com.platform.module.pms.dto.TeamDto;
import com.platform.module.pms.exception.PmsErrorCode;
import com.platform.module.pms.repository.ProjectRepository;
import com.platform.module.pms.repository.TeamChangeRepository;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 투입 계획 변경 요청 → 승인 → 반영. 프로젝트 관리자·투입 인력을 바꾸려면 변경 요청(사유 필수)을 만들고, 승인자가 승인해야
 * 팀이 바뀐다. 요청자 본인은 승인/반려할 수 없다. 모든 요청은 변경 전/후 스냅샷과 함께 이력으로 남는다.
 */
@Service
@Transactional(readOnly = true)
public class TeamChangeService {

    static final String KIND = ProjectMemberService.KIND_EXEC;
    static final String PENDING = "승인 대기";

    private final TeamChangeRepository changeRepository;
    private final ProjectRepository projectRepository;
    private final ProjectService projectService;
    private final ProjectMemberService memberService;
    private final UserNameResolver names;

    public TeamChangeService(TeamChangeRepository changeRepository, ProjectRepository projectRepository,
                             ProjectService projectService, ProjectMemberService memberService, UserNameResolver names) {
        this.changeRepository = changeRepository;
        this.projectRepository = projectRepository;
        this.projectService = projectService;
        this.memberService = memberService;
        this.names = names;
    }

    @Transactional
    public TeamChangeDto.Response request(String projectId, TeamChangeDto.Request request) {
        Project project = projectService.requireProject(projectId);
        if (!"수행 중".equals(project.getStatusCode())) {
            throw new BusinessException(PmsErrorCode.TEAM_CHANGE_INVALID, "수행 중인 프로젝트만 투입 계획 변경을 요청할 수 있습니다: " + project.getStatusCode());
        }
        if (changeRepository.existsByProjectIdAndStatusCodeAndDeletedYn(projectId, PENDING, "N")) {
            throw new BusinessException(PmsErrorCode.TEAM_CHANGE_INVALID, "승인 대기 중인 변경 요청이 있어 새로 요청할 수 없습니다.");
        }
        TeamDto.Spec before = memberService.currentSpec(projectId, KIND);
        if (before == null) {
            throw new BusinessException(PmsErrorCode.TEAM_CHANGE_INVALID, "현재 투입 계획이 없습니다.");
        }
        before = new TeamDto.Spec(before.manager(), before.team(), "Y".equals(project.getViewRestrictedYn()));

        String code = PmsSupport.nextCode("CHG-" + java.time.Year.now().getValue() + "-",
                changeRepository.findAll().stream().map(TeamChange::getChangeCode).toList(), 3);
        TeamChange c = TeamChange.create().withChangeCode(code).withProjectId(projectId).withStatusCode(PENDING)
                .withRequesterUserId(PmsSupport.currentUserId()).withRequestedAt(LocalDateTime.now())
                .withReason(request.reason().trim()).withBeforeJson(PmsJson.write(before)).withAfterJson(PmsJson.write(request.after()))
                .withSummaryText(request.summaryText()).withConflictJson(PmsSupport.blankToNull(request.conflictJson()));
        return toResponse(changeRepository.saveAndFlush(c), project);
    }

    public List<TeamChangeDto.Response> getList(String projectId, String status) {
        List<TeamChange> list = changeRepository.search(nz(projectId), nz(status));
        Set<String> userIds = new HashSet<>();
        Set<String> projectIds = new HashSet<>();
        for (TeamChange c : list) {
            userIds.add(c.getRequesterUserId());
            userIds.add(c.getDecidedBy());
            projectIds.add(c.getProjectId());
        }
        Map<String, String> userNames = names.resolve(userIds);
        Map<String, Project> projects = new HashMap<>();
        projectRepository.findAllById(projectIds).forEach(p -> projects.put(p.getId(), p));
        return list.stream().map(c -> {
            Project p = projects.get(c.getProjectId());
            return TeamChangeDto.Response.of(c, p == null ? null : p.getProjectCode(), p == null ? null : p.getProjectName(), userNames,
                    PmsJson.readMap(c.getBeforeJson()), PmsJson.readMap(c.getAfterJson()));
        }).toList();
    }

    @Transactional
    public TeamChangeDto.Response approve(String id, String comment) {
        TeamChange c = findPendingOrThrow(id);
        String me = PmsSupport.currentUserId();
        if (me.equals(c.getRequesterUserId())) {
            throw new BusinessException(PmsErrorCode.TEAM_CHANGE_INVALID, "요청자 본인은 승인/반려할 수 없습니다.");
        }
        Project project = projectService.requireProject(c.getProjectId());
        if (!"수행 중".equals(project.getStatusCode())) {
            throw new BusinessException(PmsErrorCode.TEAM_CHANGE_INVALID, "수행 중인 프로젝트가 아니어서 반영할 수 없습니다: " + project.getStatusCode());
        }
        TeamDto.Spec after = PmsJson.read(c.getAfterJson(), TeamDto.Spec.class);
        memberService.replaceTeam(c.getProjectId(), KIND, after);
        String pm = "PM".equals(after.manager().roleCode()) ? after.manager().userId() : null;
        projectService.updateExecutionSettings(c.getProjectId(), pm, Boolean.TRUE.equals(after.restricted()));
        c.withStatusCode("승인").withDecidedBy(me).withDecidedAt(LocalDateTime.now())
                .withDecisionComment(PmsSupport.blankToNull(comment));
        return toResponse(c, project);
    }

    @Transactional
    public TeamChangeDto.Response reject(String id, String comment) {
        if (comment == null || comment.isBlank()) {
            throw new BusinessException(PmsErrorCode.TEAM_CHANGE_INVALID, "반려 사유를 입력하세요.");
        }
        TeamChange c = findPendingOrThrow(id);
        String me = PmsSupport.currentUserId();
        if (me.equals(c.getRequesterUserId())) {
            throw new BusinessException(PmsErrorCode.TEAM_CHANGE_INVALID, "요청자 본인은 승인/반려할 수 없습니다.");
        }
        c.withStatusCode("반려").withDecidedBy(me).withDecidedAt(LocalDateTime.now()).withDecisionComment(comment.trim());
        return toResponse(c, projectRepository.findById(c.getProjectId()).orElse(null));
    }

    @Transactional
    public TeamChangeDto.Response cancel(String id) {
        TeamChange c = findPendingOrThrow(id);
        String me = PmsSupport.currentUserId();
        if (!me.equals(c.getRequesterUserId())) {
            throw new BusinessException(PmsErrorCode.TEAM_CHANGE_INVALID, "요청자 본인만 취소할 수 있습니다.");
        }
        c.withStatusCode("취소").withDecidedBy(me).withDecidedAt(LocalDateTime.now());
        return toResponse(c, projectRepository.findById(c.getProjectId()).orElse(null));
    }

    private TeamChange findPendingOrThrow(String id) {
        TeamChange c = changeRepository.findByIdAndDeletedYn(id, "N")
                .orElseThrow(() -> new TenantException(PmsErrorCode.TEAM_CHANGE_NOT_FOUND, "존재하지 않는 변경 요청입니다: " + id));
        if (!PENDING.equals(c.getStatusCode())) {
            throw new BusinessException(PmsErrorCode.TEAM_CHANGE_INVALID, "승인 대기 상태가 아닙니다: " + c.getStatusCode());
        }
        return c;
    }

    private TeamChangeDto.Response toResponse(TeamChange c, Project p) {
        Set<String> userIds = new HashSet<>();
        userIds.add(c.getRequesterUserId());
        userIds.add(c.getDecidedBy());
        return TeamChangeDto.Response.of(c, p == null ? null : p.getProjectCode(), p == null ? null : p.getProjectName(),
                names.resolve(userIds), PmsJson.readMap(c.getBeforeJson()), PmsJson.readMap(c.getAfterJson()));
    }

    private static String nz(String s) {
        return s == null ? "" : s.trim();
    }
}