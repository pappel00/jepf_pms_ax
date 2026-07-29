package com.platform.module.file.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.platform.framework.core.common.exception.BusinessException;
import com.platform.framework.core.common.exception.TenantException;
import com.platform.module.file.domain.FileGroup;
import com.platform.module.file.dto.FileGroupDto;
import com.platform.module.file.repository.FileGroupRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FileGroupServiceTest {

    @Mock
    private FileGroupRepository fileGroupRepository;

    @Test
    void createFailsWhenGroupCodeDuplicated() {
        FileGroupService service = new FileGroupService(fileGroupRepository);
        when(fileGroupRepository.existsByGroupCode("NOTICE_ATTACH")).thenReturn(true);

        var request = new FileGroupDto.CreateRequest("NOTICE_ATTACH", "공지사항 첨부파일", null);

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void getByIdThrowsWhenNotFound() {
        FileGroupService service = new FileGroupService(fileGroupRepository);
        when(fileGroupRepository.findByIdAndDeletedYn("FGR_unknown", "N")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById("FGR_unknown"))
                .isInstanceOf(TenantException.class);
    }

    @Test
    void getByIdReturnsResponseWhenFound() {
        FileGroupService service = new FileGroupService(fileGroupRepository);
        FileGroup group = FileGroup.create("NOTICE_ATTACH", "공지사항 첨부파일", "설명");
        when(fileGroupRepository.findByIdAndDeletedYn(group.getId(), "N")).thenReturn(Optional.of(group));

        FileGroupDto.Response response = service.getById(group.getId());

        assertThat(response.groupCode()).isEqualTo("NOTICE_ATTACH");
    }
}
