package com.platform.module.portal.domain;

import com.platform.framework.core.common.utils.PrefixedIdGenerator;
import com.platform.framework.security.tenant.TenantAwareEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * Page - DataService 매핑(N:M). 사진 속 페이지 관리 화면의 "데이터서비스" 탭 대응.
 * "이 페이지는 이 데이터서비스들을 호출한다"는 연결 정보만 담는 순수 매핑 엔티티라, 다른
 * 엔티티들과 달리 markDeleted() 소프트 삭제 대신 하드 삭제(repository.delete)로 관리한다
 * (BaseEntity 상속 필드는 그대로 남지만 deletedYn 은 항상 "N"으로 미사용).
 */
@Entity
@Table(name = "prt_page_data_service",
        uniqueConstraints = @UniqueConstraint(name = "uk_pds_tenant_page_dsvc",
                columnNames = {"tenant_id", "page_id", "data_service_id"}))
public class PageDataService extends TenantAwareEntity {

    @Column(name = "page_id", length = 64, nullable = false)
    private String pageId;

    @Column(name = "data_service_id", length = 64, nullable = false)
    private String dataServiceId;

    protected PageDataService() {
    }

    private PageDataService(String pageId, String dataServiceId) {
        super(PrefixedIdGenerator.generate("PDS"));
        this.pageId = pageId;
        this.dataServiceId = dataServiceId;
    }

    public static PageDataService create(String pageId, String dataServiceId) {
        return new PageDataService(pageId, dataServiceId);
    }

    public String getPageId() {
        return pageId;
    }

    public String getDataServiceId() {
        return dataServiceId;
    }
}
