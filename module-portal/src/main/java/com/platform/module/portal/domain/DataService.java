package com.platform.module.portal.domain;

import com.platform.framework.core.common.utils.PrefixedIdGenerator;
import com.platform.framework.security.tenant.TenantAwareEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * 데이터서비스(프론트가 호출하는 API 엔드포인트 메타데이터) 마스터.
 * 사진 속 BackOffice "데이터서비스 관리" 화면 대응. 내부 API URL을 노출하는 민감한 정보라
 * DataServiceController 는 전체 ADMIN 전용으로 막는다 (조회 포함).
 */
@Entity
@Table(name = "prt_data_service",
        uniqueConstraints = @UniqueConstraint(name = "uk_dsvc_tenant_code", columnNames = {"tenant_id", "data_service_code"}))
public class DataService extends TenantAwareEntity {

    @Column(name = "data_service_code", length = 30, nullable = false)
    private String dataServiceCode;

    @Column(name = "data_service_name", length = 200, nullable = false)
    private String dataServiceName;

    /** GET / POST / PUT / DELETE */
    @Column(name = "http_method", length = 10, nullable = false)
    private String httpMethod;

    @Column(name = "url", length = 500, nullable = false)
    private String url;

    @Column(name = "use_yn", length = 1, nullable = false)
    private String useYn = "Y";

    protected DataService() {
    }

    private DataService(String dataServiceCode, String dataServiceName, String httpMethod, String url) {
        super(PrefixedIdGenerator.generate("DTS"));
        this.dataServiceCode = dataServiceCode;
        this.dataServiceName = dataServiceName;
        this.httpMethod = httpMethod;
        this.url = url;
    }

    public static DataService create(String dataServiceCode, String dataServiceName, String httpMethod, String url) {
        return new DataService(dataServiceCode, dataServiceName, httpMethod, url);
    }

    public void update(String dataServiceName, String httpMethod, String url) {
        this.dataServiceName = dataServiceName;
        this.httpMethod = httpMethod;
        this.url = url;
    }

    public void activate() {
        this.useYn = "Y";
    }

    public void deactivate() {
        this.useYn = "N";
    }

    public String getDataServiceCode() {
        return dataServiceCode;
    }

    public String getDataServiceName() {
        return dataServiceName;
    }

    public String getHttpMethod() {
        return httpMethod;
    }

    public String getUrl() {
        return url;
    }

    public String getUseYn() {
        return useYn;
    }
}
