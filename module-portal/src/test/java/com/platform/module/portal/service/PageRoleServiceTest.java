package com.platform.module.portal.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.platform.module.portal.domain.PageRole;
import com.platform.module.portal.repository.PageRepository;
import com.platform.module.portal.repository.PageRoleRepository;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PageRoleServiceTest {

    @Mock
    private PageRoleRepository pageRoleRepository;

    @Mock
    private PageRepository pageRepository;

    @Test
    void isAccessibleWhenNoMappingsExist() {
        PageRoleService service = new PageRoleService(pageRoleRepository, pageRepository);
        when(pageRoleRepository.findAllByPageId("PGE_1")).thenReturn(List.of());

        // 매핑이 하나도 없으면 로그인한 사용자 누구나 접근 가능해야 한다 (기본 개방).
        assertThat(service.isAccessible("PGE_1", Set.of("ROLE_USER"))).isTrue();
    }

    @Test
    void isAccessibleWhenUserHasMatchingRole() {
        PageRoleService service = new PageRoleService(pageRoleRepository, pageRepository);
        when(pageRoleRepository.findAllByPageId("PGE_1"))
                .thenReturn(List.of(PageRole.create("PGE_1", "ROLE_ADMIN")));

        assertThat(service.isAccessible("PGE_1", Set.of("ROLE_USER", "ROLE_ADMIN"))).isTrue();
    }

    @Test
    void isNotAccessibleWhenUserLacksMatchingRole() {
        PageRoleService service = new PageRoleService(pageRoleRepository, pageRepository);
        when(pageRoleRepository.findAllByPageId("PGE_1"))
                .thenReturn(List.of(PageRole.create("PGE_1", "ROLE_ADMIN")));

        assertThat(service.isAccessible("PGE_1", Set.of("ROLE_USER"))).isFalse();
    }
}
