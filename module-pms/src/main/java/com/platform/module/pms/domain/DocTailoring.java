package com.platform.module.pms.domain;

import com.platform.framework.core.common.utils.PrefixedIdGenerator;
import com.platform.framework.security.tenant.TenantAwareEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * 문서 테일러링 (프로젝트별 적용 산출물)
 * 테이블: pms_doc_tailoring (docs/pms/03-ERD.md). 생성된 골격이며, 상태 전이 등 도메인 메서드는 서비스 구현 시 추가한다.
 */
@Entity
@Table(name = "pms_doc_tailoring")
public class DocTailoring extends TenantAwareEntity {

    /** 프로젝트 ID */
    @Column(name = "project_id", length = 64, nullable = false)
    private String projectId;

    /** 복사 원본 표준 산출물 ID */
    @Column(name = "std_doc_id", length = 64)
    private String stdDocId;

    /** 단계 */
    @Column(name = "phase_code", length = 20, nullable = false)
    private String phaseCode;

    /** 문서 종류 */
    @Column(name = "doc_name", length = 200, nullable = false)
    private String docName;

    /** 표준 필수 여부 */
    @Column(name = "required_yn", length = 1, nullable = false)
    private String requiredYn = "N";

    /** 적용/미적용 */
    @Column(name = "applied_code", length = 10, nullable = false)
    private String appliedCode;

    /** 미적용 사유 */
    @Column(name = "not_applied_reason", length = 500)
    private String notAppliedReason;

    /** 담당자 */
    @Column(name = "owner_user_id", length = 64)
    private String ownerUserId;

    /** 프로젝트 고유 추가 문서 여부 */
    @Column(name = "custom_yn", length = 1, nullable = false)
    private String customYn = "N";

    /** 비고 */
    @Column(name = "note", length = 500)
    private String note;

    /** 검토 완료 여부 */
    @Column(name = "reviewed_yn", length = 1, nullable = false)
    private String reviewedYn = "N";

    protected DocTailoring() {
    }

    private DocTailoring(String id) {
        super(id);
    }

    public static DocTailoring create() {
        return new DocTailoring(PrefixedIdGenerator.generate("DTL"));
    }

    public DocTailoring withProjectId(String value) {
        this.projectId = value;
        return this;
    }

    public DocTailoring withStdDocId(String value) {
        this.stdDocId = value;
        return this;
    }

    public DocTailoring withPhaseCode(String value) {
        this.phaseCode = value;
        return this;
    }

    public DocTailoring withDocName(String value) {
        this.docName = value;
        return this;
    }

    public DocTailoring withRequiredYn(String value) {
        this.requiredYn = value;
        return this;
    }

    public DocTailoring withAppliedCode(String value) {
        this.appliedCode = value;
        return this;
    }

    public DocTailoring withNotAppliedReason(String value) {
        this.notAppliedReason = value;
        return this;
    }

    public DocTailoring withOwnerUserId(String value) {
        this.ownerUserId = value;
        return this;
    }

    public DocTailoring withCustomYn(String value) {
        this.customYn = value;
        return this;
    }

    public DocTailoring withNote(String value) {
        this.note = value;
        return this;
    }

    public DocTailoring withReviewedYn(String value) {
        this.reviewedYn = value;
        return this;
    }

    public String getProjectId() {
        return projectId;
    }

    public String getStdDocId() {
        return stdDocId;
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

    public String getAppliedCode() {
        return appliedCode;
    }

    public String getNotAppliedReason() {
        return notAppliedReason;
    }

    public String getOwnerUserId() {
        return ownerUserId;
    }

    public String getCustomYn() {
        return customYn;
    }

    public String getNote() {
        return note;
    }

    public String getReviewedYn() {
        return reviewedYn;
    }
}
