package com.platform.module.tenant.dto;

import com.platform.module.tenant.domain.Department;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class DepartmentDto {

    private DepartmentDto() {
    }

    @Schema(name = "DepartmentCreateRequest")
    public record CreateRequest(
            @NotBlank @Size(max = 30) @Schema(example = "DEV") String departmentCode,
            @NotBlank @Size(max = 200) @Schema(example = "개발팀") String departmentName,
            @Schema(example = "DEPT_parent0000000000000000000000") String parentDepartmentId
    ) {
    }

    @Schema(name = "DepartmentUpdateRequest")
    public record UpdateRequest(
            @NotBlank @Size(max = 200) @Schema(example = "개발팀") String departmentName,
            @Schema(example = "DEPT_parent0000000000000000000000") String parentDepartmentId,
            @NotBlank @Schema(example = "Y") String useYn
    ) {
    }

    @Schema(name = "DepartmentResponse")
    public record Response(
            String id,
            String tenantId,
            String departmentCode,
            String departmentName,
            String parentDepartmentId,
            String useYn,
            String deletedYn
    ) {
        public static Response from(Department department) {
            return new Response(
                    department.getId(),
                    department.getTenantId(),
                    department.getDepartmentCode(),
                    department.getDepartmentName(),
                    department.getParentDepartmentId(),
                    department.getUseYn(),
                    department.getDeletedYn()
            );
        }
    }
}
