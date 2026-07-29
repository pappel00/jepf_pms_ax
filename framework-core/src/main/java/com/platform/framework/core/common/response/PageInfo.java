package com.platform.framework.core.common.response;

import org.springframework.data.domain.Page;

/**
 * MAD 4500 Response 규격의 pagination 블록.
 */
public record PageInfo(int page, int size, long totalElements, int totalPages, boolean hasNext) {

    /** Spring Data {@link Page} -> PageInfo 변환. page는 1-base로 노출한다. */
    public static PageInfo of(Page<?> page) {
        return new PageInfo(
                page.getNumber() + 1,
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.hasNext()
        );
    }
}
