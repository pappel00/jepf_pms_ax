-- =====================================================================
-- PMS (module-pms) PostgreSQL DDL
-- =====================================================================
-- docs/pms/03-ERD.md 의 테이블을 module-pms 엔티티와 1:1로 맞춘 스크립트다.
-- 기존 01_ddl_schema.sql 과 같은 원칙: 진짜 FOREIGN KEY 없음(느슨한 문자열 참조), 공통 컬럼(BaseEntity+tenant_id),
-- 논리 삭제(deleted_yn), ID는 애플리케이션 생성 문자열.
--
-- ⚠️ 반드시 jepf 계정으로 접속한 상태에서 실행한다(postgres 로 실행하면 테이블 소유자가 postgres 가 되어
--    애플리케이션이 접근하지 못한다 — 00b_fix_table_ownership.sql 참고).
-- ⚠️ 재실행하면 "already exists" 오류가 난다. 다시 만들려면 파일 맨 아래의 DROP 스크립트(주석)를 먼저 실행한다.
-- =====================================================================

-- 고객사 마스터
CREATE TABLE pms_customer (
    id                        VARCHAR(64)     NOT NULL,
    tenant_id                 VARCHAR(64)     NOT NULL,
    customer_code             VARCHAR(30)     NOT NULL,
    customer_name             VARCHAR(200)    NOT NULL,
    industry_code             VARCHAR(30),
    deal_status_code          VARCHAR(30)     NOT NULL,
    biz_no                    VARCHAR(20),
    phone                     VARCHAR(50),
    address                   VARCHAR(300),
    homepage                  VARCHAR(300),
    memo                      VARCHAR(1000),
    created_by                VARCHAR(64),
    created_date              TIMESTAMP,
    modified_by               VARCHAR(64),
    modified_date             TIMESTAMP,
    version                   BIGINT,
    deleted_yn                VARCHAR(1)      NOT NULL,
    deleted_date              TIMESTAMP,
    CONSTRAINT pk_pms_customer PRIMARY KEY (id),
    CONSTRAINT uk_pms_customer_code UNIQUE (tenant_id, customer_code)
);
CREATE INDEX idx_pms_customer_tenant ON pms_customer (tenant_id);

-- 고객사 담당자 마스터 (로그인 계정과 연결하지 않음)
CREATE TABLE pms_customer_contact (
    id                        VARCHAR(64)     NOT NULL,
    tenant_id                 VARCHAR(64)     NOT NULL,
    contact_code              VARCHAR(30)     NOT NULL,
    customer_id               VARCHAR(64)     NOT NULL,
    contact_name              VARCHAR(100)    NOT NULL,
    job_title                 VARCHAR(100),
    dept_name                 VARCHAR(100),
    phone                     VARCHAR(50),
    email                     VARCHAR(200),
    memo                      VARCHAR(1000),
    use_yn                    VARCHAR(1)      NOT NULL,
    created_by                VARCHAR(64),
    created_date              TIMESTAMP,
    modified_by               VARCHAR(64),
    modified_date             TIMESTAMP,
    version                   BIGINT,
    deleted_yn                VARCHAR(1)      NOT NULL,
    deleted_date              TIMESTAMP,
    CONSTRAINT pk_pms_customer_contact PRIMARY KEY (id),
    CONSTRAINT uk_pms_contact_code UNIQUE (tenant_id, contact_code)
);
CREATE INDEX idx_pms_customer_contact_tenant ON pms_customer_contact (tenant_id);
CREATE INDEX idx_pms_contact_customer ON pms_customer_contact (customer_id);

-- 프로젝트(사업) — 영업부터 운영까지 1건
CREATE TABLE pms_project (
    id                        VARCHAR(64)     NOT NULL,
    tenant_id                 VARCHAR(64)     NOT NULL,
    project_code              VARCHAR(30)     NOT NULL,
    project_name              VARCHAR(300)    NOT NULL,
    status_code               VARCHAR(30)     NOT NULL,
    probability_pct           NUMERIC(5,2)    NOT NULL,
    customer_id               VARCHAR(64)     NOT NULL,
    client_name               VARCHAR(200)    NOT NULL,
    contact_id                VARCHAR(64),
    contact_name              VARCHAR(100),
    contact_title             VARCHAR(100),
    contact_phone             VARCHAR(50),
    contact_email             VARCHAR(200),
    start_date                DATE            NOT NULL,
    end_date                  DATE            NOT NULL,
    amount                    BIGINT          NOT NULL,
    vat_included_yn           VARCHAR(1)      NOT NULL,
    sales_user_id             VARCHAR(64)     NOT NULL,
    pm_user_id                VARCHAR(64),
    business_type_code        VARCHAR(30),
    bid_type_code             VARCHAR(30),
    notice_no                 VARCHAR(50),
    due_date                  DATE,
    present_date              DATE,
    competitors               VARCHAR(500),
    cost_estimate             BIGINT,
    cost_source_type          VARCHAR(20),
    cost_source_note          VARCHAR(300),
    fail_reason_code          VARCHAR(50),
    fail_reason_detail        VARCHAR(1000),
    exec_start_date           DATE,
    exec_end_date             DATE,
    exec_end_memo             VARCHAR(1000),
    view_restricted_yn        VARCHAR(1)      NOT NULL,
    memo                      VARCHAR(1000),
    created_by                VARCHAR(64),
    created_date              TIMESTAMP,
    modified_by               VARCHAR(64),
    modified_date             TIMESTAMP,
    version                   BIGINT,
    deleted_yn                VARCHAR(1)      NOT NULL,
    deleted_date              TIMESTAMP,
    CONSTRAINT pk_pms_project PRIMARY KEY (id),
    CONSTRAINT uk_pms_project_code UNIQUE (tenant_id, project_code)
);
CREATE INDEX idx_pms_project_tenant ON pms_project (tenant_id);
CREATE INDEX idx_pms_project_status ON pms_project (tenant_id, status_code);
CREATE INDEX idx_pms_project_customer ON pms_project (customer_id);
CREATE INDEX idx_pms_project_sales ON pms_project (sales_user_id);

-- 영업 노트(영업 활동 기록)
CREATE TABLE pms_sales_note (
    id                        VARCHAR(64)     NOT NULL,
    tenant_id                 VARCHAR(64)     NOT NULL,
    note_code                 VARCHAR(30)     NOT NULL,
    note_date                 DATE            NOT NULL,
    note_type_code            VARCHAR(30)     NOT NULL,
    author_user_id            VARCHAR(64)     NOT NULL,
    project_id                VARCHAR(64),
    customer_id               VARCHAR(64),
    contact_id                VARCHAR(64),
    title                     VARCHAR(300)    NOT NULL,
    content                   TEXT,
    next_action               VARCHAR(500),
    next_date                 DATE,
    done_yn                   VARCHAR(1)      NOT NULL,
    created_by                VARCHAR(64),
    created_date              TIMESTAMP,
    modified_by               VARCHAR(64),
    modified_date             TIMESTAMP,
    version                   BIGINT,
    deleted_yn                VARCHAR(1)      NOT NULL,
    deleted_date              TIMESTAMP,
    CONSTRAINT pk_pms_sales_note PRIMARY KEY (id),
    CONSTRAINT uk_pms_note_code UNIQUE (tenant_id, note_code)
);
CREATE INDEX idx_pms_sales_note_tenant ON pms_sales_note (tenant_id);
CREATE INDEX idx_pms_note_project ON pms_sales_note (project_id);
CREATE INDEX idx_pms_note_customer ON pms_sales_note (customer_id);

