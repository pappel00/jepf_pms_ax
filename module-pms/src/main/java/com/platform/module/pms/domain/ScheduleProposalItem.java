package com.platform.module.pms.domain;

import com.platform.framework.core.common.utils.PrefixedIdGenerator;
import com.platform.framework.security.tenant.TenantAwareEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalDate;

/**
 * 일정 조정 제안 대상 업무와 변경 전후 일정
 * 테이블: pms_schedule_proposal_item (docs/pms/03-ERD.md). 생성된 골격이며, 상태 전이 등 도메인 메서드는 서비스 구현 시 추가한다.
 */
@Entity
@Table(name = "pms_schedule_proposal_item")
public class ScheduleProposalItem extends TenantAwareEntity {

    /** 일정 조정 제안 ID */
    @Column(name = "proposal_id", length = 64, nullable = false)
    private String proposalId;

    /** WBS 업무 ID */
    @Column(name = "task_id", length = 64, nullable = false)
    private String taskId;

    /** 변경 전 시작일 */
    @Column(name = "old_start")
    private LocalDate oldStart;

    /** 변경 전 종료일 */
    @Column(name = "old_end")
    private LocalDate oldEnd;

    /** 변경 후 시작일 */
    @Column(name = "new_start")
    private LocalDate newStart;

    /** 변경 후 종료일 */
    @Column(name = "new_end")
    private LocalDate newEnd;

    /** 반영 여부 */
    @Column(name = "applied_yn", length = 1, nullable = false)
    private String appliedYn = "N";

    protected ScheduleProposalItem() {
    }

    private ScheduleProposalItem(String id) {
        super(id);
    }

    public static ScheduleProposalItem create() {
        return new ScheduleProposalItem(PrefixedIdGenerator.generate("SPI"));
    }

    public ScheduleProposalItem withProposalId(String value) {
        this.proposalId = value;
        return this;
    }

    public ScheduleProposalItem withTaskId(String value) {
        this.taskId = value;
        return this;
    }

    public ScheduleProposalItem withOldStart(LocalDate value) {
        this.oldStart = value;
        return this;
    }

    public ScheduleProposalItem withOldEnd(LocalDate value) {
        this.oldEnd = value;
        return this;
    }

    public ScheduleProposalItem withNewStart(LocalDate value) {
        this.newStart = value;
        return this;
    }

    public ScheduleProposalItem withNewEnd(LocalDate value) {
        this.newEnd = value;
        return this;
    }

    public ScheduleProposalItem withAppliedYn(String value) {
        this.appliedYn = value;
        return this;
    }

    public String getProposalId() {
        return proposalId;
    }

    public String getTaskId() {
        return taskId;
    }

    public LocalDate getOldStart() {
        return oldStart;
    }

    public LocalDate getOldEnd() {
        return oldEnd;
    }

    public LocalDate getNewStart() {
        return newStart;
    }

    public LocalDate getNewEnd() {
        return newEnd;
    }

    public String getAppliedYn() {
        return appliedYn;
    }
}
