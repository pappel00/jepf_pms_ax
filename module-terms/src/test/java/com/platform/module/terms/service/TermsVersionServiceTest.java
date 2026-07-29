package com.platform.module.terms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.platform.framework.core.common.exception.TenantException;
import com.platform.module.terms.domain.TermsGroup;
import com.platform.module.terms.dto.TermsVersionDto;
import com.platform.module.terms.repository.TermsGroupRepository;
import com.platform.module.terms.repository.TermsVersionRepository;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TermsVersionServiceTest {

    @Mock
    private TermsVersionRepository termsVersionRepository;

    @Mock
    private TermsGroupRepository termsGroupRepository;

    private TermsVersionService service() {
        return new TermsVersionService(termsVersionRepository, termsGroupRepository);
    }

    @Test
    void createFailsWhenGroupNotFound() {
        when(termsGroupRepository.findByIdAndDeletedYn("TGR_unknown", "N")).thenReturn(Optional.empty());

        var request = new TermsVersionDto.CreateRequest("1.0", "내용", LocalDate.of(2026, 1, 1), null);

        assertThatThrownBy(() -> service().create("TGR_unknown", request))
                .isInstanceOf(TenantException.class);
    }

    @Test
    void createSucceedsWhenGroupExists() {
        TermsGroup group = TermsGroup.create("PRT_1", "SERVICE_TERMS", "서비스 이용약관", "필수", null);
        when(termsGroupRepository.findByIdAndDeletedYn(group.getId(), "N")).thenReturn(Optional.of(group));

        var request = new TermsVersionDto.CreateRequest("1.0", "제1조...", LocalDate.of(2026, 1, 1), "최초 시행");
        TermsVersionDto.Response response = service().create(group.getId(), request);

        assertThat(response.versionNo()).isEqualTo("1.0");
        assertThat(response.effectiveDate()).isEqualTo(LocalDate.of(2026, 1, 1));
    }
}
