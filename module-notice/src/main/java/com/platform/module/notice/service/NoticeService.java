package com.platform.module.notice.service;

import com.platform.framework.core.common.exception.TenantException;
import com.platform.framework.core.common.response.PageInfo;
import com.platform.framework.core.common.response.PagedResult;
import com.platform.module.notice.domain.Notice;
import com.platform.module.notice.dto.NoticeDto;
import com.platform.module.notice.exception.NoticeModuleErrorCode;
import com.platform.module.notice.repository.NoticeRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class NoticeService {

    private final NoticeRepository noticeRepository;

    public NoticeService(NoticeRepository noticeRepository) {
        this.noticeRepository = noticeRepository;
    }

    @Transactional
    public NoticeDto.Response create(NoticeDto.CreateRequest request) {
        Notice notice = Notice.create(
                request.portalId(), request.noticeType(), request.title(), request.content(),
                request.startDate(), request.endDate(), request.popupYn(),
                request.applyType(), request.popupFrequency(), request.popupSize(),
                request.popupShape(), request.popupPosition());
        return NoticeDto.Response.from(noticeRepository.saveAndFlush(notice));
    }

    public NoticeDto.Response getById(String id) {
        return NoticeDto.Response.from(findActiveOrThrow(id));
    }

    /** portalId가 주어지면 그 포탈의 공지/동의서만, 없으면 테넌트 전체를 조회한다. */
    public PagedResult<NoticeDto.Response> getList(String portalId, Pageable pageable) {
        Page<Notice> page = (portalId != null && !portalId.isBlank())
                ? noticeRepository.findAllByPortalId(portalId, pageable)
                : noticeRepository.findAll(pageable);
        return new PagedResult<>(page.map(NoticeDto.Response::from).getContent(), PageInfo.of(page));
    }

    @Transactional
    public NoticeDto.Response update(String id, NoticeDto.UpdateRequest request) {
        Notice notice = findActiveOrThrow(id);
        notice.update(request.noticeType(), request.title(), request.content(),
                request.startDate(), request.endDate(), request.popupYn(),
                request.applyType(), request.popupFrequency(), request.popupSize(),
                request.popupShape(), request.popupPosition());
        if ("Y".equals(request.useYn())) {
            notice.activate();
        } else {
            notice.deactivate();
        }
        return NoticeDto.Response.from(notice);
    }

    @Transactional
    public void delete(String id) {
        findActiveOrThrow(id).markDeleted();
    }

    private Notice findActiveOrThrow(String id) {
        return noticeRepository.findByIdAndDeletedYn(id, "N")
                .orElseThrow(() -> new TenantException(NoticeModuleErrorCode.NOTICE_NOT_FOUND,
                        "존재하지 않는 공지입니다: " + id));
    }
}
