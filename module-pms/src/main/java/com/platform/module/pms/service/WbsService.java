package com.platform.module.pms.service;

import com.platform.framework.core.common.exception.BusinessException;
import com.platform.framework.core.common.exception.TenantException;
import com.platform.module.pms.domain.Project;
import com.platform.module.pms.domain.TaskProgressLog;
import com.platform.module.pms.domain.WbsTask;
import com.platform.module.pms.dto.WbsDto;
import com.platform.module.pms.exception.PmsErrorCode;
import com.platform.module.pms.repository.IssueRepository;
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

    private final WbsTaskRepository taskRepository;
    private final TaskProgressLogRepository logRepository;
    private final IssueRepository issueRepository;
    private final ProjectRepository projectRepository;
    private final ProjectService projectService;
    private final UserNameResolver names;

    public WbsService(WbsTaskRepository taskRepository, TaskProgressLogRepository logRepository, IssueRepository issueRepository,
                      ProjectRepository projectRepository, ProjectService projectService, UserNameResolver names) {
        this.taskRepository = taskRepository;
        this.logRepository = logRepository;
        this.issueRepository = issueRepository;
        this.projectRepository = projectRepository;
        this.projectService = projectService;
        this.names = names;
    }

    public List<WbsDto.Response> getList(String projectId) {
        return toResponses(taskRepository.search(projectId == null ? "" : projectId.trim()));
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
        WbsTask t = findOrThrow(id);
        if (!"LEAF".equals(t.getNodeTypeCode())) {
            throw new BusinessException(PmsErrorCode.TASK_INVALID, "진척률은 하위 업무(리프)에만 입력할 수 있습니다.");
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
                .withInputAt(LocalDateTime.now()).withChannelCode(proxy ? "ADMIN_PROXY" : "WEB").withNote(PmsSupport.blankToNull(request.note())));
        return toResponses(List.of(t)).get(0);
    }

    /** 업무 복사 — 담당자만 바꿔 한 건 더 만든다. 가중치는 원본+복사본 합계를 균등 분배한다(진척 0 초기화). */
    @Transactional
    public List<WbsDto.Response> copy(String id, WbsDto.CopyRequest request) {
        WbsTask src = findOrThrow(id);
        if (!"LEAF".equals(src.getNodeTypeCode())) {
            throw new BusinessException(PmsErrorCode.TASK_INVALID, "하위 업무(리프)만 복사할 수 있습니다.");
        }
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
