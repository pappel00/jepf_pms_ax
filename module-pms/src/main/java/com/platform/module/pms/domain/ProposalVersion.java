package com.platform.module.pms.domain;

import com.platform.framework.core.common.utils.PrefixedIdGenerator;
import com.platform.framework.security.tenant.TenantAwareEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDate;

/**
 * 제안서 버전 이력
 * 테이블: pms_proposal_version (docs/pms/03-ERD.md). 생성된 골격이며, 상태 전이 등 도메인 메서드는 서비스 구현 시 추가한다.
 */
@Entity
@Table(name = "pms_proposal_version", uniqueConstraints = {
        @UniqueConstraint(name = "uk_pms_proposal_version", columnNames = {"tenant_id", "proposal_id", "ver"})
})
public class ProposalVersion extends TenantAwareEntity {

    /** 제안서 ID */
    @Column(name = "proposal_id", length = 64, nullable = false)
    private String proposalId;

    /** 버전 (v0.1, v1.0 ...) */
    @Column(name = "ver", length = 20, nullable = false)
    private String ver;

    /** 버전 일자 */
    @Column(name = "version_date", nullable = false)
    private LocalDate versionDate;

    /** 작성자 */
    @Column(name = "author_user_id", length = 64, nullable = false)
    private String authorUserId;

    /** 변경 요약 */
    @Column(name = "summary", length = 500)
    private String summary;

    /** 파일 (cmn_file_group.id) */
    @Column(name = "file_group_id", length = 64)
    private String fileGroupId;

    /** 파일명 */
    @Column(name = "file_name", length = 300)
    private String fileName;

    /** 파일 크기 표시 */
    @Column(name = "file_size", length = 30)
    private String fileSize;

    /** 제출본 여부 */
    @Column(name = "submitted_yn", length = 1, nullable = false)
    private String submittedYn = "N";

    protected ProposalVersion() {
    }

    private ProposalVersion(String id) {
        super(id);
    }

    public static ProposalVersion create() {
        return new ProposalVersion(PrefixedIdGenerator.generate("PRV"));
    }

    public ProposalVersion withProposalId(String value) {
        this.proposalId = value;
        return this;
    }

    public ProposalVersion withVer(String value) {
        this.ver = value;
        return this;
    }

    public ProposalVersion withVersionDate(LocalDate value) {
        this.versionDate = value;
        return this;
    }

    public ProposalVersion withAuthorUserId(String value) {
        this.authorUserId = value;
        return this;
    }

    public ProposalVersion withSummary(String value) {
        this.summary = value;
        return this;
    }

    public ProposalVersion withFileGroupId(String value) {
        this.fileGroupId = value;
        return this;
    }

    public ProposalVersion withFileName(String value) {
        this.fileName = value;
        return this;
    }

    public ProposalVersion withFileSize(String value) {
        this.fileSize = value;
        return this;
    }

    public ProposalVersion withSubmittedYn(String value) {
        this.submittedYn = value;
        return this;
    }

    public String getProposalId() {
        return proposalId;
    }

    public String getVer() {
        return ver;
    }

    public LocalDate getVersionDate() {
        return versionDate;
    }

    public String getAuthorUserId() {
        return authorUserId;
    }

    public String getSummary() {
        return summary;
    }

    public String getFileGroupId() {
        return fileGroupId;
    }

    public String getFileName() {
        return fileName;
    }

    public String getFileSize() {
        return fileSize;
    }

    public String getSubmittedYn() {
        return submittedYn;
    }
}
