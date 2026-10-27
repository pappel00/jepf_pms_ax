package com.platform.module.pms.dto;

import com.platform.module.pms.domain.Customer;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class CustomerDto {

    private CustomerDto() {
    }

    /** 등록/수정 공통 요청 (고객사 코드는 서버가 채번한다). */
    @Schema(name = "PmsCustomerRequest")
    public record Request(
            @NotBlank @Size(max = 200) String customerName,
            @Size(max = 30) String industryCode,
            @NotBlank @Size(max = 30) @Schema(example = "거래 중") String dealStatusCode,
            @Size(max = 20) String bizNo,
            @Size(max = 50) String phone,
            @Size(max = 300) String address,
            @Size(max = 300) String homepage,
            @Size(max = 1000) String memo
    ) {
    }

    @Schema(name = "PmsCustomerResponse")
    public record Response(
            String id,
            String customerCode,
            String customerName,
            String industryCode,
            String dealStatusCode,
            String bizNo,
            String phone,
            String address,
            String homepage,
            String memo
    ) {
        public static Response from(Customer c) {
            return new Response(c.getId(), c.getCustomerCode(), c.getCustomerName(), c.getIndustryCode(),
                    c.getDealStatusCode(), c.getBizNo(), c.getPhone(), c.getAddress(), c.getHomepage(), c.getMemo());
        }
    }
}
