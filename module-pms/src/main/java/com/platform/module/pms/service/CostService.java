package com.platform.module.pms.service;

import com.platform.framework.core.common.exception.BusinessException;
import com.platform.framework.core.common.exception.TenantException;
import com.platform.module.pms.domain.ActualMm;
import com.platform.module.pms.domain.Project;
import com.platform.module.pms.domain.Rate;
import com.platform.module.pms.domain.Setting;
import com.platform.module.pms.dto.CostDto;
import com.platform.module.pms.exception.PmsErrorCode;
import com.platform.module.pms.repository.ActualMmRepository;
import com.platform.module.pms.repository.ProjectMemberRepository;
import com.platform.module.pms.repository.ProjectRepository;
import com.platform.module.pms.repository.RateRepository;
import com.platform.module.pms.repository.SettingRepository;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 인력·원가 — 직무 × 등급별 M/M 단가(적용 기간), 월별 실적 M/M, PMS 설정(키-값).
 *  - 단가: 같은 직무·등급의 적용 기간이 겹칠 수 없다(서버 검증). 조회는 원가 열람 권한, 등록·수정·삭제는 승인 권한이 필요하다(컨트롤러).
 *  - 실적 M/M: 프로젝트 관리자(또는 승인 권한자)만 입력한다. 운영 중 프로젝트는 OPS, 그 외는 EXEC 로 구분한다.
 *  - 예상 인건비는 저장하지 않고 화면이 계산한다: 월별 계획 M/M × 그 달 적용 단가(결과는 프로젝트 예상 원가로 반영).
 */
@Service
@Transactional(readOnly = true)
public class CostService {

    static final Set<String> GRADES = Set.of("특급", "고급", "중급", "초급");
    /** 변경할 수 있는 설정 키와 허용 범위. */
    private static final Map<String, int[]> SETTING_RANGE = Map.of(
            "staffing.over_limit_pct", new int[]{100, 200},
            "staffing.idle_limit_pct", new int[]{0, 100});

    private final RateRepository rateRepository;
    private final ActualMmRepository actualRepository;
    private final SettingRepository settingRepository;
    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository memberRepository;
    private final ProjectService projectService;
    private final UserNameResolver names;

    public CostService(RateRepository rateRepository, ActualMmRepository actualRepository, SettingRepository settingRepository,
                       ProjectRepository projectRepository, ProjectMemberRepository memberRepository,
                       ProjectService projectService, UserNameResolver names) {
        this.rateRepository = rateRepository;
        this.actualRepository = actualRepository;
        this.settingRepository = settingRepository;
        this.projectRepository = projectRepository;
        this.memberRepository = memberRepository;
        this.projectService = projectService;
        this.names = names;
    }

    // ---------- 단가 ----------

    public List<CostDto.RateResponse> getRates() {
        return rateRepository.findAllByDeletedYnOrderByJobCodeAscGradeCodeAscFromDateAsc("N").stream().map(CostService::toRate).toList();
    }

    @Transactional
    public CostDto.RateResponse createRate(CostDto.RateRequest r) {
        validate(r, null);
        return toRate(rateRepository.saveAndFlush(apply(Rate.create(), r)));
    }

    @Transactional
    public CostDto.RateResponse updateRate(String id, CostDto.RateRequest r) {
        Rate rate = findRate(id);
        validate(r, id);
        return toRate(apply(rate, r));
    }

    @Transactional
    public void deleteRate(String id) {
        findRate(id).markDeleted();
    }

    private void validate(CostDto.RateRequest r, String selfId) {
        if (!GRADES.contains(r.gradeCode())) {
            throw new BusinessException(PmsErrorCode.RATE_INVALID, "알 수 없는 등급입니다: " + r.gradeCode());
        }
        if (r.toDate() != null && r.toDate().isBefore(r.fromDate())) {
            throw new BusinessException(PmsErrorCode.RATE_INVALID, "적용 종료일이 시작일보다 빠릅니다.");
        }
        LocalDate end = r.toDate() == null ? LocalDate.of(9999, 12, 31) : r.toDate();
        for (Rate o : rateRepository.findAllByJobCodeAndGradeCodeAndDeletedYn(r.jobCode(), r.gradeCode(), "N")) {
            if (o.getId().equals(selfId)) {
                continue;
            }
            LocalDate oEnd = o.getToDate() == null ? LocalDate.of(9999, 12, 31) : o.getToDate();
            if (!o.getFromDate().isAfter(end) && !r.fromDate().isAfter(oEnd)) {
                throw new BusinessException(PmsErrorCode.RATE_INVALID,
                        "같은 직무·등급의 적용 기간이 겹칩니다: " + o.getFromDate() + " ~ " + (o.getToDate() == null ? "계속" : o.getToDate()));
            }
        }
    }

