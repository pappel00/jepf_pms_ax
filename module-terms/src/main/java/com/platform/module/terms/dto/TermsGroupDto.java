package com.platform.module.terms.dto;

import com.platform.module.terms.domain.TermsGroup;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class TermsGroupDto {

    private TermsGroupDto() {
    }

    @Schema(name = "TermsGroupCreateRequest")
    public record CreateRequest(
            @NotBlank @Schema(example = "PRT_a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4") String portalId,
            @NotBlank @Size(max = 50) @Schema(example = "SERVICE_TERMS") String termsCode,
            @NotBlank @Size(max = 200) @Schema(example = "서비스 이용약관") String termsName,
            @NotBlank @Size(max = 20) @Schema(example = "필수") String termsType,
            @Size(max = 500) @Schema(example = "회원가입 시 동의하는 서비스 이용약관") String description
    ) {
    }

    @Schema(name = "TermsGroupUpdateRequest")
    public record UpdateRequest(
            @NotBlank @Size(max = 200) @Schema(example = "서비스 이용약관") String termsName,
            @NotBlank @Size(max = 20) @Schema(example = "필수") String termsType,
            @Size(max = 500) @Schema(example = "회원가입 시 동의하는 서비스 이용약관") String description,
            @NotBlank @Schema(example = "Y") String useYn
    ) {
        // portalId는 수정 대상에서 뺐다 — 다른 포탈로 옮기는 건 "새 약관을 만드는 것"에 가깝고,
        // 이미 쌓인 버전 이력(TermsVersion)의 맥락(어느 포탈 기준으로 동의를 받았는지)이 바뀌어버려
        // 이력 무결성이 깨질 수 있다. 포탈을 바꿔야 한다면 새로 만들고 기존 것은 비활성화(useYn=N)하는
        // 쪽을 권장한다.
    }

    @Schema(name = "TermsGroupResponse")
    public record Response(
            String id,
            String tenantId,
            String portalId,
            String termsCode,
            String termsName,
            String termsType,
            String description,
            String useYn,
            String deletedYn
    ) {
        public static Response from(TermsGroup termsGroup) {
            return new Response(
                    termsGroup.getId(),
                    termsGroup.getTenantId(),
                    termsGroup.getPortalId(),
                    termsGroup.getTermsCode(),
                    termsGroup.getTermsName(),
                    termsGroup.getTermsType(),
                    termsGroup.getDescription(),
                    termsGroup.getUseYn(),
                    termsGroup.getDeletedYn()
            );
        }
    }
}
