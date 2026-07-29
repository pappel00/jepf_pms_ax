package com.platform.module.portal.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.platform.framework.core.common.exception.BusinessException;
import com.platform.framework.core.common.exception.TenantException;
import com.platform.module.portal.domain.Page;
import com.platform.module.portal.domain.PageDataService;
import com.platform.module.portal.dto.PageDataServiceDto;
import com.platform.module.portal.repository.PageDataServiceRepository;
import com.platform.module.portal.repository.PageRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PageDataServiceServiceTest {

    @Mock
    private PageDataServiceRepository pageDataServiceRepository;

    @Mock
    private PageRepository pageRepository;

    @Test
    void createFailsWhenPageNotFound() {
        PageDataServiceService service = new PageDataServiceService(pageDataServiceRepository, pageRepository);
        when(pageRepository.findByIdAndDeletedYn("PGE_unknown", "N")).thenReturn(Optional.empty());

        var request = new PageDataServiceDto.CreateRequest("DTS_1");

        assertThatThrownBy(() -> service.create("PGE_unknown", request))
                .isInstanceOf(TenantException.class);
    }

    @Test
    void createFailsWhenMappingDuplicated() {
        PageDataServiceService service = new PageDataServiceService(pageDataServiceRepository, pageRepository);
        Page page = Page.create("USR_LIST", "사용자 목록", "일반", "PC", null, null);
        when(pageRepository.findByIdAndDeletedYn(page.getId(), "N")).thenReturn(Optional.of(page));
        when(pageDataServiceRepository.existsByPageIdAndDataServiceId(page.getId(), "DTS_1")).thenReturn(true);

        var request = new PageDataServiceDto.CreateRequest("DTS_1");

        assertThatThrownBy(() -> service.create(page.getId(), request))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void createSucceedsWhenPageExistsAndNotDuplicated() {
        PageDataServiceService service = new PageDataServiceService(pageDataServiceRepository, pageRepository);
        Page page = Page.create("USR_LIST", "사용자 목록", "일반", "PC", null, null);
        when(pageRepository.findByIdAndDeletedYn(page.getId(), "N")).thenReturn(Optional.of(page));
        when(pageDataServiceRepository.existsByPageIdAndDataServiceId(page.getId(), "DTS_1")).thenReturn(false);

        PageDataService saved = PageDataService.create(page.getId(), "DTS_1");
        when(pageDataServiceRepository.saveAndFlush(org.mockito.ArgumentMatchers.any(PageDataService.class)))
                .thenReturn(saved);

        var request = new PageDataServiceDto.CreateRequest("DTS_1");
        PageDataServiceDto.Response response = service.create(page.getId(), request);

        assertThat(response.pageId()).isEqualTo(page.getId());
        assertThat(response.dataServiceId()).isEqualTo("DTS_1");
    }
}
