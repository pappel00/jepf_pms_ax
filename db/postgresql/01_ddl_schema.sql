-- =====================================================================
-- Java Enterprise Platform Framework (JEPF) — PostgreSQL DDL
-- =====================================================================
-- 이 스크립트는 지금까지 만든 29개 엔티티(module-tenant 2개, module-user 6개,
-- module-portal 10개, module-code 2개, module-file 2개, module-terms 2개, module-notice 1개,
-- module-privacy 2개, module-form 2개)를 실제 자바 엔티티 정의(@Column length/nullable,
-- @UniqueConstraint)와 1:1로 맞춰서 작성했다. Hibernate ddl-auto: update 를 대체하기 위한 목적이다.
--
-- ⚠️ 이 프로젝트 전반의 설계 원칙: 모든 엔티티 간 참조는 진짜 FK가 아니라 "느슨한 문자열
-- 참조 컬럼"이다(N+1/캐스케이드 복잡도 회피, 모듈 간 순환 의존 회피 — 예: PageRole.role_code는
-- Role 테이블을 FK로 참조하지 않는다. module-portal이 module-user에 의존하지 않기 때문이다).
-- 그래서 이 스크립트에는 FOREIGN KEY 제약이 하나도 없다 — 참조 무결성은 애플리케이션
-- 레이어(서비스 코드의 findByIdAndDeletedYn 검증)에서만 보장된다. 실수가 아니라 의도다.
--
-- 공통 컬럼(BaseEntity): id, created_by, created_date, modified_by, modified_date,
--   version, deleted_yn, deleted_date
-- 공통 컬럼(TenantAwareEntity, BaseEntity 포함): 위 + tenant_id
-- (tnt_company 만 예외 — 테넌트 자신을 정의하는 주체라 tenant_id 가 없다)
--
-- id/tenant_id/user_id 등은 전부 애플리케이션(PrefixedIdGenerator)에서 생성하는 문자열이라
-- DB 시퀀스/SERIAL 을 쓰지 않는다.
--
-- ⚠️ 이 스크립트는 반드시 jepf 계정으로 접속한 상태에서 실행해야 한다 (postgres 관리자 계정으로
-- 실행하면 테이블 소유자가 postgres가 되어, jepf(애플리케이션이 실제로 접속하는 계정)가 이
-- 테이블들에 접근할 권한이 없어서 "테이블에 대한 접근 권한 없음" 에러가 난다 — 실제로 겪은 문제,
-- 00b_fix_table_ownership.sql 참고). DBeaver에서 jepf 계정으로 새 연결을 만들어 그걸로 실행할 것.
-- =====================================================================


-- =====================================================================
-- module-tenant
-- =====================================================================

-- Company: 테넌트 마스터. TenantAwareEntity 를 상속하지 않는다(테넌트를 "정의"하는 주체이므로
-- 그 자신은 테넌트에 속하지 않는다) — 그래서 tenant_id 컬럼이 없다.
CREATE TABLE tnt_company (
    id              VARCHAR(64)   NOT NULL,
    created_by      VARCHAR(64),
    created_date    TIMESTAMP,
    modified_by     VARCHAR(64),
    modified_date   TIMESTAMP,
    version         BIGINT,
    deleted_yn      VARCHAR(1)    NOT NULL,
    deleted_date    TIMESTAMP,
    company_code    VARCHAR(30)   NOT NULL,
    company_name    VARCHAR(200)  NOT NULL,
    description     VARCHAR(500),
    use_yn          VARCHAR(1)    NOT NULL,
    CONSTRAINT pk_tnt_company PRIMARY KEY (id),
    CONSTRAINT uk_company_code UNIQUE (company_code)
);

-- Department: 부서. parent_department_id 는 자기참조지만 진짜 FK 아님(트리 구조,
-- 빈 문자열은 절대 저장되지 않는다 — Department.java 의 normalize() 참고).
CREATE TABLE tnt_department (
    id                      VARCHAR(64)   NOT NULL,
    tenant_id               VARCHAR(64)   NOT NULL,
    created_by              VARCHAR(64),
    created_date            TIMESTAMP,
    modified_by             VARCHAR(64),
    modified_date           TIMESTAMP,
    version                 BIGINT,
    deleted_yn              VARCHAR(1)    NOT NULL,
    deleted_date            TIMESTAMP,
    department_code         VARCHAR(30)   NOT NULL,
    department_name         VARCHAR(200)  NOT NULL,
    parent_department_id    VARCHAR(64),
    use_yn                  VARCHAR(1)    NOT NULL,
    CONSTRAINT pk_tnt_department PRIMARY KEY (id)
);
CREATE INDEX idx_department_tenant ON tnt_department (tenant_id);
CREATE INDEX idx_department_parent ON tnt_department (parent_department_id);


-- =====================================================================
-- module-user
-- =====================================================================

