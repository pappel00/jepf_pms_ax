package com.platform.module.pms.domain;

import com.platform.framework.core.common.utils.PrefixedIdGenerator;
import com.platform.framework.security.tenant.TenantAwareEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDate;

/**
 * RFP(제안요청서, 차수 단위)
 * 테이블: pms_rfp (docs/pms/03-ERD.md). 생성된 골격이며, 상태 전이 등 도메인 메서드는 서비스 구현 시 추가한다.
 */
@Entity
@Table(name = "pms_rfp", uniqueConstraints = {
        @UniqueConstraint(name = "uk_pms_rfp_code", columnNames = {"tenant_id", "rfp_code"})
})
public class Rfp extends TenantAwareEntity {

    /** RFP 코드 (RFP-연도-###) */
    @Column(name = "rfp_code", length = 30, nullable = false)
    private String rfpCode;

    /** 프로젝트 ID */
    @Column(name = "project_id", length = 64, nullable = false)
    private String projectId;

    /** RFP명 */
    @Column(name = "title", length = 300, nullable = false)
    private String title;

    /** 차수: 1차/2차/재공고/변경 공고 */
    @Column(name = "round_code", length = 30, nullable = false)
    private String roundCode;

    /** 접수일 */
    @Column(name = "received_date")
    private LocalDate receivedDate;

    /** 제출 마감일 */
    @Column(name = "due_date")
    private LocalDate dueDate;

    /** 원본 문서 (cmn_file_group.id) */
    @Column(name = "file_group_id", length = 64)
    private String fileGroupId;

    protected Rfp() {
    }

    private Rfp(String id) {
        super(id);
    }

    public static Rfp create() {
        return new Rfp(PrefixedIdGenerator.generate("RFP"));
    }

    public Rfp withRfpCode(String value) {
        this.rfpCode = value;
        return this;
    }

    public Rfp withProjectId(String value) {
        this.projectId = value;
        return this;
    }

    public Rfp withTitle(String value) {
        this.title = value;
        return this;
    }

    public Rfp withRoundCode(String value) {
        this.roundCode = value;
        return this;
    }

    public Rfp withReceivedDate(LocalDate value) {
        this.receivedDate = value;
        return this;
    }

    public Rfp withDueDate(LocalDate value) {
        this.dueDate = value;
        return this;
    }

    public Rfp withFileGroupId(String value) {
        this.fileGroupId = value;
        return this;
    }

    public String getRfpCode() {
        return rfpCode;
    }

    public String getProjectId() {
        return projectId;
    }

    public String getTitle() {
        return title;
    }

    public String getRoundCode() {
        return roundCode;
    }

    public LocalDate getReceivedDate() {
        return receivedDate;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public String getFileGroupId() {
        return fileGroupId;
    }
}
