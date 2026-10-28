package com.platform.module.pms.service;

import com.platform.framework.core.common.exception.BusinessException;
import com.platform.framework.core.common.exception.InvalidRequestException;
import com.platform.framework.core.common.exception.TenantException;
import com.platform.framework.core.common.response.ErrorDetail;
import com.platform.framework.core.common.response.PageInfo;
import com.platform.framework.core.common.response.PagedResult;
import com.platform.module.pms.domain.Customer;
import com.platform.module.pms.domain.CustomerContact;
import com.platform.module.pms.domain.Project;
import com.platform.module.pms.domain.StatusHistory;
import com.platform.module.pms.dto.ProjectDto;
import com.platform.module.pms.exception.PmsErrorCode;
import com.platform.module.pms.repository.CustomerContactRepository;
import com.platform.module.pms.repository.CustomerRepository;
import com.platform.module.pms.repository.ProjectRepository;
import com.platform.module.pms.repository.StatusHistoryRepository;
import com.platform.module.user.domain.User;
import com.platform.module.user.repository.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 프로젝트(사업) 마스터 — 영업(공고 전)부터 운영까지 1건. 상태 변경은 공통 상태 이력(pms_status_history)에 남긴다.
 * 수행 관리 정보(관리자·투입 인력)·운영 정보·WBS 는 이후 단계에서 별도 테이블로 붙는다.
 */
@Service
@Transactional(readOnly = true)
public class ProjectService {

    static final String TARGET_TYPE = "PROJECT";

    /** 상태 흐름 순서 (영업 → 수주 → 수행 → 운영, 종료 상태는 맨 뒤). */
    static final List<String> STATUSES = List.of("공고 전", "공고 중", "제안 중", "평가 중", "협상 중",
            "수주", "수행 중", "수행 종료", "운영 중", "실패", "보류/취소");
    private static final Set<String> FAIL_STATUSES = Set.of("실패", "보류/취소");
    /** 삭제 가능한 상태: 영업 진행 중이거나 종료(실패/보류)된 프로젝트. 수주 이후 프로젝트는 삭제할 수 없다. */
    private static final Set<String> DELETABLE = Set.of("공고 전", "공고 중", "제안 중", "평가 중", "협상 중", "실패", "보류/취소");

    private final ProjectAccess access;
    private final ProjectRepository projectRepository;
    private final CustomerRepository customerRepository;
    private final CustomerContactRepository contactRepository;
    private final StatusHistoryRepository historyRepository;
    private final UserRepository userRepository;