-- User: 더 이상 역할을 직접 갖지 않는다(Role -> UserGroup -> User 구조, usr_user_group_role/
-- usr_user_group_user 참고).
CREATE TABLE usr_user (
    id              VARCHAR(64)   NOT NULL,
    tenant_id       VARCHAR(64)   NOT NULL,
    created_by      VARCHAR(64),
    created_date    TIMESTAMP,
    modified_by     VARCHAR(64),
    modified_date   TIMESTAMP,
    version         BIGINT,
    deleted_yn      VARCHAR(1)    NOT NULL,
    deleted_date    TIMESTAMP,
    login_id        VARCHAR(100)  NOT NULL,
    password_hash   VARCHAR(200)  NOT NULL,
    email           VARCHAR(200),
    user_name       VARCHAR(100)  NOT NULL,
    department_id   VARCHAR(64),
    use_yn          VARCHAR(1)    NOT NULL,
    CONSTRAINT pk_usr_user PRIMARY KEY (id),
    CONSTRAINT uk_user_tenant_login UNIQUE (tenant_id, login_id)
);
CREATE INDEX idx_user_tenant ON usr_user (tenant_id);
CREATE INDEX idx_user_department ON usr_user (department_id);

-- Role: 테넌트별 역할 마스터. role_code 는 User가 실제로 갖는 GrantedAuthority 문자열과
-- 정확히 일치해야 하는 값이다(예: "ROLE_ADMIN").
CREATE TABLE usr_role (
    id              VARCHAR(64)   NOT NULL,
    tenant_id       VARCHAR(64)   NOT NULL,
    created_by      VARCHAR(64),
    created_date    TIMESTAMP,
    modified_by     VARCHAR(64),
    modified_date   TIMESTAMP,
    version         BIGINT,
    deleted_yn      VARCHAR(1)    NOT NULL,
    deleted_date    TIMESTAMP,
    role_code       VARCHAR(50)   NOT NULL,
    role_name       VARCHAR(100)  NOT NULL,
    role_alias      VARCHAR(50),
    category        VARCHAR(50),
    description     VARCHAR(500),
    use_yn          VARCHAR(1)    NOT NULL,
    CONSTRAINT pk_usr_role PRIMARY KEY (id),
    CONSTRAINT uk_role_tenant_code UNIQUE (tenant_id, role_code)
);
CREATE INDEX idx_role_tenant ON usr_role (tenant_id);

-- RefreshToken: token_hash 는 원문이 아니라 SHA-256 해시(비밀번호 해시와 동일한 이유).
CREATE TABLE usr_refresh_token (
    id              VARCHAR(64)   NOT NULL,
    tenant_id       VARCHAR(64)   NOT NULL,
    created_by      VARCHAR(64),
    created_date    TIMESTAMP,
    modified_by     VARCHAR(64),
    modified_date   TIMESTAMP,
    version         BIGINT,
    deleted_yn      VARCHAR(1)    NOT NULL,
    deleted_date    TIMESTAMP,
    user_id         VARCHAR(64)   NOT NULL,
    token_hash      VARCHAR(64)   NOT NULL,
    expires_at      TIMESTAMP     NOT NULL,
    revoked_yn      VARCHAR(1)    NOT NULL,
    CONSTRAINT pk_usr_refresh_token PRIMARY KEY (id),
    CONSTRAINT uk_refresh_token_hash UNIQUE (token_hash)
);
CREATE INDEX idx_refresh_token_tenant ON usr_refresh_token (tenant_id);
CREATE INDEX idx_refresh_token_user ON usr_refresh_token (user_id);

-- UserGroup: 사용자그룹. default_yn="Y" 인 그룹이 있으면 회원가입 시 자동 배정된다
-- (TenantBootstrapListener/AuthService.register() 참고).
CREATE TABLE usr_user_group (
    id              VARCHAR(64)   NOT NULL,
    tenant_id       VARCHAR(64)   NOT NULL,
    created_by      VARCHAR(64),
    created_date    TIMESTAMP,
    modified_by     VARCHAR(64),
    modified_date   TIMESTAMP,
    version         BIGINT,
    deleted_yn      VARCHAR(1)    NOT NULL,
    deleted_date    TIMESTAMP,
    group_code      VARCHAR(30)   NOT NULL,
    group_name      VARCHAR(200)  NOT NULL,
    -- category(분류: 컨텐츠관리/시스템관리/데이터입력관리)의 유효한 값 목록은 스키마로
    -- 강제하지 않는다(CHECK 제약 없음, 길이 제약만) — module-code의 공통코드
    -- (USER_GROUP_CATEGORY 코드그룹, UserGroupCodeBootstrapListener가 테넌트 생성 시 자동
    -- 시딩)에서 관리하기 때문이다. Page.pageType/deviceType과 동일한 패턴이다.
    category        VARCHAR(50),
    portal_id       VARCHAR(64),
    default_yn      VARCHAR(1)    NOT NULL,
    use_yn          VARCHAR(1)    NOT NULL,
    CONSTRAINT pk_usr_user_group PRIMARY KEY (id),
    CONSTRAINT uk_ugrp_tenant_code UNIQUE (tenant_id, group_code)
);
CREATE INDEX idx_user_group_tenant ON usr_user_group (tenant_id);
CREATE INDEX idx_user_group_default ON usr_user_group (tenant_id, default_yn);

