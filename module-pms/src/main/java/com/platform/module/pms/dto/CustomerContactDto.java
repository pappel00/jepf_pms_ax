package com.platform.module.pms.dto;

import com.platform.module.pms.domain.CustomerContact;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class CustomerContactDto {

    private CustomerContactDto() {
    }

    /** 등록/수정 공통 요청 (담당자 코드는 서버가 채번한다). 연락처/이메일은 둘 중 하나가 필수(서비스에서 검증). */
    @Schema(name = "PmsCustomerContactRequest")
    public record Request(
            @NotBlank String customerId,
            @NotBlank @Size(max = 100) String contactName,
            @Size(max = 100) String jobTitle,
            @Size(max = 100) String deptName,
            @Size(max = 50) String phone,
            @Size(max = 200) String email,
            @Size(max = 1000) String memo,
            @Schema(example = "Y") String useYn
    ) {
    }

    @Schema(name = "PmsCustomerContactResponse")
    public record Response(
            String id,
            String contactCode,
            String customerId,
            String contactName,
            String jobTitle,
            String deptName,
            String phone,
            String email,
            String memo,
            String useYn
    ) {
        public static Response from(CustomerContact c) {
            return new Response(c.getId(), c.getContactCode(), c.getCustomerId(), c.getContactName(), c.getJobTitle(),
                    c.getDeptName(), c.getPhone(), c.getEmail(), c.getMemo(), c.getUseYn());
        }
    }
}
