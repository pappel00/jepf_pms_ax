package com.platform.module.pms.service;

import com.platform.framework.core.common.exception.BusinessException;
import com.platform.framework.core.common.exception.TenantException;
import com.platform.module.pms.domain.Proposal;
import com.platform.module.pms.domain.ProposalChecklist;
import com.platform.module.pms.domain.ProposalMapping;
import com.platform.module.pms.domain.ProposalVersion;
import com.platform.module.pms.domain.Rfp;
import com.platform.module.pms.dto.ProposalDto;
import com.platform.module.pms.exception.PmsErrorCode;
import com.platform.module.pms.repository.ProjectRepository;
import com.platform.module.pms.repository.ProposalChecklistRepository;
import com.platform.module.pms.repository.ProposalMappingRepository;
import com.platform.module.pms.repository.ProposalRepository;
import com.platform.module.pms.repository.ProposalVersionRepository;
import com.platform.module.pms.repository.RfpRepository;
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
 * 제안서 — RFP별 제안서 1 : N 버전 이력(누적, 삭제 없음) / RFP 항목 대응표 / 제출 체크리스트.
 * 제안서의 프로젝트는 RFP 의 프로젝트를 따른다.
 */
@Service
@Transactional(readOnly = true)
public class ProposalService {

    private static final Set<String> STATUSES = Set.of("작성 중", "내부 검토", "제출 완료", "결과 확정");
    private static final Set<String> RESPONSES = Set.of("수용", "일부 수용", "대안 제시", "미수용");

    private final ProposalRepository proposalRepository;
    private final ProposalVersionRepository versionRepository;
    private final ProposalMappingRepository mappingRepository;
    private final ProposalChecklistRepository checklistRepository;
    private final RfpRepository rfpRepository;
    private final ProjectRepository projectRepository;
    private final UserNameResolver names;

    public ProposalService(ProposalRepository proposalRepository, ProposalVersionRepository versionRepository,
                           ProposalMappingRepository mappingRepository, ProposalChecklistRepository checklistRepository,
                           RfpRepository rfpRepository, ProjectRepository projectRepository, UserNameResolver names) {
        this.proposalRepository = proposalRepository;
        this.versionRepository = versionRepository;
        this.mappingRepository = mappingRepository;
        this.checklistRepository = checklistRepository;
        this.rfpRepository = rfpRepository;
        this.projectRepository = projectRepository;
        this.names = names;
    }

    public List<ProposalDto.Response> getList() {
        return toResponses(proposalRepository.findAllByDeletedYnOrderByProposalCodeAsc("N"));
    }

    @Transactional
    public ProposalDto.Response create(ProposalDto.Request r) {
        validate(r);
        Rfp rfp = findRfp(r.rfpId());
        String code = PmsSupport.nextCode("PROP-" + java.time.Year.now().getValue() + "-",
                proposalRepository.findAll().stream().map(Proposal::getProposalCode).toList(), 3);
        Proposal p = apply(Proposal.create().withProposalCode(code).withProjectId(rfp.getProjectId()).withRfpId(rfp.getId()), r);
        proposalRepository.saveAndFlush(p);
        int order = 0;
        for (String name : r.checklist() == null ? List.<String>of() : r.checklist()) {
            checklistRepository.save(ProposalChecklist.create().withProposalId(p.getId()).withItemName(name.trim()).withDoneYn("N").withSortOrder(order++));
        }
        return toResponses(List.of(p)).get(0);
    }

    @Transactional
    public ProposalDto.Response update(String id, ProposalDto.Request r) {
        validate(r);
        Proposal p = find(id);
        if (!p.getRfpId().equals(r.rfpId())) {
            Rfp rfp = findRfp(r.rfpId());
            p.withRfpId(rfp.getId()).withProjectId(rfp.getProjectId());
        }
        apply(p, r);
        return toResponses(List.of(p)).get(0);
    }

    @Transactional
    public void delete(String id) {
        Proposal p = find(id);
        versionRepository.findAllByProposalIdAndDeletedYnOrderByCreatedDateAsc(id, "N").forEach(ProposalVersion::markDeleted);
        mappingRepository.findAllByProposalIdAndDeletedYn(id, "N").forEach(ProposalMapping::markDeleted);
        checklistRepository.findAllByProposalIdAndDeletedYn(id, "N").forEach(ProposalChecklist::markDeleted);
        p.markDeleted();
    }

