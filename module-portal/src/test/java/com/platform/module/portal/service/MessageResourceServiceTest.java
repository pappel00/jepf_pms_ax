package com.platform.module.portal.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.platform.framework.core.common.exception.BusinessException;
import com.platform.framework.core.common.exception.TenantException;
import com.platform.module.portal.domain.MessageResource;
import com.platform.module.portal.dto.MessageResourceDto;
import com.platform.module.portal.repository.MessageResourceRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MessageResourceServiceTest {

    @Mock
    private MessageResourceRepository messageResourceRepository;

    @Test
    void createFailsWhenCodeAndLanguageDuplicated() {
        MessageResourceService service = new MessageResourceService(messageResourceRepository);
        when(messageResourceRepository.existsByMessageResourceCodeAndLanguageId("MSG_ALT_CHK_UNIT_DUP", "ko"))
                .thenReturn(true);

        var request = new MessageResourceDto.CreateRequest(
                "MSG_ALT_CHK_UNIT_DUP", "Alert", "ko", "이미 등록된 정보가 존재합니다.", null);

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void resolveThrowsWhenNotFound() {
        MessageResourceService service = new MessageResourceService(messageResourceRepository);
        when(messageResourceRepository.findByMessageResourceCodeAndLanguageIdAndDeletedYn(
                "MSG_UNKNOWN", "ko", "N")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.resolve("MSG_UNKNOWN", "ko"))
                .isInstanceOf(TenantException.class);
    }

    @Test
    void resolveReturnsResponseWhenFound() {
        MessageResourceService service = new MessageResourceService(messageResourceRepository);
        MessageResource resource = MessageResource.create(
                "MSG_ALT_CHK_UNIT_DUP", "Alert", "ko", "이미 등록된 정보가 존재합니다.", null);
        when(messageResourceRepository.findByMessageResourceCodeAndLanguageIdAndDeletedYn(
                "MSG_ALT_CHK_UNIT_DUP", "ko", "N")).thenReturn(Optional.of(resource));

        MessageResourceDto.Response response = service.resolve("MSG_ALT_CHK_UNIT_DUP", "ko");

        assertThat(response.value()).isEqualTo("이미 등록된 정보가 존재합니다.");
        assertThat(response.languageId()).isEqualTo("ko");
    }
}
