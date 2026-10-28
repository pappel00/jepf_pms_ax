package com.platform.module.pms.service;

import com.platform.framework.core.common.exception.BusinessException;
import com.platform.module.pms.domain.ProgressSnapshot;
import com.platform.module.pms.domain.Project;
import com.platform.module.pms.domain.Report;
import com.platform.module.pms.domain.ReportHistory;
import com.platform.module.pms.domain.ReportVersion;
import com.platform.module.pms.dto.ReportDto;
import com.platform.module.pms.exception.PmsErrorCode;
import com.platform.module.pms.repository.ProgressSnapshotRepository;
import com.platform.module.pms.repository.ProjectRepository;
import com.platform.module.pms.repository.ReportHistoryRepository;
import com.platform.module.pms.repository.ReportRepository;
import com.platform.module.pms.repository.ReportVersionRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 주간·월간 보고서 작성/제출 상태와 주간 진척 스냅샷.
 *  - 보고서는 (프로젝트, 주간/월간, 기간 시작일) 단위 1건: 작성 중 → 제출 완료 → 수정 중 → 제출 완료(재제출).
 *    제출할 때마다 제출 시점 요약(전체 진척·목표·특이사항)을 버전으로 남기고, 모든 상태 변경은 이력에 쌓는다.
 *  - 보고 본문(진행 업무·차기 업무·이슈 목록)은 저장하지 않고 화면이 조회 시점에 계산한다.
 *  - 스냅샷은 프로젝트별 주(월요일) 단위 전체 진척·목표. 같은 주 재저장은 덮어쓴다(자동 저장 스케줄러는 후속).
 */
@Service
@Transactional(readOnly = true)
public class ReportService {

    static final String DRAFTING = "작성 중";
    static final String SUBMITTED = "제출 완료";
    static final String EDITING = "수정 중";

    private final ProjectAccess access;
    private final ReportRepository reportRepository;
    private final ReportVersionRepository versionRepository;
    private final ReportHistoryRepository historyRepository;
    private final ProgressSnapshotRepository snapshotRepository;
    private final ProjectRepository projectRepository;
    private final ProjectService projectService;
    private final UserNameResolver names;

    public ReportService(ProjectAccess access, ReportRepository reportRepository, ReportVersionRepository versionRepository,
                         ReportHistoryRepository historyRepository, ProgressSnapshotRepository snapshotRepository,
                         ProjectRepository projectRepository, ProjectService projectService, UserNameResolver names) {
        this.access = access;
        this.reportRepository = reportRepository;
        this.versionRepository = versionRepository;
        this.historyRepository = historyRepository;
        this.snapshotRepository = snapshotRepository;
        this.projectRepository = projectRepository;
        this.projectService = projectService;
        this.names = names;
    }

    // ---------- 보고서 ----------

    public List<ReportDto.Response> getList() {
        return access.filterMine(toResponses(reportRepository.findAllByDeletedYnOrderByPeriodStartAsc("N")), ReportDto.Response::projectId);
    }

    /** 특이사항 저장 — 작성 중/수정 중에만 가능. 첫 저장이면 「작성」 이력을 남긴다. */
    @Transactional
    public ReportDto.Response saveDraft(String projectId, String type, LocalDate periodStart, String memo) {
        Report r = findOrCreate(projectId, type, periodStart);
        if (SUBMITTED.equals(r.getStatusCode())) {
            throw new BusinessException(PmsErrorCode.REPORT_INVALID, "제출한 보고서는 수정을 시작한 뒤에 고칠 수 있습니다.");
        }
        r.withMemo(PmsSupport.blankToNull(memo));
        return toResponses(List.of(r)).get(0);
    }

    /** 제출(최초/재제출) — 제출 시점의 요약을 버전으로 남긴다. */
    @Transactional
    public ReportDto.Response submit(String projectId, String type, LocalDate periodStart, ReportDto.SubmitRequest req) {
        Report r = findOrCreate(projectId, type, periodStart);
        if (SUBMITTED.equals(r.getStatusCode())) {
            throw new BusinessException(PmsErrorCode.REPORT_INVALID, "이미 제출된 보고서입니다.");
        }
        if (req.memo() != null) {
            r.withMemo(PmsSupport.blankToNull(req.memo()));
        }
        int version = r.getSubmitVersion() + 1;
        String me = PmsSupport.currentUserId();
        r.withStatusCode(SUBMITTED).withSubmitVersion(version);
        versionRepository.save(ReportVersion.create().withReportId(r.getId()).withVersionNo(version).withSubmittedBy(me)
                .withSubmittedAt(LocalDateTime.now()).withOverallPct(req.overallPct()).withTargetPct(req.targetPct()).withMemo(r.getMemo()));
        addHistory(r.getId(), "제출", me, version == 1 ? "제출 (v" + version + ")" : "재제출 (v" + version + ")");
        return toResponses(List.of(r)).get(0);
    }

