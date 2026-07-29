package com.platform.module.privacy.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.platform.framework.core.common.exception.TenantException;
import com.platform.module.privacy.domain.PrivacyPolicyGroup;
import com.platform.module.privacy.dto.PrivacyPolicyVersionDto;
import com.platform.module.privacy.repository.PrivacyPolicyGroupRepository;
import com.platform.module.privacy.repository.PrivacyPolicyVersionRepository;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PrivacyPolicyVersionServiceTest {

    @Mock
    private PrivacyPolicyVersionRepository privacyPolicyVersionRepository;

    @Mock
    private PrivacyPolicyGroupRepository privacyPolicyGroupRepository;

    private PrivacyPolicyVersionService service() {
        return new PrivacyPolicyVersionService(privacyPolicyVersionRepository, privacyPolicyGroupRepository);
    }

    @Test
    void createFailsWhenGroupNotFound() {
        when(privacyPolicyGroupRepository.findByIdAndDeletedYn("PPG_unknown", "N")).thenReturn(Optional.empty());

        var request = new PrivacyPolicyVersionDto.CreateRequest("1.0", "내용", LocalDate.of(2026, 1, 1), null);

        assertThatThrownBy(() -> service().create("PPG_unknown", request))
                .isInstanceOf(TenantException.class);
    }

    @Test
    void createSucceedsWhenGroupExists() {
        PrivacyPolicyGroup group = PrivacyPolicyGroup.create("PRT_1", "PRIVACY_POLICY", "개인정보처리방침", null);
        when(privacyPolicyGroupRepository.findByIdAndDeletedYn(group.getId(), "N")).thenReturn(Optional.of(group));

        var request = new PrivacyPolicyVersionDto.CreateRequest("1.0", "제1조...", LocalDate.of(2026, 1, 1), "최초 시행");
        PrivacyPolicyVersionDto.Response response = service().create(group.getId(), request);

        assertThat(response.versionNo()).isEqualTo("1.0");
        assertThat(response.effectiveDate()).isEqualTo(LocalDate.of(2026, 1, 1));
    }
}
