package com.platform.module.notice.dto;

import com.platform.module.notice.domain.Notice;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public final class NoticeDto {

    private NoticeDto() {
    }

    @Schema(name = "NoticeCreateRequest")
    public record CreateRequest(
            @NotBlank @Schema(example = "PRT_a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4") String portalId,
            @NotBlank @Size(max = 20) @Schema(example = "공지") String noticeType,
            @NotBlank @Size(max = 300) @Schema(example = "설 연휴 고객센터 운영 안내") String title,
            @NotBlank @Schema(example = "안녕하세요. 설 연휴 기간 고객센터 운영시간을...") String content,
            @NotNull @Schema(example = "2026-02-14") LocalDate startDate,
            @NotNull @Schema(example = "2026-02-18") LocalDate endDate,
            @Schema(example = "N", defaultValue = "N") String popupYn,
            @Size(max = 20) @Schema(example = "로그인") String applyType,
            @Size(max = 20) @Schema(example = "1회") String popupFrequency,
            @Size(max = 20) @Schema(example = "중형") String popupSize,
            @Size(max = 20) @Schema(example = "레이어팝업") String popupShape,
            @Size(max = 20) @Schema(example = "중앙") String popupPosition
    ) {
    }

    @Schema(name = "NoticeUpdateRequest")
    public record UpdateRequest(
            @NotBlank @Size(max = 20) @Schema(example = "공지") String noticeType,
            @NotBlank @Size(max = 300) @Schema(example = "설 연휴 고객센터 운영 안내") String title,
            @NotBlank @Schema(example = "안녕하세요. 설 연휴 기간 고객센터 운영시간을...") String content,
            @NotNull @Schema(example = "2026-02-14") LocalDate startDate,
            @NotNull @Schema(example = "2026-02-18") LocalDate endDate,
            @Schema(example = "N") String popupYn,
            @Size(max = 20) @Schema(example = "로그인") String applyType,
            @Size(max = 20) @Schema(example = "1회") String popupFrequency,
            @Size(max = 20) @Schema(example = "중형") String popupSize,
            @Size(max = 20) @Schema(example = "레이어팝업") String popupShape,
            @Size(max = 20) @Schema(example = "중앙") String popupPosition,
            @NotBlank @Schema(example = "Y") String useYn
    ) {
        // portalId는 수정 대상에서 뺐다 — TermsGroup과 동일한 이유(다른 포탈로 옮기는 건
        // 사실상 새 공지를 만드는 것에 가깝다).
    }

    @Schema(name = "NoticeResponse")
    public record Response(
            String id,
            String tenantId,
            String portalId,
            String noticeType,
            String title,
            String content,
            LocalDate startDate,
            LocalDate endDate,
            String popupYn,
            String applyType,
            String popupFrequency,
            String popupSize,
            String popupShape,
            String popupPosition,
            String useYn,
            String deletedYn
    ) {
        public static Response from(Notice notice) {
            return new Response(
                    notice.getId(),
                    notice.getTenantId(),
                    notice.getPortalId(),
                    notice.getNoticeType(),
                    notice.getTitle(),
                    notice.getContent(),
                    notice.getStartDate(),
                    notice.getEndDate(),
                    notice.getPopupYn(),
                    notice.getApplyType(),
                    notice.getPopupFrequency(),
                    notice.getPopupSize(),
                    notice.getPopupShape(),
                    notice.getPopupPosition(),
                    notice.getUseYn(),
                    notice.getDeletedYn()
            );
        }
    }
}
