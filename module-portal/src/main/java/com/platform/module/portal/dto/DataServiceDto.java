package com.platform.module.portal.dto;

import com.platform.module.portal.domain.DataService;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class DataServiceDto {

    private DataServiceDto() {
    }

    @Schema(name = "DataServiceCreateRequest")
    public record CreateRequest(
            @NotBlank @Size(max = 30) @Schema(example = "DTS_USR_LIST") String dataServiceCode,
            @NotBlank @Size(max = 200) @Schema(example = "사용자 목록 조회") String dataServiceName,
            @NotBlank @Size(max = 10) @Schema(example = "GET") String httpMethod,
            @NotBlank @Size(max = 500) @Schema(example = "/api/v1/users") String url
    ) {
    }

    @Schema(name = "DataServiceUpdateRequest")
    public record UpdateRequest(
            @NotBlank @Size(max = 200) @Schema(example = "사용자 목록 조회") String dataServiceName,
            @NotBlank @Size(max = 10) @Schema(example = "GET") String httpMethod,
            @NotBlank @Size(max = 500) @Schema(example = "/api/v1/users") String url,
            @NotBlank @Schema(example = "Y") String useYn
    ) {
    }

    @Schema(name = "DataServiceResponse")
    public record Response(
            String id,
            String tenantId,
            String dataServiceCode,
            String dataServiceName,
            String httpMethod,
            String url,
            String useYn,
            String deletedYn
    ) {
        public static Response from(DataService dataService) {
            return new Response(
                    dataService.getId(),
                    dataService.getTenantId(),
                    dataService.getDataServiceCode(),
                    dataService.getDataServiceName(),
                    dataService.getHttpMethod(),
                    dataService.getUrl(),
                    dataService.getUseYn(),
                    dataService.getDeletedYn()
            );
        }
    }
}