-- UserGroupRole: UserGroup - Role 매핑(N:M). 순수 매핑이라 하드 삭제로 관리된다
-- (deleted_yn 컬럼은 BaseEntity 상속으로 존재하지만 실제로는 사용하지 않는다).
CREATE TABLE usr_user_group_role (
    id              VARCHAR(64)   NOT NULL,
    tenant_id       VARCHAR(64)   NOT NULL,
    created_by      VARCHAR(64),
    created_date    TIMESTAMP,
    modified_by     VARCHAR(64),
    modified_date   TIMESTAMP,
    version         BIGINT,
    deleted_yn      VARCHAR(1)    NOT NULL,
    deleted_date    TIMESTAMP,
    user_group_id   VARCHAR(64)   NOT NULL,
    role_code       VARCHAR(50)   NOT NULL,
    CONSTRAINT pk_usr_user_group_role PRIMARY KEY (id),
    CONSTRAINT uk_ugr_tenant_group_role UNIQUE (tenant_id, user_group_id, role_code)
);
CREATE INDEX idx_ugr_group ON usr_user_group_role (user_group_id);
CREATE INDEX idx_ugr_role_code ON usr_user_group_role (role_code);

-- UserGroupUser: UserGroup - User 매핑(N:M). 순수 매핑이라 하드 삭제로 관리된다.
CREATE TABLE usr_user_group_user (
    id              VARCHAR(64)   NOT NULL,
    tenant_id       VARCHAR(64)   NOT NULL,
    created_by      VARCHAR(64),
    created_date    TIMESTAMP,
    modified_by     VARCHAR(64),
    modified_date   TIMESTAMP,
    version         BIGINT,
    deleted_yn      VARCHAR(1)    NOT NULL,
    deleted_date    TIMESTAMP,
    user_group_id   VARCHAR(64)   NOT NULL,
    user_id         VARCHAR(64)   NOT NULL,
    CONSTRAINT pk_usr_user_group_user PRIMARY KEY (id),
    CONSTRAINT uk_ugu_tenant_group_user UNIQUE (tenant_id, user_group_id, user_id)
);
CREATE INDEX idx_ugu_group ON usr_user_group_user (user_group_id);
CREATE INDEX idx_ugu_user ON usr_user_group_user (user_id);


-- =====================================================================
-- module-portal
-- =====================================================================

CREATE TABLE prt_portal (
    id              VARCHAR(64)   NOT NULL,
    tenant_id       VARCHAR(64)   NOT NULL,
    created_by      VARCHAR(64),
    created_date    TIMESTAMP,
    modified_by     VARCHAR(64),
    modified_date   TIMESTAMP,
    version         BIGINT,
    deleted_yn      VARCHAR(1)    NOT NULL,
    deleted_date    TIMESTAMP,
    portal_code     VARCHAR(30)   NOT NULL,
    portal_name     VARCHAR(200)  NOT NULL,
    device_type     VARCHAR(10)   NOT NULL,
    description     VARCHAR(500),
    use_yn          VARCHAR(1)    NOT NULL,
    CONSTRAINT pk_prt_portal PRIMARY KEY (id),
    CONSTRAINT uk_portal_tenant_code UNIQUE (tenant_id, portal_code)
);
CREATE INDEX idx_portal_tenant ON prt_portal (tenant_id);

CREATE TABLE prt_application (
    id                  VARCHAR(64)   NOT NULL,
    tenant_id           VARCHAR(64)   NOT NULL,
    created_by          VARCHAR(64),
    created_date        TIMESTAMP,
    modified_by         VARCHAR(64),
    modified_date       TIMESTAMP,
    version             BIGINT,
    deleted_yn          VARCHAR(1)    NOT NULL,
    deleted_date        TIMESTAMP,
    application_code    VARCHAR(30)   NOT NULL,
    application_name    VARCHAR(200)  NOT NULL,
    portal_id           VARCHAR(64)   NOT NULL,
    -- 운영 화면 셸(어플리케이션 선택 UI)에서의 노출 순서. Menu.sort_order와 동일한 패턴.
    sort_order          INTEGER       NOT NULL DEFAULT 0,
    use_yn              VARCHAR(1)    NOT NULL,
    CONSTRAINT pk_prt_application PRIMARY KEY (id),
    CONSTRAINT uk_app_tenant_code UNIQUE (tenant_id, application_code)
);
CREATE INDEX idx_application_tenant ON prt_application (tenant_id);
CREATE INDEX idx_application_portal ON prt_application (portal_id);

-- ApplicationRole: Application - Role 매핑(어플리케이션 1 : Role N). prt_page_role과 동일한
-- 패턴 — 순수 매핑이라 하드 삭제로 관리된다(deleted_yn 컬럼은 BaseEntity 상속으로 존재하지만
-- 실제로는 사용하지 않는다). role_code는 Role 엔티티의 PK가 아니라 GrantedAuthority 문자열
-- (예: "ROLE_ADMIN")과 정확히 일치해야 하는 값이다.
CREATE TABLE prt_application_role (
    id              VARCHAR(64)   NOT NULL,
    tenant_id       VARCHAR(64)   NOT NULL,
    created_by      VARCHAR(64),
    created_date    TIMESTAMP,
    modified_by     VARCHAR(64),
    modified_date   TIMESTAMP,
    version         BIGINT,
    deleted_yn      VARCHAR(1)    NOT NULL,
    deleted_date    TIMESTAMP,
    application_id  VARCHAR(64)   NOT NULL,
    role_code       VARCHAR(50)   NOT NULL,
    CONSTRAINT pk_prt_application_role PRIMARY KEY (id),
    CONSTRAINT uk_apr_tenant_app_role UNIQUE (tenant_id, application_id, role_code)
);
CREATE INDEX idx_application_role_app ON prt_application_role (application_id);
CREATE INDEX idx_application_role_code ON prt_application_role (role_code);