    public ProjectService(ProjectAccess access, ProjectRepository projectRepository, CustomerRepository customerRepository,
                          CustomerContactRepository contactRepository, StatusHistoryRepository historyRepository,
                          UserRepository userRepository) {
        this.access = access;
        this.projectRepository = projectRepository;
        this.customerRepository = customerRepository;
        this.contactRepository = contactRepository;
        this.historyRepository = historyRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public ProjectDto.Response create(ProjectDto.Request request) {
        validate(request);
        String code = PmsSupport.nextCode("PRJ-" + java.time.Year.now().getValue() + "-",
                projectRepository.findAll().stream().map(Project::getProjectCode).toList(), 3);
        Project project = apply(Project.create().withProjectCode(code), request);
        Project saved = projectRepository.saveAndFlush(project);
        recordHistory(saved.getId(), null, saved.getStatusCode(), "프로젝트 등록");
        return toResponse(saved);
    }

    public ProjectDto.Response getById(String id) {
        access.requireAccess(id);
        return toResponse(findActiveOrThrow(id));
    }

    public PagedResult<ProjectDto.Response> getList(String status, String customerId, String salesUserId,
                                                    String keyword, Pageable pageable) {
        Page<Project> page = projectRepository.search(nz(status), nz(customerId), nz(salesUserId),
                nz(keyword).toLowerCase(), pageable);
        Map<String, String> names = userNames(page.getContent());
        List<ProjectDto.Response> content = page.map(p -> ProjectDto.Response.from(p, names, PmsSupport.canViewCost())).getContent();
        // 참여 중인 프로젝트만(전체 열람 권한이 아니면). 페이지 합계는 걸러내기 전 값이다.
        return new PagedResult<>(access.filter(content, ProjectDto.Response::id), PageInfo.of(page));
    }

    @Transactional
    public ProjectDto.Response update(String id, ProjectDto.Request request) {
        validate(request);
        Project project = findActiveOrThrow(id);
        String before = project.getStatusCode();
        apply(project, request);
        if (!before.equals(project.getStatusCode())) {
            recordHistory(project.getId(), before, project.getStatusCode(), request.memo());
        }
        return toResponse(project);
    }

    /** 상태만 변경 — 단계전환·수행 종료·운영 전환 등 다른 화면에서 호출한다. */
    @Transactional
    public ProjectDto.Response changeStatus(String id, ProjectDto.StatusRequest request) {
        if (!STATUSES.contains(request.statusCode())) {
            throw new BusinessException(PmsErrorCode.PROJECT_STATUS_INVALID, "알 수 없는 프로젝트 상태입니다: " + request.statusCode());
        }
        Project project = findActiveOrThrow(id);
        String before = project.getStatusCode();
        if (!before.equals(request.statusCode())) {
            project.withStatusCode(request.statusCode());
            recordHistory(project.getId(), before, request.statusCode(), request.memo());
        }
        return toResponse(project);
    }

    /** 수행단계전환 승인 — 상태를 「수행 중」으로, 전환일·수행 PM·열람 제한을 반영한다(상태 이력 기록). */
    @Transactional
    public void startExecution(String projectId, java.time.LocalDate date, String pmUserId, boolean restricted, String memo) {
        Project project = findActiveOrThrow(projectId);
        String before = project.getStatusCode();
        project.withStatusCode("수행 중").withExecStartDate(date).withPmUserId(PmsSupport.blankToNull(pmUserId))
                .withViewRestrictedYn(restricted ? "Y" : "N");
        if (!before.equals("수행 중")) {
            recordHistory(project.getId(), before, "수행 중", memo);
        }
    }

    /** 단계전환 롤백 — 「수주」로 되돌리고 전환일·수행 PM·열람 제한을 지운다. */
    @Transactional
    public void rollbackExecution(String projectId, String reason) {
        Project project = findActiveOrThrow(projectId);
        String before = project.getStatusCode();
        project.withStatusCode("수주").withExecStartDate(null).withPmUserId(null).withViewRestrictedYn("N");
        recordHistory(project.getId(), before, "수주", "단계전환 롤백: " + reason);
    }

    /** 수행 종료 처리 — 「수행 종료」로 바꾸고 종료일·비고를 남긴다(상태 이력 기록). */
    @Transactional
    public void endExecution(String projectId, java.time.LocalDate date, String memo) {
        Project project = findActiveOrThrow(projectId);
        if (!"수행 중".equals(project.getStatusCode())) {
            throw new BusinessException(PmsErrorCode.PROJECT_STATUS_INVALID, "수행 중인 프로젝트만 수행 종료할 수 있습니다: " + project.getStatusCode());
        }
        project.withStatusCode("수행 종료").withExecEndDate(date).withExecEndMemo(PmsSupport.blankToNull(memo));
        recordHistory(project.getId(), "수행 중", "수행 종료", PmsSupport.blankToNull(memo));
    }

    /** 투입 계획 변경 승인 반영 — 수행 PM·열람 제한. */
    @Transactional
    public void updateExecutionSettings(String projectId, String pmUserId, boolean restricted) {
        findActiveOrThrow(projectId).withPmUserId(PmsSupport.blankToNull(pmUserId)).withViewRestrictedYn(restricted ? "Y" : "N");
    }

    /** 프로젝트 존재/상태 확인용(전환·변경 요청 서비스가 사용). */
    public Project requireProject(String projectId) {
        access.requireAccess(projectId);
        return findActiveOrThrow(projectId);
    }

    @Transactional
    public void delete(String id) {
        Project project = findActiveOrThrow(id);
        if (!DELETABLE.contains(project.getStatusCode())) {
            throw new BusinessException(PmsErrorCode.PROJECT_DELETE_NOT_ALLOWED,
                    "수주 이후(수주·수행·운영) 프로젝트는 삭제할 수 없습니다: " + project.getStatusCode());
        }
        project.markDeleted();
    }

    public List<ProjectDto.HistoryResponse> getHistory(String id) {
        findActiveOrThrow(id);
        List<StatusHistory> list = historyRepository.findAllByTargetTypeCodeAndTargetIdOrderByActedAtAsc(TARGET_TYPE, id);
        Set<String> ids = new HashSet<>();
        list.forEach(h -> ids.add(h.getActorUserId()));
        Map<String, String> names = userNames(ids);
        return list.stream().map(h -> ProjectDto.HistoryResponse.from(h, names)).toList();
    }

    // ---------- 내부 ----------

    private void validate(ProjectDto.Request r) {
        List<ErrorDetail> errors = new ArrayList<>();
        if (!STATUSES.contains(r.statusCode())) {
            errors.add(new ErrorDetail("statusCode", r.statusCode(), "알 수 없는 프로젝트 상태입니다."));
        }
        if (r.endDate().isBefore(r.startDate())) {
            errors.add(new ErrorDetail("endDate", r.endDate(), "종료일이 시작일보다 빠릅니다."));
        }
        if (r.amount() < 0) {
            errors.add(new ErrorDetail("amount", r.amount(), "발주금액은 0 이상이어야 합니다."));
        }
        if (r.probabilityPct().compareTo(BigDecimal.ZERO) < 0 || r.probabilityPct().compareTo(BigDecimal.valueOf(100)) > 0) {
            errors.add(new ErrorDetail("probabilityPct", r.probabilityPct(), "확률도는 0~100 사이여야 합니다."));
        }

        Customer customer = customerRepository.findByIdAndDeletedYn(r.customerId(), "N").orElse(null);
        if (customer == null) {
            errors.add(new ErrorDetail("customerId", r.customerId(), "존재하지 않는 고객사입니다."));
        }
        CustomerContact contact = null;
        if (r.contactId() != null && !r.contactId().isBlank()) {
            contact = contactRepository.findByIdAndDeletedYn(r.contactId(), "N").orElse(null);
            if (contact == null) {
                errors.add(new ErrorDetail("contactId", r.contactId(), "존재하지 않는 담당자입니다."));
            } else if (customer != null && !contact.getCustomerId().equals(customer.getId())) {
                errors.add(new ErrorDetail("contactId", r.contactId(), "선택한 고객사의 담당자가 아닙니다."));
            }
        }
        if (userRepository.findById(r.salesUserId()).isEmpty()) {
            errors.add(new ErrorDetail("salesUserId", r.salesUserId(), "존재하지 않는 영업 담당자입니다."));
        }
        if (r.pmUserId() != null && !r.pmUserId().isBlank() && userRepository.findById(r.pmUserId()).isEmpty()) {
            errors.add(new ErrorDetail("pmUserId", r.pmUserId(), "존재하지 않는 수행 PM입니다."));
        }

        // 상태에 따른 조건부 필수 (프론트 목업과 동일한 규칙)
        int idx = STATUSES.indexOf(r.statusCode());
        boolean fail = FAIL_STATUSES.contains(r.statusCode());
        if (idx >= STATUSES.indexOf("제안 중") && !fail) {
            if (contact == null) {
                errors.add(new ErrorDetail("contactId", r.contactId(), "제안 단계부터 발주사 담당자는 필수입니다."));
            } else if (contact.getPhone() == null || contact.getPhone().isBlank()) {
                errors.add(new ErrorDetail("contactId", r.contactId(), "제안 단계부터 담당자 연락처는 필수입니다."));
            }
        }
        if (("공고 중".equals(r.statusCode()) || "제안 중".equals(r.statusCode())) && r.dueDate() == null) {
            errors.add(new ErrorDetail("dueDate", null, "공고 중/제안 중에는 제안 마감일이 필요합니다."));
        }
        if (fail && (r.failReasonCode() == null || r.failReasonCode().isBlank())) {
            errors.add(new ErrorDetail("failReasonCode", null, "실패/보류 사유를 선택하세요."));
        }
        if (!errors.isEmpty()) {
            throw new InvalidRequestException(PmsErrorCode.PROJECT_INVALID, "프로젝트 입력값을 확인하세요.", errors);
        }
    }

    private Project apply(Project p, ProjectDto.Request r) {
        Customer customer = customerRepository.findByIdAndDeletedYn(r.customerId(), "N").orElseThrow();
        CustomerContact contact = r.contactId() == null || r.contactId().isBlank()
                ? null : contactRepository.findByIdAndDeletedYn(r.contactId(), "N").orElse(null);
        // 발주사·담당자 정보는 프로젝트에 복사 저장한다(마스터가 바뀌어도 과거 프로젝트 기록 유지).
        return p.withProjectName(r.projectName().trim())
                .withStatusCode(r.statusCode())
                .withProbabilityPct(r.probabilityPct())
                .withCustomerId(customer.getId())
                .withClientName(customer.getCustomerName())
                .withContactId(contact == null ? null : contact.getId())
                .withContactName(contact == null ? null : contact.getContactName())
                .withContactTitle(contact == null ? null : contact.getJobTitle())
                .withContactPhone(contact == null ? null : contact.getPhone())
                .withContactEmail(contact == null ? null : contact.getEmail())
                .withStartDate(r.startDate())
                .withEndDate(r.endDate())
                .withAmount(r.amount())
                .withVatIncludedYn("Y".equals(r.vatIncludedYn()) ? "Y" : "N")
                .withSalesUserId(r.salesUserId())
                .withPmUserId(PmsSupport.blankToNull(r.pmUserId()))
                .withBusinessTypeCode(PmsSupport.blankToNull(r.businessTypeCode()))
                .withBidTypeCode(PmsSupport.blankToNull(r.bidTypeCode()))
                .withNoticeNo(PmsSupport.blankToNull(r.noticeNo()))
                .withDueDate(r.dueDate())
                .withPresentDate(r.presentDate())
                .withCompetitors(PmsSupport.blankToNull(r.competitors()))
                // 원가 열람 권한이 없는 사용자의 수정은 원가 값을 건드리지 않는다(가려진 값으로 덮어쓰기 방지).
                .withCostEstimate(PmsSupport.canViewCost() ? r.costEstimate() : p.getCostEstimate())
                .withCostSourceType(PmsSupport.canViewCost() ? PmsSupport.blankToNull(r.costSourceType()) : p.getCostSourceType())
                .withCostSourceNote(PmsSupport.canViewCost() ? PmsSupport.blankToNull(r.costSourceNote()) : p.getCostSourceNote())
                .withFailReasonCode(PmsSupport.blankToNull(r.failReasonCode()))
                .withFailReasonDetail(PmsSupport.blankToNull(r.failReasonDetail()))
                .withViewRestrictedYn("Y".equals(r.viewRestrictedYn()) ? "Y" : "N")
                .withMemo(PmsSupport.blankToNull(r.memo()));
    }

    private void recordHistory(String projectId, String from, String to, String memo) {
        historyRepository.save(StatusHistory.create()
                .withTargetTypeCode(TARGET_TYPE)
                .withTargetId(projectId)
                .withFromStatus(from)
                .withToStatus(to)
                .withActorUserId(PmsSupport.currentUserId())
                .withActedAt(LocalDateTime.now())
                .withMemo(PmsSupport.blankToNull(memo)));
    }

    private ProjectDto.Response toResponse(Project p) {
        return ProjectDto.Response.from(p, userNames(List.of(p)), PmsSupport.canViewCost());
    }

    private Map<String, String> userNames(List<Project> projects) {
        Set<String> ids = new HashSet<>();
        for (Project p : projects) {
            ids.add(p.getSalesUserId());
            if (p.getPmUserId() != null) {
                ids.add(p.getPmUserId());
            }
        }
        return userNames(ids);
    }

    private Map<String, String> userNames(Set<String> ids) {
        Map<String, String> names = new HashMap<>();
        for (User u : userRepository.findAllById(ids)) {
            names.put(u.getId(), u.getUserName());
        }
        return names;
    }

    private Project findActiveOrThrow(String id) {
        return projectRepository.findByIdAndDeletedYn(id, "N")
                .orElseThrow(() -> new TenantException(PmsErrorCode.PROJECT_NOT_FOUND, "존재하지 않는 프로젝트입니다: " + id));
    }

    private static String nz(String s) {
        return s == null ? "" : s.trim();
    }
}
