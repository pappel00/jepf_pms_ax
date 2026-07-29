package com.platform.module.file.domain;

import com.platform.framework.core.common.utils.PrefixedIdGenerator;
import com.platform.framework.security.tenant.TenantAwareEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * 첨부파일 그룹(첨부그룹). 사진 속 BackOffice "Data Management > 첨부파일 그룹 관리" 화면의
 * 상위(그룹) 대응. 예: groupCode="NOTICE_ATTACH" — 이 그룹 아래에 FileItem으로 실제 첨부파일
 * 메타데이터가 달린다.
 *
 * ⚠️ 이 모듈은 실제 파일 업로드/스토리지(S3, 로컬 디스크 등)를 다루지 않는다 — DataService가
 * 실제 HTTP 호출을 안 하고 URL 메타데이터만 관리하는 것과 같은 이유로, FileItem도 파일
 * 메타데이터(파일명/경로/크기)만 관리한다. 실제 업로드 연동은 다음 단계 과제로 남겨둔다.
 */
@Entity
@Table(name = "cmn_file_group",
        uniqueConstraints = @UniqueConstraint(name = "uk_file_group_tenant_code", columnNames = {"tenant_id", "group_code"}))
public class FileGroup extends TenantAwareEntity {

    @Column(name = "group_code", length = 50, nullable = false)
    private String groupCode;

    @Column(name = "group_name", length = 200, nullable = false)
    private String groupName;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "use_yn", length = 1, nullable = false)
    private String useYn = "Y";

    protected FileGroup() {
    }

    private FileGroup(String groupCode, String groupName, String description) {
        super(PrefixedIdGenerator.generate("FGR"));
        this.groupCode = groupCode;
        this.groupName = groupName;
        this.description = description;
    }

    public static FileGroup create(String groupCode, String groupName, String description) {
        return new FileGroup(groupCode, groupName, description);
    }

    public void update(String groupName, String description) {
        this.groupName = groupName;
        this.description = description;
    }

    public void activate() {
        this.useYn = "Y";
    }

    public void deactivate() {
        this.useYn = "N";
    }

    public String getGroupCode() {
        return groupCode;
    }

    public String getGroupName() {
        return groupName;
    }

    public String getDescription() {
        return description;
    }

    public String getUseYn() {
        return useYn;
    }
}
