package com.platform.module.pms.service;

import com.platform.framework.core.common.exception.BusinessException;
import com.platform.framework.core.common.exception.TenantException;
import com.platform.module.pms.domain.Project;
import com.platform.module.pms.domain.TaskProgressLog;
import com.platform.module.pms.domain.WbsTask;
import com.platform.module.pms.dto.WbsDto;
import com.platform.module.pms.exception.PmsErrorCode;
import com.platform.module.pms.repository.IssueRepository;
import com.platform.module.pms.repository.IssueTaskRepository;
import com.platform.module.pms.repository.ProjectRepository;
import com.platform.module.pms.repository.TaskProgressLogRepository;
import com.platform.module.pms.repository.WbsTaskRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
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
 * WBS 업무 — 단계 → 분류 → 작업(자기참조 트리). 리프만 진척률을 저장하고(상위는 화면이 가중평균으로 계산),
 * 선행 업무가 100%가 아니면 진척을 입력할 수 없다(finish-to-start). 진척 입력은 이력으로 남는다.
 */
@Service
@Transactional(readOnly = true)
public class WbsService {

    private final ProjectAccess access;
    private final WbsTaskRepository taskRepository;
    private final TaskProgressLogRepository logRepository;
    private final IssueRepository issueRepository;
    private final IssueTaskRepository issueTaskRepository;
    private final ProjectRepository projectRepository;
    private final ProjectService projectService;
    private final UserNameResolver names;

    public WbsService(ProjectAccess access, WbsTaskRepository taskRepository, TaskProgressLogRepository logRepository, IssueRepository issueRepository,
                      IssueTaskRepository issueTaskRepository, ProjectRepository projectRepository, ProjectService projectService, UserNameResolver names) {
        this.access = access;
        this.taskRepository = taskRepository;
        this.logRepository = logRepository;
        this.issueRepository = issueRepository;
        this.issueTaskRepository = issueTaskRepository;
        this.projectRepository = projectRepository;
        this.projectService = projectService;
        this.names = names;
    }

    public List<WbsDto.Response> getList(String projectId) {
        return access.filterMine(toResponses(taskRepository.search(projectId == null ? "" : projectId.trim())), WbsDto.Response::projectId);
    }

    /** 단계전환 승인 직후 화면이 만든 WBS 를 저장한다. 이미 WBS 가 있는 프로젝트는 거부한다. */
    @Transactional
    public List<WbsDto.Response> createBulk(String projectId, WbsDto.BulkRequest request) {
        Project project = projectService.requireProject(projectId);
        if (taskRepository.existsByProjectIdAndDeletedYn(projectId, "N")) {
            throw new BusinessException(PmsErrorCode.TASK_INVALID, "이미 WBS가 있는 프로젝트입니다: " + project.getProjectCode());
        }
        Map<String, WbsTask> byKey = new HashMap<>();
        List<WbsTask> created = new ArrayList<>();
        int order = 0;
        for (WbsDto.Node n : request.tasks()) {
            if (byKey.containsKey(n.key())) {
                throw new BusinessException(PmsErrorCode.TASK_INVALID, "중복된 업무 키입니다: " + n.key());
            }
            boolean leaf = "LEAF".equals(n.nodeTypeCode());
            WbsTask t = WbsTask.create().withProjectId(projectId).withNodeTypeCode(n.nodeTypeCode()).withPhaseCode(n.phaseCode())
                    .withCategoryName(PmsSupport.blankToNull(n.categoryName())).withTaskName(n.taskName().trim())
                    .withRfpItemId(PmsSupport.blankToNull(n.rfpItemId())).withReqCode(PmsSupport.blankToNull(n.reqCode()))
                    .withFeatureName(PmsSupport.blankToNull(n.featureName())).withAssigneeUserId(PmsSupport.blankToNull(n.assigneeUserId()))
                    .withStartDate(n.startDate()).withEndDate(n.endDate()).withWeight(n.weight())
                    .withProgressPct(leaf ? (n.progressPct() == null ? BigDecimal.ZERO : n.progressPct()) : null)
                    .withSortOrder(order++);
            byKey.put(n.key(), t);
            created.add(t);
        }
        for (WbsDto.Node n : request.tasks()) {
            WbsTask t = byKey.get(n.key());
            if (n.parentKey() != null) {
                t.withParentId(requireKey(byKey, n.parentKey()).getId());
            }
            if (n.predKey() != null) {
                t.withPredTaskId(requireKey(byKey, n.predKey()).getId());
            }
        }
        return toResponses(taskRepository.saveAll(created));
    }

