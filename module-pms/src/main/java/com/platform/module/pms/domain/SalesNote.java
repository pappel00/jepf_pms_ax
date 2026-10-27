package com.platform.module.pms.domain;

import com.platform.framework.core.common.utils.PrefixedIdGenerator;
import com.platform.framework.security.tenant.TenantAwareEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDate;

/**
 * 영업 노트(영업 활동 기록)
 * 테이블: pms_sales_note (docs/pms/03-ERD.md). 생성된 골격이며, 상태 전이 등 도메인 메서드는 서비스 구현 시 추가한다.
 */
@Entity
@Table(name = "pms_sales_note", uniqueConstraints = {
        @UniqueConstraint(name = "uk_pms_note_code", columnNames = {"tenant_id", "note_code"})
})
public class SalesNote extends TenantAwareEntity {

    /** 노트 코드 (SN-###) */
    @Column(name = "note_code", length = 30, nullable = false)
    private String noteCode;

    /** 활동일 */
    @Column(name = "note_date", nullable = false)
    private LocalDate noteDate;

    /** 유형 (PMS_NOTE_TYPE) */
    @Column(name = "note_type_code", length = 30, nullable = false)
    private String noteTypeCode;

    /** 작성자 */
    @Column(name = "author_user_id", length = 64, nullable = false)
    private String authorUserId;

    /** 프로젝트 ID (프로젝트 또는 고객사 중 하나 필수) */
    @Column(name = "project_id", length = 64)
    private String projectId;

    /** 고객사 ID */
    @Column(name = "customer_id", length = 64)
    private String customerId;

    /** 만난 고객사 담당자 ID */
    @Column(name = "contact_id", length = 64)
    private String contactId;

    /** 제목 */
    @Column(name = "title", length = 300, nullable = false)
    private String title;

    /** 내용 */
    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    /** 다음 할 일 */
    @Column(name = "next_action", length = 500)
    private String nextAction;

    /** 다음 할 일 예정일 */
    @Column(name = "next_date")
    private LocalDate nextDate;

    /** 후속 조치 완료 여부 */
    @Column(name = "done_yn", length = 1, nullable = false)
    private String doneYn = "N";

    protected SalesNote() {
    }

    private SalesNote(String id) {
        super(id);
    }

    public static SalesNote create() {
        return new SalesNote(PrefixedIdGenerator.generate("SNT"));
    }

    public SalesNote withNoteCode(String value) {
        this.noteCode = value;
        return this;
    }

    public SalesNote withNoteDate(LocalDate value) {
        this.noteDate = value;
        return this;
    }

    public SalesNote withNoteTypeCode(String value) {
        this.noteTypeCode = value;
        return this;
    }

    public SalesNote withAuthorUserId(String value) {
        this.authorUserId = value;
        return this;
    }

    public SalesNote withProjectId(String value) {
        this.projectId = value;
        return this;
    }

    public SalesNote withCustomerId(String value) {
        this.customerId = value;
        return this;
    }

    public SalesNote withContactId(String value) {
        this.contactId = value;
        return this;
    }

    public SalesNote withTitle(String value) {
        this.title = value;
        return this;
    }

    public SalesNote withContent(String value) {
        this.content = value;
        return this;
    }

    public SalesNote withNextAction(String value) {
        this.nextAction = value;
        return this;
    }

    public SalesNote withNextDate(LocalDate value) {
        this.nextDate = value;
        return this;
    }

    public SalesNote withDoneYn(String value) {
        this.doneYn = value;
        return this;
    }

    public String getNoteCode() {
        return noteCode;
    }

    public LocalDate getNoteDate() {
        return noteDate;
    }

    public String getNoteTypeCode() {
        return noteTypeCode;
    }

    public String getAuthorUserId() {
        return authorUserId;
    }

    public String getProjectId() {
        return projectId;
    }

    public String getCustomerId() {
        return customerId;
    }

    public String getContactId() {
        return contactId;
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public String getNextAction() {
        return nextAction;
    }

    public LocalDate getNextDate() {
        return nextDate;
    }

    public String getDoneYn() {
        return doneYn;
    }
}