    /** 제출 후 수정 시작 — 사유 필수. */
    @Transactional
    public ReportDto.Response reopen(String projectId, String type, LocalDate periodStart, String reason) {
        Report r = require(projectId, type, periodStart);
        if (!SUBMITTED.equals(r.getStatusCode())) {
            throw new BusinessException(PmsErrorCode.REPORT_INVALID, "제출 완료 상태에서만 수정을 시작할 수 있습니다: " + r.getStatusCode());
        }
        r.withStatusCode(EDITING);
        addHistory(r.getId(), "수정", PmsSupport.currentUserId(), "수정 시작: " + reason.trim());
        return toResponses(List.of(r)).get(0);
    }

    /** 수정 취소 — 마지막 제출본의 특이사항으로 되돌리고 제출 완료 상태로 복귀. */
    @Transactional
    public ReportDto.Response cancelEdit(String projectId, String type, LocalDate periodStart) {
        Report r = require(projectId, type, periodStart);
        if (!EDITING.equals(r.getStatusCode())) {
            throw new BusinessException(PmsErrorCode.REPORT_INVALID, "수정 중인 보고서만 취소할 수 있습니다: " + r.getStatusCode());
        }
        List<ReportVersion> versions = versionRepository.findAllByReportIdInAndDeletedYnOrderByVersionNoAsc(List.of(r.getId()), "N");
        if (!versions.isEmpty()) {
            r.withMemo(versions.get(versions.size() - 1).getMemo());
        }
        r.withStatusCode(SUBMITTED);
        addHistory(r.getId(), "수정 취소", PmsSupport.currentUserId(), "수정 취소 (제출본 v" + r.getSubmitVersion() + " 유지)");
        return toResponses(List.of(r)).get(0);
    }

    // ---------- 스냅샷 ----------

    public List<ReportDto.SnapshotResponse> getSnapshots() {
        List<ProgressSnapshot> list = snapshotRepository.findAllByDeletedYnOrderBySnapshotDateAsc("N");
        Map<String, String> codes = projectCodes(list.stream().map(ProgressSnapshot::getProjectId).collect(java.util.stream.Collectors.toSet()));
        return access.filterMine(list, ProgressSnapshot::getProjectId).stream().map(s -> toSnapshot(s, codes.get(s.getProjectId()))).toList();
    }

    /** 스냅샷 저장 — 날짜가 속한 주의 월요일로 맞추고, 같은 주 스냅샷이 있으면 덮어쓴다. */
    @Transactional
    public ReportDto.SnapshotResponse saveSnapshot(String projectId, ReportDto.SnapshotRequest req) {
        Project project = projectService.requireProject(projectId);
        LocalDate monday = req.snapshotDate().with(DayOfWeek.MONDAY);
        ProgressSnapshot s = snapshotRepository.findByProjectIdAndSnapshotDateAndDeletedYn(projectId, monday, "N")
                .orElseGet(() -> ProgressSnapshot.create().withProjectId(projectId).withSnapshotDate(monday));
        s.withOverallPct(req.overallPct().setScale(1, RoundingMode.HALF_UP)).withTargetPct(req.targetPct().setScale(1, RoundingMode.HALF_UP))
                .withSavedBy(PmsSupport.currentUserId());
        return toSnapshot(snapshotRepository.save(s), project.getProjectCode());
    }

    // ---------- 내부 ----------