-- 공통 상태 변경 이력 (처리 현황 모달의 지난 처리)
CREATE TABLE pms_status_history (
    id                        VARCHAR(64)     NOT NULL,
    tenant_id                 VARCHAR(64)     NOT NULL,
    target_type_code          VARCHAR(30)     NOT NULL,
    target_id                 VARCHAR(64)     NOT NULL,
    from_status               VARCHAR(30),
    to_status                 VARCHAR(30)     NOT NULL,
    actor_user_id             VARCHAR(64)     NOT NULL,
    acted_at                  TIMESTAMP       NOT NULL,
    memo                      VARCHAR(1000),
    created_by                VARCHAR(64),
    created_date              TIMESTAMP,
    modified_by               VARCHAR(64),
    modified_date             TIMESTAMP,
    version                   BIGINT,
    deleted_yn                VARCHAR(1)      NOT NULL,
    deleted_date              TIMESTAMP,
    CONSTRAINT pk_pms_status_history PRIMARY KEY (id)
);
CREATE INDEX idx_pms_status_history_tenant ON pms_status_history (tenant_id);
CREATE INDEX idx_pms_status_hist_target ON pms_status_history (target_type_code, target_id, acted_at);

-- RFP(제안요청서, 차수 단위)
CREATE TABLE pms_rfp (
    id                        VARCHAR(64)     NOT NULL,
    tenant_id                 VARCHAR(64)     NOT NULL,
    rfp_code                  VARCHAR(30)     NOT NULL,
    project_id                VARCHAR(64)     NOT NULL,
    title                     VARCHAR(300)    NOT NULL,
    round_code                VARCHAR(30)     NOT NULL,
    received_date             DATE,
    due_date                  DATE,
    file_group_id             VARCHAR(64),
    created_by                VARCHAR(64),
    created_date              TIMESTAMP,
    modified_by               VARCHAR(64),
    modified_date             TIMESTAMP,
    version                   BIGINT,
    deleted_yn                VARCHAR(1)      NOT NULL,
    deleted_date              TIMESTAMP,
    CONSTRAINT pk_pms_rfp PRIMARY KEY (id),
    CONSTRAINT uk_pms_rfp_code UNIQUE (tenant_id, rfp_code)
);
CREATE INDEX idx_pms_rfp_tenant ON pms_rfp (tenant_id);
CREATE INDEX idx_pms_rfp_project ON pms_rfp (project_id);

-- RFP 항목(세분화된 요구사항) + 확정 M/D
CREATE TABLE pms_rfp_item (
    id                        VARCHAR(64)     NOT NULL,
    tenant_id                 VARCHAR(64)     NOT NULL,
    rfp_id                    VARCHAR(64)     NOT NULL,
    item_code                 VARCHAR(30)     NOT NULL,
    category_code             VARCHAR(10)     NOT NULL,
    area                      VARCHAR(100),
    item_name                 VARCHAR(300)    NOT NULL,
    content                   TEXT,
    source_page               INT,
    mandatory_yn              VARCHAR(1)      NOT NULL,
    sort_order                INT             NOT NULL,
    confirmed_evaluator_user_idVARCHAR(64),
    confirmed_md              NUMERIC(6,2),
    confirmed_edited_yn       VARCHAR(1),
    confirmed_by              VARCHAR(64),
    confirmed_at              TIMESTAMP,
    created_by                VARCHAR(64),
    created_date              TIMESTAMP,
    modified_by               VARCHAR(64),
    modified_date             TIMESTAMP,
    version                   BIGINT,
    deleted_yn                VARCHAR(1)      NOT NULL,
    deleted_date              TIMESTAMP,
    CONSTRAINT pk_pms_rfp_item PRIMARY KEY (id),
    CONSTRAINT uk_pms_rfp_item_code UNIQUE (tenant_id, rfp_id, item_code)
);
CREATE INDEX idx_pms_rfp_item_tenant ON pms_rfp_item (tenant_id);
CREATE INDEX idx_pms_rfp_item_rfp ON pms_rfp_item (rfp_id);

-- RFP 평가자 지정 + 제출 상태
CREATE TABLE pms_rfp_evaluator (
    id                        VARCHAR(64)     NOT NULL,
    tenant_id                 VARCHAR(64)     NOT NULL,
    rfp_id                    VARCHAR(64)     NOT NULL,
    evaluator_user_id         VARCHAR(64)     NOT NULL,
    submitted_yn              VARCHAR(1)      NOT NULL,
    submitted_at              TIMESTAMP,
    created_by                VARCHAR(64),
    created_date              TIMESTAMP,
    modified_by               VARCHAR(64),
    modified_date             TIMESTAMP,
    version                   BIGINT,
    deleted_yn                VARCHAR(1)      NOT NULL,
    deleted_date              TIMESTAMP,
    CONSTRAINT pk_pms_rfp_evaluator PRIMARY KEY (id),
    CONSTRAINT uk_pms_rfp_evaluator UNIQUE (tenant_id, rfp_id, evaluator_user_id)
);
CREATE INDEX idx_pms_rfp_evaluator_tenant ON pms_rfp_evaluator (tenant_id);

-- RFP 항목 평가 (항목 × 평가자)
CREATE TABLE pms_rfp_eval (
    id                        VARCHAR(64)     NOT NULL,
    tenant_id                 VARCHAR(64)     NOT NULL,
    rfp_item_id               VARCHAR(64)     NOT NULL,
    evaluator_user_id         VARCHAR(64)     NOT NULL,
    kind_code                 VARCHAR(10),
    impl_code                 VARCHAR(20),
    difficulty_code           VARCHAR(10),
    md                        NUMERIC(6,2),
    comment_text              VARCHAR(1000),
    created_by                VARCHAR(64),
    created_date              TIMESTAMP,
    modified_by               VARCHAR(64),
    modified_date             TIMESTAMP,
    version                   BIGINT,
    deleted_yn                VARCHAR(1)      NOT NULL,
    deleted_date              TIMESTAMP,
    CONSTRAINT pk_pms_rfp_eval PRIMARY KEY (id),
    CONSTRAINT uk_pms_rfp_eval UNIQUE (tenant_id, rfp_item_id, evaluator_user_id)
);
CREATE INDEX idx_pms_rfp_eval_tenant ON pms_rfp_eval (tenant_id);

-- 제안서
CREATE TABLE pms_proposal (
    id                        VARCHAR(64)     NOT NULL,
    tenant_id                 VARCHAR(64)     NOT NULL,
    proposal_code             VARCHAR(30)     NOT NULL,
    project_id                VARCHAR(64)     NOT NULL,
    rfp_id                    VARCHAR(64)     NOT NULL,
    title                     VARCHAR(300)    NOT NULL,
    status_code               VARCHAR(30)     NOT NULL,
    owner_user_id             VARCHAR(64)     NOT NULL,
    due_date                  DATE,
    submitted_date            DATE,
    price                     BIGINT,
    cost_estimate             BIGINT,
    created_by                VARCHAR(64),
    created_date              TIMESTAMP,
    modified_by               VARCHAR(64),
    modified_date             TIMESTAMP,
    version                   BIGINT,
    deleted_yn                VARCHAR(1)      NOT NULL,
    deleted_date              TIMESTAMP,
    CONSTRAINT pk_pms_proposal PRIMARY KEY (id),
    CONSTRAINT uk_pms_proposal_code UNIQUE (tenant_id, proposal_code)
);
CREATE INDEX idx_pms_proposal_tenant ON pms_proposal (tenant_id);
CREATE INDEX idx_pms_proposal_project ON pms_proposal (project_id);
CREATE INDEX idx_pms_proposal_rfp ON pms_proposal (rfp_id);

