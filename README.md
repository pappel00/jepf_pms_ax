# Java Enterprise Platform Framework

Master Architecture Document (MAD) v0.3 기준 구현.

> ⚠️ 이 프로젝트는 네트워크가 제한된 샌드박스 환경에서 작성되어 **Maven Central 접속 후
> `gradle build`/`gradle test` 실행 검증을 하지 못했습니다.** 로컬(또는 사내 Nexus)에서
> 반드시 빌드 및 테스트를 한 번 돌려보시고, 버전 충돌이나 API 변경 사항이 있으면 알려주세요.
> (`gradle.properties`의 `springBootVersion`을 실제 사용 가능한 최신 안정 버전으로 조정하세요.)

## 모듈 구성

```
java-enterprise-platform-framework
├── framework-core       // 공통 유틸, Response/Exception 표준, BaseEntity, TraceId
├── framework-security   // JWT 인증, 멀티테넌트 컨텍스트/Hibernate 연동, 최소 SecurityConfig, CORS
├── module-tenant        // Company(테넌트 마스터)/Department 관리 — 공유 스키마 멀티테넌시 첫 검증
├── module-user           // 로그인/회원가입, User/Role/UserGroup 관리, Role 기반 인가
├── module-portal          // Portal/Application/Page/Menu/DataService 관리 (BackOffice 화면 대응)
├── module-code             // CodeGroup(코드기본)/CodeDetail(코드상세) 관리 (Data Management 화면 대응)
├── module-file              // FileGroup(첨부기본)/FileItem(첨부항목) 관리 + 실제 업로드/다운로드 (Data Management 화면 대응)
├── module-terms             // TermsGroup(약관기본)/TermsVersion(버전 이력) 관리 (Data Management 화면 대응)
├── module-notice            // Notice(공지/동의서) 단일 목록 관리 (Data Management 화면 대응)
├── module-privacy           // PrivacyPolicyGroup/PrivacyPolicyVersion 관리 (Data Management 화면 대응, module-terms와 동일 구조)
├── module-form              // FormGroup/FormVersion 관리 (Data Management 화면 대응, module-terms와 동일 구조 + 분류(category))
├── app-sandbox           // 로컬 수동 테스트용 실행 가능 애플리케이션 (PostgreSQL, 샘플 데이터 자동 시딩)
├── db/postgresql          // PostgreSQL DDL/DML 스크립트 (참고/수동 실행용)
└── frontend               // React(Vite) — 전체 관리 화면 (별도 npm 프로젝트, Gradle 빌드 아님)
```

MAD 4300 Module Structure의 나머지 모듈은 다음 단계 이후에 이 위에 얹어서 개발합니다.

## framework-core 에 구현된 것 (MAD 대응)

| MAD 항목 | 구현 내용 |
|---|---|
| 4800 Database 설계 규칙 | `common.entity.BaseEntity` — UUID PK(애플리케이션 생성), Audit, Soft Delete, `@Version` |
| 4800 PK 접두사 규칙 | `common.utils.PrefixedIdGenerator` — `USR_xxxxxxxx` 형태 지원 |
| 4500 Response 규격 | `common.response.ApiResponse` / `PageInfo` / `PagedResult` / `ErrorDetail` / `TraceIdHolder` |
| 4600 Exception 규격 | `common.exception.BaseException`과 5개 하위 예외 + `GlobalExceptionHandler` |
| 4600 에러코드 규칙 | `common.exception.CommonErrorCode` (`ERR-CORE-XXXX`) — 모듈별 코드는 각 모듈에서 확장 |
| traceId 전파 | `common.web.TraceIdFilter` (MDC 기반) |
| Audit 사용자 정보 | `common.config.JpaAuditingConfig` (기본 "SYSTEM", security 모듈 추가 시 자동 교체) |
| API 문서 자동화 | `common.config.OpenApiConfig` (springdoc-openapi) — `/swagger-ui/index.html`, `/v3/api-docs` 자동 노출, JWT Bearer Authorize 버튼 포함 |

## framework-security 에 구현된 것 (MAD 대응)

| MAD 항목 | 구현 내용 |
|---|---|
| 4200 core/security | `jwt.JwtTokenProvider` — 발급/검증/파싱 (jjwt 0.12.x) |
| 4200 core/tenant | `tenant.TenantContext` — ThreadLocal 기반 현재 테넌트 컨텍스트 |
| 4200 core/tenant (멀티테넌시 데이터 격리) | `tenant.TenantAwareEntity` + `tenant.TenantIdentifierResolver` + `config.TenantHibernateConfig` — **공유 스키마 방식**, Hibernate 6+ `@TenantId` 파티션 전략 |
| open-in-view 기본값 강제 | `config.OpenInViewDefaultEnvironmentPostProcessor` — `spring.jpa.open-in-view=false`를 기본값으로 강제 (아래 "⚠️ open-in-view는 반드시 false" 참고) |
| 인증 필터 | `filter.JwtAuthenticationFilter` — Authorization: Bearer 검사, SecurityContext/TenantContext 설정 |
| SecurityConfig | `config.SecurityConfig` — Stateless, `/api/v1/auth/**` permitAll, 나머지 인증 필요, `@EnableMethodSecurity`로 `@PreAuthorize` 활성화 |
| CORS | `config.CorsProperties`(`framework.security.cors.allowed-origins`) — 기본값 `http://localhost:5173`(Vite)/`:3000`(CRA). 프론트엔드 개발 시작 시 별도 설정 없이 바로 통하도록 함 |
| Role 기반 인가 | `access.SystemRoles` — `SYSTEM_ADMIN`/`ADMIN`/`USER` 표준 역할 상수. 각 모듈 컨트롤러에서 `@PreAuthorize("hasRole('ADMIN')")` 형태로 사용 |
| 표준 에러 응답 | `web.RestAuthenticationEntryPoint`(401) / `web.RestAccessDeniedHandler`(403) — framework-core의 `ApiResponse` 포맷 사용. `@PreAuthorize` 거부도 별도 배선 없이 이 핸들러가 그대로 처리 |

### Role 기반 인가 정책 (현재 적용된 규칙)

| API | 필요 권한 |
|---|---|
| `POST/PUT/DELETE /api/v1/companies` | `SYSTEM_ADMIN` |
| `GET /api/v1/companies` | 로그인만 되어 있으면 가능 |
| `POST/PUT/DELETE /api/v1/departments` | `ADMIN` |
| `GET /api/v1/departments` | 로그인만 되어 있으면 가능 |
| `/api/v1/users/**` (전체) | `ADMIN` |
| `/api/v1/roles/**` (전체) | `ADMIN` |
| `/api/v1/auth/**` | 인증 불필요 |

회원가입(`/api/v1/auth/register`)으로 만들어진 사용자는 `defaultYn=Y`인 `UserGroup`이 있으면
자동 배정되고(`app-sandbox`에서는 `DEFAULT_USER` 그룹, `ROLE_USER`만 매핑됨), 없으면 아무
그룹에도 속하지 않은 채로 가입되어 로그인은 되지만 `ADMIN` 권한이 필요한 API는 전부 403이
난다. `ADMIN`/`SYSTEM_ADMIN` 부여는 `UserGroupUserController`(사용자를 그룹에 넣기)와
`UserGroupRoleController`(그룹에 Role 매핑하기) 조합으로 한다 — User에 직접 부여하는 API는
없다. `app-sandbox`의 `DevDataSeeder`가 만드는 샘플 `admin` 계정은 `ADMIN_GROUP`
(`ROLE_USER`/`ROLE_ADMIN`/`ROLE_SYSTEM_ADMIN` 매핑)에 속해 있어 바로 테스트 가능하다.

> ⚠️ **`GET /api/v1/roles`가 비어있던 문제 (실제로 겪음)**: `SystemRoles`는 `UserGroupRoleService`의
> 검증에서 `Role` 마스터 조회 없이 항상 허용되도록 예외 처리해뒀는데(위 "역할 코드 검증" 참고),
> `DevDataSeeder`가 `UserGroupRole` 매핑만 만들고 `Role` 마스터에는 아무 행도 안 만들어서,
> admin이 실제로 `ROLE_ADMIN` 등을 갖고 있는데도 Role 관리 화면(`GET /api/v1/roles`)엔 아무 것도
> 안 보이는 상태였다. `DevDataSeeder`가 이제 이 세 `SystemRole`에 대해서도 `Role` 마스터 행을
> 같이 만들어서, 목록에서도 실제로 보이게 했다 (검증 로직의 예외 처리 자체는 안전장치로 그대로 둠).

### ⚠️ `@PreAuthorize` 거부가 403 대신 500으로 나가던 버그 (실제로 겪음)

`@PreAuthorize`로 막힌 요청(`AuthorizationDeniedException`, `AccessDeniedException`의 하위 타입)이
`framework-core`의 `GlobalExceptionHandler`에 있는 범용 `@ExceptionHandler(Exception.class)`에
먼저 잡혀서 403 대신 500(`UnhandledException`)으로 응답되는 버그가 실제로 발생했다.

원인: `@PreAuthorize` 거부는 Spring MVC 디스패치 안에서 발생하는 예외라 서블릿 필터 체인의
`AccessDeniedHandler`보다 `@RestControllerAdvice`(`@ExceptionHandler`)가 먼저 가로챈다. 그런데
`framework-core`는 계층상 `spring-security`를 모르기 때문에(`framework-security`가 `framework-core`에
의존하는 방향이지 그 반대가 아님) `AccessDeniedException` 타입 자체를 처리할 수 없었다.

해결: `framework-security`에 `SecurityExceptionHandler`(`@RestControllerAdvice`,
`@Order(Ordered.HIGHEST_PRECEDENCE)`)를 추가해 `AccessDeniedException`/`AuthenticationException`을
먼저 잡도록 했고, `GlobalExceptionHandler`에는 `@Order(Ordered.LOWEST_PRECEDENCE)`를 명시해 항상
맨 마지막 안전망 역할만 하도록 순서를 고정했다. Spring은 여러 `@ControllerAdvice` 중 "먼저 매치되는"
것을 쓰지, 전체를 통틀어 가장 구체적인 핸들러를 찾아주지 않기 때문에 순서 보장이 꼭 필요하다.

### 멀티테넌시 동작 원리 (공유 스키마)

1. 로그인 시 발급되는 JWT에 `tenantId` claim이 담긴다 (Company의 PK).
2. `JwtAuthenticationFilter`가 요청마다 이 값을 `TenantContext`(ThreadLocal)에 설정한다.
3. `TenantIdentifierResolver`가 Hibernate에게 "현재 테넌트가 뭐냐"는 질문을 받으면 `TenantContext` 값을 그대로 반환한다.
4. `TenantAwareEntity`를 상속한 엔티티(예: `Department`)는 `@TenantId` 컬럼을 가지며,
   Hibernate가 INSERT 시 자동으로 채우고 SELECT/UPDATE/DELETE 시 자동으로 `tenant_id = 현재테넌트` 조건을 추가한다.
   **개발자가 리포지토리/서비스 코드에서 tenantId를 직접 다룰 필요가 없다.**
5. `Company`(테넌트 자신을 정의하는 마스터)는 `TenantAwareEntity`를 상속하지 않는다 — 테넌트에 "속하는" 데이터가 아니기 때문.

### ⚠️ `spring.jpa.open-in-view`는 반드시 `false` (실제로 겪은 버그)

`open-in-view`(Spring Boot 기본값 `true`)가 켜져 있으면 **HTTP 요청 하나당 Hibernate Session 하나가
요청이 끝날 때까지 재사용**된다. `@TenantId`는 "Session이 열리는 시점"에 현재 테넌트를 확정하기 때문에,
로그인처럼 요청 초반엔 `TenantContext`가 비어있다가 중간에 채워지는 흐름에서는 **첫 DB 접근 시점(테넌트
미확정)에 캐싱된 값이 이후 `TenantContext`를 바꿔도 반영되지 않는다.**

실제로 `module-user`의 로그인 API에서 이 버그가 발생했다 — Company 조회(테넌트 무관) 시점에 Session이
"SYSTEM"(기본값)으로 캐싱되어버려서, 그 다음 `TenantContext.setTenantId()`를 호출해도 이미 늦은 상태였고,
User 조회 SQL에 실제로는 `tenant_id = 'SYSTEM'`이 바인딩되어 항상 로그인 실패로 이어졌다.

