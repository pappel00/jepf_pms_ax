package com.platform.framework.core.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Version;
import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * 모든 엔티티의 최상위 공통 클래스.
 *
 * MAD 4800. Database 설계 규칙:
 *  - PK: DB Auto Increment 미사용. 애플리케이션(JPA) 레벨에서 UUID를 직접 생성하여 채운다.
 *  - Audit: 등록자 / 등록일시 / 수정자 / 수정일시 자동 기록 (Spring Data JPA Auditing)
 *  - Soft Delete: 실제 DELETE 대신 deletedYn 플래그로 논리 삭제 처리
 *  - Version: 낙관적 락(Optimistic Lock)
 *
 * 하위 엔티티는 기본적으로 이 클래스를 상속하며, 모듈별 PK 접두사가 필요한 경우
 * {@link com.platform.framework.core.common.utils.PrefixedIdGenerator} 를 사용해
 * 생성자에서 id 값을 재설정한다. (예: USR_xxxxxxxx)
 */
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {

    @Id
    @Column(name = "id", length = 64, updatable = false, nullable = false)
    protected String id = UUID.randomUUID().toString();

    @CreatedBy
    @Column(name = "created_by", length = 64, updatable = false)
    protected String createdBy;

    @CreatedDate
    @Column(name = "created_date", updatable = false)
    protected LocalDateTime createdDate;

    @LastModifiedBy
    @Column(name = "modified_by", length = 64)
    protected String modifiedBy;

    @LastModifiedDate
    @Column(name = "modified_date")
    protected LocalDateTime modifiedDate;

    @Version
    @Column(name = "version")
    protected Long version;

    @Column(name = "deleted_yn", length = 1, nullable = false)
    protected String deletedYn = "N";

    @Column(name = "deleted_date")
    protected LocalDateTime deletedDate;

    protected BaseEntity() {
    }

    /** 모듈별 PK 접두사를 사용하는 하위 엔티티를 위한 생성자. */
    protected BaseEntity(String id) {
        this.id = id;
    }

    public void markDeleted() {
        this.deletedYn = "Y";
        this.deletedDate = LocalDateTime.now();
    }

    public boolean isDeleted() {
        return "Y".equals(this.deletedYn);
    }

    public String getId() {
        return id;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public String getModifiedBy() {
        return modifiedBy;
    }

    public LocalDateTime getModifiedDate() {
        return modifiedDate;
    }

    public Long getVersion() {
        return version;
    }

    public String getDeletedYn() {
        return deletedYn;
    }

    public LocalDateTime getDeletedDate() {
        return deletedDate;
    }
}
