package com.platform.module.pms.exception;

/** 에러 코드 규칙: ERR-PMS-[네자리숫자]. */
public final class PmsErrorCode {

    private PmsErrorCode() {
    }

    public static final String CUSTOMER_NOT_FOUND = "ERR-PMS-0001";
    public static final String CUSTOMER_IN_USE = "ERR-PMS-0002";
    public static final String CONTACT_NOT_FOUND = "ERR-PMS-0003";
    public static final String CONTACT_IN_USE = "ERR-PMS-0004";
    public static final String CONTACT_CUSTOMER_MISMATCH = "ERR-PMS-0005";
    public static final String PROJECT_NOT_FOUND = "ERR-PMS-0006";
    public static final String PROJECT_INVALID = "ERR-PMS-0007";
    public static final String PROJECT_DELETE_NOT_ALLOWED = "ERR-PMS-0008";
    public static final String USER_NOT_FOUND = "ERR-PMS-0009";
    public static final String PROJECT_STATUS_INVALID = "ERR-PMS-0010";
    public static final String CONTACT_INVALID = "ERR-PMS-0011";
    public static final String TRANSITION_NOT_FOUND = "ERR-PMS-0012";
    public static final String TRANSITION_INVALID = "ERR-PMS-0013";
    public static final String TEAM_CHANGE_NOT_FOUND = "ERR-PMS-0014";
    public static final String TEAM_CHANGE_INVALID = "ERR-PMS-0015";
}
