package com.platform.module.privacy.exception;

/** MAD 4600 에러 코드 규칙: ERR-PRIVACY-[네자리숫자]. */
public final class PrivacyModuleErrorCode {

    private PrivacyModuleErrorCode() {
    }

    public static final String PRIVACY_GROUP_NOT_FOUND = "ERR-PRIVACY-0001";
    public static final String PRIVACY_GROUP_CODE_DUPLICATED = "ERR-PRIVACY-0002";
    public static final String PRIVACY_VERSION_NOT_FOUND = "ERR-PRIVACY-0003";
}
