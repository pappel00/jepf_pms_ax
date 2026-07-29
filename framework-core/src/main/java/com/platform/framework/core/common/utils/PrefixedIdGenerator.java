package com.platform.framework.core.common.utils;

import java.util.UUID;

/**
 * MAD 4800: "엔터티[모듈]prefix_UUID" 명명 규칙에 따른 PK 생성 유틸리티.
 *
 * 예) PrefixedIdGenerator.generate("USR") -> "USR_3f2504e04f8911d39a0c0305e82c3301"
 */
public final class PrefixedIdGenerator {

    private PrefixedIdGenerator() {
    }

    public static String generate(String modulePrefix) {
        String uuid = UUID.randomUUID().toString().replace("-", "");
        if (modulePrefix == null || modulePrefix.isBlank()) {
            return uuid;
        }
        return modulePrefix.toUpperCase() + "_" + uuid;
    }
}
