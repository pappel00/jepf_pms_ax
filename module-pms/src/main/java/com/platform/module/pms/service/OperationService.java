package com.platform.module.pms.service;

import com.platform.framework.core.common.exception.BusinessException;
import com.platform.module.pms.domain.HandoverItem;
import com.platform.module.pms.domain.Project;
import com.platform.module.pms.domain.ProjectFeature;
import com.platform.module.pms.domain.ProjectOperation;
import com.platform.module.pms.dto.OperationDto;
import com.platform.module.pms.dto.ProjectDto;
import com.platform.module.pms.exception.PmsErrorCode;
import com.platform.module.pms.repository.HandoverItemRepository;
import com.platform.module.pms.repository.ProjectFeatureRepository;
import com.platform.module.pms.repository.ProjectOperationRepository;
import com.platform.module.pms.repository.ProjectRepository;
import com.platform.module.pms.repository.TransitionRepository;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 수행 종료와 운영단계전환.
 *  - 수행 종료: 「수행 중」 → 「수행 종료」, 종료일·비고와 운영 인수 후보 기능 목록(pms_project_feature)을 남긴다.
 *  - 운영단계전환: 「수행 종료」 → 「운영 중」. 운영 정보(형태·기간·계약 금액·SLA)·운영 팀을 만들고, 인수 대상 기능(handover_yn)·산출물·
 *    미해결 이슈·차기 사업 기회를 인수 항목(pms_handover_item)으로 남긴다. 선택한 차기 사업 기회는 운영 영업 기회로 이관한다.
 *    전환 이력은 pms_transition(kind=OPS, 승인)에 남는다 — 별도 승인 없이 바로 확정된다.
 */
@Service
@Transactional(readOnly = true)
public class OperationService {

    static final String KIND_OPS = "OPS";

    private final ProjectOperationRepository operationRepository;
    private final ProjectFeatureRepository featureRepository;
    private final HandoverItemRepository handoverRepository;
    private final TransitionRepository transitionRepository;
    private final ProjectRepository projectRepository;
    private final ProjectService projectService;
    private final ProjectMemberService memberService;
    private final TransitionService transitionService;
    private final OpsService opsService;

    public OperationService(ProjectOperationRepository operationRepository, ProjectFeatureRepository featureRepository,
                            HandoverItemRepository handoverRepository, TransitionRepository transitionRepository,
                            ProjectRepository projectRepository, ProjectService projectService, ProjectMemberService memberService,
                            TransitionService transitionService, OpsService opsService) {
        this.operationRepository = operationRepository;
        this.featureRepository = featureRepository;
        this.handoverRepository = handoverRepository;
        this.transitionRepository = transitionRepository;
        this.projectRepository = projectRepository;
        this.projectService = projectService;
        this.memberService = memberService;
        this.transitionService = transitionService;
        this.opsService = opsService;
    }

    public List<OperationDto.Response> getOperations() {
        List<ProjectOperation> list = operationRepository.findAllByDeletedYn("N");
        Map<String, String> codes = new HashMap<>();
        projectRepository.findAllById(list.stream().map(ProjectOperation::getProjectId).toList()).forEach(p -> codes.put(p.getId(), p.getProjectCode()));
        Map<String, List<OperationDto.HandoverResponse>> handover = new HashMap<>();
        handoverRepository.findAllByDeletedYnOrderByCreatedDateAsc("N").forEach(h -> handover.computeIfAbsent(h.getProjectId(), k -> new ArrayList<>())
                .add(new OperationDto.HandoverResponse(h.getItemTypeCode(), h.getRefId(), h.getItemName())));
        Map<String, LocalDate> dates = new HashMap<>();
        transitionRepository.findAll().stream().filter(t -> KIND_OPS.equals(t.getKindCode()) && "승인".equals(t.getStatusCode()))
                .forEach(t -> dates.put(t.getProjectId(), t.getDecidedAt() == null ? null : t.getDecidedAt().toLocalDate()));
        return list.stream().map(o -> toResponse(o, codes.get(o.getProjectId()), dates.get(o.getProjectId()),
                handover.getOrDefault(o.getProjectId(), List.of()))).toList();
    }

    public List<OperationDto.FeatureResponse> getFeatures() {
        List<ProjectFeature> list = featureRepository.findAllByDeletedYnOrderByCreatedDateAsc("N");
        Map<String, String> codes = new HashMap<>();
        projectRepository.findAllById(list.stream().map(ProjectFeature::getProjectId).distinct().toList()).forEach(p -> codes.put(p.getId(), p.getProjectCode()));
        return list.stream().map(f -> new OperationDto.FeatureResponse(f.getProjectId(), codes.get(f.getProjectId()), f.getFeatureName(),
                "Y".equals(f.getHandoverYn()))).toList();
    }

    /** 수행 종료 처리 — 프로젝트 관리자가 준공/검수 완료를 확정한다. */
    @Transactional
    public void endExecution(String projectId, OperationDto.EndRequest r) {
        projectService.endExecution(projectId, r.date(), r.memo());
        featureRepository.findAllByProjectIdAndDeletedYn(projectId, "N").forEach(ProjectFeature::markDeleted);
        for (String name : new java.util.LinkedHashSet<>(r.features() == null ? List.<String>of() : r.features())) {
            if (name != null && !name.isBlank()) {
                featureRepository.save(ProjectFeature.create().withProjectId(projectId).withFeatureName(name.trim()).withHandoverYn("Y")
                        .withSourceCode("MANUAL"));
            }
        }
    }

