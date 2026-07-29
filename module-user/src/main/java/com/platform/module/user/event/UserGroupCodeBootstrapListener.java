package com.platform.module.user.event;

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
 * 사용자그룹 관리 화면의 분류(컨텐츠관리/시스템관리/데이터입력관리)를 하드코딩된 선택지가
 * 아니라 공통코드(module-code)로 관리한다 — module-portal의 PageCodeBootstrapListener,
 * module-notice의 NoticeCodeBootstrapListener와 동일한 이유/패턴이다. 화면에서 선택지를
 * 추가/변경하고 싶으면 코드 관리 화면에서 바로 할 수 있다.
 *
 * 새 테넌트가 생기면 이 코드그룹을 기본값으로 채워둔다 — 관리자가 처음부터 빈 셀렉트를
 * 마주치지 않게 하기 위함이다. 이미 있으면 건드리지 않는다(find-or-create, 멱등).
 *
 * @TransactionalEventListener(phase = AFTER_COMMIT)을 쓰는 이유는 TenantBootstrapListener와
 * 동일하다 — 발행하는 트랜잭션(Company 생성)이 아직 열려있는 도중에 실행되면 이미 열린
 * Session을 그대로 재사용하게 되어 TenantContext 설정이 무시되는 문제가 생긴다.
 */
@Component
public class UserGroupCodeBootstrapListener {

    private static final Logger log = LoggerFactory.getLogger(UserGroupCodeBootstrapListener.class);

    /** 사용자그룹 분류 코드그룹. UserGroup.category 에 저장되는 값의 마스터. */
    public static final String GROUP_USER_GROUP_CATEGORY = "USER_GROUP_CATEGORY";

    private static final List<String> CATEGORY_VALUES = List.of("컨텐츠관리", "시스템관리", "데이터입력관리");

    private final CodeGroupRepository codeGroupRepository;
    private final CodeDetailRepository codeDetailRepository;

    public UserGroupCodeBootstrapListener(CodeGroupRepository codeGroupRepository,
                                           CodeDetailRepository codeDetailRepository) {
        this.codeGroupRepository = codeGroupRepository;
        this.codeDetailRepository = codeDetailRepository;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onTenantCreated(TenantCreatedEvent event) {
        TenantContext.setTenantId(event.getTenantId());
        try {
            seedDefaults();
            log.info("[UserGroupCodeBootstrapListener] companyCode={} 사용자그룹 분류 공통코드 부트스트랩 완료",
                    event.getCompanyCode());
        } finally {
            TenantContext.clear();
        }
    }

    /**
     * 현재 TenantContext에 설정된 테넌트에 기본 코드그룹/코드상세를 채운다 (멱등).
     *
     * public 인 이유: app-sandbox의 DevDataSeeder는 CompanyService.create()(TenantCreatedEvent
     * 발행)를 거치지 않고 회사를 직접 만들기 때문에 이 시딩을 직접 호출해야 한다
     * (PageCodeBootstrapListener와 동일한 이유 — 기본값 목록을 두 곳에 복사해두면 값이
     * 어긋나기 쉬워서, 아예 시딩 로직 자체를 재사용하도록 열어뒀다).
     */
    public void seedDefaults() {
        CodeGroup group = codeGroupRepository.findByGroupCodeAndDeletedYn(GROUP_USER_GROUP_CATEGORY, "N")
                .orElseGet(() -> codeGroupRepository.saveAndFlush(
                        CodeGroup.create(GROUP_USER_GROUP_CATEGORY, "사용자그룹 분류", null)));

        int sortOrder = 0;
        for (String value : CATEGORY_VALUES) {
            if (!codeDetailRepository.existsByGroupIdAndCode(group.getId(), value)) {
                codeDetailRepository.saveAndFlush(CodeDetail.create(group.getId(), value, value, sortOrder, null));
            }
            sortOrder++;
        }
    }
}
