package com.platform.module.pms.service;

import com.platform.framework.core.common.exception.BusinessException;
import com.platform.framework.core.common.exception.TenantException;
import com.platform.module.pms.domain.Project;
import com.platform.module.pms.domain.ProposalMapping;
import com.platform.module.pms.domain.Rfp;
import com.platform.module.pms.domain.RfpEval;
import com.platform.module.pms.domain.RfpEvaluator;
import com.platform.module.pms.domain.RfpItem;
import com.platform.module.pms.dto.RfpDto;
import com.platform.module.pms.exception.PmsErrorCode;
import com.platform.module.pms.repository.ProjectRepository;
import com.platform.module.pms.repository.ProposalMappingRepository;
import com.platform.module.pms.repository.ProposalRepository;
import com.platform.module.pms.repository.RfpEvalRepository;
import com.platform.module.pms.repository.RfpEvaluatorRepository;
import com.platform.module.pms.repository.RfpItemRepository;
import com.platform.module.pms.repository.RfpRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * RFP(제안요청서) — 프로젝트 1 : N RFP(차수), RFP 1 : N 항목, 항목 × 평가자 평가, 확정 M/D(원가 산정용).
 * 평가는 블라인드다: 본인 평가는 항상, 다른 평가자의 평가는 제출된 것만 조회된다. 평가자별 M/D는 자동 취합하지 않고,
 * 평가 관리자가 항목마다 확정 평가자를 고르면 그 값이 확정 M/D가 된다(직접 수정 가능). RFP 상태는 저장하지 않고 화면이 계산한다.
 */
@Service
@Transactional(readOnly = true)
public class RfpService {

    private final RfpRepository rfpRepository;
    private final RfpItemRepository itemRepository;
    private final RfpEvaluatorRepository evaluatorRepository;
    private final RfpEvalRepository evalRepository;
    private final ProposalRepository proposalRepository;
    private final ProposalMappingRepository mappingRepository;
    private final ProjectRepository projectRepository;
    private final ProjectService projectService;
    private final UserNameResolver names;

    public RfpService(RfpRepository rfpRepository, RfpItemRepository itemRepository, RfpEvaluatorRepository evaluatorRepository,
                      RfpEvalRepository evalRepository, ProposalRepository proposalRepository,
                      ProposalMappingRepository mappingRepository, ProjectRepository projectRepository,
                      ProjectService projectService, UserNameResolver names) {
        this.rfpRepository = rfpRepository;
        this.itemRepository = itemRepository;
        this.evaluatorRepository = evaluatorRepository;
        this.evalRepository = evalRepository;
        this.proposalRepository = proposalRepository;
        this.mappingRepository = mappingRepository;
        this.projectRepository = projectRepository;
        this.projectService = projectService;
        this.names = names;
    }

    // ---------- 조회 ----------

    public List<RfpDto.Response> getList(String projectId) {
        List<Rfp> rfps = projectId == null || projectId.isBlank()
                ? rfpRepository.findAllByDeletedYnOrderByRfpCodeAsc("N")
                : rfpRepository.findAllByProjectIdAndDeletedYnOrderByRfpCodeAsc(projectId.trim(), "N");
        return toResponses(rfps);
    }

    /** 평가 목록 — 본인 평가는 항상, 다른 평가자의 평가는 제출된 것만(블라인드). */
    public List<RfpDto.EvalResponse> getVisibleEvals() {
        String me = PmsSupport.currentUserId();
        List<Rfp> rfps = rfpRepository.findAllByDeletedYnOrderByRfpCodeAsc("N");
        Map<String, List<RfpItem>> items = itemsByRfp(rfps);
        Map<String, List<RfpEvaluator>> evaluators = evaluatorsByRfp(rfps);
        List<RfpDto.EvalResponse> out = new ArrayList<>();
        Set<String> userIds = new HashSet<>();
        List<RfpEval> visible = new ArrayList<>();
        for (Rfp rfp : rfps) {
            Set<String> submitted = new HashSet<>();
            evaluators.getOrDefault(rfp.getId(), List.of()).stream().filter(e -> "Y".equals(e.getSubmittedYn()))
                    .forEach(e -> submitted.add(e.getEvaluatorUserId()));
            List<String> itemIds = items.getOrDefault(rfp.getId(), List.of()).stream().map(RfpItem::getId).toList();
            for (RfpEval ev : evalRepository.findAllByRfpItemIdInAndDeletedYn(itemIds, "N")) {
                if (ev.getEvaluatorUserId().equals(me) || submitted.contains(ev.getEvaluatorUserId())) {
                    visible.add(ev);
                    userIds.add(ev.getEvaluatorUserId());
                }
            }
        }
        Map<String, String> userNames = names.resolve(userIds);
        for (RfpEval ev : visible) {
            out.add(new RfpDto.EvalResponse(ev.getRfpItemId(), ev.getEvaluatorUserId(), userNames.get(ev.getEvaluatorUserId()),
                    ev.getKindCode(), ev.getImplCode(), ev.getDifficultyCode(), ev.getMd(), ev.getCommentText()));
        }
        return out;
    }

