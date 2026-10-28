package com.platform.module.pms.service;

import com.platform.framework.core.common.exception.BusinessException;
import com.platform.framework.core.common.exception.TenantException;
import com.platform.module.pms.domain.NextOpportunity;
import com.platform.module.pms.domain.OpsIncident;
import com.platform.module.pms.domain.OpsOpportunity;
import com.platform.module.pms.domain.OpsTask;
import com.platform.module.pms.domain.Project;
import com.platform.module.pms.dto.OpsDto;
import com.platform.module.pms.exception.PmsErrorCode;
import com.platform.module.pms.repository.NextOpportunityRepository;
import com.platform.module.pms.repository.OpsIncidentRepository;
import com.platform.module.pms.repository.OpsOpportunityRepository;
import com.platform.module.pms.repository.OpsTaskRepository;
import com.platform.module.pms.repository.ProjectOperationRepository;
import com.platform.module.pms.repository.ProjectRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 차기 사업 기회(수행 단계) · 운영 업무 · 운영 이슈(장애·문의) · 운영 영업.
 *  - 반복 운영 업무를 「완료」하면 다음 주기(매주 +7일 / 매월 +1개월 / 분기 +3개월) 업무를 자동 생성한다(상태 대기, 이전 업무 연결).
 *    반복 종료일·운영 종료일을 넘기거나 같은 기한의 동일 업무가 있으면 만들지 않고 사유를 돌려준다.
 *  - 운영 이슈의 응답/해결 시각은 화면이 보낸 값을 저장한다(SLA 판정은 운영 정보의 목표 시간으로 화면이 계산, 저장 안 함).
 *  - 운영단계전환 때 선택한 차기 사업 기회는 운영 영업 기회로 이관한다(transferNext).
 */
@Service
@Transactional(readOnly = true)
public class OpsService {

    private static final Map<String, String> OPS_KIND = Map.of(
            "추가 기능·범위 외 요구", "유상 개선", "후속 단계(차기 사업)", "신규 제안", "고도화·개선", "추가 개발", "타 부서·계열사 확산", "증설·확장",
            "인프라·증설", "증설·확장", "운영·유지보수 전환", "재계약·갱신", "컨설팅·교육", "교육·컨설팅", "기타", "신규 제안");
    private static final Map<String, Integer> STAGE_PROB = Map.of("발굴", 10, "검토 중", 20, "제안 준비", 30, "제안 완료", 50);

    private final NextOpportunityRepository nextRepository;
    private final OpsTaskRepository taskRepository;
    private final OpsIncidentRepository incidentRepository;
    private final OpsOpportunityRepository oppRepository;
    private final ProjectOperationRepository operationRepository;
    private final ProjectRepository projectRepository;
    private final ProjectService projectService;
    private final UserNameResolver names;

    public OpsService(NextOpportunityRepository nextRepository, OpsTaskRepository taskRepository, OpsIncidentRepository incidentRepository,
                      OpsOpportunityRepository oppRepository, ProjectOperationRepository operationRepository,
                      ProjectRepository projectRepository, ProjectService projectService, UserNameResolver names) {
        this.nextRepository = nextRepository;
        this.taskRepository = taskRepository;
        this.incidentRepository = incidentRepository;
        this.oppRepository = oppRepository;
        this.operationRepository = operationRepository;
        this.projectRepository = projectRepository;
        this.projectService = projectService;
        this.names = names;
    }

    // ========== 차기 사업 기회 ==========

    public List<OpsDto.NextResponse> getNexts() {
        return nextResponses(nextRepository.findAllByDeletedYnOrderByOppCodeAsc("N"));
    }

    @Transactional
    public OpsDto.NextResponse createNext(String projectId, OpsDto.NextRequest r) {
        Project project = projectService.requireProject(projectId);
        String code = PmsSupport.nextCode("NB-", nextRepository.findAll().stream().map(NextOpportunity::getOppCode).toList(), 3);
        NextOpportunity n = applyNext(NextOpportunity.create().withOppCode(code).withProjectId(project.getId()), r);
        return nextResponses(List.of(nextRepository.saveAndFlush(n))).get(0);
    }

    @Transactional
    public OpsDto.NextResponse updateNext(String id, OpsDto.NextRequest r) {
        return nextResponses(List.of(applyNext(findNext(id), r))).get(0);
    }

    @Transactional
    public void deleteNext(String id) {
        findNext(id).markDeleted();
    }

