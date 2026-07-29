package com.platform.module.terms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.platform.framework.core.common.exception.BusinessException;
import com.platform.framework.core.common.exception.TenantException;
import com.platform.module.terms.domain.TermsGroup;
import com.platform.module.terms.dto.TermsGroupDto;
import com.platform.module.terms.repository.TermsGroupRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TermsGroupServiceTest {

    @Mock
    private TermsGroupRepository termsGroupRepository;

    @Test
    void createFailsWhenTermsCodeDuplicatedWithinSamePortal() {
        TermsGroupService service = new TermsGroupService(termsGroupRepository);
        when(termsGroupRepository.existsByPortalIdAndTermsCode("PRT_1", "SERVICE_TERMS")).thenReturn(true);

        var request = new TermsGroupDto.CreateRequest("PRT_1", "SERVICE_TERMS", "서비스 이용약관", "필수", null);

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void createSucceedsWhenSameTermsCodeUsedByDifferentPortal() {
        TermsGroupService service = new TermsGroupService(termsGroupRepository);
        // PRT_2에는 아직 없음 (PRT_1에 같은 코드가 있어도 무관)
        when(termsGroupRepository.existsByPortalIdAndTermsCode("PRT_2", "SERVICE_TERMS")).thenReturn(false);
        when(termsGroupRepository.saveAndFlush(org.mockito.ArgumentMatchers.any(TermsGroup.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var request = new TermsGroupDto.CreateRequest("PRT_2", "SERVICE_TERMS", "서비스 이용약관", "필수", null);
        TermsGroupDto.Response response = service.create(request);

        assertThat(response.portalId()).isEqualTo("PRT_2");
        assertThat(response.termsCode()).isEqualTo("SERVICE_TERMS");
    }

    @Test
    void getByIdThrowsWhenNotFound() {
        TermsGroupService service = new TermsGroupService(termsGroupRepository);
        when(termsGroupRepository.findByIdAndDeletedYn("TGR_unknown", "N")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById("TGR_unknown"))
                .isInstanceOf(TenantException.class);
    }

    @Test
    void getByIdReturnsResponseWhenFound() {
        TermsGroupService service = new TermsGroupService(termsGroupRepository);
        TermsGroup group = TermsGroup.create("PRT_1", "SERVICE_TERMS", "서비스 이용약관", "필수", "설명");
        when(termsGroupRepository.findByIdAndDeletedYn(group.getId(), "N")).thenReturn(Optional.of(group));

        TermsGroupDto.Response response = service.getById(group.getId());

        assertThat(response.portalId()).isEqualTo("PRT_1");
        assertThat(response.termsCode()).isEqualTo("SERVICE_TERMS");
        assertThat(response.termsType()).isEqualTo("필수");
    }
}