    /** 버전 추가 — 이력은 삭제 없이 누적한다. 버전 번호는 제안서 안에서 유일해야 한다. */
    @Transactional
    public ProposalDto.Response addVersion(String id, ProposalDto.VersionRequest r) {
        Proposal p = find(id);
        boolean exists = versionRepository.findAllByProposalIdAndDeletedYnOrderByCreatedDateAsc(id, "N").stream()
                .anyMatch(v -> v.getVer().equals(r.ver().trim()));
        if (exists) {
            throw new BusinessException(PmsErrorCode.PROPOSAL_DOC_INVALID, "이미 있는 버전입니다: " + r.ver());
        }
        versionRepository.save(ProposalVersion.create().withProposalId(id).withVer(r.ver().trim())
                .withVersionDate(r.versionDate() != null ? r.versionDate() : LocalDate.now()).withAuthorUserId(PmsSupport.currentUserId())
                .withSummary(PmsSupport.blankToNull(r.summary())).withFileName(PmsSupport.blankToNull(r.fileName()))
                .withFileSize(PmsSupport.blankToNull(r.fileSize())).withSubmittedYn("N"));
        return toResponses(List.of(p)).get(0);
    }

    /** RFP 항목 대응 저장 — 대응 방식이 비어 있고 나머지도 없으면 대응을 지운다(null 필드는 유지). */
    @Transactional
    public ProposalDto.Response saveMapping(String id, String rfpItemId, ProposalDto.MappingRequest r) {
        Proposal p = find(id);
        if (r.responseCode() != null && !r.responseCode().isEmpty() && !RESPONSES.contains(r.responseCode())) {
            throw new BusinessException(PmsErrorCode.PROPOSAL_DOC_INVALID, "알 수 없는 대응 방식입니다: " + r.responseCode());
        }
        ProposalMapping m = mappingRepository.findByProposalIdAndRfpItemIdAndDeletedYn(id, rfpItemId, "N")
                .orElseGet(() -> ProposalMapping.create().withProposalId(id).withRfpItemId(rfpItemId));
        if (r.section() != null) {
            m.withSection(PmsSupport.blankToNull(r.section()));
        }
        if (r.responseCode() != null) {
            m.withResponseCode(PmsSupport.blankToNull(r.responseCode()));
        }
        if (r.summary() != null) {
            m.withSummary(PmsSupport.blankToNull(r.summary()));
        }
        if (r.ownerUserId() != null) {
            m.withOwnerUserId(PmsSupport.blankToNull(r.ownerUserId()));
        }
        mappingRepository.save(m);
        return toResponses(List.of(p)).get(0);
    }

    @Transactional
    public ProposalDto.Response deleteMapping(String id, String rfpItemId) {
        Proposal p = find(id);
        mappingRepository.findByProposalIdAndRfpItemIdAndDeletedYn(id, rfpItemId, "N").ifPresent(ProposalMapping::markDeleted);
        return toResponses(List.of(p)).get(0);
    }

    @Transactional
    public ProposalDto.Response addCheck(String id, String itemName) {
        Proposal p = find(id);
        int order = checklistRepository.findAllByProposalIdAndDeletedYn(id, "N").stream().mapToInt(ProposalChecklist::getSortOrder).max().orElse(-1) + 1;
        checklistRepository.save(ProposalChecklist.create().withProposalId(id).withItemName(itemName.trim()).withDoneYn("N").withSortOrder(order));
        return toResponses(List.of(p)).get(0);
    }

    @Transactional
    public ProposalDto.Response toggleCheck(String id, String checkId) {
        Proposal p = find(id);
        ProposalChecklist c = findCheck(id, checkId);
        c.withDoneYn("Y".equals(c.getDoneYn()) ? "N" : "Y");
        return toResponses(List.of(p)).get(0);
    }

    @Transactional
    public ProposalDto.Response removeCheck(String id, String checkId) {
        Proposal p = find(id);
        findCheck(id, checkId).markDeleted();
        return toResponses(List.of(p)).get(0);
    }

    // ---------- 내부 ----------

    private void validate(ProposalDto.Request r) {
        if (!STATUSES.contains(r.statusCode())) {
            throw new BusinessException(PmsErrorCode.PROPOSAL_DOC_INVALID, "알 수 없는 제안서 상태입니다: " + r.statusCode());
        }
        if (names.resolve(List.of(r.ownerUserId())).isEmpty()) {
            throw new TenantException(PmsErrorCode.USER_NOT_FOUND, "존재하지 않는 사용자입니다: " + r.ownerUserId());
        }
    }

    private Proposal apply(Proposal p, ProposalDto.Request r) {
        return p.withTitle(r.title().trim()).withStatusCode(r.statusCode()).withOwnerUserId(r.ownerUserId()).withDueDate(r.dueDate())
                .withSubmittedDate(r.submittedDate()).withPrice(r.price()).withCostEstimate(r.costEstimate());
    }

