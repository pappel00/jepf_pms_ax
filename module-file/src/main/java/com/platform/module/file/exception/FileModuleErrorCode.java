package com.platform.module.file.exception;

/** MAD 4600 에러 코드 규칙: ERR-FILE-[네자리숫자]. */
public final class FileModuleErrorCode {

    private FileModuleErrorCode() {
    }

    public static final String FILE_GROUP_NOT_FOUND = "ERR-FILE-0001";
    public static final String FILE_GROUP_CODE_DUPLICATED = "ERR-FILE-0002";
    public static final String FILE_ITEM_NOT_FOUND = "ERR-FILE-0003";
    public static final String EMPTY_FILE = "ERR-FILE-0004";
    public static final String FILE_STORAGE_ERROR = "ERR-FILE-0005";
    public static final String FILE_NOT_FOUND_ON_DISK = "ERR-FILE-0006";
}