    // ---------- RFP ----------

    @Transactional
    public RfpDto.Response create(String projectId, RfpDto.Request r) {
        Project project = projectService.requireProject(projectId);
        String code = PmsSupport.nextCode("RFP-" + java.time.Year.now().getValue() + "-",
                rfpRepository.findAll().stream().map(Rfp::getRfpCode).toList(), 3);
        Rfp rfp = apply(Rfp.create().withRfpCode(code).withProjectId(project.getId()), r);
        return toResponses(List.of(rfpRepository.saveAndFlush(rfp))).get(0);
    }

    @Transactional
    public RfpDto.Response update(String id, RfpDto.Request r) {
        Rfp rfp = findRfp(id);
        apply(rfp, r);
        return toResponses(List.of(rfp)).get(0);
    }

    @Transactional
    public void delete(String id) {
        Rfp rfp = findRfp(id);
        if (proposalRepository.existsByRfpIdAndDeletedYn(id, "N")) {
            throw new BusinessException(PmsErrorCode.RFP_INVALID, "이 RFP에 대응하는 제안서가 있어 삭제할 수 없습니다.");
        }
        List<RfpItem> items = itemRepository.findAllByRfpIdAndDeletedYnOrderBySortOrderAscCreatedDateAsc(id, "N");
        // 항목이 수천 건일 수 있어(문서 자동 분해) 평가·제안서 대응은 한 번에 조회해 지운다.
        List<String> itemIds = items.stream().map(RfpItem::getId).toList();
        if (!itemIds.isEmpty()) {
            evalRepository.findAllByRfpItemIdInAndDeletedYn(itemIds, "N").forEach(RfpEval::markDeleted);
            mappingRepository.findAllByRfpItemIdInAndDeletedYn(itemIds, "N").forEach(ProposalMapping::markDeleted);
        }
        items.forEach(RfpItem::markDeleted);
        evaluatorRepository.findAllByRfpIdAndDeletedYnOrderByCreatedDateAsc(id, "N").forEach(RfpEvaluator::markDeleted);
        rfp.markDeleted();
    }

    @Transactional
    public RfpDto.Response setEvaluators(String id, List<String> userIds) {
        Rfp rfp = findRfp(id);
        Set<String> wanted = new LinkedHashSet<>(userIds);
        Map<String, String> found = names.resolve(wanted);
        for (String uid : wanted) {
            if (!found.containsKey(uid)) {
                throw new TenantException(PmsErrorCode.USER_NOT_FOUND, "존재하지 않는 사용자입니다: " + uid);
            }
        }
        List<RfpEvaluator> current = evaluatorRepository.findAllByRfpIdAndDeletedYnOrderByCreatedDateAsc(id, "N");
        Set<String> kept = new HashSet<>();
        List<String> itemIds = itemRepository.findAllByRfpIdAndDeletedYnOrderBySortOrderAscCreatedDateAsc(id, "N").stream().map(RfpItem::getId).toList();
        for (RfpEvaluator e : current) {
            if (wanted.contains(e.getEvaluatorUserId())) {
                kept.add(e.getEvaluatorUserId());
            } else {
                e.markDeleted();
                evalRepository.findAllByRfpItemIdInAndDeletedYn(itemIds, "N").stream()
                        .filter(ev -> ev.getEvaluatorUserId().equals(e.getEvaluatorUserId())).forEach(RfpEval::markDeleted);
            }
        }
        for (String uid : wanted) {
            if (!kept.contains(uid)) {
                evaluatorRepository.save(RfpEvaluator.create().withRfpId(id).withEvaluatorUserId(uid).withSubmittedYn("N"));
            }
        }
        return toResponses(List.of(rfp)).get(0);
    }

