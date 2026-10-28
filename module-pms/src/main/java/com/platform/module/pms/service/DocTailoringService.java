package com.platform.module.pms.service;

import com.platform.framework.core.common.exception.BusinessException;
import com.platform.framework.core.common.exception.TenantException;
import com.platform.module.pms.domain.DocTailoring;
import com.platform.module.pms.domain.DocVersion;
import com.platform.module.pms.domain.Project;
import com.platform.module.pms.domain.StdDoc;
import com.platform.module.pms.dto.DocDto;
import com.platform.module.pms.exception.PmsErrorCode;
import com.platform.module.pms.repository.DocTailoringRepository;
import com.platform.module.pms.repository.DocVersionRepository;
import com.platform.module.pms.repository.ProjectRepository;
import com.platform.module.pms.repository.StdDocRepository;
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
 * 문서 테일러링(프로젝트별 적용 산출물)과 단계말 산출물 버전.
 *  - 표준 산출물 마스터(pms_std_doc)에서 프로젝트로 복사해 만든다(마스터가 바뀌어도 기존 프로젝트는 유지).
 *    필수 문서는 적용, 권장 문서는 선택한 RFP 항목 분류가 트리거(rfp_category_code)일 때만 적용한다.
 *  - 필수 산출물을 미적용으로 바꾸려면 사유가 필요하다. 산출물 파일은 버전으로 누적(v1.0, v1.1 …)하고 삭제는 최신 버전만 가능하다.
 *  - 새 버전 등록·최신 버전 수정/삭제는 「검토 완료」를 해제한다. 파일 자체는 아직 저장하지 않는다(module-file 연동 시 지원).
 */
@Service
@Transactional(readOnly = true)
public class DocTailoringService {

    static final List<String> PHASES = List.of("분석", "설계", "개발", "테스트");
    static final String APPLIED = "적용";
    static final String NOT_APPLIED = "미적용";

    private final DocTailoringRepository tailoringRepository;
    private final DocVersionRepository versionRepository;
    private final StdDocRepository stdDocRepository;
    private final ProjectRepository projectRepository;
    private final ProjectService projectService;
    private final UserNameResolver names;

    public DocTailoringService(DocTailoringRepository tailoringRepository, DocVersionRepository versionRepository,
                               StdDocRepository stdDocRepository, ProjectRepository projectRepository,
                               ProjectService projectService, UserNameResolver names) {
        this.tailoringRepository = tailoringRepository;
        this.versionRepository = versionRepository;
        this.stdDocRepository = stdDocRepository;
        this.projectRepository = projectRepository;
        this.projectService = projectService;
        this.names = names;
    }

    public List<DocDto.Response> getList() {
        return toResponses(tailoringRepository.findAllByDeletedYnOrderByProjectIdAscCreatedDateAsc("N"));
    }

    /** 단계전환 승인 직후 — 선택한 단계의 표준 산출물로 테일러링을 만든다(이미 있으면 거부). */
    @Transactional
    public List<DocDto.Response> init(String projectId, DocDto.InitRequest req) {
        Project project = projectService.requireProject(projectId);
        if (tailoringRepository.existsByProjectIdAndDeletedYn(projectId, "N")) {
            throw new BusinessException(PmsErrorCode.DOC_INVALID, "이미 문서 테일러링이 있는 프로젝트입니다: " + project.getProjectCode());
        }
        Set<String> categories = new HashSet<>(req.categories() == null ? List.of() : req.categories());
        Map<String, String> owners = req.owners() == null ? Map.of() : req.owners();
        List<DocTailoring> created = new ArrayList<>();
        for (StdDoc std : stdDocRepository.findAllByUseYnAndDeletedYnOrderByPhaseCodeAscSortOrderAsc("Y", "N")) {
            if (!req.phases().contains(std.getPhaseCode())) {
                continue;
            }
            DocTailoring t = fromStd(projectId, std, PmsSupport.blankToNull(owners.get(std.getPhaseCode())));
            if (!"Y".equals(std.getRequiredYn())) {
                if (std.getRfpCategoryCode() != null && categories.contains(std.getRfpCategoryCode())) {
                    t.withNote("RFP 선택 항목 분류에 따라 자동 적용");
                } else {
                    t.withAppliedCode(NOT_APPLIED).withNotAppliedReason("RFP 선택 항목에 해당 분류가 없어 자동 제외 (필요 시 적용으로 변경)");
                }
            }
            created.add(t);
        }
        return toResponses(tailoringRepository.saveAll(created));
    }