    /** 운영단계전환 — 선택한 열린 차기 사업 기회를 운영 영업 기회로 이관한다(고객사 담당자·금액·예상 시기 유지, 수행 쪽은 「이관 완료」). */
    @Transactional
    public int transferNext(String projectId, List<String> ids) {
        int moved = 0;
        for (String id : ids) {
            NextOpportunity n = nextRepository.findByIdAndDeletedYn(id, "N").orElse(null);
            if (n == null || !n.getProjectId().equals(projectId) || "보류/실패".equals(n.getStageCode()) || "이관 완료".equals(n.getStageCode())) {
                continue;
            }
            String code = PmsSupport.nextCode("OS-", oppRepository.findAll().stream().map(OpsOpportunity::getOppCode).toList(), 3);
            String memo = n.getContent() == null || n.getContent().isBlank() ? "수행 단계 차기 사업 기회 " + n.getOppCode() + "에서 이관"
                    : "수행 단계 차기 사업 기회 " + n.getOppCode() + "에서 이관 — " + n.getContent();
            OpsOpportunity o = oppRepository.saveAndFlush(OpsOpportunity.create().withOppCode(code).withProjectId(projectId)
                    .withKindCode(OPS_KIND.getOrDefault(n.getKindCode(), "신규 제안")).withTitle(n.getTitle()).withStageCode("발굴")
                    .withAmount(n.getAmount()).withProbabilityPct(BigDecimal.valueOf(STAGE_PROB.getOrDefault(n.getStageCode(), 10)))
                    .withOwnerUserId(n.getOwnerUserId()).withExpectedDate(quarterEnd(n.getQuarter())).withNextAction(n.getNextAction())
                    .withNextDate(n.getNextDate()).withMemo(memo).withContactId(n.getRequesterContactId()).withSourceNextOppId(n.getId()));
            n.withStageCode("이관 완료").withOpsOpportunityId(o.getId());
            moved++;
        }
        return moved;
    }

    private static LocalDate quarterEnd(String q) {
        if (q == null || !q.matches("\\d{4}-Q[1-4]")) {
            return null;
        }
        int year = Integer.parseInt(q.substring(0, 4));
        int quarter = q.charAt(6) - '0';
        return YearMonth.of(year, quarter * 3).atEndOfMonth();
    }

    private NextOpportunity applyNext(NextOpportunity n, OpsDto.NextRequest r) {
        return n.withTitle(r.title().trim()).withKindCode(r.kindCode()).withSourceCode(r.sourceCode()).withContent(PmsSupport.blankToNull(r.content()))
                .withRequesterContactId(PmsSupport.blankToNull(r.requesterContactId()))
                .withDecisionMakerContactId(PmsSupport.blankToNull(r.decisionMakerContactId())).withBudgetCode(PmsSupport.blankToNull(r.budgetCode()))
                .withEstMm(r.estMm()).withAmount(r.amount()).withQuarter(PmsSupport.blankToNull(r.quarter())).withStageCode(r.stageCode())
                .withPriorityCode(r.priorityCode()).withOwnerUserId(r.ownerUserId()).withIssueId(PmsSupport.blankToNull(r.issueId()))
                .withNextAction(PmsSupport.blankToNull(r.nextAction())).withNextDate(r.nextDate())
                .withScopeReason(PmsSupport.blankToNull(r.scopeReason())).withMemo(PmsSupport.blankToNull(r.memo()));
    }

    private NextOpportunity findNext(String id) {
        return nextRepository.findByIdAndDeletedYn(id, "N")
                .orElseThrow(() -> new TenantException(PmsErrorCode.OPS_NOT_FOUND, "존재하지 않는 차기 사업 기회입니다: " + id));
    }

    private List<OpsDto.NextResponse> nextResponses(List<NextOpportunity> list) {
        Map<String, String> userNames = names.resolve(ids(list, NextOpportunity::getOwnerUserId));
        Map<String, String> codes = projectCodes(ids(list, NextOpportunity::getProjectId));
        return list.stream().map(n -> new OpsDto.NextResponse(n.getId(), n.getOppCode(), n.getProjectId(), codes.get(n.getProjectId()), n.getTitle(),
                n.getKindCode(), n.getSourceCode(), n.getContent(), n.getRequesterContactId(), n.getDecisionMakerContactId(), n.getBudgetCode(),
                n.getEstMm(), n.getAmount(), n.getQuarter(), n.getStageCode(), n.getPriorityCode(), n.getOwnerUserId(),
                userNames.get(n.getOwnerUserId()), n.getIssueId(), n.getNextAction(), n.getNextDate(), n.getScopeReason(), n.getMemo(),
                n.getOpsOpportunityId())).toList();
    }

