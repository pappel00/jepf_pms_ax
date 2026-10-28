package com.platform.module.pms.service;

import com.platform.framework.core.common.exception.BusinessException;
import com.platform.framework.core.common.exception.TenantException;
import com.platform.module.pms.domain.Issue;
import com.platform.module.pms.domain.IssueTask;
import com.platform.module.pms.domain.ScheduleProposal;
import com.platform.module.pms.domain.ScheduleProposalItem;
import com.platform.module.pms.domain.WbsTask;
import com.platform.module.pms.dto.ScheduleProposalDto;
import com.platform.module.pms.exception.PmsErrorCode;
import com.platform.module.pms.repository.IssueRepository;
import com.platform.module.pms.repository.IssueTaskRepository;
import com.platform.module.pms.repository.ProjectRepository;
import com.platform.module.pms.repository.ScheduleProposalItemRepository;
import com.platform.module.pms.repository.ScheduleProposalRepository;
import com.platform.module.pms.repository.WbsTaskRepository;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 이슈 해결 시 일정 조정 제안. 연결 업무(지연 업무)는 종료일을, 후행 업무(선행 체인으로 이어진 업무)는 시작·종료일을
 * 일정 영향 영업일만큼 민다. 진척 100%인 업무는 제외하고, 승인하면 WBS 일정에 반영한다(반려/무시는 바꾸지 않음).
 */
@Service
@Transactional(readOnly = true)
public class ScheduleProposalService {

    static final String PROPOSED = "제안";

    private final ScheduleProposalRepository proposalRepository;
    private final ScheduleProposalItemRepository itemRepository;
    private final WbsTaskRepository taskRepository;
    private final IssueRepository issueRepository;
    private final IssueTaskRepository issueTaskRepository;
    private final ProjectRepository projectRepository;
    private final UserNameResolver names;

    public ScheduleProposalService(ScheduleProposalRepository proposalRepository, ScheduleProposalItemRepository itemRepository,
                                   WbsTaskRepository taskRepository, IssueRepository issueRepository,
                                   IssueTaskRepository issueTaskRepository, ProjectRepository projectRepository,
                                   UserNameResolver names) {
        this.proposalRepository = proposalRepository;
        this.itemRepository = itemRepository;
        this.taskRepository = taskRepository;
        this.issueRepository = issueRepository;
        this.issueTaskRepository = issueTaskRepository;
        this.projectRepository = projectRepository;
        this.names = names;
    }

    public List<ScheduleProposalDto.Response> getList(String projectId) {
        return toResponses(proposalRepository.search(projectId == null ? "" : projectId.trim()));
    }

    /** 이슈를 해결로 바꿀 때 호출 — 제안을 만들었으면 그 코드를, 아니면 null 을 돌려준다. */
    @Transactional
    public String createForIssue(Issue issue, List<String> linkedTaskIds) {
        int days = issue.getImpactDays() == null ? 0 : issue.getImpactDays();
        if (days <= 0 || linkedTaskIds.isEmpty()) {
            return null;
        }
        if (proposalRepository.existsByIssueIdAndStatusCodeAndDeletedYn(issue.getId(), PROPOSED, "N")) {
            return null;
        }
        List<WbsTask> all = taskRepository.findAllByProjectIdAndDeletedYnOrderBySortOrderAscCreatedDateAsc(issue.getProjectId(), "N");
        Set<String> linkedSet = new HashSet<>(linkedTaskIds);
        List<WbsTask> linked = all.stream().filter(t -> linkedSet.contains(t.getId()) && isLeaf(t) && isOpen(t)).toList();

        List<WbsTask> successors = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        linked.forEach(t -> seen.add(t.getId()));
        Set<String> frontier = new HashSet<>(seen);
        while (!frontier.isEmpty()) {
            Set<String> next = new HashSet<>();
            for (WbsTask t : all) {
                if (t.getPredTaskId() != null && frontier.contains(t.getPredTaskId()) && seen.add(t.getId())) {
                    if (isOpen(t)) {
                        successors.add(t);
                        next.add(t.getId());
                    }
                }
            }
            frontier = next;
        }
        if (linked.isEmpty() && successors.isEmpty()) {
            return null;
        }

        String code = PmsSupport.nextCode("SCP-" + java.time.Year.now().getValue() + "-",
                proposalRepository.findAll().stream().map(ScheduleProposal::getProposalCode).toList(), 3);
        ScheduleProposal p = proposalRepository.save(ScheduleProposal.create().withProposalCode(code).withProjectId(issue.getProjectId())
                .withIssueId(issue.getId()).withStatusCode(PROPOSED).withShiftDays(days)
                .withBasisText(issue.getIssueCode() + " 해결, 일정 영향 " + days + "영업일"));
        List<ScheduleProposalItem> items = new ArrayList<>();
        for (WbsTask t : linked) {
            items.add(item(p, t, t.getStartDate(), shift(t.getEndDate(), days)));
        }
        for (WbsTask t : successors) {
            items.add(item(p, t, shift(t.getStartDate(), days), shift(t.getEndDate(), days)));
        }
        itemRepository.saveAll(items);
        return code;
    }