`framework-security`의 `OpenInViewDefaultEnvironmentPostProcessor`가 `spring.jpa.open-in-view=false`를
기본값으로 강제하므로 별도 설정 없이도 안전하지만, 혹시 어딘가에서 `open-in-view: true`로 명시적으로
덮어쓰면 이 버그가 재발할 수 있다는 점을 기억해두면 좋다.

### 아직 구현하지 않은 것 (의도적으로 최소 범위로 유지)

- Role/권한 세부 매핑, `@PreAuthorize` 기반 인가 — module-user/module-portal 연동 시 확장
- Refresh Token 재발급 플로우 — module-user와 함께 추후 구현
- 스키마별/DB별 완전 물리 격리 — 지금은 공유 스키마(shared schema)만 구현 (요구사항 확정에 따른 선택)

## module-tenant 에 구현된 것

| 구성 요소 | 설명 |
|---|---|
| `domain.Company` | 테넌트(회사) 마스터. `TenantAwareEntity` 미상속 (테넌트 자신을 정의) |
| `domain.Department` | 테넌트 종속 데이터 예시. `TenantAwareEntity` 상속 → 자동 격리 검증용 |
| `repository`, `service`, `controller`, `dto` | Company/Department 각각 CRUD (`/api/v1/companies`, `/api/v1/departments`) |
| `exception.TenantModuleErrorCode` | `ERR-TENANT-0001` ~ `0004` |
| `config.ModuleTenantAutoConfiguration` | `@EntityScan`/`@EnableJpaRepositories`/`@ComponentScan`으로 이 모듈만 추가하면 자동 활성화 |

> ⚠️ **로컬 검증 꼭 필요합니다**: `@TenantId`(Hibernate 6.2+ 파티션 멀티테넌시)가 사용하시는
> 정확한 Hibernate/Spring Boot 버전에서 기대대로 동작하는지, 실제 DB(H2 등)에 붙여서
> "테넌트 A로 로그인 → Department 생성 → 테넌트 B로 로그인 → 조회 시 안 보이는지"를
> 한 번은 직접 테스트해보시길 권장합니다. 단위 테스트(`CompanyServiceTest`/`DepartmentServiceTest`)는
> Repository를 Mock으로 대체한 서비스 로직 검증까지만 커버하며, 실제 Hibernate 필터링 동작 자체는
> 검증하지 못합니다 (이 샌드박스에 DB 의존성을 추가할 네트워크 접근이 없었습니다).

## module-user 에 구현된 것

**⚠️ 구조 변경 (실제로 겪음)**: 처음엔 `User`가 역할을 직접 가지는 구조(`User.roles`,
`PUT /users/{id}/roles`, `RoleUserController`)로 만들었는데, 사진 속 BackOffice 화면을 다시
보니 User Management가 **사용자그룹 관리 / 사용자 관리 / Role 관리** 3개로 구성되어 있고
실제 권한 부여 흐름은 `Role ↔ UserGroup ↔ User` 3단계였다. User 직접 Role 부여는 **완전히
제거**하고 `UserGroup`을 중간에 끼워넣는 구조로 다시 만들었다.

```
Role  ←→(UserGroupRole)→  UserGroup  ←→(UserGroupUser)→  User
```

> ⚠️ **`UserGroup`의 분류(`category`) 선택지도 하드코딩하지 않고 공통코드(`module-code`)로
> 관리한다** — `USER_GROUP_CATEGORY`(컨텐츠관리/시스템관리/데이터입력관리) 코드그룹이다.
> `module-portal`의 `PAGE_TYPE`/`PAGE_DEVICE_TYPE`, `module-notice`의 팝업 상세 설정 5종과
> 완전히 같은 패턴으로, `module-user`가 `module-code`에 의존하고
> `UserGroupCodeBootstrapListener`(`TenantCreatedEvent` 리스닝, `AFTER_COMMIT`)가 새 테넌트에
> 이 코드그룹/코드상세를 자동으로 채워둔다(find-or-create, 멱등, 시딩 로직은 `PageCodeBootstrapListener`와
> 동일하게 `public seedDefaults()`로 열어둬서 `DevDataSeeder`가 값을 복사하지 않고 재사용한다).
> `TenantBootstrapListener`/`DevDataSeeder`가 만드는 `DEFAULT_USER`/`ADMIN_GROUP`의 `category`도
> 임의 문자열("시스템") 대신 실제 공통코드 값("시스템관리")으로 맞췄다.

| 구성 요소 | 설명 |
|---|---|
| `domain.User` | `TenantAwareEntity` 상속. `(tenant_id, login_id)` 복합 유니크. **더 이상 역할을 직접 갖지 않는다**. `departmentId`(선택)로 소속 부서와 연결된다 |
| `domain.Role` | `TenantAwareEntity` 상속. `(tenant_id, role_code)` 복합 유니크 — 테넌트별 역할 마스터 (사진 속 "Role 관리" 화면 대응) |
| `domain.UserGroup` | `TenantAwareEntity` 상속. `groupCode`/`groupName`/`category`/`portalId`/`defaultYn` (사진 속 "사용자그룹 관리" 화면 대응) |
| `domain.UserGroupRole` / `domain.UserGroupUser` | N:M 매핑 (하드 삭제로 관리, Page 하위 매핑들과 동일 패턴) |
| `service.EffectiveRoleService` | 사용자가 속한 **모든 UserGroup의 Role을 합쳐서** 최종 권한(effective roles)을 계산 — 로그인/refresh 시 JWT `roles` claim, `UserService` 응답의 `roles` 필드가 이 값을 쓴다 |
| `event.TenantBootstrapListener` | **새 회사(테넌트)가 생기면 자동으로 기초 데이터를 채운다** (아래 "테넌트 기초 데이터 자동 부트스트랩" 참고) |
| `service.AuthService` | 회원가입/로그인/refresh/logout. **의도적으로 `@Transactional` 미사용** (아래 설명). 가입 시 `defaultYn=Y`인 UserGroup이 있으면 자동 배정 |
| `controller.AuthController` | `/api/v1/auth/register`, `/login`, `/refresh`, `/logout` — `SecurityConfig`의 기존 permitAll 패턴에 자동으로 걸림 |
| `controller.UserController` | `/api/v1/users/**` — `ADMIN` 권한 필요, 요청자의 테넌트로 자동 범위 제한. `POST`로 관리자가 직접 사용자 생성 가능(회원가입과 별개 경로, 둘 다 defaultYn=Y 그룹 자동 배정). 역할 부여 API는 없다(아래 참고) |
| `controller.RoleController` | `/api/v1/roles/**` — `ADMIN` 권한 필요 |
| `controller.UserGroupController` | `/api/v1/user-groups/**` — `ADMIN` 권한 필요, 사용자그룹 CRUD |
| `controller.UserGroupRoleController` | `/api/v1/user-groups/{userGroupId}/roles/**` — 이 그룹에 Role 부여/제거 (사진 속 "사용자그룹"/"Role" 관리 화면의 "권한"/"사용자그룹" 탭 대응) |
| `controller.UserGroupUserController` | `/api/v1/user-groups/{userGroupId}/users/**` — 이 그룹에 사용자 소속/제거 (사진 속 "사용자그룹 관리"의 "사용자" 탭 대응) |
| `controller.RoleUserGroupController` | `/api/v1/roles/{roleId}/user-groups/**` — **Role 관점**의 역방향 조회/추가/제거. `UserGroupRoleController`와 저장소를 공유하는 반대 방향 API (같은 `UserGroupRole` 테이블) |
| `exception.UserModuleErrorCode` | `ERR-USER-0001` ~ `0016` |

> 역할 부여는 이제 `UserGroupUserController`(사용자를 그룹에 넣기)와 `UserGroupRoleController`
> (그룹에 Role 매핑하기) 조합으로만 이뤄진다. "이 사용자에게 이 역할을 직접 줘"라는 API는
> 더 이상 없다 — 반드시 그룹을 거쳐야 한다(사진 속 화면 구조와 정확히 일치시킨 결과).

### Role 기준 역방향 매핑 API (`module-user`/`module-portal` 둘 다)

"이 Role이 어떤 사용자그룹/페이지에 매핑돼 있는지"를 Role 쪽에서 조회/추가/삭제할 수 있다.
`UserGroupRoleController`/`PageRoleMappingController`(소유하는 쪽에서 관리)와 저장소를
공유하는 반대 방향 API일 뿐, 별도의 매핑 테이블이 새로 생기는 게 아니다.

| 대상 | API | 비고 |
|---|---|---|
| Role → UserGroup | `/api/v1/roles/{roleId}/user-groups/**` | `roleId`는 Role의 PK. 같은 모듈(`module-user`)이라 PK를 그대로 쓸 수 있다 |
| Role → Page | `/api/v1/pages/by-role/{roleCode}/**` | ⚠️ `roleId`(PK)가 아니라 **`roleCode` 문자열**을 받는다. `module-portal`은 `module-user`(Role)에 의존하지 않는 구조라 PK를 조회할 방법이 없기 때문 — 그래서 경로도 `/api/v1/roles/...` 밑이 아니라 `module-portal`이 소유한 `/api/v1/pages/...` 아래에 있다 |

### 테넌트 기초 데이터 자동 부트스트랩 (실제로 겪은 프로덕션 갭)

`DevDataSeeder`는 `app-sandbox` 로컬 샌드박스에서 `DEMO`라는 특정 테넌트 하나만 부트스트랩한다.
그런데 실제로 `POST /api/v1/companies`로 **새 회사(테넌트)를 만들면, 그 테넌트는 Role 마스터도
UserGroup도 하나 없는 텅 빈 상태**로 시작해서 — 그 테넌트에서 회원가입해도 아무 권한도 못 받고,
관리자를 만들 방법조차 없었다.

`module-tenant`(Company)가 `module-user`(Role/UserGroup)를 모르는 구조라 직접 호출할 수 없어서,
**이벤트 기반**으로 풀었다:

1. `Company` 생성 시 `CompanyService`가 `TenantCreatedEvent`를 발행한다 (module-tenant).
2. `module-user`의 `TenantBootstrapListener`가 이 이벤트를 리스닝해서, 그 테넌트에
   `ROLE_USER`/`ROLE_ADMIN` Role + `DEFAULT_USER`(defaultYn=Y)/`ADMIN_GROUP` UserGroup을
   자동 생성한다. `ROLE_SYSTEM_ADMIN`은 일부러 자동 시딩 대상에서 뺐다 — 테넌트를 새로 만들 수
   있는 최상위 권한이라, 모든 테넌트에 자동으로 뿌려주면 안 되기 때문이다.
3. `@TransactionalEventListener(phase = AFTER_COMMIT)`을 쓴다 — `CompanyService.create()`의
   트랜잭션이 아직 열려있는 도중에 리스너가 실행되면, 이미 열린 Session을 그대로 재사용하게 되어
   `TenantContext` 설정이 무시되는 문제가 생긴다(open-in-view 버그와 같은 유형). 발행하는
   트랜잭션이 **커밋된 이후**에 실행되도록 해서, 각 리포지토리 호출이 항상 새 Session을 열게 한다.

⚠️ 새 테넌트엔 아직 아무 사용자도 없다 — `ADMIN_GROUP`은 만들어지지만 소속 사용자는 없는 채로
시작한다. 이 테넌트의 첫 사용자가 회원가입하면(`DEFAULT_USER`에 자동 배정됨) `ROLE_SYSTEM_ADMIN`
권한을 가진 누군가가 `UserGroupUserController`로 그 사용자를 `ADMIN_GROUP`에 수동으로 넣어줘야
그 테넌트의 관리자가 된다 — 흔한 B2B 온보딩 흐름과 동일하다.

### DB DDL/DML 스크립트 (`/db/postgresql`)

`ddl-auto: update`로 Hibernate가 스키마를 알아서 맞추는 것과 별개로, 실제 PostgreSQL DB에
쓸 수 있는 정적 SQL 스크립트를 만들어뒀다.

