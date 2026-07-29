package com.platform.module.form.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.platform.framework.core.common.exception.TenantException;
import com.platform.framework.security.tenant.TenantContext;
import com.platform.module.file.service.FileStorageService;
import com.platform.module.form.domain.FormGroup;
import com.platform.module.form.dto.FormVersionDto;
import com.platform.module.form.repository.FormGroupRepository;
import com.platform.module.form.repository.FormVersionRepository;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

@ExtendWith(MockitoExtension.class)
class FormVersionServiceTest {

    @Mock
    private FormVersionRepository formVersionRepository;

    @Mock
    private FormGroupRepository formGroupRepository;

    @Mock
    private FileStorageService fileStorageService;

    private FormVersionService service() {
        return new FormVersionService(formVersionRepository, formGroupRepository, fileStorageService);
    }

    @AfterEach
    void clearTenant() {
        TenantContext.clear();
    }

    @Test
    void createFailsWhenGroupNotFound() {
        when(formGroupRepository.findByIdAndDeletedYn("FMG_unknown", "N")).thenReturn(Optional.empty());

        var request = new FormVersionDto.CreateRequest(
                "1.0", "leave.xlsx", "/forms/leave.xlsx", 1024L, LocalDate.of(2026, 1, 1), null);

        assertThatThrownBy(() -> service().create("FMG_unknown", request))
                .isInstanceOf(TenantException.class);
    }

    @Test
    void createSucceedsWhenGroupExists() {
        FormGroup group = FormGroup.create("PRT_1", "LEAVE_REQUEST", "휴가신청서", "인사", null);
        when(formGroupRepository.findByIdAndDeletedYn(group.getId(), "N")).thenReturn(Optional.of(group));

        var request = new FormVersionDto.CreateRequest(
                "1.0", "leave.xlsx", "/forms/leave.xlsx", 1024L, LocalDate.of(2026, 1, 1), "최초 시행");
        FormVersionDto.Response response = service().create(group.getId(), request);

        assertThat(response.versionNo()).isEqualTo("1.0");
        assertThat(response.fileName()).isEqualTo("leave.xlsx");
        assertThat(response.effectiveDate()).isEqualTo(LocalDate.of(2026, 1, 1));
    }

    @Test
    void uploadFailsWhenGroupNotFound() {
        when(formGroupRepository.findByIdAndDeletedYn("FMG_unknown", "N")).thenReturn(Optional.empty());
        var file = new MockMultipartFile("file", "leave.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "content".getBytes());

        assertThatThrownBy(() -> service().upload("FMG_unknown", file, "1.0", LocalDate.of(2026, 1, 1), null))
                .isInstanceOf(TenantException.class);
    }

    @Test
    void uploadSavesFileAndCreatesVersionWithActualNameAndSize() {
        TenantContext.setTenantId("TNT_1");
        FormGroup group = FormGroup.create("PRT_1", "LEAVE_REQUEST", "휴가신청서", "인사", null);
        when(formGroupRepository.findByIdAndDeletedYn(group.getId(), "N")).thenReturn(Optional.of(group));
        when(fileStorageService.save(any(), anyString()))
                .thenReturn("TNT_1/" + group.getId() + "/abc123_leave.xlsx");

        var file = new MockMultipartFile("file", "leave.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "hello world".getBytes());
        FormVersionDto.Response response = service().upload(group.getId(), file, "1.0", LocalDate.of(2026, 1, 1), "최초 시행");

        assertThat(response.fileName()).isEqualTo("leave.xlsx");
        assertThat(response.fileSize()).isEqualTo(file.getSize());
        assertThat(response.versionNo()).isEqualTo("1.0");
        assertThat(response.filePath()).isEqualTo("TNT_1/" + group.getId() + "/abc123_leave.xlsx");
    }
}
