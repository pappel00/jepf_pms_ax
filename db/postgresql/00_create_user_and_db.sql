-- =====================================================================
-- JEPF 로컬 개발용 PostgreSQL 계정/DB 생성 스크립트
-- =====================================================================
-- DBeaver에서 postgres(관리자) 계정으로 접속한 상태에서 실행한다.
-- app-sandbox/src/main/resources/application.yml 의 기본값(jdbc:postgresql://localhost:5432/jepf,
-- 계정 jepf/jepf)과 그대로 맞춘 스크립트다 — 계정/비밀번호/DB명을 바꾸고 싶으면 아래 'jepf' 를
-- 전부 원하는 값으로 바꿔서 실행하면 된다.
--
-- ⚠️ CREATE DATABASE는 트랜잭션 안에서 실행할 수 없다. DBeaver에서 이 스크립트 전체를
-- 한 번에 실행(Execute Script, Alt+X)하면 보통 문제없지만, 만약 오류가 나면 파일을 두 부분으로
-- 나눠서(DB 생성 전/후) 따로 실행해야 한다.
-- =====================================================================

-- 1) 로그인 가능한 계정(Role) 생성 + 비밀번호 설정
CREATE ROLE jepf WITH LOGIN PASSWORD 'jepf';

-- 2) 데이터베이스 생성 (소유자를 jepf로 지정)
CREATE DATABASE jepf OWNER jepf;

-- 3) DB 전체 권한 부여 (연결 권한 포함)
GRANT ALL PRIVILEGES ON DATABASE jepf TO jepf;

-- ---------------------------------------------------------------------
-- 여기부터는 위에서 만든 jepf DB에 접속을 "전환"한 뒤 실행해야 한다.
-- DBeaver: 좌측 트리에서 방금 만든 jepf 데이터베이스로 접속을 바꾸고(더블클릭),
--          그 상태에서 아래를 실행한다. (\connect jepf 는 psql 전용이라 DBeaver에선 안 먹는다)
-- ---------------------------------------------------------------------

-- 4) public 스키마 권한 (PostgreSQL 15부터 PUBLIC 롤에 자동으로 CREATE 권한을 안 주기 때문에,
--    이걸 안 하면 Hibernate가 테이블 생성 시 "permission denied for schema public" 로 실패한다)
GRANT ALL ON SCHEMA public TO jepf;

-- 5) (선택) 앞으로 이 DB에 다른 계정이 만드는 테이블에도 jepf가 접근할 수 있게 하려면
--    기본 권한(default privileges)도 설정해둘 수 있다 — 지금 구조에선 jepf 계정 하나만 쓰므로
--    필수는 아니지만, 나중에 배치/마이그레이션용 별도 계정을 쓰게 되면 유용하다.
-- ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO jepf;
-- ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO jepf;

-- =====================================================================
-- 확인용 쿼리 (선택)
-- =====================================================================
-- SELECT rolname, rolcanlogin FROM pg_roles WHERE rolname = 'jepf';
-- SELECT datname, datdba::regrole AS owner FROM pg_database WHERE datname = 'jepf';
