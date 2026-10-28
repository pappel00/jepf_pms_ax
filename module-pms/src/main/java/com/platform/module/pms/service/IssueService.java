package com.platform.module.pms.service;

import com.platform.framework.core.common.exception.BusinessException;
import com.platform.framework.core.common.exception.TenantException;
import com.platform.module.pms.domain.Issue;
import com.platform.module.pms.domain.IssueHistory;
import com.platform.module.pms.domain.IssueTask;
import com.platform.module.pms.domain.Project;
import com.platform.module.pms.domain.WbsTask;
import com.platform.module.pms.dto.IssueDto;
import com.platform.module.pms.exception.PmsErrorCode;
import com.platform.module.pms.repository.IssueHistoryRepository;
import com.platform.module.pms.repository.IssueRepository;
import com.platform.module.pms.repository.IssueTaskRepository;
import com.platform.module.pms.repository.ProjectRepository;
import com.platform.module.pms.repository.WbsTaskRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 이슈 — 프로젝트 소속, 업무 연결은 이슈 1 : 업무 N(pms_issue_task). 상태 변경·업무 연결/해제·메모는 처리 이력으로 남고,
 * 열린 이슈가 「해결」로 바뀌면 일정 영향이 있는 경우 일정 조정 제안을 만든다.
 */
@Service
@Transactional(readOnly = true)
public class IssueService {

    private static final Set<String> STATUSES = Set.of("오픈", "진행 중", "해결", "종결");

    private final IssueRepository issueRepository;
    private final IssueTaskRepository linkRepository;
    private final IssueHistoryRepository historyRepository;
    private final WbsTaskRepository taskRepository;
    private final ProjectRepository projectRepository;
    private final ProjectService projectService;
    private final ScheduleProposalService proposalService;
    private final UserNameResolver names;

    public IssueService(IssueRepository issueRepository, IssueTaskRepository linkRepository, IssueHistoryRepository historyRepository,
                        WbsTaskRepository taskRepository, ProjectRepository projectRepository, ProjectService projectService,
                        ScheduleProposalService proposalService, UserNameResolver names) {
        this.issueRepository = issueRepository;
        this.linkRepository = linkRepository;
        this.historyRepository = historyRepository;
        this.taskRepository = taskRepository;
        this.projectRepository = projectRepository;
        this.projectService = projectService;
        this.proposalService = proposalService;
        this.names = names;
    }

    public List<IssueDto.Response> getList(String projectId) {
        return toResponses(issueRepository.search(projectId == null ? "" : projectId.trim()), null);
    }

    @Transactional
    public IssueDto.Response create(String projectId, IssueDto.Request r) {
        Project project = projectService.requireProject(projectId);
        String status = r.statusCode() == null || r.statusCode().isBlank() ? "오픈" : r.statusCode();
        validate(status, r);
        String code = PmsSupport.nextCode("ISS-", issueRepository.findAll().stream().map(Issue::getIssueCode).toList(), 3);
        String me = PmsSupport.currentUserId();
        LocalDate today = LocalDate.now();
        Issue issue = Issue.create().withIssueCode(code).withProjectId(project.getId()).withStatusCode(status)
                .withReporterUserId(PmsSupport.blankToNull(r.reporterUserId()) != null ? r.reporterUserId() : me)
                .withRegisteredDate(r.registeredDate() != null ? r.registeredDate() : today);
        apply(issue, r);
        issue.withResolvedDate(isOpen(status) ? null : today);
        issueRepository.saveAndFlush(issue);
        addHistory(issue.getId(), "등록", me, "이슈 등록");
        if (r.taskIds() != null) {
            for (String taskId : new LinkedHashSet<>(r.taskIds())) {
                link(issue, taskId, me);
            }
        }
        return toResponses(List.of(issue), null).get(0);
    }