-- 제안서 버전 이력
CREATE TABLE pms_proposal_version (
    id                        VARCHAR(64)     NOT NULL,
    tenant_id                 VARCHAR(64)     NOT NULL,
    proposal_id               VARCHAR(64)     NOT NULL,
    ver                       VARCHAR(20)     NOT NULL,
    version_date              DATE            NOT NULL,
    author_user_id            VARCHAR(64)     NOT NULL,
    summary                   VARCHAR(500),
    file_group_id             VARCHAR(64),
    file_name                 VARCHAR(300),
    file_size                 VARCHAR(30),
    submitted_yn              VARCHAR(1)      NOT NULL,
    created_by                VARCHAR(64),
    created_date              TIMESTAMP,
    modified_by               VARCHAR(64),
    modified_date             TIMESTAMP,
    version                   BIGINT,
    deleted_yn                VARCHAR(1)      NOT NULL,
    deleted_date              TIMESTAMP,
    CONSTRAINT pk_pms_proposal_version PRIMARY KEY (id),
    CONSTRAINT uk_pms_proposal_version UNIQUE (tenant_id, proposal_id, ver)
);
CREATE INDEX idx_pms_proposal_version_tenant ON pms_proposal_version (tenant_id);

-- 제안서 RFP 항목 대응표
CREATE TABLE pms_proposal_mapping (
    id                        VARCHAR(64)     NOT NULL,
    tenant_id                 VARCHAR(64)     NOT NULL,
    proposal_id               VARCHAR(64)     NOT NULL,
    rfp_item_id               VARCHAR(64)     NOT NULL,
    section                   VARCHAR(100),
    response_code             VARCHAR(20),
    summary                   VARCHAR(1000),
    owner_user_id             VARCHAR(64),
    created_by                VARCHAR(64),
    created_date              TIMESTAMP,
    modified_by               VARCHAR(64),
    modified_date             TIMESTAMP,
    version                   BIGINT,
    deleted_yn                VARCHAR(1)      NOT NULL,
    deleted_date              TIMESTAMP,
    CONSTRAINT pk_pms_proposal_mapping PRIMARY KEY (id),
    CONSTRAINT uk_pms_proposal_mapping UNIQUE (tenant_id, proposal_id, rfp_item_id)
);
CREATE INDEX idx_pms_proposal_mapping_tenant ON pms_proposal_mapping (tenant_id);

-- 제안서 제출 체크리스트
CREATE TABLE pms_proposal_checklist (
    id                        VARCHAR(64)     NOT NULL,
    tenant_id                 VARCHAR(64)     NOT NULL,
    proposal_id               VARCHAR(64)     NOT NULL,
    item_name                 VARCHAR(200)    NOT NULL,
    done_yn                   VARCHAR(1)      NOT NULL,
    sort_order                INT             NOT NULL,
    created_by                VARCHAR(64),
    created_date              TIMESTAMP,
    modified_by               VARCHAR(64),
    modified_date             TIMESTAMP,
    version                   BIGINT,
    deleted_yn                VARCHAR(1)      NOT NULL,
    deleted_date              TIMESTAMP,
    CONSTRAINT pk_pms_proposal_checklist PRIMARY KEY (id)
);
CREATE INDEX idx_pms_proposal_checklist_tenant ON pms_proposal_checklist (tenant_id);
CREATE INDEX idx_pms_proposal_check ON pms_proposal_checklist (proposal_id);

-- PMS 사용자 확장 프로필 (usr_user 1:1)
CREATE TABLE pms_user_profile (
    id                        VARCHAR(64)     NOT NULL,
    tenant_id                 VARCHAR(64)     NOT NULL,
    user_id                   VARCHAR(64)     NOT NULL,
    org_type_code             VARCHAR(20)     NOT NULL,
    partner_company           VARCHAR(200),
    notify_channel_code       VARCHAR(20),
    telegram_chat_id          VARCHAR(100),
    mattermost_user_id        VARCHAR(100),
    job_grade_code            VARCHAR(20),
    created_by                VARCHAR(64),
    created_date              TIMESTAMP,
    modified_by               VARCHAR(64),
    modified_date             TIMESTAMP,
    version                   BIGINT,
    deleted_yn                VARCHAR(1)      NOT NULL,
    deleted_date              TIMESTAMP,
    CONSTRAINT pk_pms_user_profile PRIMARY KEY (id),
    CONSTRAINT uk_pms_user_profile UNIQUE (tenant_id, user_id)
);
CREATE INDEX idx_pms_user_profile_tenant ON pms_user_profile (tenant_id);

-- 프로젝트 관리자·투입 인력 (수행/운영)
CREATE TABLE pms_project_member (
    id                        VARCHAR(64)     NOT NULL,
    tenant_id                 VARCHAR(64)     NOT NULL,
    project_id                VARCHAR(64)     NOT NULL,
    kind_code                 VARCHAR(10)     NOT NULL,
    user_id                   VARCHAR(64)     NOT NULL,
    manager_yn                VARCHAR(1)      NOT NULL,
    manager_role_code         VARCHAR(20),
    role_code                 VARCHAR(30)     NOT NULL,
    mm                        NUMERIC(6,2),
    rate_pct                  NUMERIC(5,1),
    from_date                 DATE,
    to_date                   DATE,
    auto_end_yn               VARCHAR(1)      NOT NULL,
    access_level_code         VARCHAR(10)     NOT NULL,
    notify_channel_code       VARCHAR(20),
    created_by                VARCHAR(64),
    created_date              TIMESTAMP,
    modified_by               VARCHAR(64),
    modified_date             TIMESTAMP,
    version                   BIGINT,
    deleted_yn                VARCHAR(1)      NOT NULL,
    deleted_date              TIMESTAMP,
    CONSTRAINT pk_pms_project_member PRIMARY KEY (id),
    CONSTRAINT uk_pms_project_member UNIQUE (tenant_id, project_id, kind_code, user_id)
);
CREATE INDEX idx_pms_project_member_tenant ON pms_project_member (tenant_id);
CREATE INDEX idx_pms_member_project ON pms_project_member (project_id);
CREATE INDEX idx_pms_member_user ON pms_project_member (user_id);

-- 멤버 투입 단계
CREATE TABLE pms_member_phase (
    id                        VARCHAR(64)     NOT NULL,
    tenant_id                 VARCHAR(64)     NOT NULL,
    member_id                 VARCHAR(64)     NOT NULL,
    phase_code                VARCHAR(20)     NOT NULL,
    created_by                VARCHAR(64),
    created_date              TIMESTAMP,
    modified_by               VARCHAR(64),
    modified_date             TIMESTAMP,
    version                   BIGINT,
    deleted_yn                VARCHAR(1)      NOT NULL,
    deleted_date              TIMESTAMP,
    CONSTRAINT pk_pms_member_phase PRIMARY KEY (id),
    CONSTRAINT uk_pms_member_phase UNIQUE (tenant_id, member_id, phase_code)
);
CREATE INDEX idx_pms_member_phase_tenant ON pms_member_phase (tenant_id);

-- 멤버 투입 공백 구간 (중간에 투입이 비는 기간)
CREATE TABLE pms_member_gap (
    id                        VARCHAR(64)     NOT NULL,
    tenant_id                 VARCHAR(64)     NOT NULL,
    member_id                 VARCHAR(64)     NOT NULL,
    from_date                 DATE            NOT NULL,
    to_date                   DATE            NOT NULL,
    created_by                VARCHAR(64),
    created_date              TIMESTAMP,
    modified_by               VARCHAR(64),
    modified_date             TIMESTAMP,
    version                   BIGINT,
    deleted_yn                VARCHAR(1)      NOT NULL,
    deleted_date              TIMESTAMP,
    CONSTRAINT pk_pms_member_gap PRIMARY KEY (id)
);
CREATE INDEX idx_pms_member_gap_tenant ON pms_member_gap (tenant_id);
CREATE INDEX idx_pms_member_gap ON pms_member_gap (member_id);

