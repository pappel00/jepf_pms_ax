package com.platform.module.privacy.dto;

import com.platform.module.privacy.domain.PrivacyPolicyVersion;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public final class PrivacyPolicyVersionDto {

    private PrivacyPolicyVersionDto() {
    }

    @Schema(name = "PrivacyPolicyVersionCreateRequest")
    public record CreateRequest(
            @NotBlank @Size(max = 30) @Schema(example = "1.0") String versionNo,
            @NotBlank @Schema(example = "제1조(목적) 이 방침은...") String content,
            @NotNull @Schema(example = "2026-01-01") LocalDate effectiveDate,
            @Size(max = 500) @Schema(example = "최초 시행") String description
    ) {
    }

    @Schema(name = "PrivacyPolicyVersionUpdateRequest")
    public record UpdateRequest(
            @NotBlank @Size(max = 30) @Schema(example = "1.1") String versionNo,
            @NotBlank @Schema(example = "제1조(목적) 이 방침은...") String content,
            @NotNull @Schema(example = "2026-03-01") LocalDate effectiveDate,
            @Size(max = 500) @Schema(example = "보관기간 조항 개정") String description,
            @NotBlank @Schema(example = "Y") String useYn
    ) {
    }

    @Schema(name = "PrivacyPolicyVersionResponse")
    public record Response(
            String id,
            String tenantId,
            String groupId,
            String versionNo,
            String content,
            LocalDate effectiveDate,
            String description,
            String useYn,
            String deletedYn
    ) {
        public static Response from(PrivacyPolicyVersion version) {
            return new Response(
                    version.getId(),
                    version.getTenantId(),
                    version.getGroupId(),
                    version.getVersionNo(),
                    version.getContent(),
                    version.getEffectiveDate(),
                    version.getDescription(),
                    version.getUseYn(),
                    version.getDeletedYn()
            );
        }
    }
}