    /** 진척률 입력 — 리프만, 선행 업무가 끝나야 0% 초과 입력 가능. */
    @Transactional
    public WbsDto.Response updateProgress(String id, WbsDto.ProgressRequest request) {
        return updateProgress(id, request, null);
    }

    /** channel: 입력 경로 표시(WEB/MATTERMOST 등). null 이면 웹 입력(담당자 본인) 또는 관리자 대리 입력으로 기록한다. */
    @Transactional
    public WbsDto.Response updateProgress(String id, WbsDto.ProgressRequest request, String channel) {
        WbsTask t = findOrThrow(id);
        if (!"LEAF".equals(t.getNodeTypeCode())) {
            throw new BusinessException(PmsErrorCode.TASK_INVALID, "진척률은 하위 업무(리프)에만 입력할 수 있습니다.");
        }
        // 진척은 담당자 본인(또는 프로젝트 관리자)만 입력한다.
        if (!PmsSupport.currentUserId().equals(t.getAssigneeUserId())) {
            access.requireManager(t.getProjectId(), "다른 사람 업무의 진척 입력");
        }
        BigDecimal value = request.progressPct().setScale(1, RoundingMode.HALF_UP);
        if (value.signum() > 0 && t.getPredTaskId() != null) {
            WbsTask pred = taskRepository.findByIdAndDeletedYn(t.getPredTaskId(), "N").orElse(null);
            if (pred != null && (pred.getProgressPct() == null || pred.getProgressPct().compareTo(BigDecimal.valueOf(100)) < 0)) {
                throw new BusinessException(PmsErrorCode.TASK_INVALID, "선행 업무「" + pred.getTaskName() + "」가 끝나지 않아 진척을 입력할 수 없습니다.");
            }
        }
        t.withProgressPct(value);
        String me = PmsSupport.currentUserId();
        boolean proxy = t.getAssigneeUserId() != null && !t.getAssigneeUserId().equals(me);
        logRepository.save(TaskProgressLog.create().withTaskId(t.getId()).withProgressPct(value).withInputByUserId(me)
                .withInputAt(LocalDateTime.now()).withChannelCode(channel != null ? channel : proxy ? "ADMIN_PROXY" : "WEB").withNote(PmsSupport.blankToNull(request.note())));
        return toResponses(List.of(t)).get(0);
    }

    /** 업무 복사 — 담당자만 바꿔 한 건 더 만든다. 가중치는 원본+복사본 합계를 균등 분배한다(진척 0 초기화). */
    @Transactional
    public List<WbsDto.Response> copy(String id, WbsDto.CopyRequest request) {
        WbsTask src = findOrThrow(id);
        if (!"LEAF".equals(src.getNodeTypeCode())) {
            throw new BusinessException(PmsErrorCode.TASK_INVALID, "하위 업무(리프)만 복사할 수 있습니다.");
        }
        access.requireManager(src.getProjectId(), "업무 복사");
        String rootId = src.getCopiedFromId() != null ? src.getCopiedFromId() : src.getId();
        List<WbsTask> all = taskRepository.findAllByProjectIdAndDeletedYnOrderBySortOrderAscCreatedDateAsc(src.getProjectId(), "N");
        List<WbsTask> group = all.stream().filter(t -> t.getId().equals(rootId) || rootId.equals(t.getCopiedFromId())).toList();
        BigDecimal total = group.stream().map(WbsTask::getWeight).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal each = total.divide(BigDecimal.valueOf(group.size() + 1L), 1, RoundingMode.HALF_UP);
        int lastOrder = group.stream().mapToInt(WbsTask::getSortOrder).max().orElse(src.getSortOrder());
        List<WbsTask> changed = new ArrayList<>(group);
        for (WbsTask t : all) {
            if (t.getSortOrder() > lastOrder) {
                t.withSortOrder(t.getSortOrder() + 1);
            }
        }
        group.forEach(t -> t.withWeight(each));
        WbsTask copy = WbsTask.create().withProjectId(src.getProjectId()).withParentId(src.getParentId()).withNodeTypeCode("LEAF")
                .withPhaseCode(src.getPhaseCode()).withCategoryName(src.getCategoryName()).withTaskName(src.getTaskName())
                .withRfpItemId(src.getRfpItemId()).withReqCode(src.getReqCode()).withFeatureName(src.getFeatureName())
                .withAssigneeUserId(PmsSupport.blankToNull(request.assigneeUserId())).withStartDate(src.getStartDate())
                .withEndDate(src.getEndDate()).withWeight(each).withProgressPct(BigDecimal.ZERO).withPredTaskId(src.getPredTaskId())
                .withCopiedFromId(rootId).withSortOrder(lastOrder + 1);
        taskRepository.save(copy);
        changed.add(copy);
        return toResponses(changed);
    }