    // ========== 운영 업무 ==========

    public List<OpsDto.TaskResponse> getTasks() {
        return taskResponses(taskRepository.findAllByDeletedYnOrderByDueDateAsc("N"), null, null);
    }

    @Transactional
    public OpsDto.TaskResponse createTask(String projectId, OpsDto.TaskRequest r) {
        Project project = projectService.requireProject(projectId);
        String code = PmsSupport.nextCode("OT-", taskRepository.findAll().stream().map(OpsTask::getTaskCode).toList(), 3);
        OpsTask t = applyTask(OpsTask.create().withTaskCode(code).withProjectId(project.getId()).withAutoCreatedYn("N"), r);
        taskRepository.saveAndFlush(t);
        return finishTask(t, "완료".equals(t.getStatusCode()));
    }

    @Transactional
    public OpsDto.TaskResponse updateTask(String id, OpsDto.TaskRequest r) {
        OpsTask t = findTask(id);
        boolean becameDone = !"완료".equals(t.getStatusCode()) && "완료".equals(r.statusCode());
        applyTask(t, r);
        return finishTask(t, becameDone);
    }

    @Transactional
    public OpsDto.TaskResponse setTaskStatus(String id, String status) {
        OpsTask t = findTask(id);
        boolean becameDone = !"완료".equals(t.getStatusCode()) && "완료".equals(status);
        t.withStatusCode(status);
        return finishTask(t, becameDone);
    }

    @Transactional
    public void deleteTask(String id) {
        findTask(id).markDeleted();
    }

    /** 완료로 바뀐 반복 업무의 다음 주기 업무를 만든다. */
    private OpsDto.TaskResponse finishTask(OpsTask t, boolean becameDone) {
        String spawned = null;
        String skipped = null;
        if (becameDone && t.getRepeatCode() != null && !"없음".equals(t.getRepeatCode())) {
            LocalDate due = nextDue(t.getDueDate(), t.getRepeatCode());
            LocalDate opsEnd = operationRepository.findByProjectIdAndDeletedYn(t.getProjectId(), "N").map(o -> o.getEndDate()).orElse(null);
            List<OpsTask> same = taskRepository.findAllByProjectIdAndDeletedYn(t.getProjectId(), "N");
            if (due == null) {
                skipped = null;
            } else if (same.stream().anyMatch(x -> t.getId().equals(x.getPrevTaskId()))) {
                skipped = "이미 다음 업무가 생성되었습니다.";
            } else if (t.getRepeatEndDate() != null && due.isAfter(t.getRepeatEndDate())) {
                skipped = "반복 종료일(" + t.getRepeatEndDate() + ")을 넘어 생성하지 않았습니다.";
            } else if (opsEnd != null && due.isAfter(opsEnd)) {
                skipped = "운영 종료일(" + opsEnd + ")을 넘어 생성하지 않았습니다.";
            } else if (same.stream().anyMatch(x -> x.getTitle().equals(t.getTitle()) && due.equals(x.getDueDate()))) {
                skipped = "같은 기한(" + due + ")의 동일 업무가 이미 있습니다.";
            } else {
                String code = PmsSupport.nextCode("OT-", taskRepository.findAll().stream().map(OpsTask::getTaskCode).toList(), 3);
                OpsTask next = taskRepository.saveAndFlush(OpsTask.create().withTaskCode(code).withProjectId(t.getProjectId()).withTitle(t.getTitle())
                        .withCategoryCode(t.getCategoryCode()).withAssigneeUserId(t.getAssigneeUserId()).withDueDate(due).withStatusCode("대기")
                        .withPriorityCode(t.getPriorityCode()).withRepeatCode(t.getRepeatCode()).withRepeatEndDate(t.getRepeatEndDate())
                        .withPrevTaskId(t.getId()).withAutoCreatedYn("Y").withMemo(t.getMemo()));
                spawned = next.getTaskCode();
            }
        }
        return taskResponses(List.of(t), spawned, skipped).get(0);
    }

    /** 다음 주기 기한 — 매주 +7일, 매월 +1개월, 분기 +3개월(말일 초과 시 말일). 반복이 아니면 null. */
    static LocalDate nextDue(LocalDate due, String repeat) {
        if (due == null) {
            return null;
        }
        return switch (repeat) {
            case "매주" -> due.plusDays(7);
            case "매월" -> due.plusMonths(1);
            case "분기" -> due.plusMonths(3);
            default -> null;
        };
    }