    /** 운영단계전환 — 수행 종료 프로젝트를 「운영 중」으로 바꾸고 운영 기초데이터를 만든다. */
    @Transactional
    public OperationDto.Response transition(String projectId, OperationDto.Request r) {
        Project project = projectService.requireProject(projectId);
        if (!"수행 종료".equals(project.getStatusCode())) {
            throw new BusinessException(PmsErrorCode.PROJECT_STATUS_INVALID, "수행 종료된 프로젝트만 운영단계로 전환할 수 있습니다: " + project.getStatusCode());
        }
        if (operationRepository.findByProjectIdAndDeletedYn(projectId, "N").isPresent()) {
            throw new BusinessException(PmsErrorCode.OPS_INVALID, "이미 운영 정보가 있는 프로젝트입니다.");
        }
        validate(r);
        memberService.replaceTeam(projectId, KIND_OPS, r.team());
        String transitionId = transitionService.recordApproved(projectId, KIND_OPS, PmsJson.write(r),
                "운영단계전환 (" + r.opTypeCode() + ", " + r.startDate() + " ~ " + r.endDate() + ")");
        ProjectOperation op = apply(ProjectOperation.create().withProjectId(projectId).withTransitionId(transitionId), r);
        operationRepository.saveAndFlush(op);

        Set<String> chosen = new HashSet<>(r.features() == null ? List.<String>of() : r.features());
        for (ProjectFeature f : featureRepository.findAllByProjectIdAndDeletedYn(projectId, "N")) {
            f.withHandoverYn(chosen.contains(f.getFeatureName()) ? "Y" : "N");
        }
        List<HandoverItem> items = new ArrayList<>();
        for (OperationDto.DocRef d : r.docs() == null ? List.<OperationDto.DocRef>of() : r.docs()) {
            items.add(HandoverItem.create().withProjectId(projectId).withTransitionId(transitionId).withItemTypeCode("DOC").withRefId(d.refId())
                    .withItemName(d.phase() + "|" + d.name() + "|" + (d.version() == null ? "" : d.version())));
        }
        for (OperationDto.IssueRef i : r.issues() == null ? List.<OperationDto.IssueRef>of() : r.issues()) {
            items.add(HandoverItem.create().withProjectId(projectId).withTransitionId(transitionId).withItemTypeCode("ISSUE").withRefId(i.refId())
                    .withItemName(i.title() + "|" + (i.priority() == null ? "" : i.priority())));
        }
        List<String> nextIds = r.nextOppIds() == null ? List.of() : r.nextOppIds();
        for (String id : nextIds) {
            items.add(HandoverItem.create().withProjectId(projectId).withTransitionId(transitionId).withItemTypeCode("NEXT_OPP").withRefId(id)
                    .withItemName(id));
        }
        handoverRepository.saveAll(items);
        opsService.transferNext(projectId, nextIds);
        projectService.changeStatus(projectId, new ProjectDto.StatusRequest("운영 중", "운영단계전환"));
        return getOperations().stream().filter(o -> o.projectId().equals(projectId)).findFirst().orElseThrow();
    }

    /** 운영 정보 수정(운영 관리자) — 형태·기간·계약 금액·SLA 와 운영 팀. */
    @Transactional
    public OperationDto.Response update(String projectId, OperationDto.Request r) {
        projectService.requireProject(projectId);
        ProjectOperation op = operationRepository.findByProjectIdAndDeletedYn(projectId, "N")
                .orElseThrow(() -> new BusinessException(PmsErrorCode.OPS_INVALID, "운영 정보가 없는 프로젝트입니다."));
        validate(r);
        apply(op, r);
        memberService.replaceTeam(projectId, KIND_OPS, r.team());
        return getOperations().stream().filter(o -> o.projectId().equals(projectId)).findFirst().orElseThrow();
    }

    private void validate(OperationDto.Request r) {
        if (r.endDate().isBefore(r.startDate())) {
            throw new BusinessException(PmsErrorCode.OPS_INVALID, "운영 종료일이 시작일보다 빠릅니다.");
        }
    }

    private ProjectOperation apply(ProjectOperation op, OperationDto.Request r) {
        return op.withOpTypeCode(r.opTypeCode()).withStartDate(r.startDate()).withEndDate(r.endDate()).withContractAmount(r.contractAmount())
                .withSlaLevelCode(r.slaLevelCode()).withSlaRespondHours(r.slaRespondHours()).withSlaResolveHours(r.slaResolveHours())
                .withSlaServiceHours(PmsSupport.blankToNull(r.slaServiceHours()));
    }

    private static OperationDto.Response toResponse(ProjectOperation o, String code, LocalDate date, List<OperationDto.HandoverResponse> handover) {
        return new OperationDto.Response(o.getProjectId(), code, date, o.getOpTypeCode(), o.getStartDate(), o.getEndDate(), o.getContractAmount(),
                o.getSlaLevelCode(), o.getSlaRespondHours(), o.getSlaResolveHours(), o.getSlaServiceHours(), handover);
    }
}
