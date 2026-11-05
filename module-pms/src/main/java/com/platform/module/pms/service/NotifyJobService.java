package com.platform.module.pms.service;

import com.platform.framework.core.common.exception.BusinessException;
import com.platform.module.pms.domain.Project;
import com.platform.module.pms.domain.ProjectOperation;
import com.platform.module.pms.domain.Setting;
import com.platform.module.pms.domain.WbsTask;
import com.platform.module.pms.dto.NotifyDto;
import com.platform.module.pms.exception.PmsErrorCode;
import com.platform.module.pms.repository.IssueRepository;
import com.platform.module.pms.repository.NextOpportunityRepository;
import com.platform.module.pms.repository.NotifyLogRepository;
import com.platform.module.pms.repository.OpsIncidentRepository;
import com.platform.module.pms.repository.OpsOpportunityRepository;
import com.platform.module.pms.repository.OpsTaskRepository;
import com.platform.module.pms.repository.ProjectOperationRepository;
import com.platform.module.pms.repository.ProjectRepository;
import com.platform.module.pms.repository.ProposalRepository;
import com.platform.module.pms.repository.RfpEvaluatorRepository;
import com.platform.module.pms.repository.RfpRepository;
import com.platform.module.pms.repository.SalesNoteRepository;
import com.platform.module.pms.repository.SettingRepository;
import com.platform.module.pms.repository.WbsTaskRepository;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 기한 알림 배치 — 업무(WBS)·영업 노트·제안·이슈·운영 업무 등 기한이 있는 항목 중 기한이 도래한 미완료 건의 담당자에게
 * 메시지를 보낸다. 알림 종류별 사용 여부(NotifyType)와 발송 시각·마감 며칠 전·시작일 알림은 테넌트별 pms_setting 에 두고,
 * 같은 날 같은 항목·종류는 한 번만 보낸다. 스케줄러(NotifyScheduler)가 호출하고, 알림 관리 화면의 「지금 실행」·「대상 미리보기」도
 * 같은 메서드를 쓴다. 이미 지난 기한(지연)은 매일 반복되어 알림이 쌓이므로 보내지 않는다(SLA 초과만 예외).
 */
@Service
@Transactional(readOnly = true)
public class NotifyJobService {

    private static final Logger log = LoggerFactory.getLogger(NotifyJobService.class);

    static final String KEY_ENABLED = "notify.enabled";
    static final String KEY_HOUR = "notify.hour";
    static final String KEY_DUE_DAYS = "notify.due_days";
    static final String KEY_START = "notify.start_alert";
    static final String KEY_TYPE = "notify.type.";
    private static final int[] OPS_END_DAYS = {30, 7, 1};

    /** 보낼 알림 한 건. wbsTaskId 가 있으면 진척 입력 버튼이 달린 메시지로 보낸다. */
    private record Alert(NotifyType type, String kind, String refId, String userId, String headline, String text, String wbsTaskId, String sample) {
    }

    private final SettingRepository settingRepository;
    private final WbsTaskRepository taskRepository;
    private final ProjectRepository projectRepository;
    private final NotifyLogRepository logRepository;
    private final NotifyService notifyService;
    private final UserNameResolver names;
    private final SalesNoteRepository salesNoteRepository;
    private final ProposalRepository proposalRepository;
    private final RfpRepository rfpRepository;
    private final RfpEvaluatorRepository rfpEvaluatorRepository;
    private final IssueRepository issueRepository;
    private final OpsTaskRepository opsTaskRepository;
    private final OpsOpportunityRepository opsOpportunityRepository;
    private final NextOpportunityRepository nextOpportunityRepository;
    private final ProjectOperationRepository operationRepository;
    private final OpsIncidentRepository incidentRepository;