    private static Rate apply(Rate rate, CostDto.RateRequest r) {
        return rate.withJobCode(r.jobCode()).withGradeCode(r.gradeCode()).withUnitPrice(r.unitPrice()).withFromDate(r.fromDate())
                .withToDate(r.toDate());
    }

    private Rate findRate(String id) {
        return rateRepository.findByIdAndDeletedYn(id, "N")
                .orElseThrow(() -> new TenantException(PmsErrorCode.RATE_NOT_FOUND, "존재하지 않는 단가입니다: " + id));
    }

    private static CostDto.RateResponse toRate(Rate r) {
        return new CostDto.RateResponse(r.getId(), r.getJobCode(), r.getGradeCode(), r.getUnitPrice(), r.getFromDate(), r.getToDate());
    }

    // ---------- 실적 M/M ----------

    public List<CostDto.ActualResponse> getActuals() {
        List<ActualMm> list = actualRepository.findAllByDeletedYnOrderByActualMonthAsc("N");
        Set<String> userIds = new HashSet<>();
        Set<String> projectIds = new HashSet<>();
        list.forEach(a -> {
            userIds.add(a.getUserId());
            projectIds.add(a.getProjectId());
        });
        Map<String, String> userNames = names.resolve(userIds);
        Map<String, String> codes = new HashMap<>();
        projectRepository.findAllById(projectIds).forEach(p -> codes.put(p.getId(), p.getProjectCode()));
        return list.stream().map(a -> new CostDto.ActualResponse(a.getProjectId(), codes.get(a.getProjectId()), a.getKindCode(), a.getUserId(),
                userNames.get(a.getUserId()), a.getActualMonth(), a.getActualMm())).toList();
    }

    /** 실적 저장 — 프로젝트 관리자(또는 승인 권한자)만. actualMm 이 null 이면 삭제한다. */
    @Transactional
    public void saveActual(String projectId, CostDto.ActualRequest r) {
        Project project = projectService.requireProject(projectId);
        String kind = "운영 중".equals(project.getStatusCode()) ? "OPS" : "EXEC";
        String me = PmsSupport.currentUserId();
        boolean manager = memberRepository.findAllByProjectIdAndKindCodeOrderByCreatedDateAsc(projectId, kind).stream()
                .anyMatch(m -> "Y".equals(m.getManagerYn()) && me.equals(m.getUserId()));
        if (!manager && !PmsSupport.canApprove()) {
            throw new BusinessException(PmsErrorCode.ACTUAL_FORBIDDEN, "실적 M/M 은 프로젝트 관리자만 입력할 수 있습니다.");
        }
        ActualMm row = actualRepository.findByProjectIdAndKindCodeAndUserIdAndActualMonthAndDeletedYn(projectId, kind, r.userId(), r.month(), "N").orElse(null);
        if (r.actualMm() == null) {
            if (row != null) {
                row.markDeleted();
            }
            return;
        }
        if (row == null) {
            row = ActualMm.create().withProjectId(projectId).withKindCode(kind).withUserId(r.userId()).withActualMonth(r.month());
        }
        actualRepository.save(row.withActualMm(r.actualMm()).withInputBy(me));
    }

    // ---------- 설정 ----------

    public Map<String, String> getSettings() {
        Map<String, String> out = new HashMap<>();
        settingRepository.findAllByDeletedYn("N").forEach(s -> out.put(s.getSettingKey(), s.getSettingValue()));
        return out;
    }

    @Transactional
    public Map<String, String> saveSettings(Map<String, String> values) {
        for (Map.Entry<String, String> e : values.entrySet()) {
            int[] range = SETTING_RANGE.get(e.getKey());
            if (range == null) {
                throw new BusinessException(PmsErrorCode.SETTING_INVALID, "알 수 없는 설정입니다: " + e.getKey());
            }
            int v;
            try {
                v = Integer.parseInt(e.getValue().trim());
            } catch (RuntimeException ex) {
                throw new BusinessException(PmsErrorCode.SETTING_INVALID, "숫자를 입력하세요: " + e.getKey());
            }
            if (v < range[0] || v > range[1]) {
                throw new BusinessException(PmsErrorCode.SETTING_INVALID, e.getKey() + " 은(는) " + range[0] + "~" + range[1] + " 범위여야 합니다.");
            }
            Setting s = settingRepository.findBySettingKeyAndDeletedYn(e.getKey(), "N")
                    .orElseGet(() -> Setting.create().withSettingKey(e.getKey()));
            settingRepository.save(s.withSettingValue(String.valueOf(v)));
        }
        return getSettings();
    }
}