    /** 업무 추가 — 단계 또는 분류 아래에 하위 업무(리프)를 만든다. 프로젝트 관리자만. */
    @Transactional
    public WbsDto.Response create(String projectId, WbsDto.TaskRequest r) {
        projectService.requireProject(projectId);
        access.requireManager(projectId, "업무 추가");
        if (r.parentId() == null || r.parentId().isBlank()) {
            throw new BusinessException(PmsErrorCode.TASK_INVALID, "업무를 넣을 단계 또는 분류를 선택하세요.");
        }
        WbsTask parent = findOrThrow(r.parentId());
        if (!projectId.equals(parent.getProjectId())) {
            throw new BusinessException(PmsErrorCode.TASK_INVALID, "다른 프로젝트의 항목 아래에는 추가할 수 없습니다.");
        }
        if ("LEAF".equals(parent.getNodeTypeCode())) {
            throw new BusinessException(PmsErrorCode.TASK_INVALID, "하위 업무 아래에는 업무를 추가할 수 없습니다. 단계 또는 분류를 선택하세요.");
        }
        checkDates(r);
        List<WbsTask> all = taskRepository.findAllByProjectIdAndDeletedYnOrderBySortOrderAscCreatedDateAsc(projectId, "N");
        checkPredecessor(all, null, r.predTaskId());
        String assignee = checkAssignee(r.assigneeUserId());
        // 부모 아래 마지막 자리(부모와 모든 후손 중 가장 큰 순서 다음)에 끼우고 뒤 업무의 순서를 한 칸씩 민다.
        Set<String> subtree = new HashSet<>(Set.of(parent.getId()));
        boolean grew = true;
        while (grew) {
            grew = false;
            for (WbsTask t : all) {
                if (t.getParentId() != null && subtree.contains(t.getParentId()) && subtree.add(t.getId())) {
                    grew = true;
                }
            }
        }
        int lastOrder = all.stream().filter(t -> subtree.contains(t.getId())).mapToInt(WbsTask::getSortOrder).max().orElse(parent.getSortOrder());
        all.stream().filter(t -> t.getSortOrder() > lastOrder).forEach(t -> t.withSortOrder(t.getSortOrder() + 1));
        String category = "CATEGORY".equals(parent.getNodeTypeCode()) ? parent.getTaskName() : parent.getCategoryName();
        WbsTask created = WbsTask.create().withProjectId(projectId).withParentId(parent.getId()).withNodeTypeCode("LEAF")
                .withPhaseCode(parent.getPhaseCode()).withCategoryName(category).withTaskName(r.taskName().trim())
                .withAssigneeUserId(assignee).withStartDate(r.startDate()).withEndDate(r.endDate()).withWeight(r.weight())
                .withProgressPct(BigDecimal.ZERO).withPredTaskId(PmsSupport.blankToNull(r.predTaskId())).withSortOrder(lastOrder + 1);
        taskRepository.save(created);
        return toResponses(List.of(created)).get(0);
    }