    public NotifyJobService(SettingRepository settingRepository, WbsTaskRepository taskRepository, ProjectRepository projectRepository,
                            NotifyLogRepository logRepository, NotifyService notifyService, UserNameResolver names,
                            SalesNoteRepository salesNoteRepository, ProposalRepository proposalRepository, RfpRepository rfpRepository,
                            RfpEvaluatorRepository rfpEvaluatorRepository, IssueRepository issueRepository, OpsTaskRepository opsTaskRepository,
                            OpsOpportunityRepository opsOpportunityRepository, NextOpportunityRepository nextOpportunityRepository,
                            ProjectOperationRepository operationRepository, OpsIncidentRepository incidentRepository) {
        this.settingRepository = settingRepository;
        this.taskRepository = taskRepository;
        this.projectRepository = projectRepository;
        this.logRepository = logRepository;
        this.notifyService = notifyService;
        this.names = names;
        this.salesNoteRepository = salesNoteRepository;
        this.proposalRepository = proposalRepository;
        this.rfpRepository = rfpRepository;
        this.rfpEvaluatorRepository = rfpEvaluatorRepository;
        this.issueRepository = issueRepository;
        this.opsTaskRepository = opsTaskRepository;
        this.opsOpportunityRepository = opsOpportunityRepository;
        this.nextOpportunityRepository = nextOpportunityRepository;
        this.operationRepository = operationRepository;
        this.incidentRepository = incidentRepository;
    }

    /** 알림 종류 목록(화면의 설정 표). */
    public List<NotifyDto.TypeInfo> types() {
        return java.util.Arrays.stream(NotifyType.values())
                .map(t -> new NotifyDto.TypeInfo(t.name(), t.folder(), t.page(), t.item(), t.description(), t.defaultOn(), t.scope().name()))
                .toList();
    }

    /** 저장된 설정. 없으면 꺼짐·오전 9시·마감 당일과 1일 전·시작일 알림 켬, 종류별로는 NotifyType 기본값. */
    public NotifyDto.Settings getSettings() {
        Map<String, String> m = new HashMap<>();
        settingRepository.findAllByDeletedYn("N").forEach(s -> m.put(s.getSettingKey(), s.getSettingValue()));
        List<Integer> due = new ArrayList<>();
        for (String p : m.getOrDefault(KEY_DUE_DAYS, "0,1").split(",")) {
            if (!p.isBlank()) {
                due.add(Integer.parseInt(p.trim()));
            }
        }
        Map<String, Boolean> types = new java.util.LinkedHashMap<>();
        for (NotifyType t : NotifyType.values()) {
            types.put(t.name(), "Y".equals(m.getOrDefault(KEY_TYPE + t.name(), t.defaultOn() ? "Y" : "N")));
        }
        return new NotifyDto.Settings("Y".equals(m.getOrDefault(KEY_ENABLED, "N")), Integer.parseInt(m.getOrDefault(KEY_HOUR, "9")), due,
                "Y".equals(m.getOrDefault(KEY_START, "Y")), types);
    }

    @Transactional
    public NotifyDto.Settings saveSettings(NotifyDto.Settings s) {
        if (s.hour() < 0 || s.hour() > 23) {
            throw new BusinessException(PmsErrorCode.NOTIFY_INVALID, "발송 시각은 0~23시 사이여야 합니다.");
        }
        Set<Integer> due = new TreeSet<>(s.dueDays() == null ? List.of() : s.dueDays());
        if (due.size() > 5 || due.stream().anyMatch(d -> d == null || d < 0 || d > 14)) {
            throw new BusinessException(PmsErrorCode.NOTIFY_INVALID, "마감 알림은 0~14일 전 값을 최대 5개까지 지정할 수 있습니다.");
        }
        upsert(KEY_ENABLED, s.enabled() ? "Y" : "N");
        upsert(KEY_HOUR, String.valueOf(s.hour()));
        upsert(KEY_DUE_DAYS, String.join(",", due.stream().map(String::valueOf).toList()));
        upsert(KEY_START, s.startAlert() ? "Y" : "N");
        for (NotifyType t : NotifyType.values()) {
            Boolean on = s.types() == null ? null : s.types().get(t.name());
            if (on != null) {
                upsert(KEY_TYPE + t.name(), on ? "Y" : "N");
            }
        }
        return getSettings();
    }

