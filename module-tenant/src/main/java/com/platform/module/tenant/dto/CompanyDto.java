package com.platform.module.tenant.dto;

import com.platform.module.tenant.domain.Company;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class CompanyDto {

    private CompanyDto() {
    }

    @Schema(name = "CompanyCreateRequest")
    public record CreateRequest(
            @NotBlank @Size(max = 30) @Schema(example = "ACME") String companyCode,
            @NotBlank @Size(max = 200) @Schema(example = "에이씨엠이") String companyName,
            @Size(max = 500) @Schema(example = "샘플 회사입니다.") String description
    ) {
    }

    @Schema(name = "CompanyUpdateRequest")
    public record UpdateRequest(
            @NotBlank @Size(max = 200) @Schema(example = "에이씨엠이") String companyName,
            @Size(max = 500) @Schema(example = "샘플 회사입니다.") String description,
            @NotBlank @Schema(example = "Y") String useYn
    ) {
    }

    @Schema(name = "CompanyResponse")
    public record Response(
            String id,
            String companyCode,
            String companyName,
            String description,
            String useYn,
            String deletedYn
    ) {
        public static Response from(Company company) {
            return new Response(
                    company.getId(),
                    company.getCompanyCode(),
                    company.getCompanyName(),
                    company.getDescription(),
                    company.getUseYn(),
                    company.getDeletedYn()
            );
        }
    }
}
