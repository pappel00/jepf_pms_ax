package com.platform.module.form.exception;

/** MAD 4600 에러 코드 규칙: ERR-FORM-[네자리숫자]. */
public final class FormModuleErrorCode {

    private FormModuleErrorCode() {
    }

    public static final String FORM_GROUP_NOT_FOUND = "ERR-FORM-0001";
    public static final String FORM_GROUP_CODE_DUPLICATED = "ERR-FORM-0002";
    public static final String FORM_VERSION_NOT_FOUND = "ERR-FORM-0003";
    public static final String FORM_FILE_NOT_FOUND_ON_DISK = "ERR-FORM-0004";
}