    /**
     * 알릴 항목을 찾아 보낸다(dryRun 이면 대상만 센다). 발송마다 자체 트랜잭션으로 이력을 남기도록 여기서는 트랜잭션을 걸지 않는다.
     * scope: DAILY(매일 정한 시각 대상), SLA(시(時) 단위 시한), ALL(화면의 수동 실행). 종류별 사용 여부가 꺼진 알림은 건너뛴다.
     */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public NotifyDto.RunResult run(boolean dryRun, NotifyType.Scope scope) {
        LocalDate today = LocalDate.now();
        NotifyDto.Settings s = getSettings();
        Map<String, Project> projects = new HashMap<>();
        projectRepository.findAll().stream().filter(p -> "N".equals(p.getDeletedYn())).forEach(p -> projects.put(p.getId(), p));

        List<Alert> alerts = new ArrayList<>();
        for (NotifyType t : NotifyType.values()) {
            if (!Boolean.TRUE.equals(s.types().get(t.name())) || (scope != NotifyType.Scope.ALL && t.scope() != scope)) {
                continue;
            }
            collect(t, today, s, projects, alerts);
        }

        Set<String> userIds = new HashSet<>();
        alerts.forEach(a -> userIds.add(a.userId()));
        Map<String, String> userNames = names.resolve(userIds);

        int sent = 0;
        int skipped = 0;
        int failed = 0;
        int dup = 0;
        Map<String, Integer> byType = new java.util.LinkedHashMap<>();
        List<String> samples = new ArrayList<>();
        for (Alert a : alerts) {
            byType.merge(a.type().label(), 1, Integer::sum);
            if (logRepository.existsByRefIdAndKindCodeAndStatusCodeNotAndCreatedDateAfter(a.refId(), a.kind(), "FAILED", today.atStartOfDay())) {
                dup++;
                continue;
            }
            if (dryRun) {
                if (samples.size() < 15) {
                    samples.add("[" + a.type().label() + "] " + a.sample() + " → " + userNames.getOrDefault(a.userId(), a.userId()));
                }
                continue;
            }
            try {
                var result = a.wbsTaskId() != null
                        ? notifyService.sendProgressPrompt(a.wbsTaskId(), a.kind(), a.headline())
                        : notifyService.send(a.userId(), a.kind(), a.text(), a.refId());
                switch (result.statusCode()) {
                    case NotifyService.SENT -> sent++;
                    case NotifyService.SKIPPED -> skipped++;
                    default -> failed++;
                }
            } catch (RuntimeException e) {
                failed++;
                log.warn("기한 알림 발송 실패 type={} ref={}: {}", a.type(), a.refId(), e.getMessage());
            }
        }
        return new NotifyDto.RunResult(dryRun, alerts.size(), sent, skipped, failed, dup, byType, samples);
    }

