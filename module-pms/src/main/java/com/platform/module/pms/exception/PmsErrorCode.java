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
    public static final String TASK_NOT_FOUND = "ERR-PMS-0016";
    public static final String TASK_INVALID = "ERR-PMS-0017";
    public static final String ISSUE_NOT_FOUND = "ERR-PMS-0018";
    public static final String ISSUE_INVALID = "ERR-PMS-0019";
    public static final String PROPOSAL_NOT_FOUND = "ERR-PMS-0020";
    public static final String PROPOSAL_INVALID = "ERR-PMS-0021";
    public static final String RFP_NOT_FOUND = "ERR-PMS-0022";
    public static final String RFP_INVALID = "ERR-PMS-0023";
    public static final String PROPOSAL_DOC_NOT_FOUND = "ERR-PMS-0024";
    public static final String PROPOSAL_DOC_INVALID = "ERR-PMS-0025";
    public static final String REPORT_INVALID = "ERR-PMS-0026";
    public static final String DOC_NOT_FOUND = "ERR-PMS-0027";
    public static final String DOC_INVALID = "ERR-PMS-0028";
    public static final String RATE_NOT_FOUND = "ERR-PMS-0029";
    public static final String RATE_INVALID = "ERR-PMS-0030";
    public static final String ACTUAL_FORBIDDEN = "ERR-PMS-0031";
    public static final String SETTING_INVALID = "ERR-PMS-0032";
    public static final String NOTE_NOT_FOUND = "ERR-PMS-0033";
    public static final String OPS_NOT_FOUND = "ERR-PMS-0034";
    public static final String OPS_INVALID = "ERR-PMS-0035";
}