CREATE TABLE prt_page (
    id                        VARCHAR(64)   NOT NULL,
    tenant_id                 VARCHAR(64)   NOT NULL,
    created_by                VARCHAR(64),
    created_date              TIMESTAMP,
    modified_by               VARCHAR(64),
    modified_date             TIMESTAMP,
    version                   BIGINT,
    deleted_yn                VARCHAR(1)    NOT NULL,
    deleted_date              TIMESTAMP,
    page_code                 VARCHAR(30)   NOT NULL,
    page_name                 VARCHAR(200)  NOT NULL,
    -- page_type(용도: 일반/팝업)과 device_type(디바이스유형: PC/MOBILE/ALL)의 유효한 값 목록은
    -- 스키마로 강제하지 않는다(CHECK 제약 없음, 길이 제약만) — cmn_notice의 팝업 상세 설정과
    -- 동일하게 module-code의 공통코드(PAGE_TYPE / PAGE_DEVICE_TYPE 코드그룹,
    -- PageCodeBootstrapListener가 테넌트 생성 시 자동 시딩)에서 관리하기 때문이다.
    -- 저장되는 값은 CodeDetail.code이고, 화면에는 CodeDetail.code_name이 표시된다.
    page_type                 VARCHAR(20)   NOT NULL,
    device_type                VARCHAR(10)   NOT NULL,
    page_file                 VARCHAR(500),
    message_resource_code     VARCHAR(100),
    use_yn                    VARCHAR(1)    NOT NULL,
    CONSTRAINT pk_prt_page PRIMARY KEY (id),
    CONSTRAINT uk_page_tenant_code UNIQUE (tenant_id, page_code)
);
CREATE INDEX idx_page_tenant ON prt_page (tenant_id);

-- ⚠️ page_id 컬럼이 없다 — 메뉴(폴더)와 페이지는 1:N이 아니라 N:M 관계로 바뀌어서
-- prt_menu_page 매핑 테이블로 관리한다(아래 참고). 예전 스키마로 서버를 띄운 적이 있다면
-- page_id 컬럼이 아직 테이블에 남아있을 수 있다 — 지워야 한다(ddl-auto: update는 없어진
-- 컬럼을 절대 안 지운다는 걸 이미 겪었다):
--   ALTER TABLE prt_menu DROP COLUMN page_id;
CREATE TABLE prt_menu (
    id                  VARCHAR(64)   NOT NULL,
    tenant_id           VARCHAR(64)   NOT NULL,
    created_by          VARCHAR(64),
    created_date        TIMESTAMP,
    modified_by         VARCHAR(64),
    modified_date       TIMESTAMP,
    version             BIGINT,
    deleted_yn          VARCHAR(1)    NOT NULL,
    deleted_date        TIMESTAMP,
    menu_code           VARCHAR(30)   NOT NULL,
    menu_name           VARCHAR(200)  NOT NULL,
    application_id      VARCHAR(64)   NOT NULL,
    parent_menu_id      VARCHAR(64),
    sort_order          INTEGER       NOT NULL,
    link_url            VARCHAR(500),
    use_yn              VARCHAR(1)    NOT NULL,
    CONSTRAINT pk_prt_menu PRIMARY KEY (id),
    CONSTRAINT uk_menu_tenant_code UNIQUE (tenant_id, menu_code)
);
CREATE INDEX idx_menu_tenant ON prt_menu (tenant_id);
CREATE INDEX idx_menu_application ON prt_menu (application_id);
CREATE INDEX idx_menu_parent ON prt_menu (parent_menu_id);

CREATE TABLE prt_data_service (
    id                    VARCHAR(64)   NOT NULL,
    tenant_id             VARCHAR(64)   NOT NULL,
    created_by            VARCHAR(64),
    created_date          TIMESTAMP,
    modified_by           VARCHAR(64),
    modified_date         TIMESTAMP,
    version               BIGINT,
    deleted_yn            VARCHAR(1)    NOT NULL,
    deleted_date          TIMESTAMP,
    data_service_code     VARCHAR(30)   NOT NULL,
    data_service_name     VARCHAR(200)  NOT NULL,
    http_method           VARCHAR(10)   NOT NULL,
    url                   VARCHAR(500)  NOT NULL,
    use_yn                VARCHAR(1)    NOT NULL,
    CONSTRAINT pk_prt_data_service PRIMARY KEY (id),
    CONSTRAINT uk_dsvc_tenant_code UNIQUE (tenant_id, data_service_code)
);
CREATE INDEX idx_data_service_tenant ON prt_data_service (tenant_id);