    private void collect(NotifyType type, LocalDate today, NotifyDto.Settings s, Map<String, Project> projects, List<Alert> out) {
        switch (type) {
            case WBS_TASK -> {
                Set<String> active = new HashSet<>();
                projects.values().stream().filter(p -> "수행 중".equals(p.getStatusCode())).forEach(p -> active.add(p.getId()));
                for (WbsTask t : taskRepository.search("")) {
                    if (!"LEAF".equals(t.getNodeTypeCode()) || t.getAssigneeUserId() == null || !active.contains(t.getProjectId())
                            || (t.getProgressPct() != null && t.getProgressPct().compareTo(BigDecimal.valueOf(100)) >= 0)) {
                        continue;
                    }
                    int d = lead(t.getEndDate(), today, s);
                    if (d >= 0) {
                        String head = d == 0 ? "오늘 마감" : "마감 D-" + d;
                        out.add(new Alert(type, "TASK_DUE", t.getId(), t.getAssigneeUserId(), head, null, t.getId(), head + " · " + t.getTaskName()));
                    }
                    if (s.startAlert() && today.equals(t.getStartDate())) {
                        out.add(new Alert(type, "TASK_START", t.getId(), t.getAssigneeUserId(), "오늘 시작", null, t.getId(), "오늘 시작 · " + t.getTaskName()));
                    }
                }
            }
            case SALES_NOTE -> salesNoteRepository.findAll().stream()
                    .filter(n -> "N".equals(n.getDeletedYn()) && "N".equals(n.getDoneYn()) && n.getAuthorUserId() != null)
                    .forEach(n -> {
                        int d = lead(n.getNextDate(), today, s);
                        if (d >= 0) {
                            String what = n.getNextAction() == null || n.getNextAction().isBlank() ? n.getTitle() : n.getNextAction();
                            add(out, type, n.getId(), n.getAuthorUserId(), "영업 노트 다음 할 일", d, what + projectSuffix(projects, n.getProjectId()));
                        }
                    });
            case PROJECT_DUE -> projects.values().stream()
                    .filter(p -> List.of("공고 중", "제안 중").contains(p.getStatusCode()) && p.getSalesUserId() != null)
                    .forEach(p -> {
                        int d = lead(p.getDueDate(), today, s);
                        if (d >= 0) {
                            add(out, type, p.getId(), p.getSalesUserId(), "제안 마감", d, p.getProjectName());
                        }
                    });
            case PROPOSAL_DUE -> proposalRepository.findAll().stream()
                    .filter(p -> "N".equals(p.getDeletedYn()) && List.of("작성 중", "내부 검토").contains(p.getStatusCode()) && p.getOwnerUserId() != null)
                    .forEach(p -> {
                        int d = lead(p.getDueDate(), today, s);
                        if (d >= 0) {
                            add(out, type, p.getId(), p.getOwnerUserId(), "제안서 제출 마감", d, p.getTitle() + projectSuffix(projects, p.getProjectId()));
                        }
                    });
            case ISSUE_DUE -> issueRepository.findAll().stream()
                    .filter(i -> "N".equals(i.getDeletedYn()) && List.of("오픈", "진행 중").contains(i.getStatusCode()) && i.getAssigneeUserId() != null)
                    .forEach(i -> {
                        int d = lead(i.getDueDate(), today, s);
                        if (d >= 0) {
                            add(out, type, i.getId(), i.getAssigneeUserId(), "이슈 조치 기한", d, i.getTitle() + projectSuffix(projects, i.getProjectId()));
                        }
                    });
            case OPS_TASK_DUE -> opsTaskRepository.findAll().stream()
                    .filter(t -> "N".equals(t.getDeletedYn()) && !"완료".equals(t.getStatusCode()) && t.getAssigneeUserId() != null)
                    .forEach(t -> {
                        int d = lead(t.getDueDate(), today, s);
                        if (d >= 0) {
                            add(out, type, t.getId(), t.getAssigneeUserId(), "운영 업무 기한", d, t.getTitle() + projectSuffix(projects, t.getProjectId()));
                        }
                    });
            case RFP_DUE -> rfpRepository.findAll().stream().filter(r -> "N".equals(r.getDeletedYn())).forEach(r -> {
                int d = lead(r.getDueDate(), today, s);
                if (d < 0) {
                    return;
                }
                rfpEvaluatorRepository.findAllByRfpIdAndDeletedYnOrderByCreatedDateAsc(r.getId(), "N").stream()
                        .filter(e -> "N".equals(e.getSubmittedYn()) && e.getEvaluatorUserId() != null)
                        .forEach(e -> add(out, type, e.getId(), e.getEvaluatorUserId(), "RFP 평가 제출 마감", d,
                                r.getTitle() + " (평가 미제출)" + projectSuffix(projects, r.getProjectId())));
            });
            case OPS_OPP_NEXT -> opsOpportunityRepository.findAll().stream()
                    .filter(o -> "N".equals(o.getDeletedYn()) && !List.of("수주", "실패/보류").contains(o.getStageCode()) && o.getOwnerUserId() != null)
                    .forEach(o -> {
                        int d = lead(o.getNextDate(), today, s);
                        if (d >= 0) {
                            add(out, type, o.getId(), o.getOwnerUserId(), "운영 영업 다음 할 일", d,
                                    o.getTitle() + (o.getNextAction() == null ? "" : " — " + o.getNextAction()) + projectSuffix(projects, o.getProjectId()));
                        }
                    });
            case NEXT_OPP_NEXT -> nextOpportunityRepository.findAll().stream()
                    .filter(o -> "N".equals(o.getDeletedYn()) && !List.of("이관 완료", "보류/실패").contains(o.getStageCode()) && o.getOwnerUserId() != null)
                    .forEach(o -> {
                        int d = lead(o.getNextDate(), today, s);
                        if (d >= 0) {
                            add(out, type, o.getId(), o.getOwnerUserId(), "차기 사업 다음 할 일", d,
                                    o.getTitle() + (o.getNextAction() == null ? "" : " — " + o.getNextAction()) + projectSuffix(projects, o.getProjectId()));
                        }
                    });
            case OPS_END -> operationRepository.findAllByDeletedYn("N").forEach(op -> {
                Project p = projects.get(op.getProjectId());
                String user = p == null ? null : (p.getPmUserId() != null ? p.getPmUserId() : p.getSalesUserId());
                if (user == null || op.getEndDate() == null) {
                    return;
                }
                for (int d : OPS_END_DAYS) {
                    if (op.getEndDate().equals(today.plusDays(d))) {
                        add(out, type, op.getId(), user, "운영 계약 종료", d, p.getProjectName());
                    }
                }
            });
            case OPS_INCIDENT_SLA -> collectSla(LocalDateTime.now(), projects, out);
            default -> { }
        }
    }

