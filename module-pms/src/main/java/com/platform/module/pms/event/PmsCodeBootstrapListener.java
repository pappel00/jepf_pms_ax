package com.platform.module.pms.event;

import com.platform.framework.security.tenant.TenantContext;
import com.platform.module.code.domain.CodeDetail;
import com.platform.module.code.domain.CodeGroup;
import com.platform.module.code.repository.CodeDetailRepository;
import com.platform.module.code.repository.CodeGroupRepository;
import com.platform.module.pms.domain.StdDoc;
import com.platform.module.pms.repository.StdDocRepository;
import com.platform.module.tenant.event.TenantCreatedEvent;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * PMS 화면의 선택지를 공통코드(module-code)로 채워 둔다 — 새 테넌트가 생기면 기본값을 시딩한다(멱등, find-or-create).
 * 화면에서 선택지를 바꾸고 싶으면 코드 관리 화면에서 바로 수정할 수 있다.
 * 저장값(code)과 표시명(codeName)은 같은 한글 값이다(프로젝트 상태 등 화면 로직이 값 자체를 쓰기 때문).
 * seedDefaults()는 app-sandbox 의 개발용 시더가 이벤트를 거치지 않고 직접 호출한다(PageCodeBootstrapListener 와 동일한 이유).
 */
@Component
public class PmsCodeBootstrapListener {

    private static final Logger log = LoggerFactory.getLogger(PmsCodeBootstrapListener.class);

    /** 코드그룹 → (그룹명, 값 목록). 순서가 정렬 순서다. */
    private static final Map<String, Group> GROUPS = new LinkedHashMap<>();

    private record Group(String name, List<String> values) {
    }

    static {
        GROUPS.put("PMS_PROJECT_STATUS", new Group("PMS 프로젝트 상태",
                List.of("공고 전", "공고 중", "제안 중", "평가 중", "협상 중", "수주", "수행 중", "수행 종료", "운영 중", "실패", "보류/취소")));
        GROUPS.put("PMS_FAIL_REASON", new Group("PMS 실패/보류 사유",
                List.of("가격 경쟁력 열세", "기술·제안 평가 열세", "경쟁사 선정", "발주사 사정(예산·일정 변경)", "발주 취소·보류", "내부 사정(수행 불가)", "기타")));
        GROUPS.put("PMS_BUSINESS_TYPE", new Group("PMS 사업 구분", List.of("SI 구축", "SM 운영/유지보수", "컨설팅", "솔루션 공급", "기타")));
        GROUPS.put("PMS_BID_TYPE", new Group("PMS 계약/입찰 방식",
                List.of("공개 경쟁입찰", "제한 경쟁입찰", "협상에 의한 계약", "수의계약", "내부/그룹사")));
        GROUPS.put("PMS_INDUSTRY", new Group("PMS 고객사 업종", List.of("제조", "금융", "유통/물류", "공공", "의료/제약", "에너지", "IT/서비스", "기타")));
        GROUPS.put("PMS_CUSTOMER_STATUS", new Group("PMS 고객사 거래 상태", List.of("거래 중", "잠재", "거래 종료")));
        GROUPS.put("PMS_ORG_TYPE", new Group("PMS 소속 구분", List.of("자사", "협력사")));
        GROUPS.put("PMS_GRADE", new Group("PMS 인력 등급", List.of("특급", "고급", "중급", "초급")));
        GROUPS.put("PMS_NOTIFY_CHANNEL", new Group("PMS 알림 채널", List.of("Mattermost", "텔레그램", "이메일", "미설정")));
        GROUPS.put("PMS_NOTE_TYPE", new Group("PMS 영업 노트 유형",
                List.of("방문", "미팅", "전화", "이메일", "식사·접대", "제안·발표", "자료 전달", "정보 수집", "인수인계", "기타")));
        GROUPS.put("PMS_PRIORITY", new Group("PMS 우선순위", List.of("긴급", "높음", "보통", "낮음")));
        GROUPS.put("PMS_RFP_CATEGORY", new Group("PMS RFP 요구사항 분류",
                List.of("SFR", "PER", "SER", "ECR", "DAR", "QUR", "COR", "PMR", "PSR")));
        GROUPS.put("PMS_RFP_ROUND", new Group("PMS RFP 차수", List.of("1차", "2차", "재공고", "변경 공고")));
    }

