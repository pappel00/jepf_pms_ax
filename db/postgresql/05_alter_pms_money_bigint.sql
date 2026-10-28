-- =====================================================================
-- PMS 금액 컬럼 NUMERIC(18,0) → BIGINT 변경 (이미 03_ddl_pms.sql 로 테이블을 만든 DB용)
-- =====================================================================
-- 03_ddl_pms.sql 을 다시 생성하면서 금액(원화) 컬럼 타입을 BIGINT 로 바꿨다(엔티티는 Long). 이미 만든 테이블에는 아래를
-- 테이블 소유자 계정(jepf, 소유자가 postgres 면 postgres)으로 한 번 실행한다. 데이터는 그대로 변환된다.
-- ⚠️ "must be owner of table" 오류가 나면 먼저 00b_fix_table_ownership.sql 을 postgres 계정으로 실행해 소유자를 jepf 로 맞춘다.
-- (그렇지 않으면 서버 기동 때 Hibernate(ddl-auto: update)가 같은 ALTER 를 시도하다 경고를 남긴다 — 동작에는 영향 없음.)
-- =====================================================================
ALTER TABLE pms_project             ALTER COLUMN amount          TYPE BIGINT;
ALTER TABLE pms_project             ALTER COLUMN cost_estimate   TYPE BIGINT;
ALTER TABLE pms_project_operation   ALTER COLUMN contract_amount TYPE BIGINT;
ALTER TABLE pms_proposal            ALTER COLUMN price           TYPE BIGINT;
ALTER TABLE pms_proposal            ALTER COLUMN cost_estimate   TYPE BIGINT;
ALTER TABLE pms_rate                ALTER COLUMN unit_price      TYPE BIGINT;
ALTER TABLE pms_ops_opportunity     ALTER COLUMN amount          TYPE BIGINT;
ALTER TABLE pms_next_opportunity    ALTER COLUMN amount          TYPE BIGINT;