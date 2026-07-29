package com.platform.module.notice.event;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.platform.framework.security.tenant.TenantContext;
import com.platform.module.code.domain.CodeDetail;
import com.platform.module.code.domain.CodeGroup;
import com.platform.module.code.repository.CodeDetailRepository;
import com.platform.module.code.repository.CodeGroupRepository;
import com.platform.module.tenant.event.TenantCreatedEvent;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NoticeCodeBootstrapListenerTest {

    @Mock
    private CodeGroupRepository codeGroupRepository;

    @Mock
    private CodeDetailRepository codeDetailRepository;

    @AfterEach
    void clearTenant() {
        TenantContext.clear();
    }

    @Test
    void onTenantCreatedCreatesFiveGroupsWithTheirDetails() {
        NoticeCodeBootstrapListener listener = new NoticeCodeBootstrapListener(codeGroupRepository, codeDetailRepository);

        // 모든 그룹이 아직 없다고 가정 — find-or-create 로직을 타서 saveAndFlush로 새로 만든다.
        when(codeGroupRepository.findByGroupCodeAndDeletedYn(any(), eq("N"))).thenReturn(Optional.empty());
        when(codeGroupRepository.saveAndFlush(any(CodeGroup.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(codeDetailRepository.existsByGroupIdAndCode(any(), any())).thenReturn(false);

        listener.onTenantCreated(new TenantCreatedEvent("TNT_1", "DEMO"));

        // 5개 그룹(적용유형/팝업빈도/크기/모양/위치) 생성 확인
        verify(codeGroupRepository, times(5)).saveAndFlush(any(CodeGroup.class));
        // 값 개수 합: 2(적용유형) + 1(빈도) + 3(크기) + 2(모양) + 3(위치) = 11
        verify(codeDetailRepository, times(11)).saveAndFlush(any(CodeDetail.class));
    }

    @Test
    void onTenantCreatedSkipsExistingGroupAndDetails() {
        NoticeCodeBootstrapListener listener = new NoticeCodeBootstrapListener(codeGroupRepository, codeDetailRepository);

        CodeGroup existingGroup = CodeGroup.create(NoticeCodeBootstrapListener.GROUP_APPLY_TYPE, "공지 적용유형", null);
        when(codeGroupRepository.findByGroupCodeAndDeletedYn(eq(NoticeCodeBootstrapListener.GROUP_APPLY_TYPE), eq("N")))
                .thenReturn(Optional.of(existingGroup));
        when(codeGroupRepository.findByGroupCodeAndDeletedYn(
                        org.mockito.ArgumentMatchers.argThat(code -> !NoticeCodeBootstrapListener.GROUP_APPLY_TYPE.equals(code)), eq("N")))
                .thenReturn(Optional.empty());
        when(codeGroupRepository.saveAndFlush(any(CodeGroup.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        // 적용유형 그룹의 값들은 이미 다 있다고 가정
        when(codeDetailRepository.existsByGroupIdAndCode(eq(existingGroup.getId()), any())).thenReturn(true);
        when(codeDetailRepository.existsByGroupIdAndCode(
                        org.mockito.ArgumentMatchers.argThat(id -> !existingGroup.getId().equals(id)), any()))
                .thenReturn(false);

        listener.onTenantCreated(new TenantCreatedEvent("TNT_1", "DEMO"));

        // 적용유형 그룹은 이미 있으니 새로 안 만듦 (나머지 4개만 생성)
        verify(codeGroupRepository, times(4)).saveAndFlush(any(CodeGroup.class));
        // 적용유형 그룹의 값(2개)은 이미 있으니 저장 안 함
        verify(codeDetailRepository, never()).saveAndFlush(
                org.mockito.ArgumentMatchers.argThat(detail -> existingGroup.getId().equals(detail.getGroupId())));
    }
}
