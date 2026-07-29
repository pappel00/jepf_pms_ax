package com.platform.module.portal.event;

import com.platform.framework.security.tenant.TenantContext;
import com.platform.module.code.domain.CodeDetail;
import com.platform.module.code.domain.CodeGroup;
import com.platform.module.code.repository.CodeDetailRepository;
import com.platform.module.code.repository.CodeGroupRepository;
import com.platform.module.tenant.event.TenantCreatedEvent;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 페이지 관리 화면의 용도(일반/팝업)와 디바이스유형(PC/MOBILE/ALL)을 하드코딩된 선택지가 아니라
 * 공통코드(module-code)로 관리한다 — 화면에서 선택지를 추가/변경하고 싶으면 코드 관리 화면에서
 * 바로 할 수 있게 하기 위함이다(module-notice의 팝업 상세 설정과 동일한 이유/패턴).
 *
 * 새 테넌트가 생기면 이 두 코드그룹을 기본값으로 채워둔다 — 관리자가 처음부터 빈 셀렉트를
 * 마주치지 않게 하기 위함이다. 이미 있으면 건드리지 않는다(find-or-create, 멱등).
 *
 * ⚠️ Page 엔티티의 page_type/device_type 컬럼에는 CodeDetail의 <b>code</b>(예: "PC")가 저장되고,
 * 화면에는 codeName(예: "모바일")이 표시된다. NOTICE_* 코드들처럼 code와 codeName을 같은 값으로
 * 두지 않은 이유는 디바이스유형은 저장값이 영문 코드(PC/MOBILE/ALL)로 이미 정해져 있고 화면에는
 * 한글 표시명을 보여주는 게 자연스럽기 때문이다.
 *
 * @TransactionalEventListener(phase = AFTER_COMMIT)을 쓰는 이유는 TenantBootstrapListener/
 * NoticeCodeBootstrapListener와 동일하다 — 발행하는 트랜잭션(Company 생성)이 아직 열려있는
 * 도중에 실행되면 이미 열린 Session을 그대로 재사용하게 되어 TenantContext 설정이 무시된다.
 */
@Component
public class PageCodeBootstrapListener {

    private static final Logger log = LoggerFactory.getLogger(PageCodeBootstrapListener.class);

    /** 페이지 용도(일반/팝업) 코드그룹. Page.pageType 에 저장되는 값의 마스터. */
    public static final String GROUP_PAGE_TYPE = "PAGE_TYPE";
    /** 페이지 디바이스유형(PC/MOBILE/ALL) 코드그룹. Page.deviceType 에 저장되는 값의 마스터. */
    public static final String GROUP_PAGE_DEVICE_TYPE = "PAGE_DEVICE_TYPE";

    /** 코드상세 하나의 기본값. code는 실제 저장/비교되는 값, codeName은 화면 표시명이다. */
    public record CodeValue(String code, String codeName) {
    }

    private static final List<CodeValue> PAGE_TYPE_VALUES = List.of(
            new CodeValue("일반", "일반"),
            new CodeValue("팝업", "팝업"));

    private static final List<CodeValue> DEVICE_TYPE_VALUES = List.of(
            new CodeValue("PC", "PC"),
            new CodeValue("MOBILE", "모바일"),
            new CodeValue("ALL", "전체"));

    private final CodeGroupRepository codeGroupRepository;
    private final CodeDetailRepository codeDetailRepository;

    public PageCodeBootstrapListener(CodeGroupRepository codeGroupRepository,
                                      CodeDetailRepository codeDetailRepository) {
        this.codeGroupRepository = codeGroupRepository;
        this.codeDetailRepository = codeDetailRepository;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onTenantCreated(TenantCreatedEvent event) {
        TenantContext.setTenantId(event.getTenantId());
        try {
            seedDefaults();
            log.info("[PageCodeBootstrapListener] companyCode={} 페이지 용도/디바이스유형 공통코드 부트스트랩 완료",
                    event.getCompanyCode());
        } finally {
            TenantContext.clear();
        }
    }

    /**
     * 현재 TenantContext에 설정된 테넌트에 기본 코드그룹/코드상세를 채운다 (멱등).
     *
     * public 인 이유: app-sandbox의 DevDataSeeder는 CompanyService.create()(TenantCreatedEvent
     * 발행)를 거치지 않고 회사를 직접 만들기 때문에 이 시딩을 직접 호출해야 한다. 기본값 목록을
     * 양쪽에 복사해두면 값이 어긋나기 쉬워서(README의 NoticeCodeBootstrapListener 주의사항 참고),
     * 아예 시딩 로직 자체를 재사용하도록 열어뒀다.
     */
    public void seedDefaults() {
        seedGroup(GROUP_PAGE_TYPE, "페이지 용도", PAGE_TYPE_VALUES);
        seedGroup(GROUP_PAGE_DEVICE_TYPE, "페이지 디바이스유형", DEVICE_TYPE_VALUES);
    }

    private void seedGroup(String groupCode, String groupName, List<CodeValue> values) {
        CodeGroup group = codeGroupRepository.findByGroupCodeAndDeletedYn(groupCode, "N")
                .orElseGet(() -> codeGroupRepository.saveAndFlush(CodeGroup.create(groupCode, groupName, null)));

        int sortOrder = 0;
        for (CodeValue value : values) {
            if (!codeDetailRepository.existsByGroupIdAndCode(group.getId(), value.code())) {
                codeDetailRepository.saveAndFlush(
                        CodeDetail.create(group.getId(), value.code(), value.codeName(), sortOrder, null));
            }
            sortOrder++;
        }
    }
}
