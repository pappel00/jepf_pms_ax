-- =====================================================================
-- Java Enterprise Platform Framework (JEPF) — PostgreSQL DML (기초 데이터)
-- =====================================================================
-- DevDataSeeder(app-sandbox, 로컬 전용)와 각 모듈의 TenantCreatedEvent 부트스트랩 리스너
-- (TenantBootstrapListener/NoticeCodeBootstrapListener/PageCodeBootstrapListener/
-- UserGroupCodeBootstrapListener/PortalBootstrapListener — 실제 POST /api/v1/companies 로
-- 새 테넌트가 생길 때마다 자동 실행)가 각각 실행됐을 때 만들어지는 데이터와 동일한 모양을
-- 정적 SQL로 옮긴 것이다. 실제 운영에서는 이 스크립트를 그대로 실행하는 게 아니라, 저 둘 중
-- 하나가 알아서 채워준다 — 이 파일은 "기초 데이터가 어떤 모양이어야 하는지" 참고/검증용이다.
--
-- ⚠️ id 컬럼들은 실제로는 PrefixedIdGenerator가 런타임에 무작위로 생성한다
-- (예: "TNT_" + UUID 32자리). 이 스크립트에서는 재현 가능하도록 고정값을 썼다 — 그대로
-- 실행해도 동작은 하지만, 실제 운영 데이터의 ID 형식과 다르다는 점을 감안해야 한다.
--
-- ⚠️ password_hash 값은 진짜 BCrypt 해시로 반드시 교체해야 한다. 아래 예시는
-- "admin1234"를 BCryptPasswordEncoder(strength=10)로 인코딩한 값이 아니다 — 그대로 쓰면
-- 로그인이 안 된다. Java에서 다음처럼 직접 생성해서 바꿔 넣어야 한다:
--   new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder().encode("admin1234")
-- =====================================================================

BEGIN;

-- ---------------------------------------------------------------------
-- 1) 회사(테넌트)
-- ---------------------------------------------------------------------
INSERT INTO tnt_company (id, created_by, created_date, modified_by, modified_date, version,
                          deleted_yn, deleted_date, company_code, company_name, description, use_yn)
VALUES ('TNT_00000000000000000000000000demo', 'SYSTEM', now(), 'SYSTEM', now(), 0,
        'N', NULL, 'DEMO', '데모 회사', '로컬 개발용 샘플 회사', 'Y');

-- ---------------------------------------------------------------------
-- 2) Role 마스터 — ROLE_USER/ROLE_ADMIN 은 TenantBootstrapListener가 모든 신규 테넌트에
--    자동으로 만들어준다. ROLE_SYSTEM_ADMIN 은 일부러 자동 시딩 대상에서 뺐다(테넌트를 새로
--    만들 수 있는 최상위 권한이라, 여기서는 로컬 샌드박스 테스트를 위해 수동으로 추가했다).
-- ---------------------------------------------------------------------
INSERT INTO usr_role (id, tenant_id, created_by, created_date, modified_by, modified_date, version,
                       deleted_yn, deleted_date, role_code, role_name, role_alias, category, description, use_yn)
VALUES
    ('ROL_00000000000000000000000user01', 'TNT_00000000000000000000000000demo', 'SYSTEM', now(), 'SYSTEM', now(), 0,
     'N', NULL, 'ROLE_USER', '일반 사용자', 'User', '시스템', '기본 로그인 사용자 권한. 회원가입 시 기본으로 부여됨.', 'Y'),
    ('ROL_00000000000000000000000admin1', 'TNT_00000000000000000000000000demo', 'SYSTEM', now(), 'SYSTEM', now(), 0,
     'N', NULL, 'ROLE_ADMIN', '테넌트 관리자', 'Admin', '시스템', '이 테넌트 내 부서/사용자/역할/포탈 등을 관리할 수 있는 권한.', 'Y'),
    ('ROL_0000000000000000000sysadmin1', 'TNT_00000000000000000000000000demo', 'SYSTEM', now(), 'SYSTEM', now(), 0,
     'N', NULL, 'ROLE_SYSTEM_ADMIN', '시스템 관리자', 'System Admin', '시스템', '회사(테넌트) 자체를 생성/관리할 수 있는 최상위 권한.', 'Y');