    // ---------- 항목 ----------

    @Transactional
    public RfpDto.Response addItem(String rfpId, RfpDto.ItemRequest r) {
        Rfp rfp = findRfp(rfpId);
        addItems(rfp, List.of(r));
        return toResponses(List.of(rfp)).get(0);
    }

    @Transactional
    public RfpDto.Response addItems(String rfpId, List<RfpDto.ItemRequest> list) {
        Rfp rfp = findRfp(rfpId);
        addItems(rfp, list);
        return toResponses(List.of(rfp)).get(0);
    }

    @Transactional
    public RfpDto.Response updateItem(String itemId, RfpDto.ItemRequest r) {
        RfpItem item = findItem(itemId);
        if (!item.getCategoryCode().equals(r.categoryCode())) {
            item.withItemCode(nextItemCode(item.getRfpId(), r.categoryCode())).withCategoryCode(r.categoryCode());
        }
        item.withArea(PmsSupport.blankToNull(r.area())).withItemName(r.itemName().trim()).withContent(PmsSupport.blankToNull(r.content()))
                .withSourcePage(r.sourcePage()).withMandatoryYn(r.mandatory() ? "Y" : "N");
        return toResponses(List.of(findRfp(item.getRfpId()))).get(0);
    }

    @Transactional
    public RfpDto.Response deleteItem(String itemId) {
        RfpItem item = findItem(itemId);
        deleteItemRows(item);
        return toResponses(List.of(findRfp(item.getRfpId()))).get(0);
    }

    // ---------- 평가 ----------

    /** 내 평가 저장 — 평가자로 지정된 사용자만, 제출 전에만. */
    @Transactional
    public RfpDto.EvalResponse saveMyEval(String itemId, RfpDto.EvalRequest r) {
        RfpItem item = findItem(itemId);
        String me = PmsSupport.currentUserId();
        RfpEvaluator ev = evaluatorRepository.findByRfpIdAndEvaluatorUserIdAndDeletedYn(item.getRfpId(), me, "N")
                .orElseThrow(() -> new BusinessException(PmsErrorCode.RFP_INVALID, "이 RFP의 평가자로 지정되지 않았습니다."));
        if ("Y".equals(ev.getSubmittedYn())) {
            throw new BusinessException(PmsErrorCode.RFP_INVALID, "제출한 평가는 수정할 수 없습니다. 평가 관리자에게 재평가를 요청하세요.");
        }
        String kind = PmsSupport.blankToNull(r.kindCode());
        String impl = PmsSupport.blankToNull(r.implCode());
        BigDecimal md = r.md();
        if ("비기능".equals(kind)) {
            impl = null;
            md = null;
        } else if ("기능".equals(kind) && "미구현".equals(impl)) {
            md = BigDecimal.ZERO;
        }
        RfpEval row = evalRepository.findByRfpItemIdAndEvaluatorUserIdAndDeletedYn(itemId, me, "N")
                .orElseGet(() -> RfpEval.create().withRfpItemId(itemId).withEvaluatorUserId(me));
        row.withKindCode(kind).withImplCode(impl).withDifficultyCode(PmsSupport.blankToNull(r.difficultyCode())).withMd(md)
                .withCommentText(PmsSupport.blankToNull(r.comment()));
        evalRepository.save(row);
        return new RfpDto.EvalResponse(itemId, me, names.resolve(List.of(me)).get(me), row.getKindCode(), row.getImplCode(),
                row.getDifficultyCode(), row.getMd(), row.getCommentText());
    }

