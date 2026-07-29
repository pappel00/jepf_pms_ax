package com.platform.module.tenant.exception;

/** MAD 4600 에러 코드 규칙: ERR-TENANT-[네자리숫자]. */
public final class TenantModuleErrorCode {

    private TenantModuleErrorCode() {
    }

    public static final String COMPANY_NOT_FOUND = "ERR-TENANT-0001";
    public static final String COMPANY_CODE_DUPLICATED = "ERR-TENANT-0002";
    public static final String DEPARTMENT_NOT_FOUND = "ERR-TENANT-0003";
    public static final String DEPARTMENT_CODE_DUPLICATED = "ERR-TENANT-0004";
}
