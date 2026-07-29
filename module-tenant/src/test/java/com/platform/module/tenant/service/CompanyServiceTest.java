package com.platform.module.tenant.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.platform.framework.core.common.exception.BusinessException;
import com.platform.framework.core.common.exception.TenantException;
import com.platform.module.tenant.domain.Company;
import com.platform.module.tenant.dto.CompanyDto;
import com.platform.module.tenant.event.TenantCreatedEvent;
import com.platform.module.tenant.repository.CompanyRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class CompanyServiceTest {

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private CompanyService service() {
        return new CompanyService(companyRepository, eventPublisher);
    }

    @Test
    void createFailsWhenCompanyCodeDuplicated() {
        when(companyRepository.existsByCompanyCode("ACME")).thenReturn(true);

        var request = new CompanyDto.CreateRequest("ACME", "에이씨엠이", "설명");

        assertThatThrownBy(() -> service().create(request))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void createPublishesTenantCreatedEvent() {
        when(companyRepository.existsByCompanyCode("ACME")).thenReturn(false);
        Company company = Company.create("ACME", "에이씨엠이", "설명");
        when(companyRepository.save(any(Company.class))).thenReturn(company);

        var request = new CompanyDto.CreateRequest("ACME", "에이씨엠이", "설명");
        service().create(request);

        verify(eventPublisher).publishEvent(any(TenantCreatedEvent.class));
    }

    @Test
    void getByIdThrowsWhenNotFound() {
        when(companyRepository.findByIdAndDeletedYn("TNT_unknown", "N")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().getById("TNT_unknown"))
                .isInstanceOf(TenantException.class);
    }

    @Test
    void getByIdReturnsResponseWhenFound() {
        Company company = Company.create("ACME", "에이씨엠이", "설명");
        when(companyRepository.findByIdAndDeletedYn(company.getId(), "N")).thenReturn(Optional.of(company));

        CompanyDto.Response response = service().getById(company.getId());

        assertThat(response.companyCode()).isEqualTo("ACME");
        assertThat(response.companyName()).isEqualTo("에이씨엠이");
    }
}
