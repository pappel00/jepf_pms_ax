package com.platform.module.privacy.dto;

import com.platform.module.privacy.domain.PrivacyPolicyGroup;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class PrivacyPolicyGroupDto {

    private PrivacyPolicyGroupDto() {
    }

    @Schema(name = "PrivacyPolicyGroupCreateRequest")
    public record CreateRequest(
            @NotBlank @Schema(example = "PRT_a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4") String portalId,
            @NotBlank @Size(max = 50) @Schema(example = "PRIVACY_POLICY") String policyCode,
            @NotBlank @Size(max = 200) @Schema(example = "개인정보처리방침") String policyName,
            @Size(max = 500) @Schema(example = "회원 개인정보 수집·이용·제공에 관한 방침") String description
    ) {
    }

    @Schema(name = "PrivacyPolicyGroupUpdateRequest")
    public record UpdateRequest(
            @NotBlank @Size(max = 200) @Schema(example = "개인정보처리방침") String policyName,
            @Size(max = 500) @Schema(example = "회원 개인정보 수집·이용·제공에 관한 방침") String description,
            @NotBlank @Schema(example = "Y") String useYn
    ) {
        // portalId는 수정 대상에서 뺐다 — TermsGroup과 동일한 이유(다른 포탈로 옮기는 건
        // 사실상 새 방침을 만드는 것에 가깝고, 이미 쌓인 버전 이력의 맥락이 바뀌어버린다).
    }

    @Schema(name = "PrivacyPolicyGroupResponse")
    public record Response(
            String id,
            String tenantId,
            String portalId,
            String policyCode,
            String policyName,
            String description,
            String useYn,
            String deletedYn
    ) {
        public static Response from(PrivacyPolicyGroup group) {
            return new Response(
                    group.getId(),
                    group.getTenantId(),
                    group.getPortalId(),
                    group.getPolicyCode(),
                    group.getPolicyName(),
                    group.getDescription(),
                    group.getUseYn(),
                    group.getDeletedYn()
            );
        }
    }
}
