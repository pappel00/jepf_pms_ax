package com.platform.module.pms.service;

import java.util.Collection;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/** PMS 서비스들이 공통으로 쓰는 작은 유틸. */
final class PmsSupport {

    private PmsSupport() {
    }

    /** 현재 로그인 사용자의 ID(JWT subject = usr_user.id). 인증 정보가 없으면 "SYSTEM". */
    static String currentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return "SYSTEM";
        }
        return auth.getName();
    }

    /**
     * 화면용 코드 채번 — 기존 코드들(`PRJ-2026-003`, `CUS-012` 등) 중 prefix 로 시작하는 것의 마지막 숫자 중 최댓값 + 1.
     * 자릿수는 width 로 맞춘다(예: width=3 → `CUS-013`).
     */
    static String nextCode(String prefix, Collection<String> existingCodes, int width) {
        int max = 0;
        for (String code : existingCodes) {
            if (code == null || !code.startsWith(prefix)) {
                continue;
            }
            String tail = code.substring(code.lastIndexOf('-') + 1);
            try {
                max = Math.max(max, Integer.parseInt(tail));
            } catch (NumberFormatException ignored) {
                // 형식이 다른 코드는 채번에서 제외
            }
        }
        return prefix + String.format("%0" + width + "d", max + 1);
    }

    static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