-- MessageResource: 같은 message_resource_code 가 언어별로 여러 행을 가진다.
-- ⚠️ "value" 는 PostgreSQL 예약어가 아니지만(H2는 예약어라서 message_value로 바꿨었다),
-- 자바 필드명과 일치시키기 위해 그대로 message_value 컬럼명을 유지한다.
CREATE TABLE prt_message_resource (
    id                        VARCHAR(64)    NOT NULL,
    tenant_id                 VARCHAR(64)    NOT NULL,
    created_by                VARCHAR(64),
    created_date              TIMESTAMP,
    modified_by                VARCHAR(64),
    modified_date             TIMESTAMP,
    version                   BIGINT,
    deleted_yn                VARCHAR(1)     NOT NULL,
    deleted_date              TIMESTAMP,
    message_resource_code     VARCHAR(100)   NOT NULL,
    resource_type             VARCHAR(20)    NOT NULL,
    language_id                VARCHAR(10)    NOT NULL,
    message_value              VARCHAR(2000)  NOT NULL,
    default_value              VARCHAR(2000),
    use_yn                    VARCHAR(1)     NOT NULL,
    CONSTRAINT pk_prt_message_resource PRIMARY KEY (id),
    CONSTRAINT uk_msgres_tenant_code_lang UNIQUE (tenant_id, message_resource_code, language_id)
);
CREATE INDEX idx_message_resource_tenant ON prt_message_resource (tenant_id);
CREATE INDEX idx_message_resource_code ON prt_message_resource (tenant_id, message_resource_code);

-- PageDataService / PageMessageResource / PageRole: 순수 매핑(N:M) 테이블. 하드 삭제로 관리된다.
CREATE TABLE prt_page_data_service (
    id                  VARCHAR(64)   NOT NULL,
    tenant_id           VARCHAR(64)   NOT NULL,
    created_by          VARCHAR(64),
    created_date        TIMESTAMP,
    modified_by         VARCHAR(64),
    modified_date       TIMESTAMP,
    version             BIGINT,
    deleted_yn          VARCHAR(1)    NOT NULL,
    deleted_date        TIMESTAMP,
    page_id             VARCHAR(64)   NOT NULL,
    data_service_id     VARCHAR(64)   NOT NULL,
    CONSTRAINT pk_prt_page_data_service PRIMARY KEY (id),
    CONSTRAINT uk_pds_tenant_page_dsvc UNIQUE (tenant_id, page_id, data_service_id)
);
CREATE INDEX idx_pds_page ON prt_page_data_service (page_id);

CREATE TABLE prt_page_message_resource (
    id                        VARCHAR(64)   NOT NULL,
    tenant_id                 VARCHAR(64)   NOT NULL,
    created_by                VARCHAR(64),
    created_date              TIMESTAMP,
    modified_by               VARCHAR(64),
    modified_date             TIMESTAMP,
    version                   BIGINT,
    deleted_yn                VARCHAR(1)    NOT NULL,
    deleted_date              TIMESTAMP,
    page_id                   VARCHAR(64)   NOT NULL,
    message_resource_code     VARCHAR(100)  NOT NULL,
    CONSTRAINT pk_prt_page_message_resource PRIMARY KEY (id),
    CONSTRAINT uk_pmr_tenant_page_code UNIQUE (tenant_id, page_id, message_resource_code)
);
CREATE INDEX idx_pmr_page ON prt_page_message_resource (page_id);

CREATE TABLE prt_page_role (
    id              VARCHAR(64)   NOT NULL,
    tenant_id       VARCHAR(64)   NOT NULL,
    created_by      VARCHAR(64),
    created_date    TIMESTAMP,
    modified_by     VARCHAR(64),
    modified_date   TIMESTAMP,
    version         BIGINT,
    deleted_yn      VARCHAR(1)    NOT NULL,
    deleted_date    TIMESTAMP,
    page_id         VARCHAR(64)   NOT NULL,
    role_code       VARCHAR(50)   NOT NULL,
    CONSTRAINT pk_prt_page_role PRIMARY KEY (id),
    CONSTRAINT uk_pr_tenant_page_role UNIQUE (tenant_id, page_id, role_code)
);
CREATE INDEX idx_page_role_page ON prt_page_role (page_id);
CREATE INDEX idx_page_role_code ON prt_page_role (role_code);

-- Menu(폴더) - Page 매핑(N:M). 사진 속 "메뉴 관리" 화면에서 왼쪽 페이지를 체크해서 오른쪽
-- 선택된 폴더 하위로 저장하는 동작이 이 테이블에 행을 추가하는 것이다. sort_order로 같은
-- 폴더 안에서 페이지들의 노출 순서를 정한다.
CREATE TABLE prt_menu_page (
    id              VARCHAR(64)   NOT NULL,
    tenant_id       VARCHAR(64)   NOT NULL,
    created_by      VARCHAR(64),
    created_date    TIMESTAMP,
    modified_by     VARCHAR(64),
    modified_date   TIMESTAMP,
    version         BIGINT,
    deleted_yn      VARCHAR(1)    NOT NULL,
    deleted_date    TIMESTAMP,
    menu_id         VARCHAR(64)   NOT NULL,
    page_id         VARCHAR(64)   NOT NULL,
    sort_order      INTEGER       NOT NULL,
    CONSTRAINT pk_prt_menu_page PRIMARY KEY (id),
    CONSTRAINT uk_mp_tenant_menu_page UNIQUE (tenant_id, menu_id, page_id)
);
CREATE INDEX idx_menu_page_menu ON prt_menu_page (menu_id);
CREATE INDEX idx_menu_page_page ON prt_menu_page (page_id);


-- =====================================================================
-- module-code (Data Management > 코드 관리)
-- ⚠️ module-code/module-file/module-terms는 이 DDL 스크립트가 처음 작성된 이후에 추가된
-- 모듈이라 뒤늦게 여기 붙였다 — 실수로 빠뜨렸던 것을 발견해서 보강함.
-- =====================================================================