-- 멤버 월별 투입률 조정 (직접 수정한 달만 저장)
CREATE TABLE pms_member_month_plan (
    id                        VARCHAR(64)     NOT NULL,
    tenant_id                 VARCHAR(64)     NOT NULL,
    member_id                 VARCHAR(64)     NOT NULL,
    plan_month                VARCHAR(7)      NOT NULL,
    rate_pct                  NUMERIC(5,1)    NOT NULL,
    created_by                VARCHAR(64),
    created_date              TIMESTAMP,
    modified_by               VARCHAR(64),
    modified_date             TIMESTAMP,
    version                   BIGINT,
    deleted_yn                VARCHAR(1)      NOT NULL,
    deleted_date              TIMESTAMP,
    CONSTRAINT pk_pms_member_month_plan PRIMARY KEY (id),
    CONSTRAINT uk_pms_member_month_plan UNIQUE (tenant_id, member_id, plan_month)
);
CREATE INDEX idx_pms_member_month_plan_tenant ON pms_member_month_plan (tenant_id);

-- 단계전환 요청·승인·롤백 (수행/운영 공통)
CREATE TABLE pms_transition (
    id                        VARCHAR(64)     NOT NULL,
    tenant_id                 VARCHAR(64)     NOT NULL,
    transition_code           VARCHAR(30)     NOT NULL,
    project_id                VARCHAR(64)     NOT NULL,
    kind_code                 VARCHAR(10)     NOT NULL,
    status_code               VARCHAR(20)     NOT NULL,
    requester_user_id         VARCHAR(64)     NOT NULL,
    requested_at              TIMESTAMP       NOT NULL,
    approver_user_id          VARCHAR(64),
    decided_at                TIMESTAMP,
    decision_comment          VARCHAR(1000),
    selection_json            JSONB           NOT NULL,
    summary                   VARCHAR(500),
    rfp_id                    VARCHAR(64),
    item_count                INT,
    rolled_back_by            VARCHAR(64),
    rolled_back_at            TIMESTAMP,
    rollback_reason           VARCHAR(1000),
    created_by                VARCHAR(64),
    created_date              TIMESTAMP,
    modified_by               VARCHAR(64),
    modified_date             TIMESTAMP,
    version                   BIGINT,
    deleted_yn                VARCHAR(1)      NOT NULL,
    deleted_date              TIMESTAMP,
    CONSTRAINT pk_pms_transition PRIMARY KEY (id),
    CONSTRAINT uk_pms_transition_code UNIQUE (tenant_id, transition_code)
);
CREATE INDEX idx_pms_transition_tenant ON pms_transition (tenant_id);
CREATE INDEX idx_pms_transition_project ON pms_transition (project_id);

-- 투입 계획 변경 요청·승인·이력
CREATE TABLE pms_team_change (
    id                        VARCHAR(64)     NOT NULL,
    tenant_id                 VARCHAR(64)     NOT NULL,
    change_code               VARCHAR(30)     NOT NULL,
    project_id                VARCHAR(64)     NOT NULL,
    status_code               VARCHAR(20)     NOT NULL,
    requester_user_id         VARCHAR(64)     NOT NULL,
    requested_at              TIMESTAMP       NOT NULL,
    reason                    VARCHAR(1000)   NOT NULL,
    before_json               JSONB           NOT NULL,
    after_json                JSONB           NOT NULL,
    summary_text              TEXT,
    conflict_json             JSONB,
    decided_by                VARCHAR(64),
    decided_at                TIMESTAMP,
    decision_comment          VARCHAR(1000),
    created_by                VARCHAR(64),
    created_date              TIMESTAMP,
    modified_by               VARCHAR(64),
    modified_date             TIMESTAMP,
    version                   BIGINT,
    deleted_yn                VARCHAR(1)      NOT NULL,
    deleted_date              TIMESTAMP,
    CONSTRAINT pk_pms_team_change PRIMARY KEY (id),
    CONSTRAINT uk_pms_team_change_code UNIQUE (tenant_id, change_code)
);
CREATE INDEX idx_pms_team_change_tenant ON pms_team_change (tenant_id);
CREATE INDEX idx_pms_team_change_project ON pms_team_change (project_id);

-- 운영 정보 (프로젝트 1:0..1)
CREATE TABLE pms_project_operation (
    id                        VARCHAR(64)     NOT NULL,
    tenant_id                 VARCHAR(64)     NOT NULL,
    project_id                VARCHAR(64)     NOT NULL,
    op_type_code              VARCHAR(30)     NOT NULL,
    start_date                DATE            NOT NULL,
    end_date                  DATE            NOT NULL,
    contract_amount           BIGINT,
    sla_level_code            VARCHAR(20),
    sla_respond_hours         INT,
    sla_resolve_hours         INT,
    sla_service_hours         VARCHAR(100),
    transition_id             VARCHAR(64),
    created_by                VARCHAR(64),
    created_date              TIMESTAMP,
    modified_by               VARCHAR(64),
    modified_date             TIMESTAMP,
    version                   BIGINT,
    deleted_yn                VARCHAR(1)      NOT NULL,
    deleted_date              TIMESTAMP,
    CONSTRAINT pk_pms_project_operation PRIMARY KEY (id),
    CONSTRAINT uk_pms_project_operation UNIQUE (tenant_id, project_id)
);
CREATE INDEX idx_pms_project_operation_tenant ON pms_project_operation (tenant_id);

-- 운영 인수 대상 기능
CREATE TABLE pms_project_feature (
    id                        VARCHAR(64)     NOT NULL,
    tenant_id                 VARCHAR(64)     NOT NULL,
    project_id                VARCHAR(64)     NOT NULL,
    feature_name              VARCHAR(300)    NOT NULL,
    handover_yn               VARCHAR(1)      NOT NULL,
    source_code               VARCHAR(20),
    created_by                VARCHAR(64),
    created_date              TIMESTAMP,
    modified_by               VARCHAR(64),
    modified_date             TIMESTAMP,
    version                   BIGINT,
    deleted_yn                VARCHAR(1)      NOT NULL,
    deleted_date              TIMESTAMP,
    CONSTRAINT pk_pms_project_feature PRIMARY KEY (id)
);
CREATE INDEX idx_pms_project_feature_tenant ON pms_project_feature (tenant_id);
CREATE INDEX idx_pms_feature_project ON pms_project_feature (project_id);

-- 운영 전환 인수·이관 항목
CREATE TABLE pms_handover_item (
    id                        VARCHAR(64)     NOT NULL,
    tenant_id                 VARCHAR(64)     NOT NULL,
    project_id                VARCHAR(64)     NOT NULL,
    transition_id             VARCHAR(64),
    item_type_code            VARCHAR(20)     NOT NULL,
    ref_id                    VARCHAR(64),
    item_name                 VARCHAR(300)    NOT NULL,
    created_by                VARCHAR(64),
    created_date              TIMESTAMP,
    modified_by               VARCHAR(64),
    modified_date             TIMESTAMP,
    version                   BIGINT,
    deleted_yn                VARCHAR(1)      NOT NULL,
    deleted_date              TIMESTAMP,
    CONSTRAINT pk_pms_handover_item PRIMARY KEY (id)
);
CREATE INDEX idx_pms_handover_item_tenant ON pms_handover_item (tenant_id);
CREATE INDEX idx_pms_handover_project ON pms_handover_item (project_id);

