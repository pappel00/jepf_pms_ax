package com.platform.module.user.domain;

import com.platform.framework.core.common.utils.PrefixedIdGenerator;
import com.platform.framework.security.tenant.TenantAwareEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * 테넌트(Company) 하위의 사용자 엔티티.
 * TenantAwareEntity 를 상속하므로 Department와 마찬가지로 Hibernate @TenantId 에 의해
 * 자동으로 현재 테넌트로 필터링/스탬핑된다.
 *
 * loginId 는 테넌트 내에서만 유일하면 되므로 (tenant_id, login_id) 복합 유니크 제약을 건다
 * (전역 유니크가 아님 — 회사가 다르면 같은 loginId 를 쓸 수 있다).
 *
 * ⚠️ 회사(Company) <-> 부서(Department) <-> 사용자(User) 연결고리:
 * 회사는 이미 모든 TenantAwareEntity가 자동으로 갖는 tenant_id 컬럼으로 연결돼 있다
 * (User가 속한 회사 = tenant_id가 가리키는 Company) — 별도 필드가 필요 없다. 다만
 * 부서(Department)와의 연결은 그렇지 않았다 — User는 원래 departmentId를 전혀 갖고
 * 있지 않아서 "회사 > 부서 > 사용자" 연결고리 중 마지막 단계가 실제로 빠져 있었다
 * (실제로 겪은 문제 — 나중에 발견해서 추가함). departmentId는 선택 필드다 — 회원가입으로
 * 막 생성된 사용자는 아직 소속 부서가 정해지지 않았을 수 있어서, 나중에 배정하는 걸 허용한다.
 *
 * ⚠️ User는 더 이상 역할(roles)을 직접 가지지 않는다. 실제 권한은 Role -> UserGroup -> User
 * 3단계로 흐른다 (UserGroupRole, UserGroupUser 참고) — 사진 속 BackOffice의 "사용자그룹 관리"/
 * "Role 관리" 화면 구조와 정확히 일치시키기 위해, 예전에 있던 User 직접 Role 부여 기능
 * (RoleUserController, PUT /users/{id}/roles)은 완전히 제거했다. 로그인 시점의 최종 roles claim은
 * EffectiveRoleService 가 사용자가 속한 모든 UserGroup의 Role을 합쳐서 계산한다.
 */
@Entity
@Table(name = "usr_user",
        uniqueConstraints = @UniqueConstraint(name = "uk_user_tenant_login", columnNames = {"tenant_id", "login_id"}))
public class User extends TenantAwareEntity {

    @Column(name = "login_id", length = 100, nullable = false)
    private String loginId;

    @Column(name = "password_hash", length = 200, nullable = false)
    private String passwordHash;

    @Column(name = "email", length = 200)
    private String email;

    @Column(name = "user_name", length = 100, nullable = false)
    private String userName;

    /** 소속 부서. 다른 참조 컬럼들과 동일하게 진짜 FK가 아니라 느슨한 문자열 참조다. */
    @Column(name = "department_id", length = 64)
    private String departmentId;

    @Column(name = "use_yn", length = 1, nullable = false)
    private String useYn = "Y";

    protected User() {
    }

    private User(String loginId, String passwordHash, String email, String userName, String departmentId) {
        super(PrefixedIdGenerator.generate("USR"));
        this.loginId = loginId;
        this.passwordHash = passwordHash;
        this.email = email;
        this.userName = userName;
        this.departmentId = departmentId;
    }

    public static User create(String loginId, String passwordHash, String email, String userName) {
        return new User(loginId, passwordHash, email, userName, null);
    }

    public static User create(String loginId, String passwordHash, String email, String userName, String departmentId) {
        return new User(loginId, passwordHash, email, userName, departmentId);
    }

    public void updateProfile(String email, String userName) {
        this.email = email;
        this.userName = userName;
    }

    public void assignDepartment(String departmentId) {
        this.departmentId = departmentId;
    }

    public void changePassword(String newPasswordHash) {
        this.passwordHash = newPasswordHash;
    }

    public void activate() {
        this.useYn = "Y";
    }

    public void deactivate() {
        this.useYn = "N";
    }

    public String getLoginId() {
        return loginId;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getEmail() {
        return email;
    }

    public String getUserName() {
        return userName;
    }

    public String getDepartmentId() {
        return departmentId;
    }

    public String getUseYn() {
        return useYn;
    }
}