    /** 평가 제출 — 모든 항목이 완료여야 한다. 제출 후에는 수정할 수 없다. */
    @Transactional
    public RfpDto.Response submit(String rfpId) {
        Rfp rfp = findRfp(rfpId);
        String me = PmsSupport.currentUserId();
        RfpEvaluator ev = evaluatorRepository.findByRfpIdAndEvaluatorUserIdAndDeletedYn(rfpId, me, "N")
                .orElseThrow(() -> new BusinessException(PmsErrorCode.RFP_INVALID, "이 RFP의 평가자로 지정되지 않았습니다."));
        List<RfpItem> items = itemRepository.findAllByRfpIdAndDeletedYnOrderBySortOrderAscCreatedDateAsc(rfpId, "N");
        Map<String, RfpEval> mine = new HashMap<>();
        evalRepository.findAllByRfpItemIdInAndDeletedYn(items.stream().map(RfpItem::getId).toList(), "N").stream()
                .filter(e -> e.getEvaluatorUserId().equals(me)).forEach(e -> mine.put(e.getRfpItemId(), e));
        long missing = items.stream().filter(i -> !isComplete(mine.get(i.getId()))).count();
        if (missing > 0) {
            throw new BusinessException(PmsErrorCode.RFP_INVALID, "아직 입력이 끝나지 않은 항목이 " + missing + "건 있습니다.");
        }
        ev.withSubmittedYn("Y").withSubmittedAt(LocalDateTime.now());
        return toResponses(List.of(rfp)).get(0);
    }

    /** 재평가 요청 — 제출을 풀어 평가자가 다시 수정할 수 있게 한다. */
    @Transactional
    public RfpDto.Response reopen(String rfpId, String evaluatorUserId) {
        Rfp rfp = findRfp(rfpId);
        RfpEvaluator ev = evaluatorRepository.findByRfpIdAndEvaluatorUserIdAndDeletedYn(rfpId, evaluatorUserId, "N")
                .orElseThrow(() -> new BusinessException(PmsErrorCode.RFP_INVALID, "평가자가 아닙니다: " + evaluatorUserId));
        ev.withSubmittedYn("N").withSubmittedAt(null);
        return toResponses(List.of(rfp)).get(0);
    }

    // ---------- 확정 M/D ----------

    @Transactional
    public RfpDto.Response confirm(String itemId, RfpDto.ConfirmRequest r) {
        RfpItem item = findItem(itemId);
        String me = PmsSupport.currentUserId();
        String evaluator = PmsSupport.blankToNull(r.evaluatorUserId());
        if (evaluator == null && r.md() == null) {
            item.withConfirmedEvaluatorUserId(null).withConfirmedMd(null).withConfirmedEditedYn(null).withConfirmedBy(null).withConfirmedAt(null);
        } else if (r.md() != null) {
            // 직접 수정 — 확정 평가자는 유지(없으면 비워 둠), 「수정됨」 표시
            item.withConfirmedEvaluatorUserId(evaluator != null ? evaluator : item.getConfirmedEvaluatorUserId())
                    .withConfirmedMd(r.md()).withConfirmedEditedYn("Y").withConfirmedBy(me).withConfirmedAt(LocalDateTime.now());
        } else {
            applyConfirmFromEvaluator(item, evaluator, me);
        }
        return toResponses(List.of(findRfp(item.getRfpId()))).get(0);
    }

    @Transactional
    public RfpDto.Response confirmAll(String rfpId, String evaluatorUserId) {
        Rfp rfp = findRfp(rfpId);
        String me = PmsSupport.currentUserId();
        for (RfpItem item : itemRepository.findAllByRfpIdAndDeletedYnOrderBySortOrderAscCreatedDateAsc(rfpId, "N")) {
            applyConfirmFromEvaluator(item, evaluatorUserId, me);
        }
        return toResponses(List.of(rfp)).get(0);
    }

    private void applyConfirmFromEvaluator(RfpItem item, String evaluator, String me) {
        RfpEvaluator ev = evaluatorRepository.findByRfpIdAndEvaluatorUserIdAndDeletedYn(item.getRfpId(), evaluator, "N")
                .orElseThrow(() -> new BusinessException(PmsErrorCode.RFP_INVALID, "평가자가 아닙니다: " + evaluator));
        if (!"Y".equals(ev.getSubmittedYn())) {
            throw new BusinessException(PmsErrorCode.RFP_INVALID, "평가를 제출한 평가자만 확정 평가자로 선택할 수 있습니다.");
        }
        RfpEval eval = evalRepository.findByRfpItemIdAndEvaluatorUserIdAndDeletedYn(item.getId(), evaluator, "N").orElse(null);
        item.withConfirmedEvaluatorUserId(evaluator).withConfirmedMd(mdOf(eval)).withConfirmedEditedYn("N")
                .withConfirmedBy(me).withConfirmedAt(LocalDateTime.now());
    }

    // ---------- 내부 ----------