-- ---------------------------------------------------------------------
-- 3) 사용자그룹 — DEFAULT_USER(기본권한, 신규 가입자 자동 배정) / ADMIN_GROUP(관리자용)
--    category("시스템관리")는 아래 7)에서 시딩하는 USER_GROUP_CATEGORY 공통코드의 값 중
--    하나다(UserGroupCodeBootstrapListener/DevDataSeeder와 동일하게 맞췄다).
-- ---------------------------------------------------------------------
INSERT INTO usr_user_group (id, tenant_id, created_by, created_date, modified_by, modified_date, version,
                             deleted_yn, deleted_date, group_code, group_name, category, portal_id, default_yn, use_yn)
VALUES
    ('UGR_0000000000000000000default01', 'TNT_00000000000000000000000000demo', 'SYSTEM', now(), 'SYSTEM', now(), 0,
     'N', NULL, 'DEFAULT_USER', '기본권한 사용자', '시스템관리', NULL, 'Y', 'Y'),
    ('UGR_00000000000000000000admin001', 'TNT_00000000000000000000000000demo', 'SYSTEM', now(), 'SYSTEM', now(), 0,
     'N', NULL, 'ADMIN_GROUP', '시스템 관리자', '시스템관리', NULL, 'N', 'Y');

-- ---------------------------------------------------------------------
-- 4) 사용자그룹 - Role 매핑
--    DEFAULT_USER -> ROLE_USER
--    ADMIN_GROUP  -> ROLE_USER, ROLE_ADMIN, ROLE_SYSTEM_ADMIN
-- ---------------------------------------------------------------------
INSERT INTO usr_user_group_role (id, tenant_id, created_by, created_date, modified_by, modified_date, version,
                                  deleted_yn, deleted_date, user_group_id, role_code)
VALUES
    ('UGRL_000000000000000000000000001', 'TNT_00000000000000000000000000demo', 'SYSTEM', now(), 'SYSTEM', now(), 0,
     'N', NULL, 'UGR_0000000000000000000default01', 'ROLE_USER'),
    ('UGRL_000000000000000000000000002', 'TNT_00000000000000000000000000demo', 'SYSTEM', now(), 'SYSTEM', now(), 0,
     'N', NULL, 'UGR_00000000000000000000admin001', 'ROLE_USER'),
    ('UGRL_000000000000000000000000003', 'TNT_00000000000000000000000000demo', 'SYSTEM', now(), 'SYSTEM', now(), 0,
     'N', NULL, 'UGR_00000000000000000000admin001', 'ROLE_ADMIN'),
    ('UGRL_000000000000000000000000004', 'TNT_00000000000000000000000000demo', 'SYSTEM', now(), 'SYSTEM', now(), 0,
     'N', NULL, 'UGR_00000000000000000000admin001', 'ROLE_SYSTEM_ADMIN');

-- ---------------------------------------------------------------------
-- 5) 관리자 계정 (loginId=admin, password=admin1234)
--    ⚠️ password_hash 는 반드시 진짜 BCrypt 해시로 교체할 것 (파일 상단 주석 참고).
-- ---------------------------------------------------------------------
INSERT INTO usr_user (id, tenant_id, created_by, created_date, modified_by, modified_date, version,
                       deleted_yn, deleted_date, login_id, password_hash, email, user_name, use_yn)
VALUES ('USR_0000000000000000000000admin1', 'TNT_00000000000000000000000000demo', 'SYSTEM', now(), 'SYSTEM', now(), 0,
        'N', NULL, 'admin', '$2a$10$REPLACE_WITH_REAL_BCRYPT_HASH_FOR_admin1234xxxxxxxxxx',
        'admin@example.com', '관리자', 'Y');

-- ---------------------------------------------------------------------
-- 6) admin 계정을 ADMIN_GROUP 에 배정
-- ---------------------------------------------------------------------
INSERT INTO usr_user_group_user (id, tenant_id, created_by, created_date, modified_by, modified_date, version,
                                  deleted_yn, deleted_date, user_group_id, user_id)
VALUES ('UGU_0000000000000000000000000001', 'TNT_00000000000000000000000000demo', 'SYSTEM', now(), 'SYSTEM', now(), 0,
        'N', NULL, 'UGR_00000000000000000000admin001', 'USR_0000000000000000000000admin1');