    /** 표준 목록으로 초기화 — 적용 여부·사유·추가 문서와 등록된 산출물 버전이 모두 지워지고 표준 전체(적용)로 돌아간다. */
    @Transactional
    public List<DocDto.Response> reset(String projectId) {
        projectService.requireProject(projectId);
        List<DocTailoring> old = tailoringRepository.findAllByProjectIdAndDeletedYnOrderByCreatedDateAsc(projectId, "N");
        Map<String, String> ownerByPhase = new HashMap<>();
        old.forEach(t -> {
            if (t.getOwnerUserId() != null) {
                ownerByPhase.putIfAbsent(t.getPhaseCode(), t.getOwnerUserId());
            }
        });
        old.forEach(this::deleteRows);
        List<DocTailoring> created = new ArrayList<>();
        for (StdDoc std : stdDocRepository.findAllByUseYnAndDeletedYnOrderByPhaseCodeAscSortOrderAsc("Y", "N")) {
            created.add(fromStd(projectId, std, ownerByPhase.get(std.getPhaseCode())));
        }
        return toResponses(tailoringRepository.saveAll(created));
    }

    @Transactional
    public DocDto.Response addCustom(String projectId, DocDto.CustomRequest r) {
        projectService.requireProject(projectId);
        if (!PHASES.contains(r.phaseCode())) {
            throw new BusinessException(PmsErrorCode.DOC_INVALID, "알 수 없는 단계입니다: " + r.phaseCode());
        }
        DocTailoring t = DocTailoring.create().withProjectId(projectId).withPhaseCode(r.phaseCode()).withDocName(r.docName().trim())
                .withRequiredYn(r.required() ? "Y" : "N").withAppliedCode(APPLIED).withOwnerUserId(PmsSupport.blankToNull(r.ownerUserId()))
                .withCustomYn("Y").withReviewedYn("N");
        return toResponses(List.of(tailoringRepository.save(t))).get(0);
    }

    @Transactional
    public DocDto.Response patch(String id, DocDto.PatchRequest r) {
        DocTailoring t = find(id);
        if (r.appliedCode() != null) {
            if (!APPLIED.equals(r.appliedCode()) && !NOT_APPLIED.equals(r.appliedCode())) {
                throw new BusinessException(PmsErrorCode.DOC_INVALID, "적용 여부는 적용/미적용 입니다: " + r.appliedCode());
            }
            t.withAppliedCode(r.appliedCode());
        }
        if (r.notAppliedReason() != null) {
            t.withNotAppliedReason(PmsSupport.blankToNull(r.notAppliedReason()));
        }
        if (r.ownerUserId() != null) {
            t.withOwnerUserId(PmsSupport.blankToNull(r.ownerUserId()));
        }
        if (r.note() != null) {
            t.withNote(PmsSupport.blankToNull(r.note()));
        }
        // 필수 산출물의 미적용 사유는 입력 중에 채워지므로 저장은 허용하고, 사유 미입력은 화면이 경고한다.
        return toResponses(List.of(t)).get(0);
    }

    @Transactional
    public void delete(String id) {
        deleteRows(find(id));
    }

    /** 프로젝트의 테일러링·산출물 버전을 모두 지운다 — 단계전환 롤백에서 호출한다. */
    @Transactional
    public void deleteForRollback(String projectId) {
        tailoringRepository.findAllByProjectIdAndDeletedYnOrderByCreatedDateAsc(projectId, "N").forEach(this::deleteRows);
    }

    // ---------- 산출물 버전 ----------

    /** 새 버전 등록(첫 등록 포함) — v1.0, v1.1, … 자동 채번. 검토 완료는 해제된다. */
    @Transactional
    public DocDto.Response addVersion(String id, DocDto.VersionRequest r) {
        DocTailoring t = find(id);
        if (r.fileName() == null || r.fileName().isBlank()) {
            throw new BusinessException(PmsErrorCode.DOC_INVALID, "파일을 선택하세요.");
        }
        int count = versionRepository.findAllByTailoringIdAndDeletedYnOrderByCreatedDateAsc(id, "N").size();
        versionRepository.save(DocVersion.create().withTailoringId(id).withVer(count == 0 ? "v1.0" : "v1." + count)
                .withVersionDate(LocalDate.now()).withAuthorUserId(PmsSupport.currentUserId()).withFileName(r.fileName().trim())
                .withFileSize(PmsSupport.blankToNull(r.fileSize())).withNote(PmsSupport.blankToNull(r.note())));
        t.withReviewedYn("N");
        return toResponses(List.of(t)).get(0);
    }