CREATE TABLE cmn_code_group (
    id              VARCHAR(64)   NOT NULL,
    tenant_id       VARCHAR(64)   NOT NULL,
    created_by      VARCHAR(64),
    created_date    TIMESTAMP,
    modified_by     VARCHAR(64),
    modified_date   TIMESTAMP,
    version         BIGINT,
    deleted_yn      VARCHAR(1)    NOT NULL,
    deleted_date    TIMESTAMP,
    group_code      VARCHAR(50)   NOT NULL,
    group_name      VARCHAR(200)  NOT NULL,
    description     VARCHAR(500),
    use_yn          VARCHAR(1)    NOT NULL,
    CONSTRAINT pk_cmn_code_group PRIMARY KEY (id),
    CONSTRAINT uk_code_group_tenant_code UNIQUE (tenant_id, group_code)
);
CREATE INDEX idx_code_group_tenant ON cmn_code_group (tenant_id);

CREATE TABLE cmn_code_detail (
    id              VARCHAR(64)   NOT NULL,
    tenant_id       VARCHAR(64)   NOT NULL,
    created_by      VARCHAR(64),
    created_date    TIMESTAMP,
    modified_by     VARCHAR(64),
    modified_date   TIMESTAMP,
    version         BIGINT,
    deleted_yn      VARCHAR(1)    NOT NULL,
    deleted_date    TIMESTAMP,
    group_id        VARCHAR(64)   NOT NULL,
    code            VARCHAR(50)   NOT NULL,
    code_name       VARCHAR(200)  NOT NULL,
    sort_order      INTEGER       NOT NULL,
    description     VARCHAR(500),
    use_yn          VARCHAR(1)    NOT NULL,
    CONSTRAINT pk_cmn_code_detail PRIMARY KEY (id),
    CONSTRAINT uk_code_detail_tenant_group_code UNIQUE (tenant_id, group_id, code)
);
CREATE INDEX idx_code_detail_group ON cmn_code_detail (group_id);


-- =====================================================================
-- module-file (Data Management > 첨부파일 그룹 관리)
-- =====================================================================

CREATE TABLE cmn_file_group (
    id              VARCHAR(64)   NOT NULL,
    tenant_id       VARCHAR(64)   NOT NULL,
    created_by      VARCHAR(64),
    created_date    TIMESTAMP,
    modified_by     VARCHAR(64),
    modified_date   TIMESTAMP,
    version         BIGINT,
    deleted_yn      VARCHAR(1)    NOT NULL,
    deleted_date    TIMESTAMP,
    group_code      VARCHAR(50)   NOT NULL,
    group_name      VARCHAR(200)  NOT NULL,
    description     VARCHAR(500),
    use_yn          VARCHAR(1)    NOT NULL,
    CONSTRAINT pk_cmn_file_group PRIMARY KEY (id),
    CONSTRAINT uk_file_group_tenant_code UNIQUE (tenant_id, group_code)
);
CREATE INDEX idx_file_group_tenant ON cmn_file_group (tenant_id);

-- file_path에는 FileStorageProperties.basePath 기준 상대경로만 저장된다(FileStorageService 참고).
CREATE TABLE cmn_file_item (
    id              VARCHAR(64)    NOT NULL,
    tenant_id       VARCHAR(64)    NOT NULL,
    created_by      VARCHAR(64),
    created_date    TIMESTAMP,
    modified_by     VARCHAR(64),
    modified_date   TIMESTAMP,
    version         BIGINT,
    deleted_yn      VARCHAR(1)     NOT NULL,
    deleted_date    TIMESTAMP,
    group_id        VARCHAR(64)    NOT NULL,
    file_name       VARCHAR(300)   NOT NULL,
    file_path       VARCHAR(1000)  NOT NULL,
    file_size       BIGINT,
    sort_order      INTEGER        NOT NULL,
    description     VARCHAR(500),
    use_yn          VARCHAR(1)     NOT NULL,
    CONSTRAINT pk_cmn_file_item PRIMARY KEY (id)
);
CREATE INDEX idx_file_item_group ON cmn_file_item (group_id);


-- =====================================================================
-- module-terms (Data Management > 이용약관 관리)
-- ⚠️ 이용약관은 포탈별로 관리된다 — portal_id가 필수이고, 유니크 제약이
-- (tenant_id, terms_code)가 아니라 (tenant_id, portal_id, terms_code)다. 같은 약관코드라도
-- 포탈이 다르면 별개의 약관으로 취급한다(TermsGroup 클래스 주석 참고). portal_id는 다른
-- 참조 컬럼들과 동일하게 진짜 FK가 아니라 느슨한 문자열 참조다(module-terms는 module-portal에
-- 의존하지 않는다).
-- =====================================================================

CREATE TABLE cmn_terms_group (
    id              VARCHAR(64)   NOT NULL,
    tenant_id       VARCHAR(64)   NOT NULL,
    created_by      VARCHAR(64),
    created_date    TIMESTAMP,
    modified_by     VARCHAR(64),
    modified_date   TIMESTAMP,
    version         BIGINT,
    deleted_yn      VARCHAR(1)    NOT NULL,
    deleted_date    TIMESTAMP,
    portal_id       VARCHAR(64)   NOT NULL,
    terms_code      VARCHAR(50)   NOT NULL,
    terms_name      VARCHAR(200)  NOT NULL,
    terms_type      VARCHAR(20)   NOT NULL,
    description     VARCHAR(500),
    use_yn          VARCHAR(1)    NOT NULL,
    CONSTRAINT pk_cmn_terms_group PRIMARY KEY (id),
    CONSTRAINT uk_terms_group_tenant_portal_code UNIQUE (tenant_id, portal_id, terms_code)
);
CREATE INDEX idx_terms_group_tenant ON cmn_terms_group (tenant_id);
CREATE INDEX idx_terms_group_portal ON cmn_terms_group (portal_id);

