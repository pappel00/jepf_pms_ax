package com.platform.module.user.exception;

/** MAD 4600 에러 코드 규칙: ERR-USER-[네자리숫자]. */
public final class UserModuleErrorCode {

    private UserModuleErrorCode() {
    }

    public static final String COMPANY_NOT_FOUND = "ERR-USER-0001";
    public static final String LOGIN_ID_DUPLICATED = "ERR-USER-0002";
    public static final String INVALID_CREDENTIALS = "ERR-USER-0003";
    public static final String USER_DISABLED = "ERR-USER-0004";
    public static final String USER_NOT_FOUND = "ERR-USER-0005";
    public static final String ROLE_CODE_DUPLICATED = "ERR-USER-0006";
    public static final String ROLE_NOT_FOUND = "ERR-USER-0007";
    public static final String INVALID_ROLE_CODE = "ERR-USER-0008";
    public static final String REFRESH_TOKEN_NOT_FOUND = "ERR-USER-0009";
    public static final String INVALID_TOKEN_TYPE = "ERR-USER-0010";
    public static final String USER_GROUP_NOT_FOUND = "ERR-USER-0011";
    public static final String USER_GROUP_CODE_DUPLICATED = "ERR-USER-0012";
    public static final String USER_GROUP_ROLE_MAPPING_DUPLICATED = "ERR-USER-0013";
    public static final String USER_GROUP_ROLE_MAPPING_NOT_FOUND = "ERR-USER-0014";
    public static final String USER_GROUP_USER_MAPPING_DUPLICATED = "ERR-USER-0015";
    public static final String USER_GROUP_USER_MAPPING_NOT_FOUND = "ERR-USER-0016";
}