    @Transactional
    public IssueDto.Response update(String id, IssueDto.Request r) {
        Issue issue = findOrThrow(id);
        String prev = issue.getStatusCode();
        String status = r.statusCode() == null || r.statusCode().isBlank() ? prev : r.statusCode();
        validate(status, r);
        String me = PmsSupport.currentUserId();
        apply(issue, r);
        issue.withStatusCode(status);
        if (!prev.equals(status)) {
            addHistory(id, "상태 변경", me, "상태 변경: " + prev + " → " + status);
        }
        if (isOpen(status)) {
            issue.withResolvedDate(null);
        } else if (issue.getResolvedDate() == null) {
            issue.withResolvedDate(LocalDate.now());
        }
        if (r.taskIds() != null) {
            syncLinks(issue, new LinkedHashSet<>(r.taskIds()), me);
        }
        String proposalCode = null;
        if (isOpen(prev) && "해결".equals(status)) {
            issueRepository.flush();
            List<String> linked = linkRepository.findAllByIssueIdAndDeletedYn(id, "N").stream().map(IssueTask::getTaskId).toList();
            proposalCode = proposalService.createForIssue(issue, linked);
        }
        return toResponses(List.of(issue), proposalCode).get(0);
    }

    @Transactional
    public void delete(String id) {
        Issue issue = findOrThrow(id);
        linkRepository.findAllByIssueIdAndDeletedYn(id, "N").forEach(IssueTask::markDeleted);
        historyRepository.findAllByIssueIdInAndDeletedYnOrderByActedAtAsc(List.of(id), "N").forEach(IssueHistory::markDeleted);
        issue.markDeleted();
    }

    @Transactional
    public IssueDto.Response linkTask(String id, String taskId) {
        Issue issue = findOrThrow(id);
        link(issue, taskId, PmsSupport.currentUserId());
        return toResponses(List.of(issue), null).get(0);
    }

    @Transactional
    public IssueDto.Response unlinkTask(String id, String taskId) {
        Issue issue = findOrThrow(id);
        unlink(issue, taskId, PmsSupport.currentUserId());
        return toResponses(List.of(issue), null).get(0);
    }

    @Transactional
    public IssueDto.Response addNote(String id, String text) {
        Issue issue = findOrThrow(id);
        addHistory(id, "메모", PmsSupport.currentUserId(), text.trim());
        return toResponses(List.of(issue), null).get(0);
    }

    // ---------- 내부 ----------

    private void validate(String status, IssueDto.Request r) {
        if (!STATUSES.contains(status)) {
            throw new BusinessException(PmsErrorCode.ISSUE_INVALID, "알 수 없는 이슈 상태입니다: " + status);
        }
        if (!isOpen(status) && (r.resolution() == null || r.resolution().isBlank())) {
            throw new BusinessException(PmsErrorCode.ISSUE_INVALID, "해결/종결 시 조치 내용을 기록하세요.");
        }
    }

    private static boolean isOpen(String status) {
        return "오픈".equals(status) || "진행 중".equals(status);
    }

    private void apply(Issue issue, IssueDto.Request r) {
        issue.withTitle(r.title().trim()).withTypeCode(r.typeCode()).withPriorityCode(r.priorityCode())
                .withAssigneeUserId(r.assigneeUserId()).withDueDate(r.dueDate()).withImpactDays(r.impactDays())
                .withContent(r.content().trim()).withResolution(PmsSupport.blankToNull(r.resolution()));
    }

    private void syncLinks(Issue issue, Set<String> wanted, String me) {
        Set<String> current = new HashSet<>();
        linkRepository.findAllByIssueIdAndDeletedYn(issue.getId(), "N").forEach(l -> current.add(l.getTaskId()));
        for (String taskId : current) {
            if (!wanted.contains(taskId)) {
                unlink(issue, taskId, me);
            }
        }
        for (String taskId : wanted) {
            if (!current.contains(taskId)) {
                link(issue, taskId, me);
            }
        }
    }