    /**
     * 업무 수정 — 업무명·가중치(상위 항목 포함)와, 하위 업무는 기간·담당자·선행 업무. 프로젝트 관리자만.
     * 담당자를 바꿔도 이미 입력된 진척은 그대로 둔다.
     */
    @Transactional
    public WbsDto.Response update(String id, WbsDto.TaskRequest r) {
        WbsTask t = findOrThrow(id);
        access.requireManager(t.getProjectId(), "업무 수정");
        t.withTaskName(r.taskName().trim()).withWeight(r.weight());
        if ("LEAF".equals(t.getNodeTypeCode())) {
            checkDates(r);
            List<WbsTask> all = taskRepository.findAllByProjectIdAndDeletedYnOrderBySortOrderAscCreatedDateAsc(t.getProjectId(), "N");
            checkPredecessor(all, t.getId(), r.predTaskId());
            t.withStartDate(r.startDate()).withEndDate(r.endDate()).withAssigneeUserId(checkAssignee(r.assigneeUserId()))
                    .withPredTaskId(PmsSupport.blankToNull(r.predTaskId()));
        }
        return toResponses(List.of(t)).get(0);
    }

    /**
     * 업무 삭제 — 진척이 0% 이고 이슈·후속 업무가 걸려 있지 않은 하위 업무만. 같은 부모 아래 남은 업무의 가중치는
     * 삭제 전 합계가 유지되도록 비율대로 다시 맞춘다. 프로젝트 관리자만.
     */
    @Transactional
    public void delete(String id) {
        WbsTask t = findOrThrow(id);
        access.requireManager(t.getProjectId(), "업무 삭제");
        if (!"LEAF".equals(t.getNodeTypeCode())) {
            throw new BusinessException(PmsErrorCode.TASK_INVALID, "단계·분류는 삭제할 수 없습니다. 하위 업무만 삭제할 수 있습니다.");
        }
        if (t.getProgressPct() != null && t.getProgressPct().signum() > 0) {
            throw new BusinessException(PmsErrorCode.TASK_INVALID, "진척이 입력된 업무는 삭제할 수 없습니다.");
        }
        if (issueTaskRepository.existsByTaskIdAndDeletedYn(id, "N")) {
            throw new BusinessException(PmsErrorCode.TASK_INVALID, "이슈가 연결된 업무는 삭제할 수 없습니다. 이슈의 연결을 먼저 해제하세요.");
        }
        List<WbsTask> all = taskRepository.findAllByProjectIdAndDeletedYnOrderBySortOrderAscCreatedDateAsc(t.getProjectId(), "N");
        if (all.stream().anyMatch(x -> id.equals(x.getPredTaskId()))) {
            throw new BusinessException(PmsErrorCode.TASK_INVALID, "다른 업무의 선행 업무로 지정되어 있어 삭제할 수 없습니다. 후속 업무의 선행 업무를 먼저 바꾸세요.");
        }
        List<WbsTask> siblings = all.stream().filter(x -> java.util.Objects.equals(x.getParentId(), t.getParentId())).toList();
        if (siblings.size() <= 1) {
            throw new BusinessException(PmsErrorCode.TASK_INVALID, "마지막 하위 업무는 삭제할 수 없습니다. 상위 항목 아래에 업무가 하나는 있어야 합니다.");
        }
        // 복사 그룹의 기준(원본)을 지우면 남은 복사본 중 첫 번째를 새 기준으로 삼는다.
        List<WbsTask> copies = all.stream().filter(x -> id.equals(x.getCopiedFromId())).toList();
        if (!copies.isEmpty()) {
            WbsTask root = copies.get(0);
            root.withCopiedFromId(null);
            copies.stream().skip(1).forEach(x -> x.withCopiedFromId(root.getId()));
        }
        BigDecimal before = siblings.stream().map(WbsTask::getWeight).reduce(BigDecimal.ZERO, BigDecimal::add);
        List<WbsTask> rest = siblings.stream().filter(x -> !x.getId().equals(id)).toList();
        BigDecimal remain = rest.stream().map(WbsTask::getWeight).reduce(BigDecimal.ZERO, BigDecimal::add);
        if (remain.signum() > 0) {
            rest.forEach(x -> x.withWeight(x.getWeight().multiply(before).divide(remain, 1, RoundingMode.HALF_UP)));
        }
        t.markDeleted();
    }