-- ---------------------------------------------------------------------
-- 7) 화면 선택지용 공통코드
--    하드코딩된 선택지 대신 코드 관리 화면에서 추가/변경할 수 있게 공통코드로 관리하는
--    항목들이다. 실제로는 각 모듈의 부트스트랩 리스너가 테넌트 생성 시 자동으로 채운다:
--      - PAGE_TYPE / PAGE_DEVICE_TYPE            -> PageCodeBootstrapListener      (module-portal)
--      - NOTICE_* 5종(공지 팝업 상세 설정)        -> NoticeCodeBootstrapListener    (module-notice)
--      - USER_GROUP_CATEGORY(사용자그룹 분류)     -> UserGroupCodeBootstrapListener (module-user)
--    ⚠️ PAGE_DEVICE_TYPE 처럼 code(저장값)와 code_name(화면 표시명)이 다른 그룹이 있다.
--    prt_page.device_type 에는 code("MOBILE")가 저장되고 화면엔 code_name("모바일")이 보인다.
-- ---------------------------------------------------------------------
INSERT INTO cmn_code_group (id, tenant_id, created_by, created_date, modified_by, modified_date, version,
                             deleted_yn, deleted_date, group_code, group_name, description, use_yn)
VALUES
    ('CDG_000000000000000000000pagetype', 'TNT_00000000000000000000000000demo', 'SYSTEM', now(), 'SYSTEM', now(), 0,
     'N', NULL, 'PAGE_TYPE', '페이지 용도', NULL, 'Y'),
    ('CDG_0000000000000000000000pagedev', 'TNT_00000000000000000000000000demo', 'SYSTEM', now(), 'SYSTEM', now(), 0,
     'N', NULL, 'PAGE_DEVICE_TYPE', '페이지 디바이스유형', NULL, 'Y'),
    ('CDG_0000000000000000000ugrpcatego', 'TNT_00000000000000000000000000demo', 'SYSTEM', now(), 'SYSTEM', now(), 0,
     'N', NULL, 'USER_GROUP_CATEGORY', '사용자그룹 분류', NULL, 'Y'),
    ('CDG_00000000000000000000ntcapplyt', 'TNT_00000000000000000000000000demo', 'SYSTEM', now(), 'SYSTEM', now(), 0,
     'N', NULL, 'NOTICE_APPLY_TYPE', '공지 적용유형', NULL, 'Y'),
    ('CDG_000000000000000000000ntcfreqn', 'TNT_00000000000000000000000000demo', 'SYSTEM', now(), 'SYSTEM', now(), 0,
     'N', NULL, 'NOTICE_POPUP_FREQUENCY', '공지 팝업빈도', NULL, 'Y'),
    ('CDG_000000000000000000000ntcsize0', 'TNT_00000000000000000000000000demo', 'SYSTEM', now(), 'SYSTEM', now(), 0,
     'N', NULL, 'NOTICE_POPUP_SIZE', '공지 팝업크기', NULL, 'Y'),
    ('CDG_00000000000000000000ntcshape0', 'TNT_00000000000000000000000000demo', 'SYSTEM', now(), 'SYSTEM', now(), 0,
     'N', NULL, 'NOTICE_POPUP_SHAPE', '공지 팝업모양', NULL, 'Y'),
    ('CDG_0000000000000000000000ntcposn', 'TNT_00000000000000000000000000demo', 'SYSTEM', now(), 'SYSTEM', now(), 0,
     'N', NULL, 'NOTICE_POPUP_POSITION', '공지 팝업위치', NULL, 'Y');

INSERT INTO cmn_code_detail (id, tenant_id, created_by, created_date, modified_by, modified_date, version,
                              deleted_yn, deleted_date, group_id, code, code_name, sort_order, description, use_yn)
