package com.platform.framework.core.common.response;

import java.util.List;

/**
 * Service 계층에서 Controller로 페이징 결과를 넘길 때 사용하는 홀더.
 * Controller는 이를 그대로 {@code ApiResponse.successList(result.content(), result.pageInfo())} 로 응답한다.
 */
public record PagedResult<T>(List<T> content, PageInfo pageInfo) {
}
