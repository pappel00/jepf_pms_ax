package com.platform.module.pms.controller;

import com.platform.framework.core.common.response.ApiResponse;
import com.platform.module.pms.dto.OperationDto;
import com.platform.module.pms.dto.OpsDto;
import com.platform.module.pms.service.OperationService;
import com.platform.module.pms.service.OpsService;
import com.platform.module.pms.service.SalesNoteService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** 영업 노트, 차기 사업 기회, 운영 업무·이슈·영업, 수행 종료·운영단계전환·운영 정보 API. */
@RestController
@RequestMapping("/api/v1/pms")
@Tag(name = "PmsOps", description = "PMS 영업 노트·차기 사업 기회·운영 단계 (테넌트 자동 격리)")
public class OpsController {

    private final SalesNoteService noteService;
    private final OpsService opsService;
    private final OperationService operationService;

    public OpsController(SalesNoteService noteService, OpsService opsService, OperationService operationService) {
        this.noteService = noteService;
        this.opsService = opsService;
        this.operationService = operationService;
    }

    // ---------- 영업 노트 ----------

    @GetMapping("/sales-notes")
    public ApiResponse<List<OpsDto.NoteResponse>> notes() {
        return ApiResponse.success(noteService.getList());
    }

    @PostMapping("/sales-notes")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<OpsDto.NoteResponse> createNote(@Valid @RequestBody OpsDto.NoteRequest request) {
        return ApiResponse.success("201", "영업 노트를 등록했습니다.", noteService.create(request));
    }

    @PutMapping("/sales-notes/{id}")
    public ApiResponse<OpsDto.NoteResponse> updateNote(@PathVariable String id, @Valid @RequestBody OpsDto.NoteRequest request) {
        return ApiResponse.success("200", "영업 노트를 저장했습니다.", noteService.update(id, request));
    }

    @PostMapping("/sales-notes/{id}/toggle-done")
    public ApiResponse<OpsDto.NoteResponse> toggleNote(@PathVariable String id) {
        return ApiResponse.success("200", "후속 조치 상태를 바꿨습니다.", noteService.toggleDone(id));
    }

    @DeleteMapping("/sales-notes/{id}")
    public ApiResponse<Void> deleteNote(@PathVariable String id) {
        noteService.delete(id);
        return ApiResponse.success("200", "삭제되었습니다.", null);
    }

    // ---------- 차기 사업 기회 ----------

    @GetMapping("/next-opps")
    public ApiResponse<List<OpsDto.NextResponse>> nexts() {
        return ApiResponse.success(opsService.getNexts());
    }

    @PostMapping("/projects/{projectId}/next-opps")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<OpsDto.NextResponse> createNext(@PathVariable String projectId, @Valid @RequestBody OpsDto.NextRequest request) {
        return ApiResponse.success("201", "차기 사업 기회를 등록했습니다.", opsService.createNext(projectId, request));
    }

    @PutMapping("/next-opps/{id}")
    public ApiResponse<OpsDto.NextResponse> updateNext(@PathVariable String id, @Valid @RequestBody OpsDto.NextRequest request) {
        return ApiResponse.success("200", "저장했습니다.", opsService.updateNext(id, request));
    }

    @DeleteMapping("/next-opps/{id}")
    public ApiResponse<Void> deleteNext(@PathVariable String id) {
        opsService.deleteNext(id);
        return ApiResponse.success("200", "삭제되었습니다.", null);
    }

    // ---------- 운영 업무 ----------

    @GetMapping("/ops-tasks")
    public ApiResponse<List<OpsDto.TaskResponse>> tasks() {
        return ApiResponse.success(opsService.getTasks());
    }

    @PostMapping("/projects/{projectId}/ops-tasks")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<OpsDto.TaskResponse> createTask(@PathVariable String projectId, @Valid @RequestBody OpsDto.TaskRequest request) {
        return ApiResponse.success("201", "운영 업무를 등록했습니다.", opsService.createTask(projectId, request));
    }

    @PutMapping("/ops-tasks/{id}")
    public ApiResponse<OpsDto.TaskResponse> updateTask(@PathVariable String id, @Valid @RequestBody OpsDto.TaskRequest request) {
        return ApiResponse.success("200", "저장했습니다.", opsService.updateTask(id, request));
    }

