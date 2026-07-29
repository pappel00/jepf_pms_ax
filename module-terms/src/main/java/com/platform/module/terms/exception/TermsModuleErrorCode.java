package com.platform.module.terms.exception;

/** MAD 4600 에러 코드 규칙: ERR-TERMS-[네자리숫자]. */
public final class TermsModuleErrorCode {

    private TermsModuleErrorCode() {
    }

    public static final String TERMS_GROUP_NOT_FOUND = "ERR-TERMS-0001";
    public static final String TERMS_GROUP_CODE_DUPLICATED = "ERR-TERMS-0002";
    public static final String TERMS_VERSION_NOT_FOUND = "ERR-TERMS-0003";
}