- `01_ddl_schema.sql` — 28개 테이블(module-tenant 2개, module-user 6개, module-portal 9개,
  module-code 2개, module-file 2개, module-terms 2개, module-notice 1개, module-privacy 2개,
  module-form 2개)
  전체 `CREATE TABLE` + 인덱스. 자바
  엔티티의 `@Column`(length/nullable)과 `@UniqueConstraint`를 그대로 옮겼다. **FOREIGN KEY
  제약은 하나도 없다** — 이 프로젝트 전반의 설계 원칙(모든 참조는 느슨한 문자열 컬럼, 실제 FK
  아님)을 그대로 반영한 것이지 실수가 아니다.
  ⚠️ module-code/module-file/module-terms는 이 스크립트가 처음 작성된 이후에 추가된 모듈이라
  한동안 반영이 안 돼 있었다(실제로 겪음 — 새 모듈 만들 때 이 DDL 스크립트 갱신을 빠뜨리기
  쉬우니 앞으로 주의).
- `02_dml_seed.sql` — `DevDataSeeder`/`TenantBootstrapListener`가 만드는 것과 같은 모양의
  기초 데이터(DEMO 회사, Role 3종, UserGroup 2종, admin 계정, **화면 선택지용 공통코드 8개
  그룹** — `PAGE_TYPE`/`PAGE_DEVICE_TYPE`/`USER_GROUP_CATEGORY` + `NOTICE_*` 5종)를 정적 SQL로
  옮긴 예시.
  ⚠️ 공통코드 부분은 한동안 빠져 있었다(DDL과 마찬가지로, 부트스트랩 리스너를 새로 추가할 때
  이 스크립트 갱신을 빠뜨리기 쉬우니 주의).
  ⚠️ `id` 컬럼들은 실제로는 `PrefixedIdGenerator`가 런타임에 무작위 생성하는데 이 스크립트는
  재현 가능하도록 고정값을 썼고, `password_hash`는 진짜 BCrypt 해시로 반드시 교체해야
  로그인이 된다(스크립트 상단 주석 참고).



`UserGroup`에 Role을 매핑할 때 그 역할 코드가 실제로 존재하는지 검증한다: `SystemRoles`
(`ROLE_SYSTEM_ADMIN`/`ROLE_ADMIN`/`ROLE_USER`)는 프레임워크 레벨 권한이라 항상 허용하고,
그 외 값은 `Role` 마스터(`/api/v1/roles`)에 실제로 존재하는(삭제되지 않은) `roleCode`인지
확인한다. 검증에 실패하면 `InvalidRequestException`(`ERR-USER-0008`)이 400으로 응답한다.
(예전엔 `UserService.updateRoles()`에 있던 로직인데, User가 역할을 직접 안 갖게 되면서
이 검증이 실제로 일어나는 지점이 여기로 옮겨왔다.)

### Refresh Token

로그인 시 `accessToken`과 함께 `refreshToken`도 발급된다. `POST /api/v1/auth/refresh`로 새 토큰
쌍을 받고, `POST /api/v1/auth/logout`으로 무효화할 수 있다.

- **회전(rotation) 방식**: refresh할 때마다 기존 refresh token은 즉시 revoke되고 새 걸 발급한다.
  응답으로 받은 새 `refreshToken`으로 반드시 갱신해서 저장해야 하며, 같은 토큰을 재사용하면
  `ERR-USER-0009`로 실패한다 — 탈취된 토큰이 재사용되는 것을 막기 위한 표준 관행이다.
- **서버 측 추적**: `RefreshToken` 엔티티가 토큰의 SHA-256 해시(원문 아님, 비밀번호 해시와 같은 이유)와
  만료시각/revoke 여부를 DB에 저장한다. 순수 stateless JWT라면 자연 만료 전까지 탈취된 토큰을 막을
  방법이 없는데, 이 덕분에 `/logout` 호출 즉시 무효화가 가능하다.
- **refresh 시점에 권한 재확인**: refresh할 때 `EffectiveRoleService`로 최신 권한을 다시 계산해서
  새 accessToken을 만든다 — 최초 로그인 이후 사용자그룹/역할 구성이 바뀌었어도 다음 refresh 때 반영된다.
- **토큰 타입 구분**: `JwtTokenProvider`가 발급하는 JWT에 `tokenType`(`access`/`refresh`) claim이
  추가됐다. `JwtAuthenticationFilter`는 `tokenType=access`인 토큰만 인증에 사용한다 — refresh token을
  Authorization 헤더에 넣어 access token처럼 쓰는 것을 막기 위함이다.
  ⚠️ 이 변경으로 **이전에 발급된(tokenType claim이 없는) 토큰은 재배포 후 전부 무효화**된다 —
  개발 중인 프로젝트라 감수했지만, 운영 환경에 준하는 곳이라면 별도 마이그레이션 공지가 필요하다.
- **동시 refresh 레이스 컨디션 대응 (실제로 겪음)**: 같은 refreshToken으로 정확히 동시에 두 번
  refresh 요청이 들어오면(예: 프론트에서 여러 API 요청이 동시에 401을 받아 각자 독립적으로
  refresh를 시도하는 경우), 회전(rotation) 로직이 같은 `RefreshToken` 행을 두 번 갱신하려다
  `ObjectOptimisticLockingFailureException`(처리 안 된 500)이 났었다. 백엔드
  `AuthService.saveRevokedOrThrow()`가 이 예외를 잡아서 `ERR-USER-0009`(이미 사용된 토큰)로
  변환하고, 프론트 `api/http.js`는 동시에 여러 곳에서 401을 받아도 실제 refresh 네트워크 호출은
  한 번만 나가도록 "single-flight" 처리를 해뒀다(`tryRefresh()`가 진행 중인 Promise를 공유).

### AuthService가 `@Transactional`을 쓰지 않는 이유 (중요)

로그인/회원가입 시점엔 아직 JWT가 없어서 `TenantContext`가 비어있습니다. 흐름은:

1. `companyCode`로 `Company` 조회 (테넌트 무관이라 문제없음)
2. `TenantContext.setTenantId(company.getId())` 수동 설정
3. 그 다음에야 `User` 조회/저장

Hibernate는 **Session이 열리는 시점**에 현재 테넌트를 확정합니다. 만약 `login()`/`register()` 전체를
하나의 `@Transactional`로 묶으면, 1번(Company 조회) 시점에 이미 Session이 열려버려서 2번에서
TenantContext를 바꿔도 3번에 반영되지 않을 수 있습니다. 그래서 이 두 메서드는 일부러 얇게 두고,
Spring Data JPA가 각 리포지토리 호출마다 자기만의 트랜잭션(=새 Session)을 열도록 맡깁니다 —
`module-tenant`의 `TenantIsolationIntegrationTest`에서 검증된 것과 동일한 패턴입니다.

`UserService`는 반대로 일반적인 `@Transactional`을 그대로 씁니다 — 이미 인증이 끝난 요청
(`JwtAuthenticationFilter`가 `TenantContext`를 채운 이후)에서만 호출되므로 이 문제가 없습니다.

> ⚠️ 이 설계는 `spring.jpa.open-in-view=false`가 전제입니다. 실제로 이게 `true`인 상태에서
> 로그인이 항상 실패하는 버그를 겪었습니다 — 자세한 원인은 위 framework-security 섹션의
> "`spring.jpa.open-in-view`는 반드시 `false`" 참고. `framework-security`가 기본값을 강제하므로
> 별도 조치는 필요 없지만, 왜 이 설계가 성립하는지 이해할 때 꼭 같이 봐야 하는 내용입니다.



```kotlin
dependencies {
    implementation(project(":framework-core"))
    implementation(project(":framework-security"))
    implementation(project(":module-tenant"))
}
```

```yaml
framework:
  security:
    jwt:
      secret: ${JWT_SECRET}          # 운영 환경 필수 재정의
      access-token-validity-seconds: 3600
```

```java
// Controller 예시
@GetMapping("/api/v1/sample")
public ApiResponse<SampleDto> getSample() {
    return ApiResponse.success(sampleService.find());
}

// 검증 실패 시 GlobalExceptionHandler 가 자동으로 아래 형태 응답 생성
// { "success": false, "code": "ERR-CORE-0002", "message": "입력값 검증에 실패했습니다.",
//   "errors": [ { "field": "userId", "rejectedValue": "admin", "reason": "..." } ], ... }
```

## module-portal 에 구현된 것

사진 속 BackOffice 화면(포탈/어플리케이션/메뉴/페이지/데이터서비스 관리)에 대응하는 백엔드.
Company/Department/Role과 동일한 패턴(엔티티+리포지토리+서비스+컨트롤러+DTO)을 그대로 따른다.

| 엔티티 | API | 필요 권한 (GET / 나머지) |
|---|---|---|
| `Portal` | `/api/v1/portals/**` | 로그인 / `ADMIN` |
| `Application` | `/api/v1/applications/**` (`?portalId=`로 포탈별 필터링) | 로그인 / `ADMIN` |
| `Page` | `/api/v1/pages/**` | 로그인 / `ADMIN` |
| `Menu` | `/api/v1/menus/**` (`?applicationId=`로 어플리케이션별 필터링, `/tree?applicationId=`로 계층 구조 조회) | 로그인 / `ADMIN` |
| `DataService` | `/api/v1/data-services/**` | **`ADMIN`** (조회 포함 — 내부 API URL 노출) |
| `MessageResource` | `/api/v1/message-resources/**` (`?messageResourceCode=`로 코드별 언어 목록, `/resolve?code=&languageId=`로 단건 해석) | 로그인 / `ADMIN` |

- 전부 `TenantAwareEntity` 상속 — 회사마다 다른 포탈/메뉴 구성을 가질 수 있다 (사진 속 화면들도 회사별 커스텀).
- `Application.portalId`, `Menu.applicationId`/`parentMenuId`, `Page.messageResourceCode` 는
  진짜 JPA 연관관계가 아니라 `Department.parentDepartmentId`와 동일한 방식의 단순 참조 컬럼이다
  (N+1/캐스케이드 복잡도 회피).
- `Menu`는 트리 구조(`parentMenuId` 자기참조, 사진 속 화면에서는 "폴더"라고 부른다)이며,
  `linkUrl`로 외부 링크를 걸 수 있다(사진 속 화면의 "링크" 탭에 대응).
  `GET /api/v1/menus/tree?applicationId=...`로 평면 리스트가 아니라 `children`이 재귀적으로
  채워진 계층 구조 그대로 받을 수 있다 (프론트에서 직접 조립할 필요 없음). 서비스 레이어에서
  `parentMenuId` 기준으로 in-memory 그룹핑 후 재귀 조립하는 방식이라 DB 왕복은 쿼리 1번뿐이다.
  > ⚠️ **메뉴(폴더)와 페이지는 N:M 관계다 (실제로 겪은 요구사항 변경)** — 처음엔 `Menu.pageId`로
  > 1:N이었는데, 하나의 폴더에 여러 페이지를 놓거나 같은 페이지를 여러 폴더에 동시에 놓아야
  > 하는 요구사항이 생겨서 `MenuPage` 매핑 테이블(아래 "Menu 하위 매핑 API" 참고)을 중간에
  > 두는 구조로 바꿨다. `TreeNode.pageId`(단일)도 `TreeNode.pageIds`(목록)로 바뀌었다 — 폴더
  > 자체는 항상 트리에 남고, 매핑된 페이지 중 로그인 사용자가 접근 불가능한 것만 목록에서 빠진다
  > (예전엔 접근 불가능한 페이지가 연결된 메뉴는 하위 서브트리 전체가 트리에서 제외됐는데, N:M이
  > 되면서 "폴더 자체"와 "그 안의 페이지 접근권"이 분리된 개념이 되어 이 동작도 자연스럽게 바뀌었다).
