package com.platform.module.pms.controller;

import com.platform.framework.core.common.response.ApiResponse;
import com.platform.module.pms.dto.TeamDto;
import com.platform.module.pms.service.ProjectMemberService;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 프로젝트 관리자·투입 인력(팀) 조회 API. 팀을 바꾸는 API는 없다 — 단계전환 승인(최초 지정)과
 * 투입 계획 변경 요청 승인으로만 바뀐다.
 */
@RestController
@RequestMapping("/api/v1/pms")
@Tag(name = "PmsTeam", description = "PMS 프로젝트 관리자·투입 인력 조회 (테넌트 자동 격리)")
public class TeamController {

    private final ProjectMemberService memberService;

    public TeamController(ProjectMemberService memberService) {
        this.memberService = memberService;
    }

    @GetMapping("/projects/{projectId}/team")
    public ApiResponse<TeamDto.TeamResponse> team(@PathVariable String projectId, @RequestParam(defaultValue = "EXEC") String kind) {
        return ApiResponse.success(memberService.getTeam(projectId, kind));
    }

    /** 팀이 있는 모든 프로젝트의 팀(프로젝트 목록 화면용). */
    @GetMapping("/teams")
    public ApiResponse<List<TeamDto.TeamResponse>> teams(@RequestParam(defaultValue = "EXEC") String kind) {
        return ApiResponse.success(memberService.listTeams(kind));
    }
}