    private final StdDocRepository stdDocRepository;
    private final CodeGroupRepository codeGroupRepository;
    private final CodeDetailRepository codeDetailRepository;

    public PmsCodeBootstrapListener(CodeGroupRepository codeGroupRepository, CodeDetailRepository codeDetailRepository,
                                    StdDocRepository stdDocRepository) {
        this.stdDocRepository = stdDocRepository;
        this.codeGroupRepository = codeGroupRepository;
        this.codeDetailRepository = codeDetailRepository;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onTenantCreated(TenantCreatedEvent event) {
        TenantContext.setTenantId(event.getTenantId());
        try {
            seedDefaults();
            log.info("[PmsCodeBootstrapListener] companyCode={} PMS 공통코드 부트스트랩 완료", event.getCompanyCode());
        } finally {
            TenantContext.clear();
        }
    }

    /** 현재 TenantContext 의 테넌트에 PMS 공통코드를 채운다 (멱등). */
    public void seedDefaults() {
        GROUPS.forEach((groupCode, group) -> seedGroup(groupCode, group));
        seedStdDocs();
    }

    /** 표준 산출물 마스터: 단계, 문서 종류, 표준 필수 여부, 권장 문서를 자동 적용하는 RFP 요구사항 분류(없으면 빈 값). */
    private static final String[][] STD_DOCS = {
            {"분석", "요구사항 정의서", "Y", ""}, {"분석", "요구사항 추적표", "Y", ""}, {"분석", "현행 시스템 분석서", "N", ""},
            {"분석", "인터페이스 현황 분석서", "N", ""}, {"분석", "데이터 이관 분석서", "N", "DAR"}, {"분석", "업무 프로세스 정의서", "N", ""},
            {"설계", "아키텍처 설계서", "Y", ""}, {"설계", "화면 설계서", "Y", ""}, {"설계", "인터페이스 정의서", "Y", ""},
            {"설계", "테이블 정의서(ERD)", "Y", ""}, {"설계", "단위테스트 시나리오", "Y", ""}, {"설계", "프로그램 설계서", "N", "SFR"},
            {"설계", "보안 설계서", "N", "SER"},
            {"개발", "소스코드 목록", "Y", ""}, {"개발", "단위테스트 결과서", "Y", ""}, {"개발", "개발자 가이드", "N", ""},
            {"개발", "코딩 표준 점검 결과서", "N", "QUR"}, {"개발", "빌드·배포 가이드", "N", ""},
            {"테스트", "통합테스트 시나리오", "Y", ""}, {"테스트", "통합테스트 결과서", "Y", ""}, {"테스트", "결함 관리대장", "Y", ""},
            {"테스트", "성능테스트 결과서", "N", "PER"}, {"테스트", "보안 취약점 점검 결과서", "N", "SER"},
            {"테스트", "사용자 인수 테스트 결과서", "N", "QUR"},
    };

    private void seedStdDocs() {
        int order = 0;
        for (String[] d : STD_DOCS) {
            if (!stdDocRepository.existsByPhaseCodeAndDocName(d[0], d[1])) {
                stdDocRepository.saveAndFlush(StdDoc.create().withPhaseCode(d[0]).withDocName(d[1]).withRequiredYn(d[2])
                        .withRfpCategoryCode(d[3].isEmpty() ? null : d[3]).withSortOrder(order).withUseYn("Y"));
            }
            order++;
        }
    }

    private void seedGroup(String groupCode, Group group) {
        CodeGroup codeGroup = codeGroupRepository.findByGroupCodeAndDeletedYn(groupCode, "N")
                .orElseGet(() -> codeGroupRepository.saveAndFlush(CodeGroup.create(groupCode, group.name(), null)));
        int sortOrder = 0;
        for (String value : group.values()) {
            if (!codeDetailRepository.existsByGroupIdAndCode(codeGroup.getId(), value)) {
                codeDetailRepository.saveAndFlush(CodeDetail.create(codeGroup.getId(), value, value, sortOrder, null));
            }
            sortOrder++;
        }
    }
}