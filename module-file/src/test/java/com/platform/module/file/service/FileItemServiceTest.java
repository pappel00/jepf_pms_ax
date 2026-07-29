package com.platform.module.file.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.platform.framework.core.common.exception.TenantException;
import com.platform.framework.security.tenant.TenantContext;
import com.platform.module.file.domain.FileGroup;
import com.platform.module.file.dto.FileItemDto;
import com.platform.module.file.repository.FileGroupRepository;
import com.platform.module.file.repository.FileItemRepository;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

@ExtendWith(MockitoExtension.class)
class FileItemServiceTest {

    @Mock
    private FileItemRepository fileItemRepository;

    @Mock
    private FileGroupRepository fileGroupRepository;

    @Mock
    private FileStorageService fileStorageService;

    private FileItemService service() {
        return new FileItemService(fileItemRepository, fileGroupRepository, fileStorageService);
    }

    @AfterEach
    void clearTenant() {
        TenantContext.clear();
    }

    @Test
    void createFailsWhenGroupNotFound() {
        when(fileGroupRepository.findByIdAndDeletedYn("FGR_unknown", "N")).thenReturn(Optional.empty());

        var request = new FileItemDto.CreateRequest("guide.pdf", "/files/guide.pdf", 1024L, 1, null);

        assertThatThrownBy(() -> service().create("FGR_unknown", request))
                .isInstanceOf(TenantException.class);
    }

    @Test
    void createSucceedsWhenSortOrderIsNull() {
        FileGroup group = FileGroup.create("NOTICE_ATTACH", "공지사항 첨부파일", null);
        when(fileGroupRepository.findByIdAndDeletedYn(group.getId(), "N")).thenReturn(Optional.of(group));

        var request = new FileItemDto.CreateRequest("guide.pdf", "/files/guide.pdf", 1024L, null, null);
        FileItemDto.Response response = service().create(group.getId(), request);

        assertThat(response.sortOrder()).isEqualTo(0);
        assertThat(response.fileName()).isEqualTo("guide.pdf");
    }

    @Test
    void uploadFailsWhenGroupNotFound() {
        when(fileGroupRepository.findByIdAndDeletedYn("FGR_unknown", "N")).thenReturn(Optional.empty());
        var file = new MockMultipartFile("file", "guide.pdf", "application/pdf", "content".getBytes());

        assertThatThrownBy(() -> service().upload("FGR_unknown", file, null, null))
                .isInstanceOf(TenantException.class);
    }

    @Test
    void uploadSavesFileAndCreatesItemWithActualSize() {
        TenantContext.setTenantId("TNT_1");
        FileGroup group = FileGroup.create("NOTICE_ATTACH", "공지사항 첨부파일", null);
        when(fileGroupRepository.findByIdAndDeletedYn(group.getId(), "N")).thenReturn(Optional.of(group));
        when(fileStorageService.save(any(), anyString())).thenReturn("TNT_1/" + group.getId() + "/abc123_guide.pdf");

        var file = new MockMultipartFile("file", "guide.pdf", "application/pdf", "hello world".getBytes());
        FileItemDto.Response response = service().upload(group.getId(), file, 2, "설명");

        assertThat(response.fileName()).isEqualTo("guide.pdf");
        assertThat(response.fileSize()).isEqualTo(file.getSize());
        assertThat(response.sortOrder()).isEqualTo(2);
        assertThat(response.filePath()).isEqualTo("TNT_1/" + group.getId() + "/abc123_guide.pdf");
    }
}