-- WBS 업무 (자기참조 트리: 단계 → 분류 → 작업)
CREATE TABLE pms_wbs_task (
    id                        VARCHAR(64)     NOT NULL,
    tenant_id                 VARCHAR(64)     NOT NULL,
    project_id                VARCHAR(64)     NOT NULL,
    parent_id                 VARCHAR(64),
    node_type_code            VARCHAR(10)     NOT NULL,
    phase_code                VARCHAR(20)     NOT NULL,
    category_name             VARCHAR(100),
    task_name                 VARCHAR(300)    NOT NULL,
    rfp_item_id               VARCHAR(64),
    req_code                  VARCHAR(30),
    feature_name              VARCHAR(300),
    assignee_user_id          VARCHAR(64),
    start_date                DATE,
    end_date                  DATE,
    weight                    NUMERIC(8,2)    NOT NULL,
    progress_pct              NUMERIC(5,1),
    pred_task_id              VARCHAR(64),
    copied_from_id            VARCHAR(64),
    sort_order                INT             NOT NULL,
    created_by                VARCHAR(64),
    created_date              TIMESTAMP,
    modified_by               VARCHAR(64),
    modified_date             TIMESTAMP,
    version                   BIGINT,
    deleted_yn                VARCHAR(1)      NOT NULL,
    deleted_date              TIMESTAMP,
    CONSTRAINT pk_pms_wbs_task PRIMARY KEY (id)
);
CREATE INDEX idx_pms_wbs_task_tenant ON pms_wbs_task (tenant_id);
CREATE INDEX idx_pms_wbs_project_parent ON pms_wbs_task (project_id, parent_id);
CREATE INDEX idx_pms_wbs_assignee ON pms_wbs_task (assignee_user_id);
CREATE INDEX idx_pms_wbs_rfp_item ON pms_wbs_task (rfp_item_id);

-- 업무 진척 입력 이력
CREATE TABLE pms_task_progress_log (
    id                        VARCHAR(64)     NOT NULL,
    tenant_id                 VARCHAR(64)     NOT NULL,
    task_id                   VARCHAR(64)     NOT NULL,
    progress_pct              NUMERIC(5,1)    NOT NULL,
    input_by_user_id          VARCHAR(64)     NOT NULL,
    input_at                  TIMESTAMP       NOT NULL,
    channel_code              VARCHAR(20)     NOT NULL,
    note                      VARCHAR(500),
    created_by                VARCHAR(64),
    created_date              TIMESTAMP,
    modified_by               VARCHAR(64),
    modified_date             TIMESTAMP,
    version                   BIGINT,
    deleted_yn                VARCHAR(1)      NOT NULL,
    deleted_date              TIMESTAMP,
    CONSTRAINT pk_pms_task_progress_log PRIMARY KEY (id)
);
CREATE INDEX idx_pms_task_progress_log_tenant ON pms_task_progress_log (tenant_id);
CREATE INDEX idx_pms_task_progress_task ON pms_task_progress_log (task_id);

-- 주간 진척 스냅샷 (프로젝트 단위)
CREATE TABLE pms_progress_snapshot (
    id                        VARCHAR(64)     NOT NULL,
    tenant_id                 VARCHAR(64)     NOT NULL,
    project_id                VARCHAR(64)     NOT NULL,
    snapshot_date             DATE            NOT NULL,
    overall_pct               NUMERIC(5,1)    NOT NULL,
    target_pct                NUMERIC(5,1)    NOT NULL,
    saved_by                  VARCHAR(64),
    created_by                VARCHAR(64),
    created_date              TIMESTAMP,
    modified_by               VARCHAR(64),
    modified_date             TIMESTAMP,
    version                   BIGINT,
    deleted_yn                VARCHAR(1)      NOT NULL,
    deleted_date              TIMESTAMP,
    CONSTRAINT pk_pms_progress_snapshot PRIMARY KEY (id),
    CONSTRAINT uk_pms_progress_snapshot UNIQUE (tenant_id, project_id, snapshot_date)
);
CREATE INDEX idx_pms_progress_snapshot_tenant ON pms_progress_snapshot (tenant_id);

-- 주간/월간 보고서 (프로젝트 × 유형 × 기간 1건)
CREATE TABLE pms_report (
    id                        VARCHAR(64)     NOT NULL,
    tenant_id                 VARCHAR(64)     NOT NULL,
    project_id                VARCHAR(64)     NOT NULL,
    report_type_code          VARCHAR(10)     NOT NULL,
    period_start              DATE            NOT NULL,
    period_end                DATE            NOT NULL,
    status_code               VARCHAR(20)     NOT NULL,
    memo                      TEXT,
    submit_version            INT             NOT NULL,
    author_user_id            VARCHAR(64)     NOT NULL,
    created_by                VARCHAR(64),
    created_date              TIMESTAMP,
    modified_by               VARCHAR(64),
    modified_date             TIMESTAMP,
    version                   BIGINT,
    deleted_yn                VARCHAR(1)      NOT NULL,
    deleted_date              TIMESTAMP,
    CONSTRAINT pk_pms_report PRIMARY KEY (id),
    CONSTRAINT uk_pms_report_period UNIQUE (tenant_id, project_id, report_type_code, period_start)
);
CREATE INDEX idx_pms_report_tenant ON pms_report (tenant_id);

-- 보고서 제출 버전
CREATE TABLE pms_report_version (
    id                        VARCHAR(64)     NOT NULL,
    tenant_id                 VARCHAR(64)     NOT NULL,
    report_id                 VARCHAR(64)     NOT NULL,
    version_no                INT             NOT NULL,
    submitted_by              VARCHAR(64)     NOT NULL,
    submitted_at              TIMESTAMP       NOT NULL,
    overall_pct               NUMERIC(5,1),
    target_pct                NUMERIC(5,1),
    memo                      TEXT,
    created_by                VARCHAR(64),
    created_date              TIMESTAMP,
    modified_by               VARCHAR(64),
    modified_date             TIMESTAMP,
    version                   BIGINT,
    deleted_yn                VARCHAR(1)      NOT NULL,
    deleted_date              TIMESTAMP,
    CONSTRAINT pk_pms_report_version PRIMARY KEY (id),
    CONSTRAINT uk_pms_report_version UNIQUE (tenant_id, report_id, version_no)
);
CREATE INDEX idx_pms_report_version_tenant ON pms_report_version (tenant_id);

-- 보고서 처리 이력
CREATE TABLE pms_report_history (
    id                        VARCHAR(64)     NOT NULL,
    tenant_id                 VARCHAR(64)     NOT NULL,
    report_id                 VARCHAR(64)     NOT NULL,
    action_code               VARCHAR(20)     NOT NULL,
    actor_user_id             VARCHAR(64)     NOT NULL,
    acted_at                  TIMESTAMP       NOT NULL,
    history_text              VARCHAR(1000),
    created_by                VARCHAR(64),
    created_date              TIMESTAMP,
    modified_by               VARCHAR(64),
    modified_date             TIMESTAMP,
    version                   BIGINT,
    deleted_yn                VARCHAR(1)      NOT NULL,
    deleted_date              TIMESTAMP,
    CONSTRAINT pk_pms_report_history PRIMARY KEY (id)
);
CREATE INDEX idx_pms_report_history_tenant ON pms_report_history (tenant_id);
CREATE INDEX idx_pms_report_history ON pms_report_history (report_id);

