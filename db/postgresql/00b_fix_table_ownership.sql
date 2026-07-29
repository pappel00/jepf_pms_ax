-- =====================================================================
-- 이미 만들어진 테이블/시퀀스에 대한 권한을 jepf 계정에 부여 (복구용)
-- =====================================================================
-- 증상 1: "tnt_company 테이블에 대한 접근 권한 없음" 같은 에러 (SELECT/INSERT 등이 안 됨).
-- 증상 2 (실제로 겪음, 더 미묘함): SELECT/INSERT는 잘 되는데, 엔티티에 새 필드를 추가한 뒤
--        서버를 재시작하면 "department_id 칼럼 없음" 같은 에러가 난다 — ddl-auto: update가
--        컬럼을 추가해줘야 하는데 조용히 실패한 것이다.
--
-- 원인: 테이블을 postgres(관리자) 계정으로 만들면, 그 테이블의 **소유자**가 postgres가 된다.
--       00_create_user_and_db.sql 의 "GRANT ALL ON SCHEMA public TO jepf" 는 "앞으로 jepf가
--       테이블을 만들 수 있는 권한"만 준 것이지, 이미 postgres가 만들어둔 기존 테이블에 대한
--       접근 권한까지 소급해서 주는 게 아니다 — 스키마 권한과 테이블 권한은 별개다.
--       그리고 **GRANT ALL PRIVILEGES 로는 증상 2가 안 고쳐진다** — PostgreSQL에서
--       ALTER TABLE(컬럼 추가/삭제 등 스키마 변경)은 GRANT로 주는 일반 권한이 아니라
--       "테이블 소유자만" 할 수 있는 동작이기 때문이다. Hibernate의 ddl-auto: update가
--       실행하는 ALTER TABLE ADD COLUMN도 마찬가지로 소유자 권한이 필요하다 — 그래서
--       아래에서 GRANT뿐 아니라 소유권(OWNER) 자체를 jepf로 옮긴다.
--
-- 실행 방법: DBeaver에서 postgres 계정으로 접속하되, 접속 대상 데이터베이스를 반드시
--          jepf 로 전환한 상태에서 실행한다 (postgres 기본 DB에서 실행하면 안 된다).
-- =====================================================================

-- 1) 지금 이미 존재하는 모든 테이블에 대한 전체 권한을 jepf에게 부여
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO jepf;

-- 2) 시퀀스가 있다면 그것도 (이 프로젝트는 ID를 애플리케이션에서 생성해서 시퀀스를 안 쓰지만,
--    혹시 모를 다른 용도의 시퀀스가 있을 경우를 위해 포함)
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO jepf;

-- 3) (권장) 앞으로도 postgres 계정으로 뭔가를 만들 일이 있다면, 자동으로 jepf 권한이
--    같이 부여되게 해둔다 — 이 ALTER DEFAULT PRIVILEGES는 "이 문장을 실행한 계정(postgres)이
--    나중에 만드는 객체"에만 적용된다.
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO jepf;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO jepf;

-- 4) **핵심 (증상 2를 실제로 고치는 부분)** — 이미 존재하는 모든 테이블의 소유권 자체를
--    jepf로 옮긴다. 이게 있어야 ddl-auto: update가 나중에 엔티티에 필드를 추가할 때마다
--    필요한 ALTER TABLE ADD COLUMN을 jepf 계정으로 정상 실행할 수 있다. 테이블이 몇 개인지
--    몰라도 되게 스키마의 모든 테이블에 대해 반복 실행한다.
DO $$
DECLARE
    r RECORD;
BEGIN
    FOR r IN (SELECT tablename FROM pg_tables WHERE schemaname = 'public') LOOP
        EXECUTE format('ALTER TABLE public.%I OWNER TO jepf', r.tablename);
    END LOOP;
END $$;

-- =====================================================================
-- 확인용 쿼리 (선택) — jepf가 실제로 SELECT + ALTER TABLE 가능한지 확인
-- =====================================================================
-- SELECT tablename, tableowner FROM pg_tables WHERE schemaname = 'public';  -- 전부 jepf여야 함
-- SET ROLE jepf;
-- SELECT * FROM tnt_company LIMIT 1;
-- RESET ROLE;
