package com.platform.module.privacy.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.platform.framework.core.common.exception.BusinessException;
import com.platform.framework.core.common.exception.TenantException;
import com.platform.module.privacy.domain.PrivacyPolicyGroup;
import com.platform.module.privacy.dto.PrivacyPolicyGroupDto;
import com.platform.module.privacy.repository.PrivacyPolicyGroupRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PrivacyPolicyGroupServiceTest {

    @Mock
    private PrivacyPolicyGroupRepository privacyPolicyGroupRepository;

    @Test
    void createFailsWhenPolicyCodeDuplicatedWithinSamePortal() {
        PrivacyPolicyGroupService service = new PrivacyPolicyGroupService(privacyPolicyGroupRepository);
        when(privacyPolicyGroupRepository.existsByPortalIdAndPolicyCode("PRT_1", "PRIVACY_POLICY")).thenReturn(true);

        var request = new PrivacyPolicyGroupDto.CreateRequest("PRT_1", "PRIVACY_POLICY", "개인정보처리방침", null);

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void createSucceedsWhenSamePolicyCodeUsedByDifferentPortal() {
        PrivacyPolicyGroupService service = new PrivacyPolicyGroupService(privacyPolicyGroupRepository);
        when(privacyPolicyGroupRepository.existsByPortalIdAndPolicyCode("PRT_2", "PRIVACY_POLICY")).thenReturn(false);
        when(privacyPolicyGroupRepository.saveAndFlush(org.mockito.ArgumentMatchers.any(PrivacyPolicyGroup.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var request = new PrivacyPolicyGroupDto.CreateRequest("PRT_2", "PRIVACY_POLICY", "개인정보처리방침", null);
        PrivacyPolicyGroupDto.Response response = service.create(request);

        assertThat(response.portalId()).isEqualTo("PRT_2");
        assertThat(response.policyCode()).isEqualTo("PRIVACY_POLICY");
    }

    @Test
    void getByIdThrowsWhenNotFound() {
        PrivacyPolicyGroupService service = new PrivacyPolicyGroupService(privacyPolicyGroupRepository);
        when(privacyPolicyGroupRepository.findByIdAndDeletedYn("PPG_unknown", "N")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById("PPG_unknown"))
                .isInstanceOf(TenantException.class);
    }

    @Test
    void getByIdReturnsResponseWhenFound() {
        PrivacyPolicyGroupService service = new PrivacyPolicyGroupService(privacyPolicyGroupRepository);
        PrivacyPolicyGroup group = PrivacyPolicyGroup.create("PRT_1", "PRIVACY_POLICY", "개인정보처리방침", "설명");
        when(privacyPolicyGroupRepository.findByIdAndDeletedYn(group.getId(), "N")).thenReturn(Optional.of(group));

        PrivacyPolicyGroupDto.Response response = service.getById(group.getId());

        assertThat(response.portalId()).isEqualTo("PRT_1");
        assertThat(response.policyCode()).isEqualTo("PRIVACY_POLICY");
    }
}