VALUES
    -- PAGE_TYPE (용도)
    ('CDT_00000000000000000000000000001', 'TNT_00000000000000000000000000demo', 'SYSTEM', now(), 'SYSTEM', now(), 0,
     'N', NULL, 'CDG_000000000000000000000pagetype', '일반', '일반', 0, NULL, 'Y'),
    ('CDT_00000000000000000000000000002', 'TNT_00000000000000000000000000demo', 'SYSTEM', now(), 'SYSTEM', now(), 0,
     'N', NULL, 'CDG_000000000000000000000pagetype', '팝업', '팝업', 1, NULL, 'Y'),
    -- PAGE_DEVICE_TYPE (디바이스유형) — code 는 영문, code_name 은 한글 표시명
    ('CDT_00000000000000000000000000003', 'TNT_00000000000000000000000000demo', 'SYSTEM', now(), 'SYSTEM', now(), 0,
     'N', NULL, 'CDG_0000000000000000000000pagedev', 'PC', 'PC', 0, NULL, 'Y'),
    ('CDT_00000000000000000000000000004', 'TNT_00000000000000000000000000demo', 'SYSTEM', now(), 'SYSTEM', now(), 0,
     'N', NULL, 'CDG_0000000000000000000000pagedev', 'MOBILE', '모바일', 1, NULL, 'Y'),
    ('CDT_00000000000000000000000000005', 'TNT_00000000000000000000000000demo', 'SYSTEM', now(), 'SYSTEM', now(), 0,
     'N', NULL, 'CDG_0000000000000000000000pagedev', 'ALL', '전체', 2, NULL, 'Y'),
    -- USER_GROUP_CATEGORY (사용자그룹 분류)
    ('CDT_00000000000000000000000000017', 'TNT_00000000000000000000000000demo', 'SYSTEM', now(), 'SYSTEM', now(), 0,
     'N', NULL, 'CDG_0000000000000000000ugrpcatego', '컨텐츠관리', '컨텐츠관리', 0, NULL, 'Y'),
    ('CDT_00000000000000000000000000018', 'TNT_00000000000000000000000000demo', 'SYSTEM', now(), 'SYSTEM', now(), 0,
     'N', NULL, 'CDG_0000000000000000000ugrpcatego', '시스템관리', '시스템관리', 1, NULL, 'Y'),
    ('CDT_00000000000000000000000000019', 'TNT_00000000000000000000000000demo', 'SYSTEM', now(), 'SYSTEM', now(), 0,
     'N', NULL, 'CDG_0000000000000000000ugrpcatego', '데이터입력관리', '데이터입력관리', 2, NULL, 'Y'),
    -- NOTICE_APPLY_TYPE (적용유형)
    ('CDT_00000000000000000000000000006', 'TNT_00000000000000000000000000demo', 'SYSTEM', now(), 'SYSTEM', now(), 0,
     'N', NULL, 'CDG_00000000000000000000ntcapplyt', '로그인', '로그인', 0, NULL, 'Y'),
    ('CDT_00000000000000000000000000007', 'TNT_00000000000000000000000000demo', 'SYSTEM', now(), 'SYSTEM', now(), 0,
     'N', NULL, 'CDG_00000000000000000000ntcapplyt', '페이지', '페이지', 1, NULL, 'Y'),
    -- NOTICE_POPUP_FREQUENCY (팝업빈도)
    ('CDT_00000000000000000000000000008', 'TNT_00000000000000000000000000demo', 'SYSTEM', now(), 'SYSTEM', now(), 0,
     'N', NULL, 'CDG_000000000000000000000ntcfreqn', '1회', '1회', 0, NULL, 'Y'),
    -- NOTICE_POPUP_SIZE (팝업크기)
    ('CDT_00000000000000000000000000009', 'TNT_00000000000000000000000000demo', 'SYSTEM', now(), 'SYSTEM', now(), 0,
     'N', NULL, 'CDG_000000000000000000000ntcsize0', '소형', '소형', 0, NULL, 'Y'),
    ('CDT_00000000000000000000000000010', 'TNT_00000000000000000000000000demo', 'SYSTEM', now(), 'SYSTEM', now(), 0,
     'N', NULL, 'CDG_000000000000000000000ntcsize0', '중형', '중형', 1, NULL, 'Y'),
    ('CDT_00000000000000000000000000011', 'TNT_00000000000000000000000000demo', 'SYSTEM', now(), 'SYSTEM', now(), 0,
     'N', NULL, 'CDG_000000000000000000000ntcsize0', '대형', '대형', 2, NULL, 'Y'),
    -- NOTICE_POPUP_SHAPE (팝업모양)
    ('CDT_00000000000000000000000000012', 'TNT_00000000000000000000000000demo', 'SYSTEM', now(), 'SYSTEM', now(), 0,
     'N', NULL, 'CDG_00000000000000000000ntcshape0', '레이어팝업', '레이어팝업', 0, NULL, 'Y'),
    ('CDT_00000000000000000000000000013', 'TNT_00000000000000000000000000demo', 'SYSTEM', now(), 'SYSTEM', now(), 0,
     'N', NULL, 'CDG_00000000000000000000ntcshape0', '윈도우팝업', '윈도우팝업', 1, NULL, 'Y'),
    -- NOTICE_POPUP_POSITION (팝업위치)
    ('CDT_00000000000000000000000000014', 'TNT_00000000000000000000000000demo', 'SYSTEM', now(), 'SYSTEM', now(), 0,
     'N', NULL, 'CDG_0000000000000000000000ntcposn', '중앙', '중앙', 0, NULL, 'Y'),
    ('CDT_00000000000000000000000000015', 'TNT_00000000000000000000000000demo', 'SYSTEM', now(), 'SYSTEM', now(), 0,
     'N', NULL, 'CDG_0000000000000000000000ntcposn', '좌상단', '좌상단', 1, NULL, 'Y'),
    ('CDT_00000000000000000000000000016', 'TNT_00000000000000000000000000demo', 'SYSTEM', now(), 'SYSTEM', now(), 0,
     'N', NULL, 'CDG_0000000000000000000000ntcposn', '부모창', '부모창', 2, NULL, 'Y');

