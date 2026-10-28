package com.platform.module.pms.service;

import com.platform.framework.core.common.exception.BusinessException;
import com.platform.framework.core.common.exception.TenantException;
import com.platform.module.pms.domain.Project;
import com.platform.module.pms.domain.Transition;
import com.platform.module.pms.dto.TransitionDto;
import com.platform.module.pms.exception.PmsErrorCode;
import com.platform.module.pms.repository.ProjectRepository;
import com.platform.module.pms.repository.TransitionRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 수행단계전환 요청 → 승인(센터장·admin) → 수행 중, 그리고 롤백.
 * 요청은 바로 반영하지 않고 승인 대기로 두며, 승인하면 프로젝트 관리자·투입 인력을 확정하고 프로젝트를 「수행 중」으로 바꾼다.
 * (WBS·문서 테일러링 기초데이터는 WBS 단계 서버 구현 전까지 화면이 selection 으로 생성한다.)
 */
@Service
@Transactional(readOnly = true)
public class TransitionService {

    static final String KIND_EXEC = ProjectMemberService.KIND_EXEC;
    static final String PENDING = "승인 대기";

    private final ProjectAccess access;
    private final TransitionRepository transitionRepository;
    private final ProjectRepository projectRepository;
    private final ProjectService projectService;
    private final ProjectMemberService memberService;
    private final WbsService wbsService;
    private final DocTailoringService docService;
    private final UserNameResolver names;

    public TransitionService(ProjectAccess access, TransitionRepository transitionRepository, ProjectRepository projectRepository,
                             ProjectService projectService, ProjectMemberService memberService, WbsService wbsService,
                             DocTailoringService docService, UserNameResolver names) {
        this.access = access;
        this.wbsService = wbsService;
        this.docService = docService;
        this.transitionRepository = transitionRepository;
        this.projectRepository = projectRepository;
        this.projectService = projectService;
        this.memberService = memberService;
        this.names = names;
    }

    @Transactional
    public TransitionDto.Response request(String projectId, TransitionDto.Request request) {
        Project project = projectService.requireProject(projectId);
        if (!"수주".equals(project.getStatusCode())) {
            throw new BusinessException(PmsErrorCode.TRANSITION_INVALID, "수주 상태의 프로젝트만 수행단계전환을 요청할 수 있습니다: " + project.getStatusCode());
        }
        if (transitionRepository.existsByProjectIdAndKindCodeAndStatusCodeAndDeletedYn(projectId, KIND_EXEC, PENDING, "N")) {
            throw new BusinessException(PmsErrorCode.TRANSITION_INVALID, "이미 승인 대기 중인 단계전환 요청이 있습니다.");
        }
        validateTeam(request);

        String code = PmsSupport.nextCode("TRN-" + java.time.Year.now().getValue() + "-",
                transitionRepository.findAll().stream().map(Transition::getTransitionCode).toList(), 3);
        Transition t = Transition.create().withTransitionCode(code).withProjectId(projectId).withKindCode(KIND_EXEC)
                .withStatusCode(PENDING).withRequesterUserId(PmsSupport.currentUserId()).withRequestedAt(LocalDateTime.now())
                .withSelectionJson(PmsJson.write(request)).withSummary(PmsSupport.blankToNull(request.summary()))
                .withRfpId(PmsSupport.blankToNull(request.rfpId())).withItemCount(request.itemCount());
        return toResponse(transitionRepository.saveAndFlush(t), project);
    }

    public List<TransitionDto.Response> getList(String projectId, String status, String kind) {
        List<Transition> list = access.filter(transitionRepository.search(nz(projectId), nz(status), nz(kind)), Transition::getProjectId);
        return toResponses(list);
    }

    public TransitionDto.Response getById(String id) {
        Transition t = findOrThrow(id);
        return toResponse(t, projectRepository.findById(t.getProjectId()).orElse(null));
    }