    private Proposal find(String id) {
        return proposalRepository.findByIdAndDeletedYn(id, "N")
                .orElseThrow(() -> new TenantException(PmsErrorCode.PROPOSAL_DOC_NOT_FOUND, "존재하지 않는 제안서입니다: " + id));
    }

    private Rfp findRfp(String id) {
        return rfpRepository.findByIdAndDeletedYn(id, "N")
                .orElseThrow(() -> new TenantException(PmsErrorCode.RFP_NOT_FOUND, "존재하지 않는 RFP입니다: " + id));
    }

    private ProposalChecklist findCheck(String proposalId, String checkId) {
        ProposalChecklist c = checklistRepository.findByIdAndDeletedYn(checkId, "N")
                .orElseThrow(() -> new TenantException(PmsErrorCode.PROPOSAL_DOC_NOT_FOUND, "존재하지 않는 체크리스트 항목입니다: " + checkId));
        if (!c.getProposalId().equals(proposalId)) {
            throw new BusinessException(PmsErrorCode.PROPOSAL_DOC_INVALID, "다른 제안서의 체크리스트 항목입니다.");
        }
        return c;
    }

    private List<ProposalDto.Response> toResponses(List<Proposal> list) {
        if (list.isEmpty()) {
            return List.of();
        }
        List<String> ids = list.stream().map(Proposal::getId).toList();
        Map<String, List<ProposalVersion>> versions = new HashMap<>();
        versionRepository.findAllByProposalIdInAndDeletedYnOrderByCreatedDateAsc(ids, "N")
                .forEach(v -> versions.computeIfAbsent(v.getProposalId(), k -> new ArrayList<>()).add(v));
        Map<String, List<ProposalMapping>> mappings = new HashMap<>();
        mappingRepository.findAllByProposalIdInAndDeletedYn(ids, "N")
                .forEach(m -> mappings.computeIfAbsent(m.getProposalId(), k -> new ArrayList<>()).add(m));
        Map<String, List<ProposalChecklist>> checks = new HashMap<>();
        checklistRepository.findAllByProposalIdInAndDeletedYnOrderBySortOrderAscCreatedDateAsc(ids, "N")
                .forEach(c -> checks.computeIfAbsent(c.getProposalId(), k -> new ArrayList<>()).add(c));

        Set<String> userIds = new HashSet<>();
        Set<String> projectIds = new HashSet<>();
        Set<String> rfpIds = new HashSet<>();
        for (Proposal p : list) {
            userIds.add(p.getOwnerUserId());
            projectIds.add(p.getProjectId());
            rfpIds.add(p.getRfpId());
        }
        versions.values().forEach(l -> l.forEach(v -> userIds.add(v.getAuthorUserId())));
        mappings.values().forEach(l -> l.forEach(m -> userIds.add(m.getOwnerUserId())));
        Map<String, String> userNames = names.resolve(userIds);
        Map<String, String> projectCodes = new HashMap<>();
        projectRepository.findAllById(projectIds).forEach(pr -> projectCodes.put(pr.getId(), pr.getProjectCode()));
        Map<String, String> rfpCodes = new HashMap<>();
        rfpRepository.findAllById(rfpIds).forEach(r -> rfpCodes.put(r.getId(), r.getRfpCode()));

        return list.stream().map(p -> new ProposalDto.Response(p.getId(), p.getProposalCode(), p.getProjectId(),
                projectCodes.get(p.getProjectId()), p.getRfpId(), rfpCodes.get(p.getRfpId()), p.getTitle(), p.getStatusCode(),
                p.getOwnerUserId(), userNames.get(p.getOwnerUserId()), p.getDueDate(), p.getSubmittedDate(), p.getPrice(),
                p.getCostEstimate(),
                versions.getOrDefault(p.getId(), List.of()).stream().map(v -> new ProposalDto.VersionResponse(v.getId(), v.getVer(),
                        v.getVersionDate(), v.getAuthorUserId(), userNames.get(v.getAuthorUserId()), v.getSummary(), v.getFileName(),
                        v.getFileSize(), "Y".equals(v.getSubmittedYn()))).toList(),
                mappings.getOrDefault(p.getId(), List.of()).stream().map(m -> new ProposalDto.MappingResponse(m.getRfpItemId(),
                        m.getSection(), m.getResponseCode(), m.getSummary(), m.getOwnerUserId(),
                        m.getOwnerUserId() == null ? null : userNames.get(m.getOwnerUserId()))).toList(),
                checks.getOrDefault(p.getId(), List.of()).stream().map(c -> new ProposalDto.CheckResponse(c.getId(), c.getItemName(),
                        "Y".equals(c.getDoneYn()))).toList())).toList();
    }
}