-- version_no로 이름 지었다 — BaseEntity가 이미 낙관적 락용 version(BIGINT) 컬럼을 갖고 있어서
-- 약관의 "버전"(문자열, 예: "1.0")을 그냥 version이라고 지으면 컬럼명이 충돌한다(실제로 겪음).
-- content는 TEXT로 뒀다 — 약관 본문 전체가 들어가는 긴 텍스트라 VARCHAR 길이 제한을 안 둔다.
CREATE TABLE cmn_terms_version (
    id                VARCHAR(64)   NOT NULL,
    tenant_id         VARCHAR(64)   NOT NULL,
    created_by        VARCHAR(64),
    created_date      TIMESTAMP,
    modified_by       VARCHAR(64),
    modified_date     TIMESTAMP,
    version           BIGINT,
    deleted_yn        VARCHAR(1)    NOT NULL,
    deleted_date      TIMESTAMP,
    group_id          VARCHAR(64)   NOT NULL,
    version_no        VARCHAR(30)   NOT NULL,
    content           TEXT          NOT NULL,
    effective_date    DATE          NOT NULL,
    description       VARCHAR(500),
    use_yn            VARCHAR(1)    NOT NULL,
    CONSTRAINT pk_cmn_terms_version PRIMARY KEY (id)
);
CREATE INDEX idx_terms_version_group ON cmn_terms_version (group_id);


-- =====================================================================
-- module-notice (Data Management > 공지/동의서 관리)
-- ⚠️ 기본+상세(버전 이력) 2단 구조가 아니라 단일 테이블이다 — 공지 하나하나가 제목/내용/
-- 게시기간을 통째로 갖는 완결된 항목이라 개정 이력을 추적할 필요가 없다고 판단했다(약관과
-- 다른 점). notice_type으로 "공지"와 "동의서"를 한 테이블에서 같이 관리한다.
-- 이용약관과 동일하게 포탈별로 관리된다(portal_id 필수, 다만 유니크 제약은 없다 — 같은
-- 포탈 안에서 제목이 같은 공지를 여러 개 올릴 수도 있으므로).
-- =====================================================================

CREATE TABLE cmn_notice (
    id                 VARCHAR(64)    NOT NULL,
    tenant_id          VARCHAR(64)    NOT NULL,
    created_by         VARCHAR(64),
    created_date       TIMESTAMP,
    modified_by        VARCHAR(64),
    modified_date      TIMESTAMP,
    version            BIGINT,
    deleted_yn         VARCHAR(1)     NOT NULL,
    deleted_date       TIMESTAMP,
    portal_id          VARCHAR(64)    NOT NULL,
    notice_type        VARCHAR(20)    NOT NULL,
    title              VARCHAR(300)   NOT NULL,
    content            TEXT           NOT NULL,
    start_date         DATE           NOT NULL,
    end_date           DATE           NOT NULL,
    popup_yn           VARCHAR(1)     NOT NULL,
    -- 아래 5개는 popupYn="Y"일 때 의미가 있는 팝업 상세 설정이다. 선택지 자체는 코드로
    -- 강제하지 않는다(NOT NULL 아님, 길이 제약만) — 실제 유효한 값 목록은 module-code의
    -- 공통코드(NOTICE_APPLY_TYPE/NOTICE_POPUP_FREQUENCY/NOTICE_POPUP_SIZE/NOTICE_POPUP_SHAPE/
    -- NOTICE_POPUP_POSITION, NoticeCodeBootstrapListener가 테넌트 생성 시 자동 시딩)에서 관리한다.
    apply_type         VARCHAR(20),
    popup_frequency    VARCHAR(20),
    popup_size         VARCHAR(20),
    popup_shape        VARCHAR(20),
    popup_position     VARCHAR(20),
    use_yn             VARCHAR(1)     NOT NULL,
    CONSTRAINT pk_cmn_notice PRIMARY KEY (id)
);
CREATE INDEX idx_notice_tenant ON cmn_notice (tenant_id);
CREATE INDEX idx_notice_portal ON cmn_notice (portal_id);


-- =====================================================================
-- module-privacy (Data Management > 개인정보처리방침 관리)
-- module-terms(이용약관)와 완전히 같은 구조다 — 개인정보처리방침도 시점별 개정 이력을
-- 추적해야 한다(수집 당시 어떤 버전이 적용됐는지 나중에 증빙해야 하는 경우가 많다는 게
-- 이용약관과 같은 이유). 포탈별로 관리되며(portal_id 필수), 유니크 제약이
-- (tenant_id, policy_code)가 아니라 (tenant_id, portal_id, policy_code)다.
-- =====================================================================

