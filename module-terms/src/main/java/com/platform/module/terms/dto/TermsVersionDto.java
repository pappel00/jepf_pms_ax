package com.platform.module.terms.dto;

import com.platform.module.terms.domain.TermsVersion;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public final class TermsVersionDto {

    private TermsVersionDto() {
    }

    @Schema(name = "TermsVersionCreateRequest")
    public record CreateRequest(
            @NotBlank @Size(max = 30) @Schema(example = "1.0") String versionNo,
            @NotBlank @Schema(example = "제1조(목적) 이 약관은...") String content,
            @NotNull @Schema(example = "2026-01-01") LocalDate effectiveDate,
            @Size(max = 500) @Schema(example = "최초 시행") String description
    ) {
    }

    @Schema(name = "TermsVersionUpdateRequest")
    public record UpdateRequest(
            @NotBlank @Size(max = 30) @Schema(example = "1.1") String versionNo,
            @NotBlank @Schema(example = "제1조(목적) 이 약관은...") String content,
            @NotNull @Schema(example = "2026-03-01") LocalDate effectiveDate,
            @Size(max = 500) @Schema(example = "오탈자 수정") String description,
            @NotBlank @Schema(example = "Y") String useYn
    ) {
    }

    @Schema(name = "TermsVersionResponse")
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
        public static Response from(TermsVersion termsVersion) {
            return new Response(
                    termsVersion.getId(),
                    termsVersion.getTenantId(),
                    termsVersion.getGroupId(),
                    termsVersion.getVersionNo(),
                    termsVersion.getContent(),
                    termsVersion.getEffectiveDate(),
                    termsVersion.getDescription(),
                    termsVersion.getUseYn(),
                    termsVersion.getDeletedYn()
            );
        }
    }
}