    private OpsTask applyTask(OpsTask t, OpsDto.TaskRequest r) {
        return t.withTitle(r.title().trim()).withCategoryCode(r.categoryCode()).withAssigneeUserId(r.assigneeUserId()).withDueDate(r.dueDate())
                .withStatusCode(r.statusCode()).withPriorityCode(r.priorityCode())
                .withRepeatCode(r.repeatCode() == null || r.repeatCode().isBlank() ? "없음" : r.repeatCode()).withRepeatEndDate(r.repeatEndDate())
                .withMemo(PmsSupport.blankToNull(r.memo())).withIncidentId(PmsSupport.blankToNull(r.incidentId()));
    }

    private OpsTask findTask(String id) {
        return taskRepository.findByIdAndDeletedYn(id, "N")
                .orElseThrow(() -> new TenantException(PmsErrorCode.OPS_NOT_FOUND, "존재하지 않는 운영 업무입니다: " + id));
    }

    private List<OpsDto.TaskResponse> taskResponses(List<OpsTask> list, String spawned, String skipped) {
        taskRepository.flush();
        Map<String, String> userNames = names.resolve(ids(list, OpsTask::getAssigneeUserId));
        Map<String, String> codes = projectCodes(ids(list, OpsTask::getProjectId));
        return list.stream().map(t -> new OpsDto.TaskResponse(t.getId(), t.getTaskCode(), t.getProjectId(), codes.get(t.getProjectId()), t.getTitle(),
                t.getCategoryCode(), t.getAssigneeUserId(), userNames.get(t.getAssigneeUserId()), t.getDueDate(), t.getStatusCode(), t.getPriorityCode(),
                t.getRepeatCode(), t.getRepeatEndDate(), t.getPrevTaskId(), "Y".equals(t.getAutoCreatedYn()), t.getMemo(), t.getIncidentId(),
                spawned, skipped)).toList();
    }

    // ========== 운영 이슈 ==========

    public List<OpsDto.IncidentResponse> getIncidents() {
        return incidentResponses(incidentRepository.findAllByDeletedYnOrderByReceivedAtDesc("N"));
    }

    @Transactional
    public OpsDto.IncidentResponse createIncident(String projectId, OpsDto.IncidentRequest r) {
        Project project = projectService.requireProject(projectId);
        String code = PmsSupport.nextCode("OI-", incidentRepository.findAll().stream().map(OpsIncident::getIncidentCode).toList(), 3);
        OpsIncident i = applyIncident(OpsIncident.create().withIncidentCode(code).withProjectId(project.getId()), r);
        return incidentResponses(List.of(incidentRepository.saveAndFlush(i))).get(0);
    }

    @Transactional
    public OpsDto.IncidentResponse updateIncident(String id, OpsDto.IncidentRequest r) {
        OpsIncident i = findIncident(id);
        if (("해결".equals(r.statusCode()) || "종결".equals(r.statusCode())) && (r.actionText() == null || r.actionText().isBlank())) {
            throw new BusinessException(PmsErrorCode.OPS_INVALID, "해결/종결 시 조치 내용을 기록하세요.");
        }
        return incidentResponses(List.of(applyIncident(i, r))).get(0);
    }

    @Transactional
    public OpsDto.IncidentResponse linkIncidentOpportunity(String id, String opportunityId) {
        OpsIncident i = findIncident(id);
        i.withOpportunityId(PmsSupport.blankToNull(opportunityId));
        return incidentResponses(List.of(i)).get(0);
    }

    @Transactional
    public void deleteIncident(String id) {
        findIncident(id).markDeleted();
    }

    private OpsIncident applyIncident(OpsIncident i, OpsDto.IncidentRequest r) {
        return i.withTypeCode(r.typeCode()).withTitle(r.title().trim()).withContent(PmsSupport.blankToNull(r.content())).withPriorityCode(r.priorityCode())
                .withStatusCode(r.statusCode()).withReporterName(r.reporterName().trim()).withAssigneeUserId(PmsSupport.blankToNull(r.assigneeUserId()))
                .withReceivedAt(r.receivedAt()).withRespondedAt(r.respondedAt()).withResolvedAt(r.resolvedAt())
                .withActionText(PmsSupport.blankToNull(r.actionText()));
    }

    private OpsIncident findIncident(String id) {
        return incidentRepository.findByIdAndDeletedYn(id, "N")
                .orElseThrow(() -> new TenantException(PmsErrorCode.OPS_NOT_FOUND, "존재하지 않는 운영 이슈입니다: " + id));
    }