-- 표준 산출물 마스터
CREATE TABLE pms_std_doc (
    id                        VARCHAR(64)     NOT NULL,
    tenant_id                 VARCHAR(64)     NOT NULL,
    phase_code                VARCHAR(20)     NOT NULL,
    doc_name                  VARCHAR(200)    NOT NULL,
    required_yn               VARCHAR(1)      NOT NULL,
    rfp_category_code         VARCHAR(10),
    sort_order                INT             NOT NULL,
    use_yn                    VARCHAR(1)      NOT NULL,
    created_by                VARCHAR(64),
    created_date              TIMESTAMP,
    modified_by               VARCHAR(64),
    modified_date             TIMESTAMP,
    version                   BIGINT,
    deleted_yn                VARCHAR(1)      NOT NULL,
    deleted_date              TIMESTAMP,
    CONSTRAINT pk_pms_std_doc PRIMARY KEY (id),
    CONSTRAINT uk_pms_std_doc UNIQUE (tenant_id, phase_code, doc_name)
);
CREATE INDEX idx_pms_std_doc_tenant ON pms_std_doc (tenant_id);

-- 문서 테일러링 (프로젝트별 적용 산출물)
CREATE TABLE pms_doc_tailoring (
    id                        VARCHAR(64)     NOT NULL,
    tenant_id                 VARCHAR(64)     NOT NULL,
    project_id                VARCHAR(64)     NOT NULL,
    std_doc_id                VARCHAR(64),
    phase_code                VARCHAR(20)     NOT NULL,
    doc_name                  VARCHAR(200)    NOT NULL,
    required_yn               VARCHAR(1)      NOT NULL,
    applied_code              VARCHAR(10)     NOT NULL,
    not_applied_reason        VARCHAR(500),
    owner_user_id             VARCHAR(64),
    custom_yn                 VARCHAR(1)      NOT NULL,
    note                      VARCHAR(500),
    reviewed_yn               VARCHAR(1)      NOT NULL,
    created_by                VARCHAR(64),
    created_date              TIMESTAMP,
    modified_by               VARCHAR(64),
    modified_date             TIMESTAMP,
    version                   BIGINT,
    deleted_yn                VARCHAR(1)      NOT NULL,
    deleted_date              TIMESTAMP,
    CONSTRAINT pk_pms_doc_tailoring PRIMARY KEY (id)
);
CREATE INDEX idx_pms_doc_tailoring_tenant ON pms_doc_tailoring (tenant_id);
CREATE INDEX idx_pms_tailoring_project ON pms_doc_tailoring (project_id);

-- 산출물 버전
CREATE TABLE pms_doc_version (
    id                        VARCHAR(64)     NOT NULL,
    tenant_id                 VARCHAR(64)     NOT NULL,
    tailoring_id              VARCHAR(64)     NOT NULL,
    ver                       VARCHAR(20)     NOT NULL,
    version_date              DATE            NOT NULL,
    author_user_id            VARCHAR(64)     NOT NULL,
    file_item_id              VARCHAR(64),
    file_name                 VARCHAR(300),
    file_size                 VARCHAR(30),
    note                      VARCHAR(500),
    created_by                VARCHAR(64),
    created_date              TIMESTAMP,
    modified_by               VARCHAR(64),
    modified_date             TIMESTAMP,
    version                   BIGINT,
    deleted_yn                VARCHAR(1)      NOT NULL,
    deleted_date              TIMESTAMP,
    CONSTRAINT pk_pms_doc_version PRIMARY KEY (id),
    CONSTRAINT uk_pms_doc_version UNIQUE (tenant_id, tailoring_id, ver)
);
CREATE INDEX idx_pms_doc_version_tenant ON pms_doc_version (tenant_id);

-- 이슈 (프로젝트 소속, 업무 연결은 pms_issue_task)
CREATE TABLE pms_issue (
    id                        VARCHAR(64)     NOT NULL,
    tenant_id                 VARCHAR(64)     NOT NULL,
    issue_code                VARCHAR(30)     NOT NULL,
    project_id                VARCHAR(64)     NOT NULL,
    title                     VARCHAR(300)    NOT NULL,
    type_code                 VARCHAR(30)     NOT NULL,
    priority_code             VARCHAR(10)     NOT NULL,
    status_code               VARCHAR(20)     NOT NULL,
    reporter_user_id          VARCHAR(64)     NOT NULL,
    assignee_user_id          VARCHAR(64)     NOT NULL,
    registered_date           DATE,
    due_date                  DATE,
    resolved_date             DATE,
    impact_days               INT             NOT NULL,
    content                   TEXT            NOT NULL,
    resolution                TEXT,
    created_by                VARCHAR(64),
    created_date              TIMESTAMP,
    modified_by               VARCHAR(64),
    modified_date             TIMESTAMP,
    version                   BIGINT,
    deleted_yn                VARCHAR(1)      NOT NULL,
    deleted_date              TIMESTAMP,
    CONSTRAINT pk_pms_issue PRIMARY KEY (id),
    CONSTRAINT uk_pms_issue_code UNIQUE (tenant_id, issue_code)
);
CREATE INDEX idx_pms_issue_tenant ON pms_issue (tenant_id);
CREATE INDEX idx_pms_issue_project ON pms_issue (project_id);
CREATE INDEX idx_pms_issue_assignee ON pms_issue (assignee_user_id);

-- 이슈 ↔ 업무 N:M
CREATE TABLE pms_issue_task (
    id                        VARCHAR(64)     NOT NULL,
    tenant_id                 VARCHAR(64)     NOT NULL,
    issue_id                  VARCHAR(64)     NOT NULL,
    task_id                   VARCHAR(64)     NOT NULL,
    linked_by                 VARCHAR(64),
    linked_at                 TIMESTAMP,
    created_by                VARCHAR(64),
    created_date              TIMESTAMP,
    modified_by               VARCHAR(64),
    modified_date             TIMESTAMP,
    version                   BIGINT,
    deleted_yn                VARCHAR(1)      NOT NULL,
    deleted_date              TIMESTAMP,
    CONSTRAINT pk_pms_issue_task PRIMARY KEY (id),
    CONSTRAINT uk_pms_issue_task UNIQUE (tenant_id, issue_id, task_id)
);
CREATE INDEX idx_pms_issue_task_tenant ON pms_issue_task (tenant_id);
CREATE INDEX idx_pms_issue_task_task ON pms_issue_task (task_id);

-- 이슈 처리 이력
CREATE TABLE pms_issue_history (
    id                        VARCHAR(64)     NOT NULL,
    tenant_id                 VARCHAR(64)     NOT NULL,
    issue_id                  VARCHAR(64)     NOT NULL,
    action_code               VARCHAR(30)     NOT NULL,
    actor_user_id             VARCHAR(64)     NOT NULL,
    acted_at                  TIMESTAMP       NOT NULL,
    history_text              VARCHAR(1000),
    created_by                VARCHAR(64),
    created_date              TIMESTAMP,
    modified_by               VARCHAR(64),
    modified_date             TIMESTAMP,
    version                   BIGINT,
    deleted_yn                VARCHAR(1)      NOT NULL,
    deleted_date              TIMESTAMP,
    CONSTRAINT pk_pms_issue_history PRIMARY KEY (id)
);
CREATE INDEX idx_pms_issue_history_tenant ON pms_issue_history (tenant_id);
CREATE INDEX idx_pms_issue_history ON pms_issue_history (issue_id);