- ⚠️ **`Page`의 용도(`pageType`)/디바이스유형(`deviceType`) 선택지는 하드코딩하지 않고
  공통코드(`module-code`)로 관리한다** — `PAGE_TYPE`(일반/팝업), `PAGE_DEVICE_TYPE`(PC/MOBILE/ALL)
  두 개 코드그룹이다. `module-notice`의 팝업 상세 설정 5종과 완전히 같은 패턴으로,
  `module-portal`이 `module-code`에 의존하고 `PageCodeBootstrapListener`(`TenantCreatedEvent`
  리스닝, `AFTER_COMMIT`)가 새 테넌트에 이 코드그룹/코드상세를 자동으로 채워둔다(find-or-create,
  멱등). 관리자는 코드 관리 화면에서 선택지를 추가/변경할 수 있다 — 예를 들어 용도에 "레이어"를
  추가하고 싶으면 `PAGE_TYPE` 그룹에 코드상세를 추가하면 된다. (`module-code`는 `module-portal`을
  모르므로 순환 의존이 생기지 않는다.)
  - `NOTICE_*` 코드들과 달리 **`code`와 `codeName`이 다르다** — `prt_page.device_type`에 저장되는
    값은 `code`(`"MOBILE"`)이고 화면에 보이는 건 `codeName`(`"모바일"`)이다. 디바이스유형은 저장값이
    영문 코드로 이미 정해져 있어서 표시명을 따로 두는 게 자연스러웠다.
  - `PageCodeBootstrapListener`는 시딩 로직을 `public seedDefaults()`로 열어뒀고 `DevDataSeeder`가
    이걸 그대로 호출한다. `NoticeCodeBootstrapListener`는 기본값 목록이 `DevDataSeeder`에 복사돼
    있어서 값이 어긋나기 쉬운데(README 위쪽 경고 참고), 이 방식이면 복사본 자체가 없다 —
    앞으로 공통코드를 부트스트랩하는 리스너를 추가할 때는 이 패턴을 따르는 게 좋다.
- `MessageResource`는 다국어 리소스다. 같은 `messageResourceCode`가 언어별로 여러 행을 가지며
  (`(tenant_id, message_resource_code, language_id)` 복합 유니크), `Page.messageResourceCode`가 이
  코드를 참조해서 프론트가 로그인 사용자의 언어로 페이지 표시명 등을 해석할 수 있게 한다
  (`GET /api/v1/message-resources/resolve?code=...&languageId=...`).
- `PortalModuleErrorCode`: `ERR-PORTAL-0001` ~ `0020`

### Page/Menu 하위 매핑 API (N:M)

사진 속 페이지 관리 화면 하단 탭 중 "페이지"(상세정보) 다음에 오는 "데이터서비스"/"메시지 자원"/
"권한" 탭, 그리고 메뉴 관리 화면의 폴더↔페이지 매핑에 대응한다. 매핑 레코드는 순수 연결 정보라
다른 엔티티들과 달리 소프트 삭제 대신 **하드 삭제**로 관리한다.

| 매핑 | API | 필요 권한 (GET / 나머지) |
|---|---|---|
| Page ↔ DataService | `/api/v1/pages/{pageId}/data-services/**` | 로그인 / `ADMIN` |
| Page ↔ MessageResource | `/api/v1/pages/{pageId}/message-resources/**` | 로그인 / `ADMIN` |
| Page ↔ Role | `/api/v1/pages/{pageId}/roles/**` | **`ADMIN`** (조회 포함 — "누가 이 페이지에 접근 가능한지"는 민감 정보) |
| Role → Page (역방향) | `/api/v1/pages/by-role/{roleCode}/**` — `roleCode` 문자열 기준 (Role의 PK 아님, module-portal이 module-user를 모르기 때문) | **`ADMIN`** |
| Page 접근 체크 | `GET /api/v1/pages/{id}/access-check` — 현재 사용자가 접근 가능한지 조회 | 로그인 |
| Menu(폴더) ↔ Page | `/api/v1/menus/{menuId}/pages/**` | 로그인 / `ADMIN` |

- 매핑 생성 시 `pageId`가 실제 존재하는 페이지인지는 검증한다(`PageRepository` 사용, 없으면
  `ERR-PORTAL-0005`). 다만 상대방(`dataServiceId`/`messageResourceCode`)이 실제 존재하는지는
  검증하지 않는다 — 이 프로젝트 전반의 다른 참조 컬럼(`Menu.applicationId` 등)과 동일하게 느슨한 참조다.
  `MenuPage`는 예외적으로 양쪽(`menuId`/`pageId`) 다 존재 검증을 한다(`MenuPageService` 참고).
- `PageMessageResource`는 `messageResourceCode` 단위로 매핑한다(언어 무관) — 실제 언어별 값은
  여전히 `MessageResourceController.resolve()`가 담당한다.
- `MenuPage`는 `sortOrder`로 같은 폴더 안에서 페이지 노출 순서를 정한다(다른 매핑들엔 없는
  필드) — 사진 속 메뉴 관리 화면에서 폴더 하위 페이지들이 특정 순서로 나열돼야 해서 추가했다.

#### PageRole ↔ 실제 인가 연결

`PageRole.roleCode`는 Role 엔티티의 PK가 아니라 **Spring Security `GrantedAuthority` 문자열과
정확히 일치해야 하는 값**이다 (`User.roles`에 담기는 `"ROLE_ADMIN"` 같은 값, 또는 관리자가 커스텀
`Role`의 `roleCode`를 그대로 넣어도 된다). `module-portal`이 `module-user`를 몰라도 `SecurityContext`의
권한 문자열과 직접 비교만 하면 되므로, 교차 모듈 의존 없이 실제 인가 로직에 연결할 수 있었다.

연결 지점 두 곳:

1. **`GET /api/v1/pages/{id}/access-check`** — 현재 로그인한 사용자가 이 페이지에 접근 가능한지
   직접 조회한다 (`PageRoleService.isAccessible()`). 매핑이 하나도 없는 페이지는 로그인한 사용자
   누구나 접근 가능하다고 본다 — 매핑을 깜빡한 새 페이지가 기본적으로 전부 잠기는 걸 방지하기 위함.
2. **`GET /api/v1/menus/tree`** — `menu.pageId`가 연결된 메뉴는, 그 페이지에 현재 사용자가 접근
   불가능하면 **트리에서 자동으로 제외**된다(하위 메뉴 전체 포함, 재귀적으로 걸러짐). 페이지가 없는
   메뉴(폴더/외부 링크)는 항상 노출된다. 즉 사용자마다 실제로 다른 메뉴 트리를 받게 된다 — 이게
   이 매핑의 진짜 실전 활용 지점이다.

> ⚠️ **`""`(빈 문자열)과 `null`을 혼동해서 트리가 통째로 비었던 버그 (실제로 겪음)**: 클라이언트가
> `parentMenuId`/`pageId`에 `null` 대신 `""`를 보내면, `getTree()`의 "부모가 없으면 루트"
> 같은 `== null` 체크가 빈 문자열에는 걸리지 않아 최상위 메뉴들이 전부 엉뚱한 그룹으로 묶여
> **트리 응답이 빈 배열로 나왔다.** 두 군데를 고쳤다:
> 1. `Menu`(그리고 같은 패턴을 쓰는 `Department`) 엔티티가 저장 시점에 빈/공백 문자열을 `null`로
>    정규화한다 (`StringUtils.hasText(...) ? value : null`).
> 2. `MenuService`의 트리 조립 로직도 `== null` 대신 `StringUtils.hasText(...)`로 방어적으로 바꿔서,
>    엔티티 정규화 이전에 이미 `""`로 저장된 기존 데이터도 재저장 없이 정상적으로 트리가 조립된다.
>
> **앞으로 트리/계층 구조를 다루는 참조 컬럼(자기참조 FK 등)을 추가할 때는 반드시 저장 시점에
> 빈 문자열을 `null`로 정규화해야 한다** — 그렇지 않으면 이 버그가 재발한다.

> ⚠️ **H2 예약어 충돌로 서버 기동 실패했던 버그 (실제로 겪음)**: `MessageResource`의 자바 필드명
> `value`를 그대로 DB 컬럼명(`@Column(name = "value")`)으로 썼더니, `value`가 **H2의 예약어**라
> `create table` DDL 자체가 문법 오류로 실패해 서버가 기동하지 않았다. `@Column(name = "message_value")`로
> 컬럼명만 바꾸고 자바 필드/게터(`value`/`getValue()`)는 그대로 뒀다. **앞으로 새 엔티티 필드를
> 추가할 때 `value`/`key`/`order`/`group`/`user`/`table` 같은 흔한 단어를 그대로 컬럼명으로 쓰지
> 않도록 주의가 필요하다** — DB 벤더마다 예약어 목록이 달라서 H2에서는 통과해도 실제 운영 DB
> (PostgreSQL/MariaDB 등)에서 또 다른 단어가 걸릴 수 있다.

> ⚠️ 이번 단계에서도 여전히 생략한 것: Page-DataService 간 다대다 매핑(권한 탭), 팝업/차단
> 메시지, 개인정보처리방침·시스템 예외 로그 관리(연관성이 낮아 우선순위 낮음). "메시지자원
> 관리"는 Page와 직접 연결되는 항목이라 처음 스캐폴딩 때 빠뜨린 게 스코프 판단 실수였고,
> 이번에 `MessageResource` + `Page.messageResourceCode`로 추가했다.



## 빌드/테스트 (로컬에서)

> ⚠️ **`-parameters` 컴파일 플래그 필수** (실제로 겪은 버그): `@PathVariable String id`처럼
> 이름을 명시하지 않은 파라미터는 Spring이 바이트코드에서 파라미터명을 리플렉션으로 읽어야
> 하는데, 이게 없으면 `Name for argument of type [...] not specified` 런타임 에러가 난다.
> 루트 `build.gradle.kts`의 `subprojects` 블록에 `-parameters`를 이미 추가해뒀으므로 별도
> 조치는 필요 없지만, 만약 이 플래그를 임의로 제거하면 모든 컨트롤러의 `@PathVariable`/
> `@RequestParam`이 깨진다는 점을 기억해두면 좋다.

> ⚠️ **`options.encoding = "UTF-8"`도 필수** (실제로 겪은 버그): 소스 파일은 전부 UTF-8인데
> 이걸 명시하지 않으면 javac가 **JVM의 플랫폼 기본 인코딩**을 쓴다. 한국어 Windows에서는 그게
> `x-windows-949`(CP949)라서 주석의 한글이 깨져 읽히고, 운이 나쁘면
> `unmappable character for encoding x-windows-949` **컴파일 에러로 빌드 자체가 실패**한다
> (`PageCodeBootstrapListener`를 추가하다가 실제로 겪음). 기존 파일들이 여태 통과한 건 UTF-8
> 바이트가 우연히 CP949로도 해석 가능했기 때문일 뿐이라, 한글 주석이 들어간 새 파일을 추가할
> 때마다 재발할 수 있는 문제였다. 루트 `build.gradle.kts`에 추가해뒀다.

```bash
./gradlew build
./gradlew test
```

> 이 zip에는 Gradle Wrapper 바이너리(`gradle-wrapper.jar`)가 포함되어 있지 않습니다.
> 최초 1회 로컬에서 `gradle wrapper --gradle-version <버전>` 을 실행해 wrapper를 생성해주세요.

## module-code 에 구현된 것

사진 속 BackOffice "Data Management > 코드 관리" 화면 대응. 코드그룹(코드기본) + 코드상세
2단 구조다.

| 엔티티 | API | 필요 권한 (GET / 나머지) |
|---|---|---|
| `CodeGroup` | `/api/v1/code-groups/**` | 로그인 / `ADMIN` |
| `CodeDetail` | `/api/v1/code-groups/{groupId}/details/**` | 로그인 / `ADMIN` |

- `CodeDetail`은 순수 매핑이 아니라 `code`/`codeName`/`sortOrder`/`description`을 가진 진짜
  엔티티라(Page/UserGroup의 N:M 매핑들과 다름), `CodeGroup`처럼 완전한 CRUD(생성/조회/수정/삭제)를
  갖는다.
- 프론트엔드(`frontend/src/pages/CodesPage.jsx`)는 검색조건(그룹코드/그룹명 텍스트, 사용여부) +
  코드그룹 목록 + **행을 클릭하면 하단에 그 그룹의 코드상세 목록**이 나타나는 마스터-디테일
  단일 화면으로 만들었다 (사진 속 화면 구조 그대로). 검색은 지금은 백엔드 쿼리 파라미터가
  아니라 이미 불러온 목록을 클라이언트에서 필터링하는 방식이다 — 코드그룹 개수가 많지 않을
  거라는 전제하에 우선 빠르게 화면을 갖췄다.