    private void link(Issue issue, String taskId, String me) {
        WbsTask task = taskRepository.findByIdAndDeletedYn(taskId, "N")
                .orElseThrow(() -> new TenantException(PmsErrorCode.TASK_NOT_FOUND, "존재하지 않는 업무입니다: " + taskId));
        if (!task.getProjectId().equals(issue.getProjectId())) {
            throw new BusinessException(PmsErrorCode.ISSUE_INVALID, "이슈가 속한 프로젝트의 업무만 연결할 수 있습니다: " + task.getTaskName());
        }
        boolean exists = linkRepository.findAllByIssueIdAndDeletedYn(issue.getId(), "N").stream().anyMatch(l -> l.getTaskId().equals(taskId));
        if (exists) {
            return;
        }
        linkRepository.save(IssueTask.create().withIssueId(issue.getId()).withTaskId(taskId).withLinkedBy(me).withLinkedAt(LocalDateTime.now()));
        addHistory(issue.getId(), "업무 연결", me, "업무 연결: " + task.getTaskName());
    }

    private void unlink(Issue issue, String taskId, String me) {
        for (IssueTask l : linkRepository.findAllByIssueIdAndDeletedYn(issue.getId(), "N")) {
            if (l.getTaskId().equals(taskId)) {
                l.markDeleted();
                String name = taskRepository.findById(taskId).map(WbsTask::getTaskName).orElse(taskId);
                addHistory(issue.getId(), "업무 연결 해제", me, "업무 연결 해제: " + name);
            }
        }
    }

    private void addHistory(String issueId, String action, String actor, String text) {
        historyRepository.save(IssueHistory.create().withIssueId(issueId).withActionCode(action).withActorUserId(actor)
                .withActedAt(LocalDateTime.now()).withHistoryText(text));
    }

    private Issue findOrThrow(String id) {
        return issueRepository.findByIdAndDeletedYn(id, "N")
                .orElseThrow(() -> new TenantException(PmsErrorCode.ISSUE_NOT_FOUND, "존재하지 않는 이슈입니다: " + id));
    }

    private List<IssueDto.Response> toResponses(List<Issue> list, String newProposalCode) {
        if (list.isEmpty()) {
            return List.of();
        }
        List<String> ids = list.stream().map(Issue::getId).toList();
        flushPending();
        Map<String, List<String>> taskIds = new HashMap<>();
        for (IssueTask l : linkRepository.findAllByIssueIdInAndDeletedYnOrderByLinkedAtAsc(ids, "N")) {
            taskIds.computeIfAbsent(l.getIssueId(), k -> new ArrayList<>()).add(l.getTaskId());
        }
        Map<String, List<IssueHistory>> histories = new HashMap<>();
        Set<String> userIds = new HashSet<>();
        for (IssueHistory h : historyRepository.findAllByIssueIdInAndDeletedYnOrderByActedAtAsc(ids, "N")) {
            histories.computeIfAbsent(h.getIssueId(), k -> new ArrayList<>()).add(h);
            userIds.add(h.getActorUserId());
        }
        Set<String> projectIds = new HashSet<>();
        for (Issue i : list) {
            userIds.add(i.getReporterUserId());
            userIds.add(i.getAssigneeUserId());
            projectIds.add(i.getProjectId());
        }
        Map<String, String> userNames = names.resolve(userIds);
        Map<String, String> codes = new HashMap<>();
        projectRepository.findAllById(projectIds).forEach(p -> codes.put(p.getId(), p.getProjectCode()));
        return list.stream().map(i -> IssueDto.Response.of(i, codes.get(i.getProjectId()), userNames,
                taskIds.getOrDefault(i.getId(), List.of()),
                histories.getOrDefault(i.getId(), List.of()).stream()
                        .map(h -> new IssueDto.HistoryEntry(h.getActionCode(), h.getActorUserId(), userNames.get(h.getActorUserId()),
                                h.getActedAt(), h.getHistoryText()))
                        .toList(),
                newProposalCode)).toList();
    }

    /** 같은 트랜잭션에서 방금 저장한 연결·이력이 조회에 잡히도록 flush 한다. */
    private void flushPending() {
        linkRepository.flush();
    }
}