    /** 승인(WBS 일정 반영) · 반려(사유 필수) · 무시. */
    @Transactional
    public ScheduleProposalDto.Response decide(String id, String status, String comment) {
        ScheduleProposal p = proposalRepository.findByIdAndDeletedYn(id, "N")
                .orElseThrow(() -> new TenantException(PmsErrorCode.PROPOSAL_NOT_FOUND, "존재하지 않는 일정 조정 제안입니다: " + id));
        if (!PROPOSED.equals(p.getStatusCode())) {
            throw new BusinessException(PmsErrorCode.PROPOSAL_INVALID, "제안 상태가 아닙니다: " + p.getStatusCode());
        }
        if ("반려".equals(status) && (comment == null || comment.isBlank())) {
            throw new BusinessException(PmsErrorCode.PROPOSAL_INVALID, "반려 사유를 입력하세요.");
        }
        List<ScheduleProposalItem> items = itemRepository.findAllByProposalIdInAndDeletedYnOrderByCreatedDateAsc(List.of(id), "N");
        if ("승인".equals(status)) {
            Map<String, WbsTask> tasks = new HashMap<>();
            taskRepository.findAllById(items.stream().map(ScheduleProposalItem::getTaskId).toList()).forEach(t -> tasks.put(t.getId(), t));
            for (ScheduleProposalItem i : items) {
                WbsTask t = tasks.get(i.getTaskId());
                if (t != null && !t.isDeleted()) {
                    t.withStartDate(i.getNewStart()).withEndDate(i.getNewEnd());
                }
                i.withAppliedYn("Y");
            }
        }
        p.withStatusCode(status).withDecidedBy(PmsSupport.currentUserId()).withDecidedAt(LocalDateTime.now())
                .withDecisionComment(PmsSupport.blankToNull(comment));
        return toResponses(List.of(p)).get(0);
    }

    // ---------- 내부 ----------

    private static ScheduleProposalItem item(ScheduleProposal p, WbsTask t, LocalDate newStart, LocalDate newEnd) {
        return ScheduleProposalItem.create().withProposalId(p.getId()).withTaskId(t.getId()).withOldStart(t.getStartDate())
                .withOldEnd(t.getEndDate()).withNewStart(newStart).withNewEnd(newEnd).withAppliedYn("N");
    }

    private static boolean isLeaf(WbsTask t) {
        return "LEAF".equals(t.getNodeTypeCode());
    }

    private static boolean isOpen(WbsTask t) {
        return t.getProgressPct() == null || t.getProgressPct().compareTo(BigDecimal.valueOf(100)) < 0;
    }

