package com.platform.module.form.dto;

import com.platform.module.form.domain.FormGroup;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class FormGroupDto {

    private FormGroupDto() {
    }

    @Schema(name = "FormGroupCreateRequest")
    public record CreateRequest(
            @NotBlank @Schema(example = "PRT_a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4") String portalId,
            @NotBlank @Size(max = 50) @Schema(example = "LEAVE_REQUEST") String formCode,
            @NotBlank @Size(max = 200) @Schema(example = "휴가신청서") String formName,
            @Size(max = 50) @Schema(example = "인사") String category,
            @Size(max = 500) @Schema(example = "연차/반차 신청용 표준양식") String description
    ) {
    }

    @Schema(name = "FormGroupUpdateRequest")
    public record UpdateRequest(
            @NotBlank @Size(max = 200) @Schema(example = "휴가신청서") String formName,
            @Size(max = 50) @Schema(example = "인사") String category,
            @Size(max = 500) @Schema(example = "연차/반차 신청용 표준양식") String description,
            @NotBlank @Schema(example = "Y") String useYn
    ) {
        // portalId는 수정 대상에서 뺐다 — TermsGroup과 동일한 이유(다른 포탈로 옮기는 건
        // 사실상 새 양식을 만드는 것에 가깝고, 이미 쌓인 버전 이력의 맥락이 바뀌어버린다).
    }

    @Schema(name = "FormGroupResponse")
    public record Response(
            String id,
            String tenantId,
            String portalId,
            String formCode,
            String formName,
            String category,
            String description,
            String useYn,
            String deletedYn
    ) {
        public static Response from(FormGroup group) {
            return new Response(
                    group.getId(),
                    group.getTenantId(),
                    group.getPortalId(),
                    group.getFormCode(),
                    group.getFormName(),
                    group.getCategory(),
                    group.getDescription(),
                    group.getUseYn(),
                    group.getDeletedYn()
            );
        }
    }
}