    private static void checkDates(WbsDto.TaskRequest r) {
        if (r.startDate() == null || r.endDate() == null) {
            throw new BusinessException(PmsErrorCode.TASK_INVALID, "시작일과 종료일을 입력하세요.");
        }
        if (r.endDate().isBefore(r.startDate())) {
            throw new BusinessException(PmsErrorCode.TASK_INVALID, "종료일은 시작일보다 빠를 수 없습니다.");
        }
    }

    /** 선행 업무는 같은 프로젝트의 하위 업무여야 하고, 자기 자신이나 자기를 (간접으로) 기다리는 업무일 수 없다. */
    private static void checkPredecessor(List<WbsTask> all, String selfId, String predId) {
        if (predId == null || predId.isBlank()) {
            return;
        }
        Map<String, WbsTask> byId = new HashMap<>();
        all.forEach(x -> byId.put(x.getId(), x));
        WbsTask pred = byId.get(predId);
        if (pred == null || !"LEAF".equals(pred.getNodeTypeCode())) {
            throw new BusinessException(PmsErrorCode.TASK_INVALID, "선행 업무는 같은 프로젝트의 하위 업무여야 합니다.");
        }
        for (WbsTask cur = pred; cur != null; cur = cur.getPredTaskId() == null ? null : byId.get(cur.getPredTaskId())) {
            if (cur.getId().equals(selfId)) {
                throw new BusinessException(PmsErrorCode.TASK_INVALID, "선행 업무가 서로를 기다리는 순환이 생겨 지정할 수 없습니다.");
            }
        }
    }

    private String checkAssignee(String userId) {
        String id = PmsSupport.blankToNull(userId);
        if (id != null && !names.resolve(Set.of(id)).containsKey(id)) {
            throw new BusinessException(PmsErrorCode.TASK_INVALID, "존재하지 않는 담당자입니다.");
        }
        return id;
    }

    /** 단계전환 롤백 — 진척이 입력됐거나 이슈가 있으면 거부하고, 아니면 프로젝트의 WBS 를 모두 지운다. */
    @Transactional
    public void deleteForRollback(String projectId) {
        List<WbsTask> tasks = taskRepository.findAllByProjectIdAndDeletedYnOrderBySortOrderAscCreatedDateAsc(projectId, "N");
        long progressed = tasks.stream().filter(t -> "LEAF".equals(t.getNodeTypeCode()))
                .filter(t -> t.getProgressPct() != null && t.getProgressPct().signum() > 0).count();
        if (progressed > 0) {
            throw new BusinessException(PmsErrorCode.TASK_INVALID, "진척이 입력된 업무가 " + progressed + "건 있어 롤백할 수 없습니다.");
        }
        if (issueRepository.existsByProjectIdAndDeletedYn(projectId, "N")) {
            throw new BusinessException(PmsErrorCode.TASK_INVALID, "등록된 이슈가 있어 롤백할 수 없습니다.");
        }
        tasks.forEach(WbsTask::markDeleted);
    }

    private WbsTask findOrThrow(String id) {
        return taskRepository.findByIdAndDeletedYn(id, "N")
                .orElseThrow(() -> new TenantException(PmsErrorCode.TASK_NOT_FOUND, "존재하지 않는 업무입니다: " + id));
    }

    private static WbsTask requireKey(Map<String, WbsTask> byKey, String key) {
        WbsTask t = byKey.get(key);
        if (t == null) {
            throw new BusinessException(PmsErrorCode.TASK_INVALID, "참조하는 업무 키가 없습니다: " + key);
        }
        return t;
    }

    private List<WbsDto.Response> toResponses(List<WbsTask> list) {
        Set<String> userIds = new HashSet<>();
        Set<String> projectIds = new HashSet<>();
        for (WbsTask t : list) {
            userIds.add(t.getAssigneeUserId());
            projectIds.add(t.getProjectId());
        }
        Map<String, String> userNames = names.resolve(userIds);
        Map<String, String> codes = new HashMap<>();
        projectRepository.findAllById(projectIds).forEach(p -> codes.put(p.getId(), p.getProjectCode()));
        return list.stream().map(t -> WbsDto.Response.of(t, codes.get(t.getProjectId()), userNames)).toList();
    }
}