- `CodeModuleErrorCode`: `ERR-CODE-0001` ~ `0004`

## module-file 에 구현된 것

사진 속 BackOffice "Data Management > 첨부파일 그룹 관리" 화면 대응. `module-code`와 완전히
같은 패턴(첨부그룹(첨부기본) + 첨부항목 2단 구조)이다.

| 엔티티 | API | 필요 권한 (GET / 나머지) |
|---|---|---|
| `FileGroup` | `/api/v1/file-groups/**` | 로그인 / `ADMIN` |
| `FileItem` | `/api/v1/file-groups/{groupId}/items/**` | 로그인 / `ADMIN` |
| 실제 파일 업로드 | `POST /api/v1/file-groups/{groupId}/items/upload` (multipart/form-data) | `ADMIN` |
| 실제 파일 다운로드 | `GET /api/v1/file-groups/{groupId}/items/{id}/download` | 로그인 |

- 첨부항목을 등록하는 방법이 두 가지다: **실제 파일 업로드**(`upload`, 서버 디스크에 실제로
  저장)와 **메타데이터만 등록**(`create`, `filePath`를 직접 입력, 실제 파일 없음 — `DataService`가
  실제 HTTP 호출 없이 URL만 관리하는 것과 비슷한 용도, 예를 들어 외부에 이미 있는 파일을 가리킬 때).
- **저장 경로는 `application.yml`의 `framework.file.storage.base-path`로 설정한다**
  (`FileStorageProperties`, `app-sandbox`의 기본값은 `./storage/files`). `FileItem.filePath`에는
  이 base-path 기준 **상대경로**만 저장한다 — 나중에 base-path를 옮기더라도(로컬 디스크 → NAS
  마운트 경로 등) DB 데이터를 안 건드려도 된다. 실제 저장은 `FileStorageService.save()`가
  담당하고, 파일명 충돌 방지를 위해 원본 파일명 앞에 UUID 8자리를 붙인다.
- 업로드 크기 제한은 `spring.servlet.multipart.max-file-size`/`max-request-size`로 설정한다
  (`app-sandbox` 기본값 20MB — Spring Boot 기본값인 1MB/10MB는 실사용엔 너무 작아서 늘려뒀다).
- `FileItem`도 `CodeDetail`과 동일한 이유로 순수 매핑이 아니라 완전한 CRUD를 갖는 진짜
  엔티티다.
- 프론트엔드(`frontend/src/pages/FilesPage.jsx`)는 `CodesPage.jsx`를 그대로 복제한 구조다 —
  검색조건(그룹코드/그룹명 텍스트, 사용여부) + 첨부그룹 목록 + **행을 클릭하면 하단에 그
  그룹의 첨부항목 목록**이 나타나는 마스터-디테일 단일 화면. 첨부항목 목록엔 "파일 업로드"
  버튼(실제 업로드)과 "메타데이터만 등록" 버튼이 따로 있고, 각 행에 "다운로드" 버튼이 있다.
  `api/http.js`에 `uploadFile()`(FormData, 401시 자동 refresh)과 `downloadFile()`(blob으로
  받아서 브라우저 저장 다이얼로그를 띄움 — 인증 헤더가 필요해서 `<a href>` 직접 연결로는
  안 되고 fetch로 받아와야 한다)을 추가했다.
- `FileModuleErrorCode`: `ERR-FILE-0001` ~ `0003`

## module-terms 에 구현된 것

사진 속 BackOffice "Data Management > 이용약관 관리" 화면 대응. `module-code`/`module-file`과
같은 2단 구조이지만, "코드값 나열"이 아니라 **시점별 개정 이력 관리**가 핵심이라는 점이 다르다 —
실제 서비스에서는 사용자가 언제 어떤 버전의 약관에 동의했는지 증빙해야 하는 경우가 많다.

| 엔티티 | API | 필요 권한 (GET / 나머지) |
|---|---|---|
| `TermsGroup`(약관기본) | `/api/v1/terms-groups/**` | 로그인 / `ADMIN` |
| `TermsVersion`(버전/개정 이력) | `/api/v1/terms-groups/{groupId}/versions/**` | 로그인 / `ADMIN` |

- ⚠️ **이용약관은 포탈별로 관리된다** — 같은 서비스라도 웹/모바일 등 포탈(채널)마다 서로 다른
  약관 세트를 가질 수 있어야 한다는 요구사항에 따라, `TermsGroup`은 `portalId`를 **필수**로
  갖는다. 다른 참조 컬럼들과 동일하게 진짜 FK가 아니라 느슨한 문자열 참조다(`module-terms`는
  `module-portal`에 의존하지 않는다). **같은 약관코드라도 포탈이 다르면 별개의 약관으로
  취급**한다 — 유니크 제약이 `(tenant_id, terms_code)`가 아니라
  `(tenant_id, portal_id, terms_code)`인 이유다. `GET /api/v1/terms-groups?portalId=`로
  특정 포탈의 약관만 걸러볼 수 있다. 한 번 만든 약관의 포탈은 바꿀 수 없다(`UpdateRequest`에서
  `portalId` 제외) — 이미 쌓인 버전 이력의 맥락이 바뀌어버리기 때문.
- `TermsGroup`: `portalId`/`termsCode`/`termsName`/`termsType`(필수·선택 — 사용자 동의 화면에서
  필수 체크박스로 보여줄지 결정)/`description`/`useYn`.
  - `termsCode`는 "이게 어떤 종류의 약관인지" 식별하는 비즈니스 키다(예: `"SERVICE_TERMS"`,
    `"PRIVACY_TERMS"`) — 실제 서비스 화면(회원가입 등)이 "이 포탈의 SERVICE_TERMS 최신본을
    가져와줘" 식으로 특정 약관을 콕 집어 참조할 때 쓰는 값이다. `id`(랜덤 생성값)와 달리
    사람이 정하는 고정 식별자라 코드에 하드코딩하기 좋다. 생성 시에만 입력받고 수정 폼에는
    아예 안 보이게(`createOnly`) 했다 — 다른 곳에서 이 코드를 참조하고 있을 수 있어서
    바뀌면 안 된다.
- `TermsVersion`: `versionNo`/`content`(약관 본문, `@Lob`)/`effectiveDate`(시행일자)/
  `description`(개정 사유 등)/`useYn`. 목록은 시행일자 최신순(`findAllByGroupIdOrderByEffectiveDateDesc`)으로
  내려온다.
  > ⚠️ **필드명이 `version`이 아니라 `versionNo`인 이유 (실제로 겪음)**: `BaseEntity`가 이미
  > 낙관적 락용 `@Version` 필드(`Long`, `getVersion()`)를 갖고 있어서, 약관의 "버전"(문자열,
  > 예: `"1.0"`)을 그냥 `version`이라고 지으면 `getVersion()`이 `BaseEntity`의 것과 반환
  > 타입(`String` vs `Long`)이 달라 오버라이드 충돌로 컴파일 자체가 안 된다. `BaseEntity`를
  > 상속하는 엔티티에 새 필드를 추가할 때는 `version`이라는 이름을 쓰면 안 된다.
- 프론트엔드(`frontend/src/pages/TermsPage.jsx`)는 검색조건(약관코드/약관명 텍스트, **포탈**,
  약관유형, 사용여부) + 약관 목록(포탈명 컬럼 포함) + **행을 클릭하면 하단에 그 약관의 버전
  이력**이 나타나는 마스터-디테일 구조다. 포탈은 텍스트 입력이 아니라 포탈 관리 화면에서 만든
  포탈 목록을 불러와 **드롭다운으로 선택**하게 했다(포탈이 하나도 없으면 약관 생성 버튼이
  비활성화된다). 생성 후에는 포탈을 수정할 수 없다(백엔드와 동일한 이유). 버전 생성/수정
  모달은 약관 본문이 길 수 있어 `textarea`로 받는다.
- `TermsModuleErrorCode`: `ERR-TERMS-0001` ~ `0003`

## module-notice 에 구현된 것

사진 속 BackOffice "Data Management > 공지/동의서 관리" 화면 대응. `module-code`/`module-file`/
`module-terms`와 달리 **기본+상세(버전 이력) 2단 구조가 아니라 단일 목록**이다 — 공지 하나하나가
제목/내용/게시기간을 통째로 갖는 완결된 항목이라 개정 이력을 추적할 필요가 없다고 판단했다
(약관과 다른 점).

| 엔티티 | API | 필요 권한 (GET / 나머지) |
|---|---|---|
| `Notice`(공지/동의서) | `/api/v1/notices/**` | 로그인 / `ADMIN` |

- `noticeType`("공지"/"동의서")으로 두 성격의 콘텐츠를 한 테이블/화면에서 같이 관리한다 —
  둘 다 "포탈에 게시기간을 두고 노출하는 콘텐츠"라는 성격이 같아서 하나로 합쳤다.
- 이용약관과 동일하게 **포탈별로 관리**된다(`portalId` 필수, `GET /api/v1/notices?portalId=`로
  필터링 가능). 다만 약관과 달리 같은 포탈 안에서 제목이 같은 공지를 여러 개 올릴 수도 있으므로
  유니크 제약은 없다.
- 필드: `portalId`/`noticeType`/`title`/`content`(`@Lob`)/`startDate`/`endDate`(게시기간)/
  `popupYn`(포탈 접속 시 팝업 노출 여부)/`useYn`.
- **팝업 상세 설정 5종** — `applyType`(적용유형: 로그인/페이지)/`popupFrequency`(팝업빈도: 1회 등)/
  `popupSize`(크기: 소형/중형/대형)/`popupShape`(팝업모양: 레이어팝업/윈도우팝업)/
  `popupPosition`(위치: 중앙/좌상단/부모창). `popupYn="Y"`일 때 의미 있는 값들이고, 전부 선택
  입력이다(NOT NULL 아님).
  > ⚠️ **이 5종의 선택지는 하드코딩하지 않고 공통코드(`module-code`)로 관리한다** —
  > `NOTICE_APPLY_TYPE`/`NOTICE_POPUP_FREQUENCY`/`NOTICE_POPUP_SIZE`/`NOTICE_POPUP_SHAPE`/
  > `NOTICE_POPUP_POSITION` 다섯 개 코드그룹이다. `module-notice`가 `module-code`에 의존해서,
  > 새 테넌트가 생기면 `NoticeCodeBootstrapListener`(`TenantCreatedEvent` 리스닝, `AFTER_COMMIT`)가
  > 이 코드그룹/코드상세를 자동으로 채워둔다(find-or-create, 멱등). 관리자는 코드 관리
  > 화면에서 이 그룹들의 코드상세를 추가/변경해서 선택지를 늘리거나 바꿀 수 있다 — 예를 들어
  > 팝업빈도에 "매번"이나 "1일 1회"를 추가하고 싶으면 코드 관리에서 `NOTICE_POPUP_FREQUENCY`
  > 그룹에 코드상세를 추가하면 된다.
  > `DevDataSeeder`(로컬 샌드박스)는 `CompanyService.create()`(이벤트 발행)를 거치지 않고
  > 회사를 직접 만들기 때문에, 이 다섯 그룹도 별도로 직접 시딩해준다 —
  > `NoticeCodeBootstrapListener`의 그룹코드 상수(`GROUP_APPLY_TYPE` 등)를 그대로 재사용해서
  > 값이 어긋나지 않게 했다.
- 프론트엔드(`frontend/src/pages/NoticesPage.jsx`)는 검색조건(제목 텍스트, 포탈, 구분, 사용여부) +
  단일 목록(포탈명/구분/제목/게시기간/팝업/사용여부/삭제여부 컬럼) 구조다. `TermsPage.jsx`와
  동일하게 포탈은 드롭다운으로 선택하고, 생성 후에는 수정할 수 없다. `popupYn`을 체크하면
  팝업 상세 설정 5개 필드가 나타나고, 각각 공통코드에서 불러온 값으로 셀렉트를 채운다
  (해당 코드그룹이 비어있으면 텍스트 입력으로 대체 — 코드를 못 가져와도 화면이 막히지 않게).
- `NoticeModuleErrorCode`: `ERR-NOTICE-0001`

## module-privacy 에 구현된 것

