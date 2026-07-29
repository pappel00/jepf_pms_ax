package com.platform.module.notice.event;

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
 * 공지 등록 화면의 팝업 상세 설정(적용유형/팝업빈도/크기/팝업모양/위치)을 하드코딩된
 * 선택지가 아니라 공통코드(module-code)로 관리하기로 했다 — 화면에서 옵션을 추가/변경하고
 * 싶으면 코드 관리 화면에서 바로 할 수 있게 하기 위함이다.
 *
 * 새 테넌트가 생기면(TenantBootstrapListener가 Role/UserGroup을 만드는 것과 동일한 방식으로)
 * 이 다섯 개 코드그룹을 기본값으로 채워둔다 — 관리자가 처음부터 빈 화면을 마주치지 않게 하기
 * 위함이다. 이미 있으면 건드리지 않는다(find-or-create, 멱등).
 *
 * @TransactionalEventListener(phase = AFTER_COMMIT)을 쓰는 이유는 TenantBootstrapListener와
 * 동일하다 — 발행하는 트랜잭션(Company 생성)이 아직 열려있는 도중에 실행되면 이미 열린
 * Session을 그대로 재사용하게 되어 TenantContext 설정이 무시되는 문제가 생긴다.
 */
@Component
public class NoticeCodeBootstrapListener {

    private static final Logger log = LoggerFactory.getLogger(NoticeCodeBootstrapListener.class);

    public static final String GROUP_APPLY_TYPE = "NOTICE_APPLY_TYPE";
    public static final String GROUP_POPUP_FREQUENCY = "NOTICE_POPUP_FREQUENCY";
    public static final String GROUP_POPUP_SIZE = "NOTICE_POPUP_SIZE";
    public static final String GROUP_POPUP_SHAPE = "NOTICE_POPUP_SHAPE";
    public static final String GROUP_POPUP_POSITION = "NOTICE_POPUP_POSITION";

    private final CodeGroupRepository codeGroupRepository;
    private final CodeDetailRepository codeDetailRepository;

    public NoticeCodeBootstrapListener(CodeGroupRepository codeGroupRepository,
                                        CodeDetailRepository codeDetailRepository) {
        this.codeGroupRepository = codeGroupRepository;
        this.codeDetailRepository = codeDetailRepository;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onTenantCreated(TenantCreatedEvent event) {
        TenantContext.setTenantId(event.getTenantId());
        try {
            seedGroup(GROUP_APPLY_TYPE, "공지 적용유형", List.of("로그인", "페이지"));
            seedGroup(GROUP_POPUP_FREQUENCY, "공지 팝업빈도", List.of("1회"));
            seedGroup(GROUP_POPUP_SIZE, "공지 팝업크기", List.of("소형", "중형", "대형"));
            seedGroup(GROUP_POPUP_SHAPE, "공지 팝업모양", List.of("레이어팝업", "윈도우팝업"));
            seedGroup(GROUP_POPUP_POSITION, "공지 팝업위치", List.of("중앙", "좌상단", "부모창"));

            log.info("[NoticeCodeBootstrapListener] companyCode={} 공지 팝업 설정 공통코드 부트스트랩 완료",
                    event.getCompanyCode());
        } finally {
            TenantContext.clear();
        }
    }

    private void seedGroup(String groupCode, String groupName, List<String> values) {
        CodeGroup group = codeGroupRepository.findByGroupCodeAndDeletedYn(groupCode, "N")
                .orElseGet(() -> codeGroupRepository.saveAndFlush(CodeGroup.create(groupCode, groupName, null)));

        int sortOrder = 0;
        for (String value : values) {
            if (!codeDetailRepository.existsByGroupIdAndCode(group.getId(), value)) {
                codeDetailRepository.saveAndFlush(CodeDetail.create(group.getId(), value, value, sortOrder, null));
            }
            sortOrder++;
        }
    }
}
