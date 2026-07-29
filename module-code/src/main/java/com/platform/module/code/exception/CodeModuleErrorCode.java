package com.platform.module.code.exception;

/** MAD 4600 에러 코드 규칙: ERR-CODE-[네자리숫자]. */
public final class CodeModuleErrorCode {

    private CodeModuleErrorCode() {
    }

    public static final String CODE_GROUP_NOT_FOUND = "ERR-CODE-0001";
    public static final String CODE_GROUP_CODE_DUPLICATED = "ERR-CODE-0002";
    public static final String CODE_DETAIL_NOT_FOUND = "ERR-CODE-0003";
    public static final String CODE_DETAIL_CODE_DUPLICATED = "ERR-CODE-0004";
}
