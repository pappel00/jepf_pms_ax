package com.platform.module.portal.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.platform.framework.core.common.exception.BusinessException;
import com.platform.framework.core.common.exception.TenantException;
import com.platform.module.portal.domain.Page;
import com.platform.module.portal.domain.PageRole;
import com.platform.module.portal.dto.RolePageDto;
import com.platform.module.portal.repository.PageRepository;
import com.platform.module.portal.repository.PageRoleRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RolePageServiceTest {

    @Mock
    private PageRoleRepository pageRoleRepository;

    @Mock
    private PageRepository pageRepository;

    private RolePageService service() {
        return new RolePageService(pageRoleRepository, pageRepository);
    }

    @Test
    void addPageFailsWhenPageNotFound() {
        when(pageRepository.findByIdAndDeletedYn("PGE_unknown", "N")).thenReturn(Optional.empty());

        var request = new RolePageDto.CreateRequest("PGE_unknown");

        assertThatThrownBy(() -> service().addPage("ROLE_ADMIN", request))
                .isInstanceOf(TenantException.class);
    }

    @Test
    void addPageFailsWhenMappingDuplicated() {
        Page page = Page.create("USR_LIST", "사용자 목록", "일반", "PC", null, null);
        when(pageRepository.findByIdAndDeletedYn(page.getId(), "N")).thenReturn(Optional.of(page));
        when(pageRoleRepository.existsByPageIdAndRoleCode(page.getId(), "ROLE_ADMIN")).thenReturn(true);

        var request = new RolePageDto.CreateRequest(page.getId());

        assertThatThrownBy(() -> service().addPage("ROLE_ADMIN", request))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void getPagesReturnsPagesMappedToThisRoleCode() {
        Page page = Page.create("USR_LIST", "사용자 목록", "일반", "PC", null, null);
        when(pageRoleRepository.findAllByRoleCode("ROLE_ADMIN"))
                .thenReturn(List.of(PageRole.create(page.getId(), "ROLE_ADMIN")));
        when(pageRepository.findAllById(List.of(page.getId()))).thenReturn(List.of(page));

        var result = service().getPages("ROLE_ADMIN");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).pageCode()).isEqualTo("USR_LIST");
    }
}
