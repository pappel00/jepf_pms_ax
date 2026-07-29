package com.platform.module.portal.exception;

/** MAD 4600 에러 코드 규칙: ERR-PORTAL-[네자리숫자]. */
public final class PortalModuleErrorCode {

    private PortalModuleErrorCode() {
    }

    public static final String PORTAL_NOT_FOUND = "ERR-PORTAL-0001";
    public static final String PORTAL_CODE_DUPLICATED = "ERR-PORTAL-0002";
    public static final String APPLICATION_NOT_FOUND = "ERR-PORTAL-0003";
    public static final String APPLICATION_CODE_DUPLICATED = "ERR-PORTAL-0004";
    public static final String PAGE_NOT_FOUND = "ERR-PORTAL-0005";
    public static final String PAGE_CODE_DUPLICATED = "ERR-PORTAL-0006";
    public static final String MENU_NOT_FOUND = "ERR-PORTAL-0007";
    public static final String MENU_CODE_DUPLICATED = "ERR-PORTAL-0008";
    public static final String DATA_SERVICE_NOT_FOUND = "ERR-PORTAL-0009";
    public static final String DATA_SERVICE_CODE_DUPLICATED = "ERR-PORTAL-0010";
    public static final String MESSAGE_RESOURCE_NOT_FOUND = "ERR-PORTAL-0011";
    public static final String MESSAGE_RESOURCE_DUPLICATED = "ERR-PORTAL-0012";
    public static final String PAGE_DATA_SERVICE_MAPPING_DUPLICATED = "ERR-PORTAL-0013";
    public static final String PAGE_DATA_SERVICE_MAPPING_NOT_FOUND = "ERR-PORTAL-0014";
    public static final String PAGE_MESSAGE_RESOURCE_MAPPING_DUPLICATED = "ERR-PORTAL-0015";
    public static final String PAGE_MESSAGE_RESOURCE_MAPPING_NOT_FOUND = "ERR-PORTAL-0016";
    public static final String PAGE_ROLE_MAPPING_DUPLICATED = "ERR-PORTAL-0017";
    public static final String PAGE_ROLE_MAPPING_NOT_FOUND = "ERR-PORTAL-0018";
    public static final String MENU_PAGE_MAPPING_DUPLICATED = "ERR-PORTAL-0019";
    public static final String MENU_PAGE_MAPPING_NOT_FOUND = "ERR-PORTAL-0020";
}