사진 속 BackOffice "Data Management > 개인정보처리방침 관리" 화면 대응. `module-terms`(이용약관)와
**완전히 같은 구조**다 — 개인정보처리방침도 시점별 개정 이력을 추적해야 한다(수집 당시 어떤
버전이 적용됐는지 나중에 증빙해야 하는 경우가 많다는 게 이용약관과 같은 이유). 별도 모듈로
분리한 이유는 사진 속 화면 자체가 "이용약관 관리"와 "개인정보처리방침 관리"로 나뉘어 있어서다.

| 엔티티 | API | 필요 권한 (GET / 나머지) |
|---|---|---|
| `PrivacyPolicyGroup`(방침기본) | `/api/v1/privacy-policy-groups/**` | 로그인 / `ADMIN` |
| `PrivacyPolicyVersion`(버전/개정 이력) | `/api/v1/privacy-policy-groups/{groupId}/versions/**` | 로그인 / `ADMIN` |

- `module-terms`와 동일하게 **포탈별로 관리**된다(`portalId` 필수, 유니크 제약은
  `(tenant_id, portal_id, policy_code)`). `GET /api/v1/privacy-policy-groups?portalId=`로
  특정 포탈의 방침만 걸러볼 수 있다. 한 번 만든 방침의 포탈은 바꿀 수 없다.
- `PrivacyPolicyGroup`: `portalId`/`policyCode`/`policyName`/`description`/`useYn`.
  ⚠️ 이용약관의 `termsType`(필수/선택) 같은 유형 구분 필드는 없다 — 개인정보처리방침은
  성격상 "필수/선택"으로 나눌 개념이 아니라서 뺐다.
- `PrivacyPolicyVersion`: `versionNo`/`content`(방침 본문, `@Lob`)/`effectiveDate`(시행일자)/
  `description`(개정 사유 등)/`useYn`. 목록은 시행일자 최신순으로 내려온다. 필드명이
  `version`이 아니라 `versionNo`인 이유는 `TermsVersion`과 동일하다(`BaseEntity`의 낙관적 락
  `@Version` 필드와 이름 충돌 — module-terms에서 실제로 겪었던 문제).
- 프론트엔드(`frontend/src/pages/PrivacyPolicyPage.jsx`)는 `TermsPage.jsx`와 동일한 구조다 —
  검색조건(방침코드/방침명 텍스트, 포탈, 사용여부) + 방침 목록(포탈명 컬럼 포함) + **행을
  클릭하면 하단에 그 방침의 버전 이력**이 나타나는 마스터-디테일 구조. 포탈은 드롭다운으로
  선택한다.
- `PrivacyModuleErrorCode`: `ERR-PRIVACY-0001` ~ `0003`

## module-form 에 구현된 것

사진 속 BackOffice "Data Management > 표준양식 관리" 화면 대응. `module-terms`/`module-privacy`와
2단(양식기본+버전) 구조는 같지만, **버전이 텍스트가 아니라 실제 엑셀/워드 파일**이라는 점이
다르다 — 관리자가 표준양식 파일(.xlsx/.docx 등)을 그대로 업로드해서 버전별로 관리한다.

| 엔티티 | API | 필요 권한 (GET / 나머지) |
|---|---|---|
| `FormGroup`(양식기본) | `/api/v1/form-groups/**` | 로그인 / `ADMIN` |
| `FormVersion`(버전/개정 이력) | `/api/v1/form-groups/{groupId}/versions/**` | 로그인 / `ADMIN` |
| 실제 파일 업로드 | `POST /api/v1/form-groups/{groupId}/versions/upload` (multipart/form-data) | `ADMIN` |
| 실제 파일 다운로드 | `GET /api/v1/form-groups/{groupId}/versions/{id}/download` | 로그인 |

- `module-terms`/`module-privacy`와 동일하게 **포탈별로 관리**된다(`portalId` 필수, 유니크
  제약은 `(tenant_id, portal_id, form_code)`). `GET /api/v1/form-groups?portalId=`로 특정
  포탈의 양식만 걸러볼 수 있다. 한 번 만든 양식의 포탈은 바꿀 수 없다.
- `FormGroup`: `portalId`/`formCode`/`formName`/`category`(분류)/`description`/`useYn`.
  ⚠️ `category`(분류)는 약관/개인정보처리방침에는 없던 필드다 — 표준양식은 보통 인사/총무/재무
  같은 부서·업무 분류가 있는 경우가 많아서 추가했다. 검색조건에도 반영했다.
- `FormVersion`: `versionNo`/`fileName`/`filePath`/`fileSize`/`effectiveDate`(시행일자)/
  `description`(개정 사유 등)/`useYn`. 목록은 시행일자 최신순으로 내려온다. 필드명이
  `version`이 아니라 `versionNo`인 이유는 `TermsVersion`과 동일하다(`BaseEntity`의 낙관적 락
  `@Version` 필드와 이름 충돌).
  > ⚠️ **실제 파일 저장은 `module-file`의 `FileStorageService`를 그대로 재사용한다** — 같은
  > 저장 방식(`FileStorageProperties.base-path` 기준 상대경로 저장, UUID 접두사로 파일명
  > 충돌 방지)을 두 번 만들 이유가 없어서 `module-form`이 `module-file`에 의존한다.
  > `create()`(filePath를 직접 입력하는 메타데이터 전용 등록)와 `upload()`(실제 업로드) 두
  > 경로가 있는 것도 `module-file`의 `FileItem`과 동일한 설계다. 버전 정보 수정(`PUT`)은
  > 메타데이터(버전번호/시행일자/비고/사용여부)만 바꾸고 **파일은 안 바뀐다** — 파일을
  > 바꾸려면 새 버전을 업로드해야 한다.
- 프론트엔드(`frontend/src/pages/FormsPage.jsx`)는 검색조건(양식코드/양식명 텍스트, 포탈,
  **분류**, 사용여부) + 양식 목록(포탈명/분류 컬럼 포함) + **행을 클릭하면 하단에 그 양식의
  버전 이력**이 나타나는 마스터-디테일 구조. 버전 이력 화면은 `FilesPage.jsx`와 비슷하게
  "새 버전 업로드"(실제 파일)와 "수정"(메타데이터만, 파일 교체 없음)이 분리되어 있고, 각
  행에 "다운로드" 버튼이 있다.
- `FormModuleErrorCode`: `ERR-FORM-0001` ~ `0003`



`framework-core`/`framework-security`/`module-tenant`/`module-user`/`module-portal`는 전부
라이브러리 모듈이라 실행 파일이 없습니다. `app-sandbox`는 이들을 실제로 띄워서 Postman/프론트엔드로
수동 검증할 수 있게 해주는 **로컬 전용 실행 모듈**입니다. 서버 기동 시 `DevDataSeeder`가
샘플 회사(`DEMO`)와 관리자 계정(`admin` / `admin1234`)을 자동으로 만들어둡니다.
(⚠️ `DevDataSeeder`는 로컬 개발 전용이며, 운영 배포 시 제거하고 별도의 관리자 초기화 절차로 대체해야 합니다.)

### DB: PostgreSQL (H2에서 전환함)

⚠️ **로컬에 PostgreSQL DB/계정을 미리 만들어둬야 한다** — `app-sandbox/src/main/resources/application.yml`의
기본값은 `jdbc:postgresql://localhost:5432/jepf`, 계정 `jepf`/`jepf`다. 실제 값에 맞게 바꾸거나,
아래처럼 맞춰서 미리 만들어두면 된다:

```bash
createdb jepf
createuser jepf --pwprompt   # 비밀번호를 jepf로 입력
```

`ddl-auto: update`로 Hibernate가 스키마를 자동으로 맞추므로 테이블을 미리 만들 필요는 없다.
직접 스키마를 관리하고 싶다면 `/db/postgresql/01_ddl_schema.sql`을 먼저 실행하고
`ddl-auto`를 `validate`로 바꿔도 된다.

⚠️ **`ddl-auto: update`의 함정 (실제로 겪음)**: 새 컬럼은 자동으로 추가해주지만, **엔티티에서
필드를 지우거나 이름을 바꿔도 예전 컬럼은 절대 지워주지 않는다.** 예를 들어 `FormVersion`이
원래 `content`(텍스트, `NOT NULL`) 필드였다가 실제 파일 기반(`fileName`/`filePath`/`fileSize`)으로
바뀌었는데, 예전 스키마로 한 번이라도 서버를 띄운 적이 있는 DB라면 `content NOT NULL` 컬럼이
테이블에 그대로 남아있어서 이후 insert가 전부 실패한다(`null value in column "content" violates
not-null constraint`). 엔티티의 필드를 지우거나 이름을 바꿨는데 갑자기 insert가 깨지면, 먼저
그 테이블에 이제는 없는 컬럼이 남아있는지 확인할 것 — 지우면 해결된다:
```sql
ALTER TABLE <테이블명> DROP COLUMN <예전_컬럼명>;
```
근본적으로는 이런 문제 때문에 `ddl-auto: update`가 실 운영에는 위험하다고 보고, Flyway 같은
마이그레이션 도구 도입을 다음 단계로 남겨둔 것이다(아래 "다음 단계" 참고).

⚠️ **또 다른 함정 (실제로 겪음) — 테이블 소유권**: 컬럼을 지운 게 아니라 **새로 추가**했는데도
`ddl-auto: update`가 실패할 수 있다. `User`에 `departmentId`를 새로 추가했을 때
`department_id 칼럼 없음` 에러가 났다 — 원인은 `usr_user` 테이블이 `postgres` 계정으로
만들어져서 소유자가 `postgres`였기 때문이다. `GRANT ALL PRIVILEGES`는 SELECT/INSERT 같은
데이터 조작 권한만 줄 뿐, **`ALTER TABLE`(컬럼 추가 등 스키마 변경)은 PostgreSQL에서 테이블
소유자만 할 수 있는 동작**이라 `jepf` 계정으로 실행되는 Hibernate가 컬럼을 못 만든 것이다.
`/db/postgresql/00b_fix_table_ownership.sql`에 이제 GRANT뿐 아니라 **모든 기존 테이블의
소유권 자체를 jepf로 옮기는 부분**을 추가해뒀다 — 이 스크립트를 한 번 실행해두면 앞으로
엔티티에 필드를 추가할 때마다 반복해서 안 겪어도 된다. 급하면 문제되는 테이블만 직접 고쳐도
된다:
```sql
ALTER TABLE <테이블명> ADD COLUMN <새_컬럼명> <타입>;
```

### 실행

```bash
./gradlew :app-sandbox:bootRun
```

서버가 `http://localhost:8080` 에서 뜹니다. 콘솔에 `[DevDataSeeder] 샘플 관리자 계정 생성 완료` 로그가 보이면 준비 완료입니다.

### frontend — React(Vite) 관리 화면 전체