    /** 운영 이슈의 응답·해결 시한(접수 시각 + SLA 시간)이 1~2시간 안으로 다가왔거나 지난 건. */
    private void collectSla(LocalDateTime now, Map<String, Project> projects, List<Alert> out) {
        Map<String, ProjectOperation> ops = new HashMap<>();
        operationRepository.findAllByDeletedYn("N").forEach(o -> ops.put(o.getProjectId(), o));
        incidentRepository.findAll().stream()
                .filter(i -> "N".equals(i.getDeletedYn()) && !List.of("해결", "종결").contains(i.getStatusCode()) && i.getAssigneeUserId() != null
                        && i.getReceivedAt() != null)
                .forEach(i -> {
                    ProjectOperation op = ops.get(i.getProjectId());
                    if (op == null) {
                        return;
                    }
                    slaAlert(out, i.getId(), i.getAssigneeUserId(), "OPS_INCIDENT_RESPOND", "응답", i.getReceivedAt(), op.getSlaRespondHours(),
                            i.getRespondedAt() == null, 1, now, i.getTitle() + projectSuffix(projects, i.getProjectId()));
                    slaAlert(out, i.getId(), i.getAssigneeUserId(), "OPS_INCIDENT_RESOLVE", "해결", i.getReceivedAt(), op.getSlaResolveHours(),
                            true, 2, now, i.getTitle() + projectSuffix(projects, i.getProjectId()));
                });
    }

    private void slaAlert(List<Alert> out, String refId, String userId, String kind, String label, LocalDateTime received, Integer hours,
                          boolean pending, int leadHours, LocalDateTime now, String what) {
        if (hours == null || !pending) {
            return;
        }
        LocalDateTime due = received.plusHours(hours);
        long minutes = Duration.between(now, due).toMinutes();
        if (minutes > leadHours * 60L || minutes < -24 * 60L) {
            return; // 아직 멀었거나 하루 넘게 지난 건은 알리지 않는다
        }
        String when = minutes >= 0 ? "SLA " + label + " 시한 " + (minutes / 60) + "시간 " + (minutes % 60) + "분 전" : "SLA " + label + " 시한 초과";
        out.add(new Alert(NotifyType.OPS_INCIDENT_SLA, kind, refId, userId, when, "[PMS] " + when + " — " + what, null, when + " · " + what));
    }

    private static void add(List<Alert> out, NotifyType type, String refId, String userId, String title, int d, String what) {
        String when = d == 0 ? "오늘" : "D-" + d;
        out.add(new Alert(type, type.name(), refId, userId, title, "[PMS] " + title + " " + when + " — " + what, null, when + " · " + what));
    }

    /** 기한이 설정한 기준일(당일·n일 전) 중 하나이면 그 n 을, 아니면 -1. */
    private static int lead(LocalDate date, LocalDate today, NotifyDto.Settings s) {
        if (date == null) {
            return -1;
        }
        for (int d : s.dueDays()) {
            if (date.equals(today.plusDays(d))) {
                return d;
            }
        }
        return -1;
    }

    private static String projectSuffix(Map<String, Project> projects, String projectId) {
        Project p = projectId == null ? null : projects.get(projectId);
        return p == null ? "" : " (" + p.getProjectName() + ")";
    }

    private void upsert(String key, String value) {
        Setting s = settingRepository.findBySettingKeyAndDeletedYn(key, "N").orElseGet(() -> Setting.create().withSettingKey(key));
        settingRepository.save(s.withSettingValue(value));
    }
}
