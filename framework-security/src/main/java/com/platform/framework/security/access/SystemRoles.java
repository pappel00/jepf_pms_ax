package com.platform.framework.security.access;

/**
 * 프레임워크 표준 역할 코드. Spring Security의 GrantedAuthority 표현("ROLE_" 접두사 포함)과
 * 동일한 값을 그대로 상수화한다 — User.roles(JWT roles claim)에 저장되는 값과 일치해야 한다.
 *
 * @PreAuthorize("hasRole('ADMIN')") 처럼 애너테이션 안에서는 SpEL 문자열 리터럴이 필요해
 * 이 상수를 직접 참조할 수 없다(컴파일 타임 상수가 아님) — 애너테이션에는 리터럴을 그대로 쓰고,
 * 이 상수는 코드에서 역할을 부여/비교할 때(JwtTokenProvider 호출, User 생성 등) 사용한다.
 */
public final class SystemRoles {

    private SystemRoles() {
    }

    /** 회사(테넌트) 자체를 생성/관리할 수 있는 최상위 시스템 관리자. */
    public static final String SYSTEM_ADMIN = "ROLE_SYSTEM_ADMIN";

    /** 테넌트 내 부서/사용자/역할을 관리할 수 있는 테넌트 관리자. */
    public static final String ADMIN = "ROLE_ADMIN";

    /** 일반 사용자. 회원가입 시 기본으로 부여된다. */
    public static final String USER = "ROLE_USER";
}