`/frontend`에 React + Vite로 만든 프론트엔드가 있다. 로그인/회원가입/대시보드에 더해,
지금까지 만든 **모든 백엔드 엔티티의 관리 화면**이 다 있다: 회사/부서/사용자/Role/사용자그룹/
포탈/어플리케이션/페이지/메뉴/데이터서비스/메시지자원/**코드**.

**사진 속 BackOffice 화면 구조(검색조건 → 목록 → 하단 탭 상세)를 그대로 따른 화면들**이 있다 —
별도 라우트로 안 빼고 한 화면에서 행을 클릭하면 하단에 탭이 뜬다:
- **사용자그룹 관리**: 목록 → 하단 탭(기본정보 / 권한(Role) / 사용자) — Role ↔ UserGroup ↔ User
  매핑을 여기서 바로 편집
- **페이지 관리**: 목록 → 하단 탭(기본정보 / 데이터서비스 / 메시지자원 / 권한(Role))
- **코드 관리**: 목록(코드그룹) → 하단에 코드상세 목록

`components/Tabs.jsx`가 이 탭 전환을 담당하는 범용 컴포넌트다. (사진 속 페이지 관리 화면엔
컴포넌트/파라미터/매뉴얼 탭도 있었는데, 아직 백엔드에 대응 엔티티가 없어서 이번엔 뺐다.)

```bash
cd frontend
npm install
cp .env.example .env.local   # 필요하면 VITE_API_BASE_URL 수정
npm run dev
```

`http://localhost:5173`에서 뜬다. `app-sandbox`가 먼저 떠 있어야 한다 (`http://localhost:8080`).

**구조**
- `api/http.js` — 인증 필요한 호출 전용 공통 함수. 저장된 `accessToken`을 자동으로 붙이고,
  **401을 받으면 `refreshToken`으로 한 번 자동 갱신을 시도한 뒤 재요청**한다 — 화면 코드에서
  토큰 만료를 신경 쓸 필요가 없다.
- `api/crud.js` — 대부분의 엔티티가 list/get/create/update/remove 형태의 동일한 REST 패턴이라
  `createCrudApi(basePath)` 팩토리로 반복을 없앴다. N:M 매핑 하위 리소스는 `createMappingApi`
  (update가 없고 add/remove만 있음).
- `components/CrudPage.jsx` — 검색조건 + 목록 + 생성/수정 모달 + 삭제까지 다 있는 범용 CRUD
  화면. `columns`(표에 보여줄 컬럼)와 `fields`(폼 입력 필드)만 넘기면 새 엔티티 화면을 몇 줄로
  만들 수 있다. Company/Role/Portal/Application/DataService/MessageResource가 전부 이걸 그대로
  쓴다(Department/Menu는 각각 회사 정보 표시/종속 드롭다운 때문에 커스텀으로 뺐다 — 아래 참고).
  `fields`의 `createOnly`(생성 시에만 보임)와 `editOnly`(수정 시에만 보임 — `useYn`처럼
  `CreateRequest`엔 없고 `UpdateRequest`에만 있는 필드용) 옵션으로 생성/수정 폼의 차이를
  표현한다. `type: 'select'` 필드는 정적 목록이면 `options: [{value,label}]`를, API에서
  불러와야 하면 `optionsApi`/`optionsLabel`/`optionsValue`(실제 저장할 값, 기본은 `row.id`)를
  줘서 참조 필드를 텍스트 입력이 아니라 드롭다운으로 만든다. `excludeSelf: true`면 수정 중인
  자기 자신을 옵션에서 뺀다(부서의 상위부서 선택 등 자기참조 케이스). `search` 프롭을 주면
  검색바가 생긴다(`keywordFields`로 텍스트 검색, `filters`로 select 필터 — 정적/`optionsApi`
  둘 다 지원). `columns` 항목에 `resolveApi`/`resolveLabel`을 주면 참조 ID 대신 이름을
  보여준다(예: `portalId` → 포탈명, 어플리케이션 관리의 포탈 컬럼이 이 방식).
- `components/MappingPanel.jsx` — N:M 매핑 하나("입력 + 추가" / "목록 + 제거") 관리하는 범용
  패널. UserGroup/Page의 매핑 화면들이 이걸 재사용한다. `addField`에 `optionsApi`를 주면
  텍스트 입력 대신 드롭다운으로 고른다(`CrudPage`의 select 옵션과 동일한 방식).
- `User`는 직접 생성 API가 없어서(회원가입으로만 생성) `CrudPage`를 안 쓰고 목록/수정/삭제만
  있는 커스텀 화면(`UsersPage.jsx`)으로 따로 만들었다. 역할(roles)은 이 사용자가 속한
  UserGroup들의 Role을 합친 값이라 여기서 직접 편집할 수 없다 — 사용자그룹 관리에서 그룹
  매핑을 바꿔야 반영된다.

**⚠️ 알아두면 좋은 점**
- `AuthContext`가 로그인 응답(`accessToken`/`refreshToken`/`roles` 등)을 `localStorage`에
  저장한다. XSS에 취약해서 실제 운영 프론트엔드라면 권장되지 않는 방식이다(httpOnly 쿠키 등이
  더 안전) — 여기서는 "화면으로 빠르게 검증"이 목적이라 가장 단순한 방식을 썼다.
- 로그인 폼의 회사 코드 기본값은 `DEMO`로 채워져 있다 — `app-sandbox`의 `DevDataSeeder`가
  만드는 그 테넌트다.
- CORS: `framework-security`에 `CorsProperties`(`framework.security.cors.allowed-origins`)를
  추가했고 기본값이 `http://localhost:5173`이라 별도 설정 없이 바로 통한다. 다른 포트/도메인을
  쓰면 `app-sandbox`의 `application.yml`에 추가해야 한다.
- `portalId`/`applicationId`/`parentMenuId`/`pageId` 같은 참조 필드들은 지금은 그냥 텍스트
  입력이다(드롭다운으로 대상을 골라주지 않는다) — 다른 화면에서 만든 엔티티의 `id` 값을
  직접 복사해서 넣어야 한다. 빠르게 전체 화면을 갖추는 걸 우선했고, UX 개선(드롭다운/검색)은
  다음 단계 과제로 남겨뒀다.

**⚠️ 백엔드 버그 하나 발견해서 같이 고침**: `UserGroupUserController`의 목록 조회는 User
정보를 돌려주는데(다른 매핑 컨트롤러들과 달리 매핑 레코드 자체가 아님), 삭제는 매핑의 내부 PK를
요구해서 프론트에서 삭제할 방법이 없었다. `DELETE /api/v1/user-groups/{userGroupId}/users/{mappingId}`를
`{userId}` 기준으로 바로 지우도록 바꿨다 (`UserGroupUserService.removeUser`가 이제
`findByUserGroupIdAndUserId`로 조회 후 삭제).

`http://localhost:8080/swagger-ui/index.html` 접속하면 지금까지 만든 API가 모두 보입니다
(Auth/Company/Department/User/Role/Portal/Application/Page/Menu/DataService 태그로 그룹핑됨).
각 API의 요청 본문(Request body)에는 필드별로 실제 값 예시(`@Schema(example=...)`)가 채워져
있어서, "Try it out"을 눌러도 모든 API가 `"string"`처럼 똑같이 나오지 않고 DTO별로 의미있는
샘플이 채워진 채로 뜬다. 인증이 필요한 API는:

1. `/api/v1/auth/login` 을 Swagger UI에서 직접 실행해 `accessToken` 확보
2. 우측 상단 **Authorize** 버튼 클릭 → 토큰 값만 붙여넣기 (`Bearer ` 접두사 없이)
3. 이후 다른 API들도 Swagger UI에서 바로 호출 가능

#### ⚠️ DTO 스키마 이름 충돌 버그 (실제로 겪음)

모든 DTO가 관례상 중첩 record 이름을 `CreateRequest`/`UpdateRequest`/`Response`로 통일해서
썼는데(`CompanyDto.CreateRequest`, `RoleDto.CreateRequest`, `DataServiceDto.CreateRequest`...),
springdoc이 OpenAPI 스키마 이름을 **패키지 없이 단순 클래스명만으로** 생성한다. 그 결과
`CreateRequest`라는 이름의 스키마가 계속 덮어써지면서, Swagger UI에서 서로 다른 API인데
같은(마지막에 등록된) DTO의 예시/스키마가 보이는 버그가 실제로 발생했다.

해결: 모든 DTO의 중첩 record에 `@Schema(name = "...")`로 고유 이름을 명시했다
(`CompanyCreateRequest`, `RoleCreateRequest`, `DataServiceCreateRequest` 등). **앞으로 새 DTO를
추가할 때도 이 규칙을 반드시 지켜야 한다** — `record CreateRequest(...)`를 추가하면서
`@Schema(name = "...")`를 빠뜨리면 이 버그가 재발한다.


### 로그인 + 테넌트 격리 수동 검증 시나리오

```bash
# 1) 로그인 (샘플 관리자 계정)
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"companyCode":"DEMO","loginId":"admin","password":"admin1234"}'
# -> { "data": { "accessToken": "eyJ...", "refreshToken": "eyJ...", "tenantId": "TNT_...", ... } }

# 2) 두 번째 회사 생성 (인증 필요 — 1번에서 받은 토큰 사용)
curl -X POST http://localhost:8080/api/v1/companies \
  -H "Authorization: Bearer <토큰>" -H "Content-Type: application/json" \
  -d '{"companyCode":"ACME","companyName":"에이씨엠이"}'

# 3) 두 번째 회사에 사용자 회원가입
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"companyCode":"ACME","loginId":"bob","password":"bobpass1234","userName":"Bob"}'

# 4) 각 회사 계정으로 로그인해서 받은 토큰으로 /api/v1/departments 를 만들고 조회해보면
#    module-tenant 때와 동일하게 서로의 데이터가 안 보이는 것을 확인할 수 있습니다.
```

Postman이면 Authorization 탭 → Bearer Token에 로그인 응답의 `accessToken`을 넣으시면 됩니다.


## 다음 단계 제안

1. ~~`module-tenant`를 실제 DB(H2 등)에 붙여 테넌트 격리가 실제로 동작하는지 검증~~ — 완료
2. ~~`module-user` — 로그인/회원가입 API, JWT 발급을 Company와 연동~~ — 완료
3. ~~`module-user`에 Role 마스터 관리(CRUD) 추가~~ — 완료
4. ~~Role 기반 세부 인가 — `@PreAuthorize`로 실제 권한 체크 추가~~ — 완료
5. ~~`module-portal` — Portal/Application/Page/Menu/DataService 관리~~ — 완료
6. ~~`module-portal` 실제 DB/Postman 검증~~ — 완료
7. ~~Menu 트리 전체 조회 API~~ — 완료 (`GET /api/v1/menus/tree?applicationId=`)
8. ~~"메시지자원 관리"(다국어 리소스)~~ — 완료 (`MessageResource` + `Page.messageResourceCode`)
9. ~~Page-DataService/MessageResource/Role 매핑(N:M)~~ — 완료 (`/api/v1/pages/{pageId}/...`)
10. ~~`PageRole` 매핑을 실제 `@PreAuthorize` 인가 로직에 연결~~ — 완료 (`access-check` API + Menu 트리 필터링)
11. ~~Role ↔ User 매핑~~ — **`RoleUserController` 직접 매핑 방식은 폐기**하고 `Role ↔ UserGroup ↔ User`
    3단계 구조로 재설계 완료 (사진 속 BackOffice "사용자그룹 관리" 화면과 일치시킴)
12. Flyway 등 DB 마이그레이션 도구 도입 — 지금은 `ddl-auto: update`로 Hibernate가 스키마를 알아서
    맞추는 중이라, 실제 운영 DB로 갈 때 위험할 수 있음
13. ~~Refresh Token 재발급 플로우~~ — 완료 (`/api/v1/auth/refresh`, `/api/v1/auth/logout`, 토큰 회전 + 서버 측 revoke)
14. `PageRole`처럼 `UserGroupRole`도 실제 DB/Postman으로 로그인→그룹 배정→권한 반영까지 엔드투엔드 검증 필요
15. ~~새 테넌트 생성 시 기초 데이터(Role/UserGroup) 자동 부트스트랩~~ — 완료 (`TenantCreatedEvent` + `TenantBootstrapListener`)
16. ~~PostgreSQL DDL/DML 스크립트~~ — 완료 (`/db/postgresql/`, Flyway 자체 도입은 아직 — 스크립트만 준비된 상태)
17. ~~`app-sandbox`를 H2 → PostgreSQL로 전환~~ — 완료 (연결 정보는 로컬에서 채워야 함)
18. ~~React(Vite) 프론트엔드 — 로그인/회원가입/대시보드~~ — 완료 (`/frontend`)
19. ~~프론트엔드 실제 PostgreSQL 연동 상태로 로그인 → 대시보드까지 화면으로 엔드투엔드 검증~~ — 완료
20. ~~프론트엔드 범위 확장 — 전체 엔티티 관리 화면 + N:M 매핑 화면~~ — 완료 (`CrudPage`/`MappingPanel` 재사용 컴포넌트)
21. 프론트엔드로 전수 테스트 진행 중 (사용자) — 발견되는 이슈 계속 수정
22. ~~`module-code` — CodeGroup(코드기본)/CodeDetail(코드상세) 관리 + 검색조건 포함 프론트엔드~~ — 완료
23. ~~사용자그룹/페이지 매핑 화면을 별도 라우트 → 사진 속 구조(목록+하단 탭)로 전환~~ — 완료
    (`components/Tabs.jsx`)
24. ~~모든 엔티티의 `UpdateRequest`에 `useYn` 반영~~ — 완료 (13개 엔티티 백엔드 DTO/Service +
    프론트엔드 수정 폼 전체)
25. ~~모든 목록에 삭제여부(`deletedYn`) 노출~~ — 완료. `delete()`는 실제 행 삭제가 아니라
    소프트 삭제(컬럼값만 변경)라서, 관리자 화면 목록은 삭제된 데이터도 계속 보이고 상태를
    확인할 수 있어야 한다는 요구사항 반영 (13개 엔티티 `getList()`를 `findAllByDeletedYn("N",...)`
    → `findAll(...)`로 변경 + Response DTO에 `deletedYn` 추가 + 프론트 전체 목록에 컬럼 추가).
    순수 N:M 매핑 테이블(UserGroupRole/UserGroupUser/PageRole/PageDataService/PageMessageResource)은
    실제로 하드 삭제라 예외로 뒀다. `Menu.TreeNode`(실제 내비게이션 트리 API)도 의도적으로
    제외했다 — 관리 화면과 달리 실제 메뉴 렌더링에는 삭제된 항목이 안 보이는 게 맞기 때문.
    ⚠️ 후속 과제: 이미 삭제된 행에서 "삭제" 버튼을 다시 누르면 404 에러가 난다
    (`findActiveOrThrow`가 활성 상태만 대상으로 하기 때문) — 삭제된 행은 버튼을 숨기거나
    "복구" 기능으로 바꿔야 함.
26. ~~프론트엔드 UX 개선 — `portalId`/`applicationId` 등 참조 필드를 텍스트 입력 대신
    드롭다운으로~~ — 완료. `CrudPage`에 `type: 'select'` 필드 옵션(옵션을 API에서 비동기로
    불러옴, `excludeSelf`로 자기참조 제외 지원) 추가, `MappingPanel`의 `addField`에도
    `optionsApi`가 있으면 텍스트 입력 대신 드롭다운으로 렌더링하도록 추가. 적용 범위:
    어플리케이션 관리(포탈), 부서 관리(상위부서, 자기 자신 제외), 사용자그룹 관리(포탈/Role
    매핑/사용자 매핑), 페이지 관리(데이터서비스/메시지자원/Role 매핑). 메뉴 관리는
    `applicationId`를 고르면 `parentMenuId` 선택지가 그 어플리케이션 소속 메뉴로 좁혀지는
    **종속 관계**가 있어서, `CrudPage`로는 표현이 어려워 커스텀 화면(`MenusPage.jsx`)으로
    다시 만들었다.
27. 사진 속 BackOffice 레이아웃(상단 메뉴+좌측 사이드바 스타일)에 더 가깝게 다듬기 — 지금은 기능 위주의 최소 스타일
    (페이지 관리의 컴포넌트/파라미터/매뉴얼 탭 등 아직 백엔드 미대응 탭도 포함)
28. ~~`module-file` — FileGroup(첨부기본)/FileItem(첨부항목) 관리 + 검색조건 포함 프론트엔드~~ — 완료
    (`module-code`와 동일한 패턴)
29. ~~`module-file` 실제 파일 업로드/다운로드 + 저장 경로 프로퍼티화~~ — 완료
    (`FileStorageProperties`(`framework.file.storage.base-path`), `FileStorageService`,
    `POST .../items/upload`, `GET .../items/{id}/download`, 프론트 `uploadFile()`/`downloadFile()`)
30. ~~`module-terms` — TermsGroup(약관기본)/TermsVersion(버전 이력) 관리 + 검색조건 포함 프론트엔드~~ — 완료
    (`module-code`/`module-file`과 동일한 마스터-디테일 패턴, 버전은 시행일자 개정 이력 관리)
31. ~~`module-notice` — Notice(공지/동의서) 단일 목록 관리 + 검색조건 포함 프론트엔드~~ — 완료
    (기본+상세 2단 구조가 아니라 단일 목록, 포탈별 관리)
32. ~~공지 팝업 상세 설정(적용유형/팝업빈도/크기/팝업모양/위치)을 공통코드 연동으로 관리~~ — 완료
    (`NoticeCodeBootstrapListener`가 테넌트 생성 시 `module-code`에 코드그룹 5종 자동 시딩,
    프론트는 하드코딩된 옵션 대신 코드 관리 데이터를 불러와 셀렉트 구성)
33. ~~`module-privacy` — PrivacyPolicyGroup/PrivacyPolicyVersion 관리 + 검색조건 포함 프론트엔드~~ — 완료
    (`module-terms`와 완전히 동일한 구조 — 포탈별 관리 + 버전 이력, 유형 구분 필드만 없음)
34. ~~`module-form` — FormGroup/FormVersion 관리 + 검색조건 포함 프론트엔드~~ — 완료
    (`module-terms`와 동일한 구조 + 분류(category) 필드 추가. **사진 속 Data Management 화면
    6개(코드/첨부파일/이용약관/개인정보처리방침/공지·동의서/표준양식) 전부 구현 완료**)
36. ~~`module-form` 버전을 텍스트(`content`) → 실제 엑셀/워드 파일로 재설계~~ — 완료
    (`module-file`의 `FileStorageService` 재사용, `POST .../versions/upload`,
    `GET .../versions/{id}/download`, 프론트 "새 버전 업로드"/"수정"(메타데이터만) 분리)
37. ~~회사 > 부서 > 사용자 연결고리 보강~~ — 완료. 회사<->부서는 원래부터 모든 엔티티가 갖는
    `tenant_id`로 연결돼 있었지만(화면에 안 보였을 뿐), 부서<->사용자는 `User`에 필드 자체가
    없어서 진짜 빠져 있었다(실제로 겪음). `User.departmentId`(선택) 추가 —
    `UserDto`(Create/Update/Response)/`UserService`/DDL(`usr_user.department_id`) 반영.
    프론트엔드: `UsersPage`에 부서 드롭다운 + 소속 회사 표시(로그인한 관리자의 회사, 항상
    고정값), `DepartmentsPage`에도 소속 회사 표시 추가.
38. ~~부서 관리에 검색조건 추가 + 생성/수정 폼에도 소속 회사를 읽기전용으로 표시~~ — 완료.
    `DepartmentsPage`가 `CrudPage`(검색바 없음) 대신 커스텀 화면(검색조건 + 목록 + 회사 표시)으로
    바뀌었다. `UsersPage`의 생성/수정 폼에도 동일하게 소속 회사 읽기전용 필드를 추가했다
    (이전엔 화면 상단 설명글에만 있었음). ⚠️ "관리자 입장에서 여러 회사가 동시에 보이면
    관리가 힘들 것 같다"는 우려가 있었는데, 이 시스템은 Hibernate 레벨에서 테넌트(회사)
    격리를 강제하므로 로그인한 관리자는 애초에 자기 회사 데이터만 볼 수 있다(SYSTEM_ADMIN도
    로그인 시점의 회사로 고정) — 여러 회사가 동시에 섞여 보이는 상황 자체가 구조상
    발생하지 않아서, 회사 필터 UI는 따로 만들지 않고 항상 고정값으로 표시만 했다.
39. ~~`CrudPage`에 검색조건 + 참조 컬럼(ID→이름) 표시 기능 일반화~~ — 완료. 어플리케이션
    관리에 검색조건(포탈/사용여부) 추가 + 목록의 포탈 컬럼을 ID 대신 포탈명으로 표시.
    `CrudPage`의 `search`/`columns.resolveApi` 프롭으로 일반화해서 Company/Role/Portal/
    DataService/MessageResource/Menu(커스텀) 화면에도 같은 패턴으로 검색조건을 넣었다.
    `select` 필드에 정적 `options`도 지원하게 돼서 Portal의 디바이스유형, DataService의
    HTTP Method, MessageResource의 유형/언어도 텍스트 입력 대신 드롭다운으로 바뀌었다.
40. ~~폼의 select 박스 높이가 input과 안 맞던 디자인 문제 수정~~ — 완료. `.field`
    안의 `<select>`에 스타일이 아예 없어서 브라우저 기본 높이로 렌더링되던 게 원인이었다
    (`.field input`만 있고 `.field select`가 없었음). input과 동일한 padding/border/height로
    맞추고, 비활성화(readonly) 필드에도 시각적으로 구분되는 회색 스타일을 추가했다.
41. ~~Portal Management 사이드바 순서를 페이지/포탈/어플리케이션/메뉴 순으로 변경 +
    메뉴 관리를 트리 구조로 재구성~~ — 완료. 사진 속 이미지에서 "폴더"라고 부르는 게 우리가
    만든 `Menu` 엔티티다 — `Menu`가 `parentMenuId`로 스스로를 참조해서 폴더처럼 중첩되는
    구조라 트리로 표현하기 자연스러웠다. (이 항목의 최초 버전은 `Menu.pageId`로 폴더 하나에
    페이지 하나만 매달 수 있었는데, 바로 다음 항목에서 N:M으로 바뀌었다.)
42. ~~메뉴(폴더)-페이지를 1:N에서 N:M 매핑 구조로 전환 + 메뉴 관리 화면을 좌우 분할
    레이아웃(왼쪽 페이지 검색/체크, 오른쪽 포탈>어플리케이션 선택 + 폴더>페이지 트리)으로
    재구성~~ — 완료. 하나의 폴더에 여러 페이지를, 같은 페이지를 여러 폴더에 동시에 놓아야
    하는 요구사항에 따라 `Menu.pageId` 필드를 없애고 `MenuPage` 매핑 테이블(`PageRole`과
    동일한 하드 삭제 N:M 패턴)을 중간에 뒀다. `MenuDto.TreeNode.pageId`(단일)도
    `pageIds`(목록)로 바뀌었고, 인가 필터링 동작도 자연스럽게 바뀌었다 — 폴더 자체는 항상
    트리에 남고 매핑된 페이지 중 접근 불가능한 것만 목록에서 빠진다(예전엔 서브트리 전체가
    빠졌음). 프론트는 오른쪽에서 폴더를 선택하고 왼쪽에서 페이지를 체크한 뒤 저장하면
    체크한 페이지들이 선택된 폴더 하위에 매핑되는 흐름으로 다시 만들었다.
43. ~~페이지 관리의 용도(일반/팝업)·디바이스유형(PC/MOBILE/ALL)을 공통코드 연동으로 관리~~ — 완료.
    `PageCodeBootstrapListener`가 테넌트 생성 시 `PAGE_TYPE`/`PAGE_DEVICE_TYPE` 코드그룹을
    자동 시딩하고(`DevDataSeeder`도 같은 메서드를 재사용), 프론트 `PagesPage.jsx`는 하드코딩된
    텍스트 입력 대신 공통코드에서 불러온 셀렉트로 입력받는다. 목록/상세에는 저장된 코드값
    (`"MOBILE"`)이 아니라 코드명(`"모바일"`)이 보이고, 검색조건에도 용도/디바이스유형 필터를
    추가했다. 코드 로딩 로직은 `frontend/src/api/codeOptions.js`(`loadCodeOptions()`/`codeLabel()`)로
    빼서 `NoticesPage.jsx`의 팝업 설정 로딩과 공유한다 — 같은 로직이 화면마다 복사되는 걸 막았다.
44. ~~javac 소스 인코딩을 UTF-8로 고정~~ — 완료. 플랫폼 기본 인코딩(한국어 Windows에서 CP949)에
    의존하고 있어서 한글 주석이 든 새 파일을 추가하면 빌드가 깨질 수 있었다(위 "빌드/테스트" 참고).
45. ~~사용자그룹 관리의 분류(컨텐츠관리/시스템관리/데이터입력관리)를 공통코드 연동으로 관리~~ —
    완료. `UserGroupCodeBootstrapListener`가 테넌트 생성 시 `USER_GROUP_CATEGORY` 코드그룹을
    자동 시딩하고(`DevDataSeeder`도 같은 메서드를 재사용), 프론트 `UserGroupsPage.jsx`는
    하드코딩된 텍스트 입력 대신 공통코드에서 불러온 셀렉트로 입력받는다. 검색조건에도 분류
    필터를 추가했다. 기존 `DEFAULT_USER`/`ADMIN_GROUP` 시드 데이터의 `category` 값("시스템")도
    실제 공통코드 값("시스템관리")으로 맞췄다.
46. Flyway 도입, BackOffice 레이아웃 다듬기 등 이전부터 남아있던 후속 과제들