-- ---------------------------------------------------------------------
-- 8) 기본 Portal/Application — BackOffice 진입점. PortalBootstrapListener(module-portal)가
--    테넌트 생성 시 자동으로 채운다(find-or-create, 멱등).
--    - Portal(BO, "BackOffice")
--    - Application(SYS_MGT, "시스템 관리", BO 포탈 소속)
--    - ApplicationRole(SYS_MGT ↔ ROLE_SYSTEM_ADMIN) — 시스템 관리자만 접근 가능하도록 기본 매핑
--    ⚠️ ROLE_SYSTEM_ADMIN은 위 2)/4)에서 보듯 TenantBootstrapListener가 신규 테넌트에 자동으로
--    뿌려주지 않는 최상위 권한이다 — 그래서 실제 POST /api/v1/companies 로 만든 새 테넌트는
--    이 매핑이 걸려 있어도 ROLE_SYSTEM_ADMIN을 가진 사용자가 아직 없어 "시스템 관리" 화면에
--    아무도 접근하지 못하는 상태로 시작한다(의도된 동작 — PortalBootstrapListener 주석 참고).
--    이 DEMO 테넌트는 6)에서 admin 계정이 ADMIN_GROUP(ROLE_SYSTEM_ADMIN 매핑 포함)에 배정돼
--    있으므로 바로 접근 가능하다.
-- ---------------------------------------------------------------------
INSERT INTO prt_portal (id, tenant_id, created_by, created_date, modified_by, modified_date, version,
                         deleted_yn, deleted_date, portal_code, portal_name, device_type, description, use_yn)
VALUES ('PRT_0000000000000000000000000000bo', 'TNT_00000000000000000000000000demo', 'SYSTEM', now(), 'SYSTEM', now(), 0,
        'N', NULL, 'BO', 'BackOffice', 'PC', '관리자용 BackOffice 포탈', 'Y');

INSERT INTO prt_application (id, tenant_id, created_by, created_date, modified_by, modified_date, version,
                              deleted_yn, deleted_date, application_code, application_name, portal_id, use_yn)
VALUES ('APP_000000000000000000000sysmgt1', 'TNT_00000000000000000000000000demo', 'SYSTEM', now(), 'SYSTEM', now(), 0,
        'N', NULL, 'SYS_MGT', '시스템 관리', 'PRT_0000000000000000000000000000bo', 'Y');

INSERT INTO prt_application_role (id, tenant_id, created_by, created_date, modified_by, modified_date, version,
                                   deleted_yn, deleted_date, application_id, role_code)
VALUES ('APR_00000000000000000000sysadmin1', 'TNT_00000000000000000000000000demo', 'SYSTEM', now(), 'SYSTEM', now(), 0,
        'N', NULL, 'APP_000000000000000000000sysmgt1', 'ROLE_SYSTEM_ADMIN');

COMMIT;
