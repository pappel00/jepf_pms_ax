package com.platform.module.file.dto;

import com.platform.module.file.domain.FileGroup;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class FileGroupDto {

    private FileGroupDto() {
    }

    @Schema(name = "FileGroupCreateRequest")
    public record CreateRequest(
            @NotBlank @Size(max = 50) @Schema(example = "NOTICE_ATTACH") String groupCode,
            @NotBlank @Size(max = 200) @Schema(example = "공지사항 첨부파일") String groupName,
            @Size(max = 500) @Schema(example = "공지사항 게시글에 첨부되는 파일 그룹") String description
    ) {
    }

    @Schema(name = "FileGroupUpdateRequest")
    public record UpdateRequest(
            @NotBlank @Size(max = 200) @Schema(example = "공지사항 첨부파일") String groupName,
            @Size(max = 500) @Schema(example = "공지사항 게시글에 첨부되는 파일 그룹") String description,
            @NotBlank @Schema(example = "Y") String useYn
    ) {
    }

    @Schema(name = "FileGroupResponse")
    public record Response(
            String id,
            String tenantId,
            String groupCode,
            String groupName,
            String description,
            String useYn,
            String deletedYn
    ) {
        public static Response from(FileGroup fileGroup) {
            return new Response(
                    fileGroup.getId(),
                    fileGroup.getTenantId(),
                    fileGroup.getGroupCode(),
                    fileGroup.getGroupName(),
                    fileGroup.getDescription(),
                    fileGroup.getUseYn(),
                    fileGroup.getDeletedYn()
            );
        }
    }
}