    /** 승인 — 팀 확정, 프로젝트 「수행 중」 전환. */
    @Transactional
    public TransitionDto.Response approve(String id) {
        Transition t = findPendingOrThrow(id);
        Project project = projectService.requireProject(t.getProjectId());
        if (!"수주".equals(project.getStatusCode())) {
            throw new BusinessException(PmsErrorCode.TRANSITION_INVALID, "수주 상태가 아니어서 승인할 수 없습니다: " + project.getStatusCode());
        }
        TransitionDto.Request request = PmsJson.read(t.getSelectionJson(), TransitionDto.Request.class);
        memberService.replaceTeam(t.getProjectId(), KIND_EXEC, request.team());
        String pm = "PM".equals(request.team().manager().roleCode()) ? request.team().manager().userId() : null;
        projectService.startExecution(t.getProjectId(), LocalDate.now(), pm, Boolean.TRUE.equals(request.team().restricted()),
                "단계전환 승인 " + t.getTransitionCode());
        t.withStatusCode("승인").withApproverUserId(PmsSupport.currentUserId()).withDecidedAt(LocalDateTime.now());
        return toResponse(t, projectRepository.findById(t.getProjectId()).orElse(null));
    }

    @Transactional
    public TransitionDto.Response reject(String id, String comment) {
        if (comment == null || comment.isBlank()) {
            throw new BusinessException(PmsErrorCode.TRANSITION_INVALID, "반려 사유를 입력하세요.");
        }
        Transition t = findPendingOrThrow(id);
        t.withStatusCode("반려").withApproverUserId(PmsSupport.currentUserId()).withDecidedAt(LocalDateTime.now())
                .withDecisionComment(comment.trim());
        return toResponse(t, projectRepository.findById(t.getProjectId()).orElse(null));
    }

    /** 요청 취소 — 요청자 본인만. */
    @Transactional
    public TransitionDto.Response cancel(String id) {
        Transition t = findPendingOrThrow(id);
        if (!PmsSupport.currentUserId().equals(t.getRequesterUserId())) {
            throw new BusinessException(PmsErrorCode.TRANSITION_INVALID, "요청자 본인만 취소할 수 있습니다.");
        }
        t.withStatusCode("취소").withApproverUserId(PmsSupport.currentUserId()).withDecidedAt(LocalDateTime.now());
        return toResponse(t, projectRepository.findById(t.getProjectId()).orElse(null));
    }

    /**
     * 롤백 — 승인된 전환을 취소하고 프로젝트를 「수주」로 되돌린다(팀 삭제).
     * 진척·이슈가 있는지는 WBS·이슈가 서버에 올라오기 전까지 화면이 확인한 뒤 호출한다.
     */
    @Transactional
    public TransitionDto.Response rollback(String id, String reason) {
        Transition t = findOrThrow(id);
        if (!"승인".equals(t.getStatusCode())) {
            throw new BusinessException(PmsErrorCode.TRANSITION_INVALID, "승인된 단계전환만 롤백할 수 있습니다: " + t.getStatusCode());
        }
        Project project = projectService.requireProject(t.getProjectId());
        if (!"수행 중".equals(project.getStatusCode())) {
            throw new BusinessException(PmsErrorCode.TRANSITION_INVALID, "수행 중인 프로젝트만 롤백할 수 있습니다: " + project.getStatusCode());
        }
        wbsService.deleteForRollback(t.getProjectId());
        docService.deleteForRollback(t.getProjectId());
        memberService.deleteTeam(t.getProjectId(), KIND_EXEC);
        projectService.rollbackExecution(t.getProjectId(), reason);
        t.withStatusCode("롤백").withRolledBackBy(PmsSupport.currentUserId()).withRolledBackAt(LocalDateTime.now())
                .withRollbackReason(reason);
        return toResponse(t, projectRepository.findById(t.getProjectId()).orElse(null));
    }

    /**
     * 개발용 샘플 데이터 생성 전용 — 이미 승인·반영된 단계전환 이력을 만든다(팀 확정 + 전환 요청 행).
     * 프로젝트 상태·전환일은 호출하는 쪽(시더)이 이미 맞춰 둔 것으로 본다. 운영 코드에서는 호출하지 않는다.
     */
    @Transactional
    public void seedApproved(String projectId, TransitionDto.Request request, LocalDate date, String requesterUserId, String approverUserId) {
        memberService.replaceTeam(projectId, KIND_EXEC, request.team());
        String code = PmsSupport.nextCode("TRN-" + date.getYear() + "-",
                transitionRepository.findAll().stream().map(Transition::getTransitionCode).toList(), 3);
        transitionRepository.saveAndFlush(Transition.create().withTransitionCode(code).withProjectId(projectId).withKindCode(KIND_EXEC)
                .withStatusCode("승인").withRequesterUserId(requesterUserId).withRequestedAt(date.atStartOfDay())
                .withApproverUserId(approverUserId).withDecidedAt(date.atStartOfDay().plusHours(1))
                .withSelectionJson(PmsJson.write(request)).withSummary(request.summary())
                .withRfpId(PmsSupport.blankToNull(request.rfpId())).withItemCount(request.itemCount()));
    }

