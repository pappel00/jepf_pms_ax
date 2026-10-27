package com.platform.module.pms.domain;

import com.platform.framework.core.common.utils.PrefixedIdGenerator;
import com.platform.framework.security.tenant.TenantAwareEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * 표준 산출물 마스터
 * 테이블: pms_std_doc (docs/pms/03-ERD.md). 생성된 골격이며, 상태 전이 등 도메인 메서드는 서비스 구현 시 추가한다.
 */
@Entity
@Table(name = "pms_std_doc", uniqueConstraints = {
        @UniqueConstraint(name = "uk_pms_std_doc", columnNames = {"tenant_id", "phase_code", "doc_name"})
})
public class StdDoc extends TenantAwareEntity {

    /** 단계: 분석/설계/개발/테스트 */
    @Column(name = "phase_code", length = 20, nullable = false)
    private String phaseCode;

    /** 문서 종류 */
    @Column(name = "doc_name", length = 200, nullable = false)
    private String docName;

    /** 표준 필수 여부 */
    @Column(name = "required_yn", length = 1, nullable = false)
    private String requiredYn = "N";

    /** 자동 적용 트리거 RFP 분류 */
    @Column(name = "rfp_category_code", length = 10)
    private String rfpCategoryCode;

    /** 정렬 */
    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;

    /** 사용 여부 */
    @Column(name = "use_yn", length = 1, nullable = false)
    private String useYn = "Y";

    protected StdDoc() {
    }

    private StdDoc(String id) {
        super(id);
    }

    public static StdDoc create() {
        return new StdDoc(PrefixedIdGenerator.generate("SDC"));
    }

    public StdDoc withPhaseCode(String value) {
        this.phaseCode = value;
        return this;
    }

    public StdDoc withDocName(String value) {
        this.docName = value;
        return this;
    }

    public StdDoc withRequiredYn(String value) {
        this.requiredYn = value;
        return this;
    }

    public StdDoc withRfpCategoryCode(String value) {
        this.rfpCategoryCode = value;
        return this;
    }

    public StdDoc withSortOrder(Integer value) {
        this.sortOrder = value;
        return this;
    }

    public StdDoc withUseYn(String value) {
        this.useYn = value;
        return this;
    }

    public String getPhaseCode() {
        return phaseCode;
    }

    public String getDocName() {
        return docName;
    }

    public String getRequiredYn() {
        return requiredYn;
    }

    public String getRfpCategoryCode() {
        return rfpCategoryCode;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public String getUseYn() {
        return useYn;
    }
}
