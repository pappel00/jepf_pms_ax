package com.platform.module.pms.controller;

import com.platform.framework.core.common.response.ApiResponse;
import com.platform.module.pms.dto.IssueDto;
import com.platform.module.pms.dto.ScheduleProposalDto;
import com.platform.module.pms.service.IssueService;
import com.platform.module.pms.service.ScheduleProposalService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** 이슈 API(등록·수정·삭제·업무 연결·메모)와 이슈 해결 시 일정 조정 제안 API. */
@RestController
@RequestMapping("/api/v1/pms")
@Tag(name = "PmsIssue", description = "PMS 이슈·일정 조정 제안 (테넌트 자동 격리)")
public class IssueController {

    private final IssueService issueService;
    private final ScheduleProposalService proposalService;

    public IssueController(IssueService issueService, ScheduleProposalService proposalService) {
        this.issueService = issueService;
        this.proposalService = proposalService;
    }

    @GetMapping("/issues")
    public ApiResponse<List<IssueDto.Response>> list(@RequestParam(required = false) String projectId) {
        return ApiResponse.success(issueService.getList(projectId));
    }

    @PostMapping("/projects/{projectId}/issues")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<IssueDto.Response> create(@PathVariable String projectId, @Valid @RequestBody IssueDto.Request request) {
        return ApiResponse.success("201", "이슈를 등록했습니다.", issueService.create(projectId, request));
    }

    @PutMapping("/issues/{id}")
    public ApiResponse<IssueDto.Response> update(@PathVariable String id, @Valid @RequestBody IssueDto.Request request) {
        return ApiResponse.success("200", "이슈를 저장했습니다.", issueService.update(id, request));
    }

    @DeleteMapping("/issues/{id}")
    public ApiResponse<Void> delete(@PathVariable String id) {
        issueService.delete(id);
        return ApiResponse.success("200", "이슈를 삭제했습니다.", null);
    }

    @PostMapping("/issues/{id}/tasks/{taskId}")
    public ApiResponse<IssueDto.Response> linkTask(@PathVariable String id, @PathVariable String taskId) {
        return ApiResponse.success("200", "업무를 연결했습니다.", issueService.linkTask(id, taskId));
    }

    @DeleteMapping("/issues/{id}/tasks/{taskId}")
    public ApiResponse<IssueDto.Response> unlinkTask(@PathVariable String id, @PathVariable String taskId) {
        return ApiResponse.success("200", "업무 연결을 해제했습니다.", issueService.unlinkTask(id, taskId));
    }

    @PostMapping("/issues/{id}/notes")
    public ApiResponse<IssueDto.Response> addNote(@PathVariable String id, @Valid @RequestBody IssueDto.NoteRequest request) {
        return ApiResponse.success("200", "처리 내용을 기록했습니다.", issueService.addNote(id, request.text()));
    }

    @GetMapping("/schedule-proposals")
    public ApiResponse<List<ScheduleProposalDto.Response>> proposals(@RequestParam(required = false) String projectId) {
        return ApiResponse.success(proposalService.getList(projectId));
    }

    @PostMapping("/schedule-proposals/{id}/approve")
    public ApiResponse<ScheduleProposalDto.Response> approve(@PathVariable String id) {
        return ApiResponse.success("200", "승인하고 WBS 일정에 반영했습니다.", proposalService.decide(id, "승인", null));
    }

    @PostMapping("/schedule-proposals/{id}/reject")
    public ApiResponse<ScheduleProposalDto.Response> reject(@PathVariable String id, @Valid @RequestBody ScheduleProposalDto.DecisionRequest request) {
        return ApiResponse.success("200", "반려했습니다.", proposalService.decide(id, "반려", request.comment()));
    }

    @PostMapping("/schedule-proposals/{id}/ignore")
    public ApiResponse<ScheduleProposalDto.Response> ignore(@PathVariable String id, @RequestBody(required = false) ScheduleProposalDto.DecisionRequest request) {
        return ApiResponse.success("200", "무시했습니다.", proposalService.decide(id, "무시", request == null ? null : request.comment()));
    }
}