    /** 집계에 쓰는 M/D — 기능 항목만(비기능·미구현은 0). */
    static BigDecimal mdOf(RfpEval ev) {
        if (ev == null || !"기능".equals(ev.getKindCode()) || "미구현".equals(ev.getImplCode()) || ev.getMd() == null) {
            return BigDecimal.ZERO;
        }
        return ev.getMd();
    }

    /** 평가 입력이 끝났는지: 구분 필수, 기능이면 구현 여부·난이도 필수, 구현/일부 구현이면 M/D(>0) 필수. */
    static boolean isComplete(RfpEval ev) {
        if (ev == null || ev.getKindCode() == null) {
            return false;
        }
        if ("비기능".equals(ev.getKindCode())) {
            return true;
        }
        if (ev.getImplCode() == null || ev.getDifficultyCode() == null) {
            return false;
        }
        if ("구현".equals(ev.getImplCode()) || "일부 구현".equals(ev.getImplCode())) {
            return ev.getMd() != null && ev.getMd().signum() > 0;
        }
        return true;
    }

    private Rfp apply(Rfp rfp, RfpDto.Request r) {
        return rfp.withTitle(r.title().trim()).withRoundCode(r.roundCode()).withReceivedDate(r.receivedDate()).withDueDate(r.dueDate());
    }

    private void addItems(Rfp rfp, List<RfpDto.ItemRequest> list) {
        List<RfpItem> existing = itemRepository.findAllByRfpId(rfp.getId());
        int order = existing.stream().mapToInt(RfpItem::getSortOrder).max().orElse(-1) + 1;
        Map<String, Integer> counters = new HashMap<>();
        existing.forEach(i -> counters.merge(i.getCategoryCode(), codeNo(i.getItemCode()), Math::max));
        Set<String> usedCodes = new HashSet<>();
        existing.forEach(i -> usedCodes.add(i.getItemCode()));
        List<RfpItem> created = new ArrayList<>();
        for (RfpDto.ItemRequest r : list) {
            // 원문 고유번호가 있고 겹치지 않으면 그대로 쓴다(문서 자동 분해). 아니면 분류-일련번호로 채번.
            String wanted = PmsSupport.blankToNull(r.itemCode());
            String code;
            if (wanted != null && !usedCodes.contains(wanted)) {
                code = wanted;
            } else {
                int no = counters.merge(r.categoryCode(), 1, (a, b) -> a + 1);
                code = r.categoryCode() + "-" + String.format("%03d", no);
            }
            usedCodes.add(code);
            created.add(RfpItem.create().withRfpId(rfp.getId()).withItemCode(code)
                    .withCategoryCode(r.categoryCode()).withArea(PmsSupport.blankToNull(r.area())).withItemName(r.itemName().trim())
                    .withContent(PmsSupport.blankToNull(r.content())).withSourcePage(r.sourcePage())
                    .withMandatoryYn(r.mandatory() ? "Y" : "N").withSortOrder(order++));
        }
        itemRepository.saveAll(created);
    }

    private String nextItemCode(String rfpId, String category) {
        int max = itemRepository.findAllByRfpId(rfpId).stream().filter(i -> category.equals(i.getCategoryCode()))
                .mapToInt(i -> codeNo(i.getItemCode())).max().orElse(0);
        return category + "-" + String.format("%03d", max + 1);
    }

    private static int codeNo(String code) {
        try {
            return Integer.parseInt(code.substring(code.lastIndexOf('-') + 1));
        } catch (RuntimeException e) {
            return 0;
        }
    }

    /** 항목과 그 항목의 평가·제안서 대응 행을 함께 지운다. */
    private void deleteItemRows(RfpItem item) {
        evalRepository.findAllByRfpItemIdInAndDeletedYn(List.of(item.getId()), "N").forEach(RfpEval::markDeleted);
        mappingRepository.findAllByRfpItemIdInAndDeletedYn(List.of(item.getId()), "N").forEach(ProposalMapping::markDeleted);
        item.markDeleted();
    }

    private Rfp findRfp(String id) {
        return rfpRepository.findByIdAndDeletedYn(id, "N")
                .orElseThrow(() -> new TenantException(PmsErrorCode.RFP_NOT_FOUND, "존재하지 않는 RFP입니다: " + id));
    }

