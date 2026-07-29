package com.platform.module.portal.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.platform.framework.core.common.exception.BusinessException;
import com.platform.framework.core.common.exception.TenantException;
import com.platform.module.portal.domain.Portal;
import com.platform.module.portal.dto.PortalDto;
import com.platform.module.portal.repository.PortalRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PortalServiceTest {

    @Mock
    private PortalRepository portalRepository;

    @Test
    void createFailsWhenPortalCodeDuplicated() {
        PortalService service = new PortalService(portalRepository);
        when(portalRepository.existsByPortalCode("ADMIN")).thenReturn(true);

        var request = new PortalDto.CreateRequest("ADMIN", "Admin Portal", "PC", "설명");

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void getByIdThrowsWhenNotFound() {
        PortalService service = new PortalService(portalRepository);
        when(portalRepository.findByIdAndDeletedYn("PRT_unknown", "N")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById("PRT_unknown"))
                .isInstanceOf(TenantException.class);
    }

    @Test
    void getByIdReturnsResponseWhenFound() {
        PortalService service = new PortalService(portalRepository);
        Portal portal = Portal.create("ADMIN", "Admin Portal", "PC", "설명");
        when(portalRepository.findByIdAndDeletedYn(portal.getId(), "N")).thenReturn(Optional.of(portal));

        PortalDto.Response response = service.getById(portal.getId());

        assertThat(response.portalCode()).isEqualTo("ADMIN");
        assertThat(response.deviceType()).isEqualTo("PC");
    }
}
