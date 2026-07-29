package com.platform.module.notice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.platform.framework.core.common.exception.TenantException;
import com.platform.module.notice.domain.Notice;
import com.platform.module.notice.dto.NoticeDto;
import com.platform.module.notice.repository.NoticeRepository;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NoticeServiceTest {

    @Mock
    private NoticeRepository noticeRepository;

    @Test
    void getByIdThrowsWhenNotFound() {
        NoticeService service = new NoticeService(noticeRepository);
        when(noticeRepository.findByIdAndDeletedYn("NTC_unknown", "N")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById("NTC_unknown"))
                .isInstanceOf(TenantException.class);
    }

    @Test
    void getByIdReturnsResponseWhenFound() {
        NoticeService service = new NoticeService(noticeRepository);
        Notice notice = Notice.create("PRT_1", "공지", "설 연휴 안내", "내용",
                LocalDate.of(2026, 2, 14), LocalDate.of(2026, 2, 18), "N",
                "로그인", "1회", "중형", "레이어팝업", "중앙");
        when(noticeRepository.findByIdAndDeletedYn(notice.getId(), "N")).thenReturn(Optional.of(notice));

        NoticeDto.Response response = service.getById(notice.getId());

        assertThat(response.portalId()).isEqualTo("PRT_1");
        assertThat(response.noticeType()).isEqualTo("공지");
        assertThat(response.popupYn()).isEqualTo("N");
        assertThat(response.applyType()).isEqualTo("로그인");
        assertThat(response.popupSize()).isEqualTo("중형");
    }

    @Test
    void createDefaultsPopupYnToNWhenNotY() {
        NoticeService service = new NoticeService(noticeRepository);
        when(noticeRepository.saveAndFlush(org.mockito.ArgumentMatchers.any(Notice.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var request = new NoticeDto.CreateRequest(
                "PRT_1", "동의서", "마케팅 수신 동의", "내용",
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31), null,
                null, null, null, null, null);
        NoticeDto.Response response = service.create(request);

        assertThat(response.popupYn()).isEqualTo("N");
        assertThat(response.noticeType()).isEqualTo("동의서");
    }
}