    @PostMapping("/ops-tasks/{id}/status")
    public ApiResponse<OpsDto.TaskResponse> setTaskStatus(@PathVariable String id, @Valid @RequestBody OpsDto.TaskStatusRequest request) {
        return ApiResponse.success("200", "상태를 바꿨습니다.", opsService.setTaskStatus(id, request.statusCode()));
    }

    @DeleteMapping("/ops-tasks/{id}")
    public ApiResponse<Void> deleteTask(@PathVariable String id) {
        opsService.deleteTask(id);
        return ApiResponse.success("200", "삭제되었습니다.", null);
    }

    // ---------- 운영 이슈 ----------

    @GetMapping("/ops-incidents")
    public ApiResponse<List<OpsDto.IncidentResponse>> incidents() {
        return ApiResponse.success(opsService.getIncidents());
    }

    @PostMapping("/projects/{projectId}/ops-incidents")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<OpsDto.IncidentResponse> createIncident(@PathVariable String projectId, @Valid @RequestBody OpsDto.IncidentRequest request) {
        return ApiResponse.success("201", "운영 이슈를 접수했습니다.", opsService.createIncident(projectId, request));
    }

    @PutMapping("/ops-incidents/{id}")
    public ApiResponse<OpsDto.IncidentResponse> updateIncident(@PathVariable String id, @Valid @RequestBody OpsDto.IncidentRequest request) {
        return ApiResponse.success("200", "저장했습니다.", opsService.updateIncident(id, request));
    }

    @PutMapping("/ops-incidents/{id}/opportunity")
    public ApiResponse<OpsDto.IncidentResponse> linkIncident(@PathVariable String id, @RequestBody OpsDto.IncidentLinkRequest request) {
        return ApiResponse.success("200", "영업 기회를 연결했습니다.", opsService.linkIncidentOpportunity(id, request.opportunityId()));
    }

    @DeleteMapping("/ops-incidents/{id}")
    public ApiResponse<Void> deleteIncident(@PathVariable String id) {
        opsService.deleteIncident(id);
        return ApiResponse.success("200", "삭제되었습니다.", null);
    }

    // ---------- 운영 영업 ----------

    @GetMapping("/ops-opps")
    public ApiResponse<List<OpsDto.OppResponse>> opps() {
        return ApiResponse.success(opsService.getOpps());
    }

    @PostMapping("/projects/{projectId}/ops-opps")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<OpsDto.OppResponse> createOpp(@PathVariable String projectId, @Valid @RequestBody OpsDto.OppRequest request) {
        return ApiResponse.success("201", "영업 기회를 등록했습니다.", opsService.createOpp(projectId, request));
    }

    @PutMapping("/ops-opps/{id}")
    public ApiResponse<OpsDto.OppResponse> updateOpp(@PathVariable String id, @Valid @RequestBody OpsDto.OppRequest request) {
        return ApiResponse.success("200", "저장했습니다.", opsService.updateOpp(id, request));
    }

    @DeleteMapping("/ops-opps/{id}")
    public ApiResponse<Void> deleteOpp(@PathVariable String id) {
        opsService.deleteOpp(id);
        return ApiResponse.success("200", "삭제되었습니다.", null);
    }

    // ---------- 수행 종료 · 운영단계전환 · 운영 정보 ----------

    @GetMapping("/operations")
    public ApiResponse<List<OperationDto.Response>> operations() {
        return ApiResponse.success(operationService.getOperations());
    }

    @GetMapping("/project-features")
    public ApiResponse<List<OperationDto.FeatureResponse>> features() {
        return ApiResponse.success(operationService.getFeatures());
    }

    @PostMapping("/projects/{projectId}/end-execution")
    public ApiResponse<Void> endExecution(@PathVariable String projectId, @Valid @RequestBody OperationDto.EndRequest request) {
        operationService.endExecution(projectId, request);
        return ApiResponse.success("200", "수행 종료 처리했습니다.", null);
    }

    @PostMapping("/projects/{projectId}/operation-transition")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<OperationDto.Response> operationTransition(@PathVariable String projectId, @Valid @RequestBody OperationDto.Request request) {
        return ApiResponse.success("201", "운영 단계로 전환했습니다.", operationService.transition(projectId, request));
    }

    @PutMapping("/projects/{projectId}/operation")
    public ApiResponse<OperationDto.Response> updateOperation(@PathVariable String projectId, @Valid @RequestBody OperationDto.Request request) {
        return ApiResponse.success("200", "운영 정보를 저장했습니다.", operationService.update(projectId, request));
    }
}
