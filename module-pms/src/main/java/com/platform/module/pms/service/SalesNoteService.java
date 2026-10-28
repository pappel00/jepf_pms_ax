package com.platform.module.pms.service;

import com.platform.framework.core.common.exception.TenantException;
import com.platform.module.pms.domain.SalesNote;
import com.platform.module.pms.dto.OpsDto;
import com.platform.module.pms.exception.PmsErrorCode;
import com.platform.module.pms.repository.ProjectRepository;
import com.platform.module.pms.repository.SalesNoteRepository;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 영업 노트 — 영업 활동(방문·미팅·통화 등)과 고객 반응, 다음 할 일 기록. 담당자가 바뀌어도 프로젝트/고객사 단위로 이어서 본다. */
@Service
@Transactional(readOnly = true)
public class SalesNoteService {

    private final ProjectAccess access;
    private final SalesNoteRepository noteRepository;
    private final ProjectRepository projectRepository;
    private final UserNameResolver names;

    public SalesNoteService(ProjectAccess access, SalesNoteRepository noteRepository, ProjectRepository projectRepository, UserNameResolver names) {
        this.access = access;
        this.noteRepository = noteRepository;
        this.projectRepository = projectRepository;
        this.names = names;
    }

    public List<OpsDto.NoteResponse> getList() {
        List<SalesNote> all = noteRepository.findAllByDeletedYnOrderByNoteDateDescCreatedDateDesc("N");
        // 프로젝트가 있는 노트는 참여 프로젝트 기준, 프로젝트가 없는 노트는 작성자·영업·전체 열람자만.
        java.util.Set<String> ids = access.accessibleIds();
        boolean sales = ids == null || PmsSupport.hasAnyAuthority("ROLE_PMS_SALES");
        String me = PmsSupport.currentUserId();
        return toResponses(ids == null ? all : all.stream().filter(n -> n.getProjectId() == null
                ? (sales || me.equals(n.getAuthorUserId())) : ids.contains(n.getProjectId())).toList());
    }

    @Transactional
    public OpsDto.NoteResponse create(OpsDto.NoteRequest r) {
        String code = PmsSupport.nextCode("SN-", noteRepository.findAll().stream().map(SalesNote::getNoteCode).toList(), 3);
        SalesNote n = apply(SalesNote.create().withNoteCode(code).withAuthorUserId(PmsSupport.currentUserId()), r);
        return toResponses(List.of(noteRepository.saveAndFlush(n))).get(0);
    }

    @Transactional
    public OpsDto.NoteResponse update(String id, OpsDto.NoteRequest r) {
        return toResponses(List.of(apply(find(id), r))).get(0);
    }

    @Transactional
    public OpsDto.NoteResponse toggleDone(String id) {
        SalesNote n = find(id);
        n.withDoneYn("Y".equals(n.getDoneYn()) ? "N" : "Y");
        return toResponses(List.of(n)).get(0);
    }

    @Transactional
    public void delete(String id) {
        find(id).markDeleted();
    }

    private SalesNote apply(SalesNote n, OpsDto.NoteRequest r) {
        return n.withNoteDate(r.noteDate()).withNoteTypeCode(r.noteTypeCode()).withProjectId(PmsSupport.blankToNull(r.projectId()))
                .withCustomerId(PmsSupport.blankToNull(r.customerId())).withContactId(PmsSupport.blankToNull(r.contactId()))
                .withTitle(r.title().trim()).withContent(r.content().trim()).withNextAction(PmsSupport.blankToNull(r.nextAction()))
                .withNextDate(r.nextDate()).withDoneYn(Boolean.TRUE.equals(r.done()) ? "Y" : "N");
    }

    private SalesNote find(String id) {
        return noteRepository.findByIdAndDeletedYn(id, "N")
                .orElseThrow(() -> new TenantException(PmsErrorCode.NOTE_NOT_FOUND, "존재하지 않는 영업 노트입니다: " + id));
    }

    private List<OpsDto.NoteResponse> toResponses(List<SalesNote> list) {
        Set<String> userIds = new HashSet<>();
        Set<String> projectIds = new HashSet<>();
        list.forEach(n -> {
            userIds.add(n.getAuthorUserId());
            projectIds.add(n.getProjectId());
        });
        Map<String, String> userNames = names.resolve(userIds);
        Map<String, String> codes = new HashMap<>();
        projectRepository.findAllById(projectIds.stream().filter(java.util.Objects::nonNull).toList()).forEach(p -> codes.put(p.getId(), p.getProjectCode()));
        return list.stream().map(n -> new OpsDto.NoteResponse(n.getId(), n.getNoteCode(), n.getNoteDate(), n.getNoteTypeCode(),
                n.getAuthorUserId(), userNames.get(n.getAuthorUserId()), n.getProjectId(), codes.get(n.getProjectId()), n.getCustomerId(),
                n.getContactId(), n.getTitle(), n.getContent(), n.getNextAction(), n.getNextDate(), "Y".equals(n.getDoneYn()))).toList();
    }
}