-- 이슈 해결 시 일정 조정 제안
CREATE TABLE pms_schedule_proposal (
    id                        VARCHAR(64)     NOT NULL,
    tenant_id                 VARCHAR(64)     NOT NULL,
    proposal_code             VARCHAR(30)     NOT NULL,
    project_id                VARCHAR(64)     NOT NULL,
    issue_id                  VARCHAR(64)     NOT NULL,
    status_code               VARCHAR(20)     NOT NULL,
    shift_days                INT             NOT NULL,
    basis_text                VARCHAR(500),
    decided_by                VARCHAR(64),
    decided_at                TIMESTAMP,
    decision_comment          VARCHAR(1000),
    created_by                VARCHAR(64),
    created_date              TIMESTAMP,
    modified_by               VARCHAR(64),
    modified_date             TIMESTAMP,
    version                   BIGINT,
    deleted_yn                VARCHAR(1)      NOT NULL,
    deleted_date              TIMESTAMP,
    CONSTRAINT pk_pms_schedule_proposal PRIMARY KEY (id),
    CONSTRAINT uk_pms_schedule_proposal_code UNIQUE (tenant_id, proposal_code)
);
CREATE INDEX idx_pms_schedule_proposal_tenant ON pms_schedule_proposal (tenant_id);
CREATE INDEX idx_pms_schedule_proposal_issue ON pms_schedule_proposal (issue_id);

-- 일정 조정 제안 대상 업무와 변경 전후 일정
CREATE TABLE pms_schedule_proposal_item (
    id                        VARCHAR(64)     NOT NULL,
    tenant_id                 VARCHAR(64)     NOT NULL,
    proposal_id               VARCHAR(64)     NOT NULL,
    task_id                   VARCHAR(64)     NOT NULL,
    old_start                 DATE,
    old_end                   DATE,
    new_start                 DATE,
    new_end                   DATE,
    applied_yn                VARCHAR(1)      NOT NULL,
    created_by                VARCHAR(64),
    created_date              TIMESTAMP,
    modified_by               VARCHAR(64),
    modified_date             TIMESTAMP,
    version                   BIGINT,
    deleted_yn                VARCHAR(1)      NOT NULL,
    deleted_date              TIMESTAMP,
    CONSTRAINT pk_pms_schedule_proposal_item PRIMARY KEY (id)
);
CREATE INDEX idx_pms_schedule_proposal_item_tenant ON pms_schedule_proposal_item (tenant_id);
CREATE INDEX idx_pms_schedule_item_proposal ON pms_schedule_proposal_item (proposal_id);

-- 월별 실적 M/M
CREATE TABLE pms_actual_mm (
    id                        VARCHAR(64)     NOT NULL,
    tenant_id                 VARCHAR(64)     NOT NULL,
    project_id                VARCHAR(64)     NOT NULL,
    kind_code                 VARCHAR(10)     NOT NULL,
    user_id                   VARCHAR(64)     NOT NULL,
    actual_month              VARCHAR(7)      NOT NULL,
    actual_mm                 NUMERIC(6,2)    NOT NULL,
    input_by                  VARCHAR(64),
    created_by                VARCHAR(64),
    created_date              TIMESTAMP,
    modified_by               VARCHAR(64),
    modified_date             TIMESTAMP,
    version                   BIGINT,
    deleted_yn                VARCHAR(1)      NOT NULL,
    deleted_date              TIMESTAMP,
    CONSTRAINT pk_pms_actual_mm PRIMARY KEY (id),
    CONSTRAINT uk_pms_actual_mm UNIQUE (tenant_id, project_id, kind_code, user_id, actual_month)
);
CREATE INDEX idx_pms_actual_mm_tenant ON pms_actual_mm (tenant_id);

-- 직무 × 등급별 M/M 단가
CREATE TABLE pms_rate (
    id                        VARCHAR(64)     NOT NULL,
    tenant_id                 VARCHAR(64)     NOT NULL,
    job_code                  VARCHAR(30)     NOT NULL,
    grade_code                VARCHAR(20)     NOT NULL,
    unit_price                BIGINT          NOT NULL,
    from_date                 DATE            NOT NULL,
    to_date                   DATE,
    created_by                VARCHAR(64),
    created_date              TIMESTAMP,
    modified_by               VARCHAR(64),
    modified_date             TIMESTAMP,
    version                   BIGINT,
    deleted_yn                VARCHAR(1)      NOT NULL,
    deleted_date              TIMESTAMP,
    CONSTRAINT pk_pms_rate PRIMARY KEY (id)
);
CREATE INDEX idx_pms_rate_tenant ON pms_rate (tenant_id);
CREATE INDEX idx_pms_rate_job_grade ON pms_rate (job_code, grade_code);

-- PMS 설정 (키-값, 테넌트 단위)
CREATE TABLE pms_setting (
    id                        VARCHAR(64)     NOT NULL,
    tenant_id                 VARCHAR(64)     NOT NULL,
    setting_key               VARCHAR(100)    NOT NULL,
    setting_value             VARCHAR(500),
    created_by                VARCHAR(64),
    created_date              TIMESTAMP,
    modified_by               VARCHAR(64),
    modified_date             TIMESTAMP,
    version                   BIGINT,
    deleted_yn                VARCHAR(1)      NOT NULL,
    deleted_date              TIMESTAMP,
    CONSTRAINT pk_pms_setting PRIMARY KEY (id),
    CONSTRAINT uk_pms_setting_key UNIQUE (tenant_id, setting_key)
);
CREATE INDEX idx_pms_setting_tenant ON pms_setting (tenant_id);

-- 운영 업무
CREATE TABLE pms_ops_task (
    id                        VARCHAR(64)     NOT NULL,
    tenant_id                 VARCHAR(64)     NOT NULL,
    task_code                 VARCHAR(30)     NOT NULL,
    project_id                VARCHAR(64)     NOT NULL,
    title                     VARCHAR(300)    NOT NULL,
    category_code             VARCHAR(30)     NOT NULL,
    assignee_user_id          VARCHAR(64),
    due_date                  DATE,
    status_code               VARCHAR(20)     NOT NULL,
    priority_code             VARCHAR(10)     NOT NULL,
    repeat_code               VARCHAR(10)     NOT NULL,
    repeat_end_date           DATE,
    prev_task_id              VARCHAR(64),
    auto_created_yn           VARCHAR(1)      NOT NULL,
    memo                      VARCHAR(1000),
    incident_id               VARCHAR(64),
    created_by                VARCHAR(64),
    created_date              TIMESTAMP,
    modified_by               VARCHAR(64),
    modified_date             TIMESTAMP,
    version                   BIGINT,
    deleted_yn                VARCHAR(1)      NOT NULL,
    deleted_date              TIMESTAMP,
    CONSTRAINT pk_pms_ops_task PRIMARY KEY (id),
    CONSTRAINT uk_pms_ops_task_code UNIQUE (tenant_id, task_code)
);
CREATE INDEX idx_pms_ops_task_tenant ON pms_ops_task (tenant_id);
CREATE INDEX idx_pms_ops_task_project ON pms_ops_task (project_id);

-- 운영 이슈 (장애·문의·개선 요청)
CREATE TABLE pms_ops_incident (
    id                        VARCHAR(64)     NOT NULL,
    tenant_id                 VARCHAR(64)     NOT NULL,
    incident_code             VARCHAR(30)     NOT NULL,
    project_id                VARCHAR(64)     NOT NULL,
    type_code                 VARCHAR(20)     NOT NULL,
    title                     VARCHAR(300)    NOT NULL,
    content                   TEXT,
    priority_code             VARCHAR(10)     NOT NULL,
    status_code               VARCHAR(20)     NOT NULL,
    reporter_name             VARCHAR(100),
    assignee_user_id          VARCHAR(64),
    received_at               TIMESTAMP       NOT NULL,
    responded_at              TIMESTAMP,
    resolved_at               TIMESTAMP,
    action_text               TEXT,
    opportunity_id            VARCHAR(64),
    created_by                VARCHAR(64),
    created_date              TIMESTAMP,
    modified_by               VARCHAR(64),
    modified_date             TIMESTAMP,
    version                   BIGINT,
    deleted_yn                VARCHAR(1)      NOT NULL,
    deleted_date              TIMESTAMP,
    CONSTRAINT pk_pms_ops_incident PRIMARY KEY (id),
    CONSTRAINT uk_pms_ops_incident_code UNIQUE (tenant_id, incident_code)
);
CREATE INDEX idx_pms_ops_incident_tenant ON pms_ops_incident (tenant_id);
CREATE INDEX idx_pms_ops_incident_project ON pms_ops_incident (project_id);