    private Report findOrCreate(String projectId, String type, LocalDate periodStart) {
        projectService.requireProject(projectId);
        access.requireManager(projectId, "보고서 작성·제출");
        String t = normalizeType(type);
        return reportRepository.findByProjectIdAndReportTypeCodeAndPeriodStartAndDeletedYn(projectId, t, periodStart, "N")
                .orElseGet(() -> {
                    LocalDate end = "WEEK".equals(t) ? periodStart.plusDays(6) : YearMonth.from(periodStart).atEndOfMonth();
                    Report created = reportRepository.saveAndFlush(Report.create().withProjectId(projectId).withReportTypeCode(t)
                            .withPeriodStart(periodStart).withPeriodEnd(end).withStatusCode(DRAFTING).withSubmitVersion(0)
                            .withAuthorUserId(PmsSupport.currentUserId()));
                    addHistory(created.getId(), "작성", PmsSupport.currentUserId(), "보고서 작성 시작");
                    return created;
                });
    }

    private Report require(String projectId, String type, LocalDate periodStart) {
        access.requireManager(projectId, "보고서 수정");
        return reportRepository.findByProjectIdAndReportTypeCodeAndPeriodStartAndDeletedYn(projectId, normalizeType(type), periodStart, "N")
                .orElseThrow(() -> new BusinessException(PmsErrorCode.REPORT_INVALID, "아직 작성된 보고서가 없습니다."));
    }

    private static String normalizeType(String type) {
        String t = type == null ? "" : type.toUpperCase();
        if (!t.equals("WEEK") && !t.equals("MONTH")) {
            throw new BusinessException(PmsErrorCode.REPORT_INVALID, "보고서 유형은 WEEK/MONTH 입니다: " + type);
        }
        return t;
    }

    private void addHistory(String reportId, String action, String actor, String text) {
        historyRepository.save(ReportHistory.create().withReportId(reportId).withActionCode(action).withActorUserId(actor)
                .withActedAt(LocalDateTime.now()).withHistoryText(text));
    }

    private Map<String, String> projectCodes(Set<String> projectIds) {
        Map<String, String> codes = new HashMap<>();
        projectRepository.findAllById(projectIds).forEach(p -> codes.put(p.getId(), p.getProjectCode()));
        return codes;
    }

    private static ReportDto.SnapshotResponse toSnapshot(ProgressSnapshot s, String code) {
        return new ReportDto.SnapshotResponse(s.getId(), s.getProjectId(), code, s.getSnapshotDate(), s.getOverallPct(), s.getTargetPct());
    }

    private List<ReportDto.Response> toResponses(List<Report> list) {
        if (list.isEmpty()) {
            return List.of();
        }
        reportRepository.flush();
        List<String> ids = list.stream().map(Report::getId).toList();
        Map<String, List<ReportVersion>> versions = new HashMap<>();
        versionRepository.findAllByReportIdInAndDeletedYnOrderByVersionNoAsc(ids, "N")
                .forEach(v -> versions.computeIfAbsent(v.getReportId(), k -> new ArrayList<>()).add(v));
        Map<String, List<ReportHistory>> histories = new HashMap<>();
        historyRepository.findAllByReportIdInAndDeletedYnOrderByActedAtAsc(ids, "N")
                .forEach(h -> histories.computeIfAbsent(h.getReportId(), k -> new ArrayList<>()).add(h));
        Set<String> userIds = new HashSet<>();
        versions.values().forEach(l -> l.forEach(v -> userIds.add(v.getSubmittedBy())));
        histories.values().forEach(l -> l.forEach(h -> userIds.add(h.getActorUserId())));
        Map<String, String> userNames = names.resolve(userIds);
        Map<String, String> codes = projectCodes(list.stream().map(Report::getProjectId).collect(java.util.stream.Collectors.toSet()));
        return list.stream().map(r -> {
            List<ReportVersion> vs = versions.getOrDefault(r.getId(), List.of());
            ReportVersion last = vs.isEmpty() ? null : vs.get(vs.size() - 1);
            ReportDto.Submitted submitted = last == null ? null : new ReportDto.Submitted(last.getSubmittedAt(),
                    userNames.get(last.getSubmittedBy()), last.getOverallPct(), last.getTargetPct(), last.getMemo());
            return new ReportDto.Response(r.getId(), r.getProjectId(), codes.get(r.getProjectId()), r.getReportTypeCode(), r.getPeriodStart(),
                    r.getPeriodEnd(), r.getStatusCode(), r.getMemo(), r.getSubmitVersion(), submitted,
                    histories.getOrDefault(r.getId(), List.of()).stream().map(h -> new ReportDto.HistoryEntry(h.getActionCode(),
                            userNames.get(h.getActorUserId()), h.getActedAt(), h.getHistoryText())).toList());
        }).toList();
    }
}
