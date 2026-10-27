package com.platform.module.pms.domain;

import com.platform.framework.core.common.utils.PrefixedIdGenerator;
import com.platform.framework.security.tenant.TenantAwareEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * RFP 항목(세분화된 요구사항) + 확정 M/D
 * 테이블: pms_rfp_item (docs/pms/03-ERD.md). 생성된 골격이며, 상태 전이 등 도메인 메서드는 서비스 구현 시 추가한다.
 */
@Entity
@Table(name = "pms_rfp_item", uniqueConstraints = {
        @UniqueConstraint(name = "uk_pms_rfp_item_code", columnNames = {"tenant_id", "rfp_id", "item_code"})
})
public class RfpItem extends TenantAwareEntity {

    /** RFP ID */
    @Column(name = "rfp_id", length = 64, nullable = false)
    private String rfpId;

    /** 항목 코드 (분류-일련번호) */
    @Column(name = "item_code", length = 30, nullable = false)
    private String itemCode;

    /** 원문 분류 SFR/PER/SER/ECR/DAR/QUR/COR/PMR/PSR */
    @Column(name = "category_code", length = 10, nullable = false)
    private String categoryCode;

    /** 영역 */
    @Column(name = "area", length = 100)
    private String area;

    /** 항목명 */
    @Column(name = "item_name", length = 300, nullable = false)
    private String itemName;

    /** 요구사항 원문 */
    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    /** 원문 페이지 */
    @Column(name = "source_page")
    private Integer sourcePage;

    /** 필수 여부 */
    @Column(name = "mandatory_yn", length = 1, nullable = false)
    private String mandatoryYn = "N";

    /** 정렬 */
    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;

    /** 확정 평가자 (확정 M/D의 출처) */
    @Column(name = "confirmed_evaluator_user_id", length = 64)
    private String confirmedEvaluatorUserId;

    /** 확정 M/D (원가 산정용, 수정 가능) */
    @Column(name = "confirmed_md", precision = 6, scale = 2)
    private BigDecimal confirmedMd;

    /** 확정 평가자 값에서 수정됨 */
    @Column(name = "confirmed_edited_yn", length = 1)
    private String confirmedEditedYn;

    /** 확정 처리자 */
    @Column(name = "confirmed_by", length = 64)
    private String confirmedBy;

    /** 확정 일시 */
    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;

    protected RfpItem() {
    }

    private RfpItem(String id) {
        super(id);
    }

    public static RfpItem create() {
        return new RfpItem(PrefixedIdGenerator.generate("RFI"));
    }

    public RfpItem withRfpId(String value) {
        this.rfpId = value;
        return this;
    }

    public RfpItem withItemCode(String value) {
        this.itemCode = value;
        return this;
    }

    public RfpItem withCategoryCode(String value) {
        this.categoryCode = value;
        return this;
    }

    public RfpItem withArea(String value) {
        this.area = value;
        return this;
    }

    public RfpItem withItemName(String value) {
        this.itemName = value;
        return this;
    }

    public RfpItem withContent(String value) {
        this.content = value;
        return this;
    }

    public RfpItem withSourcePage(Integer value) {
        this.sourcePage = value;
        return this;
    }

    public RfpItem withMandatoryYn(String value) {
        this.mandatoryYn = value;
        return this;
    }

    public RfpItem withSortOrder(Integer value) {
        this.sortOrder = value;
        return this;
    }

    public RfpItem withConfirmedEvaluatorUserId(String value) {
        this.confirmedEvaluatorUserId = value;
        return this;
    }

    public RfpItem withConfirmedMd(BigDecimal value) {
        this.confirmedMd = value;
        return this;
    }

    public RfpItem withConfirmedEditedYn(String value) {
        this.confirmedEditedYn = value;
        return this;
    }

    public RfpItem withConfirmedBy(String value) {
        this.confirmedBy = value;
        return this;
    }

    public RfpItem withConfirmedAt(LocalDateTime value) {
        this.confirmedAt = value;
        return this;
    }

    public String getRfpId() {
        return rfpId;
    }

    public String getItemCode() {
        return itemCode;
    }

    public String getCategoryCode() {
        return categoryCode;
    }

    public String getArea() {
        return area;
    }

    public String getItemName() {
        return itemName;
    }

    public String getContent() {
        return content;
    }

    public Integer getSourcePage() {
        return sourcePage;
    }

    public String getMandatoryYn() {
        return mandatoryYn;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public String getConfirmedEvaluatorUserId() {
        return confirmedEvaluatorUserId;
    }

    public BigDecimal getConfirmedMd() {
        return confirmedMd;
    }

    public String getConfirmedEditedYn() {
        return confirmedEditedYn;
    }

    public String getConfirmedBy() {
        return confirmedBy;
    }

    public LocalDateTime getConfirmedAt() {
        return confirmedAt;
    }
}