-- 운영 영업 기회
CREATE TABLE pms_ops_opportunity (
    id                        VARCHAR(64)     NOT NULL,
    tenant_id                 VARCHAR(64)     NOT NULL,
    opp_code                  VARCHAR(30)     NOT NULL,
    project_id                VARCHAR(64)     NOT NULL,
    kind_code                 VARCHAR(30)     NOT NULL,
    title                     VARCHAR(300)    NOT NULL,
    stage_code                VARCHAR(20)     NOT NULL,
    amount                    BIGINT,
    probability_pct           NUMERIC(5,2),
    owner_user_id             VARCHAR(64),
    expected_date             DATE,
    next_action               VARCHAR(500),
    next_date                 DATE,
    memo                      VARCHAR(1000),
    source_incident_id        VARCHAR(64),
    source_next_opp_id        VARCHAR(64),
    contact_id                VARCHAR(64),
    created_by                VARCHAR(64),
    created_date              TIMESTAMP,
    modified_by               VARCHAR(64),
    modified_date             TIMESTAMP,
    version                   BIGINT,
    deleted_yn                VARCHAR(1)      NOT NULL,
    deleted_date              TIMESTAMP,
    CONSTRAINT pk_pms_ops_opportunity PRIMARY KEY (id),
    CONSTRAINT uk_pms_ops_opp_code UNIQUE (tenant_id, opp_code)
);
CREATE INDEX idx_pms_ops_opportunity_tenant ON pms_ops_opportunity (tenant_id);
CREATE INDEX idx_pms_ops_opp_project ON pms_ops_opportunity (project_id);

-- 차기 사업 기회 (수행 단계)
CREATE TABLE pms_next_opportunity (
    id                        VARCHAR(64)     NOT NULL,
    tenant_id                 VARCHAR(64)     NOT NULL,
    opp_code                  VARCHAR(30)     NOT NULL,
    project_id                VARCHAR(64)     NOT NULL,
    title                     VARCHAR(300)    NOT NULL,
    kind_code                 VARCHAR(50)     NOT NULL,
    source_code               VARCHAR(30),
    content                   TEXT,
    requester_contact_id      VARCHAR(64),
    decision_maker_contact_id VARCHAR(64),
    budget_code               VARCHAR(20),
    est_mm                    NUMERIC(6,2),
    amount                    BIGINT,
    quarter                   VARCHAR(10),
    stage_code                VARCHAR(20)     NOT NULL,
    priority_code             VARCHAR(10),
    owner_user_id             VARCHAR(64),
    issue_id                  VARCHAR(64),
    next_action               VARCHAR(500),
    next_date                 DATE,
    scope_reason              VARCHAR(500),
    memo                      VARCHAR(1000),
    ops_opportunity_id        VARCHAR(64),
    created_by                VARCHAR(64),
    created_date              TIMESTAMP,
    modified_by               VARCHAR(64),
    modified_date             TIMESTAMP,
    version                   BIGINT,
    deleted_yn                VARCHAR(1)      NOT NULL,
    deleted_date              TIMESTAMP,
    CONSTRAINT pk_pms_next_opportunity PRIMARY KEY (id),
    CONSTRAINT uk_pms_next_opp_code UNIQUE (tenant_id, opp_code)
);
CREATE INDEX idx_pms_next_opportunity_tenant ON pms_next_opportunity (tenant_id);
CREATE INDEX idx_pms_next_opp_project ON pms_next_opportunity (project_id);

-- =====================================================================
-- (필요 시) 전체 삭제 스크립트 — 주석을 풀어서 실행하면 PMS 테이블이 모두 지워진다. 데이터도 함께 삭제되므로 주의.
-- =====================================================================
-- DROP TABLE IF EXISTS pms_next_opportunity CASCADE;
-- DROP TABLE IF EXISTS pms_ops_opportunity CASCADE;
-- DROP TABLE IF EXISTS pms_ops_incident CASCADE;
-- DROP TABLE IF EXISTS pms_ops_task CASCADE;
-- DROP TABLE IF EXISTS pms_setting CASCADE;
-- DROP TABLE IF EXISTS pms_rate CASCADE;
-- DROP TABLE IF EXISTS pms_actual_mm CASCADE;
-- DROP TABLE IF EXISTS pms_schedule_proposal_item CASCADE;
-- DROP TABLE IF EXISTS pms_schedule_proposal CASCADE;
-- DROP TABLE IF EXISTS pms_issue_history CASCADE;
-- DROP TABLE IF EXISTS pms_issue_task CASCADE;
-- DROP TABLE IF EXISTS pms_issue CASCADE;
-- DROP TABLE IF EXISTS pms_doc_version CASCADE;
-- DROP TABLE IF EXISTS pms_doc_tailoring CASCADE;
-- DROP TABLE IF EXISTS pms_std_doc CASCADE;
-- DROP TABLE IF EXISTS pms_report_history CASCADE;
-- DROP TABLE IF EXISTS pms_report_version CASCADE;
-- DROP TABLE IF EXISTS pms_report CASCADE;
-- DROP TABLE IF EXISTS pms_progress_snapshot CASCADE;
-- DROP TABLE IF EXISTS pms_task_progress_log CASCADE;
-- DROP TABLE IF EXISTS pms_wbs_task CASCADE;
-- DROP TABLE IF EXISTS pms_handover_item CASCADE;
-- DROP TABLE IF EXISTS pms_project_feature CASCADE;
-- DROP TABLE IF EXISTS pms_project_operation CASCADE;
-- DROP TABLE IF EXISTS pms_team_change CASCADE;
-- DROP TABLE IF EXISTS pms_transition CASCADE;
-- DROP TABLE IF EXISTS pms_member_month_plan CASCADE;
-- DROP TABLE IF EXISTS pms_member_gap CASCADE;
-- DROP TABLE IF EXISTS pms_member_phase CASCADE;
-- DROP TABLE IF EXISTS pms_project_member CASCADE;
-- DROP TABLE IF EXISTS pms_user_profile CASCADE;
-- DROP TABLE IF EXISTS pms_proposal_checklist CASCADE;
-- DROP TABLE IF EXISTS pms_proposal_mapping CASCADE;
-- DROP TABLE IF EXISTS pms_proposal_version CASCADE;
-- DROP TABLE IF EXISTS pms_proposal CASCADE;
-- DROP TABLE IF EXISTS pms_rfp_eval CASCADE;
-- DROP TABLE IF EXISTS pms_rfp_evaluator CASCADE;
-- DROP TABLE IF EXISTS pms_rfp_item CASCADE;
-- DROP TABLE IF EXISTS pms_rfp CASCADE;
-- DROP TABLE IF EXISTS pms_status_history CASCADE;
-- DROP TABLE IF EXISTS pms_sales_note CASCADE;
-- DROP TABLE IF EXISTS pms_project CASCADE;
-- DROP TABLE IF EXISTS pms_customer_contact CASCADE;
-- DROP TABLE IF EXISTS pms_customer CASCADE;