    /** 운영단계전환처럼 별도 승인 절차 없이 바로 확정되는 전환의 이력을 「승인」 상태로 남긴다. 만든 전환 ID 를 돌려준다. */
    @Transactional
    public String recordApproved(String projectId, String kind, String selectionJson, String summary) {
        LocalDateTime now = LocalDateTime.now();
        String me = PmsSupport.currentUserId();
        String code = PmsSupport.nextCode("TRN-" + now.getYear() + "-",
                transitionRepository.findAll().stream().map(Transition::getTransitionCode).toList(), 3);
        return transitionRepository.saveAndFlush(Transition.create().withTransitionCode(code).withProjectId(projectId).withKindCode(kind)
                .withStatusCode("승인").withRequesterUserId(me).withRequestedAt(now).withApproverUserId(me).withDecidedAt(now)
                .withSelectionJson(selectionJson).withSummary(summary)).getId();
    }

    // ---------- 내부 ----------

    private void validateTeam(TransitionDto.Request request) {
        // 사용자 존재·중복 검증은 replaceTeam 과 같은 규칙 — 요청 시점에 먼저 확인해 승인 단계에서 실패하지 않게 한다.
        Set<String> ids = new HashSet<>();
        ids.add(request.team().manager().userId());
        request.team().team().forEach(m -> ids.add(m.userId()));
        Map<String, String> found = names.resolve(ids);
        for (String id : ids) {
            if (!found.containsKey(id)) {
                throw new TenantException(PmsErrorCode.USER_NOT_FOUND, "존재하지 않는 사용자입니다: " + id);
            }
        }
    }

    private Transition findOrThrow(String id) {
        return transitionRepository.findByIdAndDeletedYn(id, "N")
                .orElseThrow(() -> new TenantException(PmsErrorCode.TRANSITION_NOT_FOUND, "존재하지 않는 단계전환 요청입니다: " + id));
    }

    private Transition findPendingOrThrow(String id) {
        Transition t = findOrThrow(id);
        if (!PENDING.equals(t.getStatusCode())) {
            throw new BusinessException(PmsErrorCode.TRANSITION_INVALID, "승인 대기 상태가 아닙니다: " + t.getStatusCode());
        }
        return t;
    }

    private List<TransitionDto.Response> toResponses(List<Transition> list) {
        Set<String> userIds = new HashSet<>();
        Set<String> projectIds = new HashSet<>();
        for (Transition t : list) {
            userIds.add(t.getRequesterUserId());
            userIds.add(t.getApproverUserId());
            userIds.add(t.getRolledBackBy());
            projectIds.add(t.getProjectId());
        }
        Map<String, String> userNames = names.resolve(userIds);
        Map<String, Project> projects = new HashMap<>();
        projectRepository.findAllById(projectIds).forEach(p -> projects.put(p.getId(), p));
        return list.stream().map(t -> {
            Project p = projects.get(t.getProjectId());
            return TransitionDto.Response.of(t, p == null ? null : p.getProjectCode(), p == null ? null : p.getProjectName(),
                    userNames, PmsJson.readMap(t.getSelectionJson()));
        }).toList();
    }

    private TransitionDto.Response toResponse(Transition t, Project p) {
        Set<String> userIds = new HashSet<>();
        userIds.add(t.getRequesterUserId());
        userIds.add(t.getApproverUserId());
        userIds.add(t.getRolledBackBy());
        return TransitionDto.Response.of(t, p == null ? null : p.getProjectCode(), p == null ? null : p.getProjectName(),
                names.resolve(userIds), PmsJson.readMap(t.getSelectionJson()));
    }

    private static String nz(String s) {
        return s == null ? "" : s.trim();
    }
}