    private RfpItem findItem(String id) {
        return itemRepository.findByIdAndDeletedYn(id, "N")
                .orElseThrow(() -> new TenantException(PmsErrorCode.RFP_NOT_FOUND, "존재하지 않는 RFP 항목입니다: " + id));
    }

    private Map<String, List<RfpItem>> itemsByRfp(List<Rfp> rfps) {
        Map<String, List<RfpItem>> map = new HashMap<>();
        if (rfps.isEmpty()) {
            return map;
        }
        itemRepository.findAllByRfpIdInAndDeletedYnOrderBySortOrderAscCreatedDateAsc(rfps.stream().map(Rfp::getId).toList(), "N")
                .forEach(i -> map.computeIfAbsent(i.getRfpId(), k -> new ArrayList<>()).add(i));
        return map;
    }

    private Map<String, List<RfpEvaluator>> evaluatorsByRfp(List<Rfp> rfps) {
        Map<String, List<RfpEvaluator>> map = new HashMap<>();
        if (rfps.isEmpty()) {
            return map;
        }
        evaluatorRepository.findAllByRfpIdInAndDeletedYnOrderByCreatedDateAsc(rfps.stream().map(Rfp::getId).toList(), "N")
                .forEach(e -> map.computeIfAbsent(e.getRfpId(), k -> new ArrayList<>()).add(e));
        return map;
    }

    private List<RfpDto.Response> toResponses(List<Rfp> rfps) {
        if (rfps.isEmpty()) {
            return List.of();
        }
        Map<String, List<RfpItem>> items = itemsByRfp(rfps);
        Map<String, List<RfpEvaluator>> evaluators = evaluatorsByRfp(rfps);
        List<String> allItemIds = items.values().stream().flatMap(List::stream).map(RfpItem::getId).toList();
        Map<String, List<RfpEval>> evalsByItem = new HashMap<>();
        if (!allItemIds.isEmpty()) {
            evalRepository.findAllByRfpItemIdInAndDeletedYn(allItemIds, "N")
                    .forEach(e -> evalsByItem.computeIfAbsent(e.getRfpItemId(), k -> new ArrayList<>()).add(e));
        }
        Set<String> userIds = new HashSet<>();
        evaluators.values().forEach(l -> l.forEach(e -> userIds.add(e.getEvaluatorUserId())));
        items.values().forEach(l -> l.forEach(i -> userIds.add(i.getConfirmedEvaluatorUserId())));
        Map<String, String> userNames = names.resolve(userIds);
        Set<String> projectIds = new HashSet<>();
        rfps.forEach(r -> projectIds.add(r.getProjectId()));
        Map<String, String> codes = new HashMap<>();
        projectRepository.findAllById(projectIds).forEach(p -> codes.put(p.getId(), p.getProjectCode()));

        return rfps.stream().map(rfp -> {
            List<RfpEvaluator> evs = evaluators.getOrDefault(rfp.getId(), List.of());
            Set<String> evaluatorIds = new HashSet<>();
            evs.forEach(e -> evaluatorIds.add(e.getEvaluatorUserId()));
            List<RfpDto.ItemResponse> itemResponses = items.getOrDefault(rfp.getId(), List.of()).stream().map(i -> {
                int done = (int) evalsByItem.getOrDefault(i.getId(), List.of()).stream()
                        .filter(e -> evaluatorIds.contains(e.getEvaluatorUserId()) && isComplete(e)).count();
                String confirmed = i.getConfirmedEvaluatorUserId();
                return new RfpDto.ItemResponse(i.getId(), i.getItemCode(), i.getCategoryCode(), i.getArea(), i.getItemName(),
                        i.getContent(), i.getSourcePage(), "Y".equals(i.getMandatoryYn()), done, confirmed,
                        confirmed == null ? null : userNames.get(confirmed), i.getConfirmedMd(), "Y".equals(i.getConfirmedEditedYn()));
            }).toList();
            return new RfpDto.Response(rfp.getId(), rfp.getRfpCode(), rfp.getProjectId(), codes.get(rfp.getProjectId()), rfp.getTitle(),
                    rfp.getRoundCode(), rfp.getReceivedDate(), rfp.getDueDate(),
                    evs.stream().map(e -> new RfpDto.EvaluatorResponse(e.getEvaluatorUserId(), userNames.get(e.getEvaluatorUserId()),
                            "Y".equals(e.getSubmittedYn()))).toList(),
                    itemResponses);
        }).toList();
    }
}