    private List<OpsDto.IncidentResponse> incidentResponses(List<OpsIncident> list) {
        Map<String, String> userNames = names.resolve(ids(list, OpsIncident::getAssigneeUserId));
        Map<String, String> codes = projectCodes(ids(list, OpsIncident::getProjectId));
        return list.stream().map(i -> new OpsDto.IncidentResponse(i.getId(), i.getIncidentCode(), i.getProjectId(), codes.get(i.getProjectId()),
                i.getTypeCode(), i.getTitle(), i.getContent(), i.getPriorityCode(), i.getStatusCode(), i.getReporterName(), i.getAssigneeUserId(),
                i.getAssigneeUserId() == null ? null : userNames.get(i.getAssigneeUserId()), i.getReceivedAt(), i.getRespondedAt(), i.getResolvedAt(),
                i.getActionText(), i.getOpportunityId())).toList();
    }

    // ========== 운영 영업 ==========

    public List<OpsDto.OppResponse> getOpps() {
        return oppResponses(oppRepository.findAllByDeletedYnOrderByOppCodeAsc("N"));
    }

    @Transactional
    public OpsDto.OppResponse createOpp(String projectId, OpsDto.OppRequest r) {
        Project project = projectService.requireProject(projectId);
        String code = PmsSupport.nextCode("OS-", oppRepository.findAll().stream().map(OpsOpportunity::getOppCode).toList(), 3);
        OpsOpportunity o = applyOpp(OpsOpportunity.create().withOppCode(code).withProjectId(project.getId()), r);
        return oppResponses(List.of(oppRepository.saveAndFlush(o))).get(0);
    }

    @Transactional
    public OpsDto.OppResponse updateOpp(String id, OpsDto.OppRequest r) {
        return oppResponses(List.of(applyOpp(findOpp(id), r))).get(0);
    }

    @Transactional
    public void deleteOpp(String id) {
        findOpp(id).markDeleted();
    }

    private OpsOpportunity applyOpp(OpsOpportunity o, OpsDto.OppRequest r) {
        return o.withKindCode(r.kindCode()).withTitle(r.title().trim()).withStageCode(r.stageCode()).withAmount(r.amount())
                .withProbabilityPct(r.probabilityPct() == null ? BigDecimal.ZERO : r.probabilityPct()).withOwnerUserId(r.ownerUserId())
                .withExpectedDate(r.expectedDate()).withNextAction(PmsSupport.blankToNull(r.nextAction())).withNextDate(r.nextDate())
                .withMemo(PmsSupport.blankToNull(r.memo())).withSourceIncidentId(PmsSupport.blankToNull(r.sourceIncidentId()))
                .withContactId(PmsSupport.blankToNull(r.contactId())).withSourceNextOppId(PmsSupport.blankToNull(r.sourceNextOppId()));
    }

    private OpsOpportunity findOpp(String id) {
        return oppRepository.findByIdAndDeletedYn(id, "N")
                .orElseThrow(() -> new TenantException(PmsErrorCode.OPS_NOT_FOUND, "존재하지 않는 운영 영업 기회입니다: " + id));
    }

    private List<OpsDto.OppResponse> oppResponses(List<OpsOpportunity> list) {
        Map<String, String> userNames = names.resolve(ids(list, OpsOpportunity::getOwnerUserId));
        Map<String, String> codes = projectCodes(ids(list, OpsOpportunity::getProjectId));
        return list.stream().map(o -> new OpsDto.OppResponse(o.getId(), o.getOppCode(), o.getProjectId(), codes.get(o.getProjectId()), o.getKindCode(),
                o.getTitle(), o.getStageCode(), o.getAmount(), o.getProbabilityPct(), o.getOwnerUserId(), userNames.get(o.getOwnerUserId()),
                o.getExpectedDate(), o.getNextAction(), o.getNextDate(), o.getMemo(), o.getSourceIncidentId(), o.getContactId(),
                o.getSourceNextOppId())).toList();
    }

    // ========== 내부 ==========

    private static <T> Set<String> ids(List<T> list, java.util.function.Function<T, String> f) {
        Set<String> out = new HashSet<>();
        list.forEach(x -> {
            String v = f.apply(x);
            if (v != null) {
                out.add(v);
            }
        });
        return out;
    }

    private Map<String, String> projectCodes(Set<String> projectIds) {
        Map<String, String> codes = new HashMap<>();
        projectRepository.findAllById(projectIds).forEach(p -> codes.put(p.getId(), p.getProjectCode()));
        return codes;
    }
}