    /** 최신 버전 수정(파일 교체·비고 변경) — 검토 완료는 해제된다. */
    @Transactional
    public DocDto.Response updateLatest(String id, DocDto.VersionRequest r) {
        DocTailoring t = find(id);
        DocVersion latest = latest(id);
        if (r.fileName() != null && !r.fileName().isBlank()) {
            latest.withFileName(r.fileName().trim()).withFileSize(PmsSupport.blankToNull(r.fileSize()))
                    .withVersionDate(LocalDate.now()).withAuthorUserId(PmsSupport.currentUserId());
        }
        if (r.note() != null) {
            latest.withNote(PmsSupport.blankToNull(r.note()));
        }
        t.withReviewedYn("N");
        return toResponses(List.of(t)).get(0);
    }

    /** 삭제는 최신 버전만. */
    @Transactional
    public DocDto.Response deleteLatest(String id) {
        DocTailoring t = find(id);
        latest(id).markDeleted();
        t.withReviewedYn("N");
        return toResponses(List.of(t)).get(0);
    }

    @Transactional
    public DocDto.Response setReviewed(String id, boolean reviewed) {
        DocTailoring t = find(id);
        if (reviewed && versionRepository.findAllByTailoringIdAndDeletedYnOrderByCreatedDateAsc(id, "N").isEmpty()) {
            throw new BusinessException(PmsErrorCode.DOC_INVALID, "등록된 산출물이 없어 검토 완료로 바꿀 수 없습니다.");
        }
        t.withReviewedYn(reviewed ? "Y" : "N");
        return toResponses(List.of(t)).get(0);
    }

    // ---------- 내부 ----------

    private DocVersion latest(String id) {
        List<DocVersion> versions = versionRepository.findAllByTailoringIdAndDeletedYnOrderByCreatedDateAsc(id, "N");
        if (versions.isEmpty()) {
            throw new BusinessException(PmsErrorCode.DOC_INVALID, "등록된 산출물 버전이 없습니다.");
        }
        return versions.get(versions.size() - 1);
    }

    private DocTailoring fromStd(String projectId, StdDoc std, String ownerUserId) {
        return DocTailoring.create().withProjectId(projectId).withStdDocId(std.getId()).withPhaseCode(std.getPhaseCode())
                .withDocName(std.getDocName()).withRequiredYn(std.getRequiredYn()).withAppliedCode(APPLIED).withOwnerUserId(ownerUserId)
                .withCustomYn("N").withReviewedYn("N");
    }

    private void deleteRows(DocTailoring t) {
        versionRepository.findAllByTailoringIdAndDeletedYnOrderByCreatedDateAsc(t.getId(), "N").forEach(DocVersion::markDeleted);
        t.markDeleted();
    }

    private DocTailoring find(String id) {
        return tailoringRepository.findByIdAndDeletedYn(id, "N")
                .orElseThrow(() -> new TenantException(PmsErrorCode.DOC_NOT_FOUND, "존재하지 않는 산출물 항목입니다: " + id));
    }

    private List<DocDto.Response> toResponses(List<DocTailoring> list) {
        if (list.isEmpty()) {
            return List.of();
        }
        tailoringRepository.flush();
        List<String> ids = list.stream().map(DocTailoring::getId).toList();
        Map<String, List<DocVersion>> versions = new HashMap<>();
        versionRepository.findAllByTailoringIdInAndDeletedYnOrderByCreatedDateAsc(ids, "N")
                .forEach(v -> versions.computeIfAbsent(v.getTailoringId(), k -> new ArrayList<>()).add(v));
        Set<String> userIds = new HashSet<>();
        list.forEach(t -> userIds.add(t.getOwnerUserId()));
        versions.values().forEach(l -> l.forEach(v -> userIds.add(v.getAuthorUserId())));
        Map<String, String> userNames = names.resolve(userIds);
        Map<String, String> codes = new HashMap<>();
        projectRepository.findAllById(list.stream().map(DocTailoring::getProjectId).collect(java.util.stream.Collectors.toSet()))
                .forEach(p -> codes.put(p.getId(), p.getProjectCode()));
        return list.stream().map(t -> new DocDto.Response(t.getId(), t.getProjectId(), codes.get(t.getProjectId()), t.getPhaseCode(),
                t.getDocName(), "Y".equals(t.getRequiredYn()), t.getAppliedCode(), t.getNotAppliedReason(), t.getOwnerUserId(),
                t.getOwnerUserId() == null ? null : userNames.get(t.getOwnerUserId()), "Y".equals(t.getCustomYn()), t.getNote(),
                "Y".equals(t.getReviewedYn()),
                versions.getOrDefault(t.getId(), List.of()).stream().map(v -> new DocDto.VersionResponse(v.getId(), v.getVer(),
                        v.getVersionDate(), userNames.get(v.getAuthorUserId()), v.getFileName(), v.getFileSize(), v.getNote())).toList())).toList();
    }
}