    /** date 에서 n영업일 이동(음수면 앞당김). 주말은 건너뛴다. */
    static LocalDate shift(LocalDate date, int n) {
        if (date == null) {
            return null;
        }
        int step = n >= 0 ? 1 : -1;
        int left = Math.abs(n);
        LocalDate d = date;
        while (left > 0) {
            d = d.plusDays(step);
            if (!weekend(d)) {
                left--;
            }
        }
        while (weekend(d)) {
            d = d.plusDays(step);
        }
        return d;
    }

    private static boolean weekend(LocalDate d) {
        return d.getDayOfWeek() == DayOfWeek.SATURDAY || d.getDayOfWeek() == DayOfWeek.SUNDAY;
    }

    private List<ScheduleProposalDto.Response> toResponses(List<ScheduleProposal> list) {
        if (list.isEmpty()) {
            return List.of();
        }
        List<String> ids = list.stream().map(ScheduleProposal::getId).toList();
        Map<String, List<ScheduleProposalItem>> itemsByProposal = new HashMap<>();
        Set<String> taskIds = new HashSet<>();
        for (ScheduleProposalItem i : itemRepository.findAllByProposalIdInAndDeletedYnOrderByCreatedDateAsc(ids, "N")) {
            itemsByProposal.computeIfAbsent(i.getProposalId(), k -> new ArrayList<>()).add(i);
            taskIds.add(i.getTaskId());
        }
        Map<String, String> taskNames = new HashMap<>();
        taskRepository.findAllById(taskIds).forEach(t -> taskNames.put(t.getId(), t.getTaskName()));

        Set<String> issueIds = new HashSet<>();
        Set<String> projectIds = new HashSet<>();
        Set<String> userIds = new HashSet<>();
        for (ScheduleProposal p : list) {
            issueIds.add(p.getIssueId());
            projectIds.add(p.getProjectId());
            userIds.add(p.getDecidedBy());
        }
        Map<String, Issue> issues = new HashMap<>();
        issueRepository.findAllById(issueIds).forEach(i -> issues.put(i.getId(), i));
        Map<String, Set<String>> linkedByIssue = new HashMap<>();
        for (IssueTask it : issueTaskRepository.findAllByIssueIdInAndDeletedYnOrderByLinkedAtAsc(issueIds, "N")) {
            linkedByIssue.computeIfAbsent(it.getIssueId(), k -> new HashSet<>()).add(it.getTaskId());
        }
        Map<String, String> codes = new HashMap<>();
        projectRepository.findAllById(projectIds).forEach(pr -> codes.put(pr.getId(), pr.getProjectCode()));
        Map<String, String> userNames = names.resolve(userIds);

        return list.stream().map(p -> {
            Issue issue = issues.get(p.getIssueId());
            Set<String> linked = linkedByIssue.getOrDefault(p.getIssueId(), Set.of());
            List<ScheduleProposalDto.ItemResponse> items = itemsByProposal.getOrDefault(p.getId(), List.of()).stream()
                    .map(i -> new ScheduleProposalDto.ItemResponse(i.getTaskId(), taskNames.get(i.getTaskId()),
                            linked.contains(i.getTaskId()) ? "지연 업무" : "후행 업무", i.getOldStart(), i.getOldEnd(), i.getNewStart(),
                            i.getNewEnd(), "Y".equals(i.getAppliedYn())))
                    .toList();
            return new ScheduleProposalDto.Response(p.getId(), p.getProposalCode(), p.getProjectId(), codes.get(p.getProjectId()),
                    p.getIssueId(), issue == null ? null : issue.getIssueCode(), issue == null ? null : issue.getTitle(),
                    p.getStatusCode(), p.getShiftDays(), p.getBasisText(), p.getCreatedDate(), p.getDecidedBy(),
                    p.getDecidedBy() == null ? null : userNames.get(p.getDecidedBy()), p.getDecidedAt(), p.getDecisionComment(), items);
        }).toList();
    }
}
