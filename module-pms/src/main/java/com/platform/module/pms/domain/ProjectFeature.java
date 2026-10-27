package com.platform.module.pms.domain;

import com.platform.framework.core.common.utils.PrefixedIdGenerator;
import com.platform.framework.security.tenant.TenantAwareEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * 운영 인수 대상 기능
 * 테이블: pms_project_feature (docs/pms/03-ERD.md). 생성된 골격이며, 상태 전이 등 도메인 메서드는 서비스 구현 시 추가한다.
 */
@Entity
@Table(name = "pms_project_feature")
public class ProjectFeature extends TenantAwareEntity {

    /** 프로젝트 ID */
    @Column(name = "project_id", length = 64, nullable = false)
    private String projectId;

    /** 기능명 */
    @Column(name = "feature_name", length = 300, nullable = false)
    private String featureName;

    /** 운영 인수 대상 여부 */
    @Column(name = "handover_yn", length = 1, nullable = false)
    private String handoverYn = "N";

    /** 출처: WBS/RFP/수동 */
    @Column(name = "source_code", length = 20)
    private String sourceCode;

    protected ProjectFeature() {
    }

    private ProjectFeature(String id) {
        super(id);
    }

    public static ProjectFeature create() {
        return new ProjectFeature(PrefixedIdGenerator.generate("PFT"));
    }

    public ProjectFeature withProjectId(String value) {
        this.projectId = value;
        return this;
    }

    public ProjectFeature withFeatureName(String value) {
        this.featureName = value;
        return this;
    }

    public ProjectFeature withHandoverYn(String value) {
        this.handoverYn = value;
        return this;
    }

    public ProjectFeature withSourceCode(String value) {
        this.sourceCode = value;
        return this;
    }

    public String getProjectId() {
        return projectId;
    }

    public String getFeatureName() {
        return featureName;
    }

    public String getHandoverYn() {
        return handoverYn;
    }

    public String getSourceCode() {
        return sourceCode;
    }
}