CREATE TABLE cmn_privacy_policy_group (
    id              VARCHAR(64)   NOT NULL,
    tenant_id       VARCHAR(64)   NOT NULL,
    created_by      VARCHAR(64),
    created_date    TIMESTAMP,
    modified_by     VARCHAR(64),
    modified_date   TIMESTAMP,
    version         BIGINT,
    deleted_yn      VARCHAR(1)    NOT NULL,
    deleted_date    TIMESTAMP,
    portal_id       VARCHAR(64)   NOT NULL,
    policy_code     VARCHAR(50)   NOT NULL,
    policy_name     VARCHAR(200)  NOT NULL,
    description     VARCHAR(500),
    use_yn          VARCHAR(1)    NOT NULL,
    CONSTRAINT pk_cmn_privacy_policy_group PRIMARY KEY (id),
    CONSTRAINT uk_privacy_group_tenant_portal_code UNIQUE (tenant_id, portal_id, policy_code)
);
CREATE INDEX idx_privacy_group_tenant ON cmn_privacy_policy_group (tenant_id);
CREATE INDEX idx_privacy_group_portal ON cmn_privacy_policy_group (portal_id);

-- version_no로 이름 지었다 — BaseEntity의 낙관적 락 version(BIGINT) 컬럼과 충돌 방지
-- (TermsVersion에서 실제로 겪었던 문제와 동일한 이유).
CREATE TABLE cmn_privacy_policy_version (
    id                VARCHAR(64)   NOT NULL,
    tenant_id         VARCHAR(64)   NOT NULL,
    created_by        VARCHAR(64),
    created_date      TIMESTAMP,
    modified_by       VARCHAR(64),
    modified_date     TIMESTAMP,
    version           BIGINT,
    deleted_yn        VARCHAR(1)    NOT NULL,
    deleted_date      TIMESTAMP,
    group_id          VARCHAR(64)   NOT NULL,
    version_no        VARCHAR(30)   NOT NULL,
    content           TEXT          NOT NULL,
    effective_date    DATE          NOT NULL,
    description       VARCHAR(500),
    use_yn            VARCHAR(1)    NOT NULL,
    CONSTRAINT pk_cmn_privacy_policy_version PRIMARY KEY (id)
);
CREATE INDEX idx_privacy_version_group ON cmn_privacy_policy_version (group_id);


-- =====================================================================
-- module-form (Data Management > 표준양식 관리)
-- module-terms/module-privacy와 완전히 같은 구조다 — 표준양식도 시점별 개정 이력을 추적해야
-- 한다. category(분류)는 약관/개인정보처리방침에는 없던 컬럼이다(인사/총무/재무 등 부서·업무
-- 분류용). 포탈별로 관리되며(portal_id 필수), 유니크 제약이 (tenant_id, portal_id, form_code)다.
-- =====================================================================

CREATE TABLE cmn_form_group (
    id              VARCHAR(64)   NOT NULL,
    tenant_id       VARCHAR(64)   NOT NULL,
    created_by      VARCHAR(64),
    created_date    TIMESTAMP,
    modified_by     VARCHAR(64),
    modified_date   TIMESTAMP,
    version         BIGINT,
    deleted_yn      VARCHAR(1)    NOT NULL,
    deleted_date    TIMESTAMP,
    portal_id       VARCHAR(64)   NOT NULL,
    form_code       VARCHAR(50)   NOT NULL,
    form_name       VARCHAR(200)  NOT NULL,
    category        VARCHAR(50),
    description     VARCHAR(500),
    use_yn          VARCHAR(1)    NOT NULL,
    CONSTRAINT pk_cmn_form_group PRIMARY KEY (id),
    CONSTRAINT uk_form_group_tenant_portal_code UNIQUE (tenant_id, portal_id, form_code)
);
CREATE INDEX idx_form_group_tenant ON cmn_form_group (tenant_id);
CREATE INDEX idx_form_group_portal ON cmn_form_group (portal_id);

-- version_no로 이름 지었다 — BaseEntity의 낙관적 락 version(BIGINT) 컬럼과 충돌 방지
-- (TermsVersion에서 실제로 겪었던 문제와 동일한 이유).
-- ⚠️ 텍스트(content)가 아니라 실제 엑셀/워드 파일로 관리한다 — file_path에는
-- FileStorageProperties.base-path 기준 상대경로만 저장된다(module-file의 FileStorageService를
-- 그대로 재사용, module-file 섹션 참고).
CREATE TABLE cmn_form_version (
    id                VARCHAR(64)    NOT NULL,
    tenant_id         VARCHAR(64)    NOT NULL,
    created_by        VARCHAR(64),
    created_date      TIMESTAMP,
    modified_by       VARCHAR(64),
    modified_date     TIMESTAMP,
    version           BIGINT,
    deleted_yn        VARCHAR(1)     NOT NULL,
    deleted_date      TIMESTAMP,
    group_id          VARCHAR(64)    NOT NULL,
    version_no        VARCHAR(30)    NOT NULL,
    file_name         VARCHAR(300)   NOT NULL,
    file_path         VARCHAR(1000)  NOT NULL,
    file_size         BIGINT,
    effective_date    DATE           NOT NULL,
    description       VARCHAR(500),
    use_yn            VARCHAR(1)     NOT NULL,
    CONSTRAINT pk_cmn_form_version PRIMARY KEY (id)
);
CREATE INDEX idx_form_version_group ON cmn_form_version (group_id);
