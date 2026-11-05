package com.platform.module.pms.service;

/**
 * 기한 알림 종류 — 메뉴 순서(폴더 > 페이지)대로 나열하고, 페이지가 관리하는 날짜(item)를 업무 종류로 삼는다.
 * defaultOn 은 설정을 저장하기 전의 기본값이다(영업·수행·운영의 핵심 기한은 켜고 나머지는 끈다).
 * DAILY 는 매일 정한 시각에, SLA 는 시한이 시(時) 단위라 매시 확인한다.
 */
public enum NotifyType {
    // 영업단계
    SALES_NOTE("영업단계", "영업 노트", "다음 할 일 예정일", "작성자 · 미완료 노트", true, Scope.DAILY),
    PROJECT_DUE("영업단계", "프로젝트 관리", "입찰 정보 · 제안 마감일", "영업 담당 · 공고 중/제안 중 프로젝트", true, Scope.DAILY),
    RFP_DUE("영업단계", "RFP 관리", "평가 마감일", "평가 미제출 평가자 · RFP 평가 마감일(pms_rfp.due_date)", false, Scope.DAILY),
    PROPOSAL_DUE("영업단계", "제안서 관리", "제출 마감일", "제안서 책임 · 작성 중/내부 검토 제안서", true, Scope.DAILY),
    // 수행 단계
    WBS_TASK("수행 단계", "WBS / 진척 관리", "업무 마감일 · 시작일", "업무 담당 · 진척 입력 버튼이 달린 메시지", true, Scope.DAILY),
    ISSUE_DUE("수행 단계", "이슈 관리", "조치 기한", "이슈 담당 · 오픈/진행 중 이슈", true, Scope.DAILY),
    NEXT_OPP_NEXT("수행 단계", "차기 사업 기회 관리", "다음 할 일 예정일", "기회 담당 · 진행 중 기회", false, Scope.DAILY),
    // 운영 단계
    OPS_END("운영 단계", "프로젝트 관리", "운영 종료일", "프로젝트 PM · 30일/7일/1일 전", false, Scope.DAILY),
    OPS_TASK_DUE("운영 단계", "운영 업무 관리", "업무 기한", "업무 담당 · 미완료 업무", true, Scope.DAILY),
    OPS_INCIDENT_SLA("운영 단계", "운영 이슈 관리", "SLA 응답 · 해결 시한", "이슈 담당 · 시한 1~2시간 전 또는 초과 (매시 확인)", false, Scope.SLA),
    OPS_OPP_NEXT("운영 단계", "운영 영업 관리", "다음 할 일 예정일", "기회 담당 · 진행 중 기회", false, Scope.DAILY);

    /** 실행 주기. */
    public enum Scope { DAILY, SLA, ALL }

    private final String folder;
    private final String page;
    private final String item;
    private final String description;
    private final boolean defaultOn;
    private final Scope scope;

    NotifyType(String folder, String page, String item, String description, boolean defaultOn, Scope scope) {
        this.folder = folder;
        this.page = page;
        this.item = item;
        this.description = description;
        this.defaultOn = defaultOn;
        this.scope = scope;
    }

    public String folder() {
        return folder;
    }

    public String page() {
        return page;
    }

    public String item() {
        return item;
    }

    /** 메시지·집계에 쓰는 이름(페이지 : 업무 종류). */
    public String label() {
        return page + " : " + item;
    }

    public String description() {
        return description;
    }

    public boolean defaultOn() {
        return defaultOn;
    }

    public Scope scope() {
        return scope;
    }
}
