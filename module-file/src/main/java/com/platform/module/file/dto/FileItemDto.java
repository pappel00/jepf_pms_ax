package com.platform.module.file.dto;

import com.platform.module.file.domain.FileItem;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class FileItemDto {

    private FileItemDto() {
    }

    @Schema(name = "FileItemCreateRequest")
    public record CreateRequest(
            @NotBlank @Size(max = 300) @Schema(example = "guide.pdf") String fileName,
            @NotBlank @Size(max = 1000) @Schema(example = "/files/notice/guide.pdf") String filePath,
            @Schema(example = "204800") Long fileSize,
            @Schema(example = "1", defaultValue = "0") Integer sortOrder,
            @Size(max = 500) @Schema(example = "이용 안내 파일") String description
    ) {
        /** sortOrder를 안 보내면(null) 0으로 취급한다 — Menu/CodeDetail의 CreateRequest와 동일한 이유. */
        public CreateRequest {
            if (sortOrder == null) {
                sortOrder = 0;
            }
        }
    }

    @Schema(name = "FileItemUpdateRequest")
    public record UpdateRequest(
            @NotBlank @Size(max = 300) @Schema(example = "guide.pdf") String fileName,
            @NotBlank @Size(max = 1000) @Schema(example = "/files/notice/guide.pdf") String filePath,
            @Schema(example = "204800") Long fileSize,
            @Schema(example = "1", defaultValue = "0") Integer sortOrder,
            @Size(max = 500) @Schema(example = "이용 안내 파일") String description,
            @NotBlank @Schema(example = "Y") String useYn
    ) {
        public UpdateRequest {
            if (sortOrder == null) {
                sortOrder = 0;
            }
        }
    }

    @Schema(name = "FileItemResponse")
    public record Response(
            String id,
            String tenantId,
            String groupId,
            String fileName,
            String filePath,
            Long fileSize,
            int sortOrder,
            String description,
            String useYn,
            String deletedYn
    ) {
        public static Response from(FileItem fileItem) {
            return new Response(
                    fileItem.getId(),
                    fileItem.getTenantId(),
                    fileItem.getGroupId(),
                    fileItem.getFileName(),
                    fileItem.getFilePath(),
                    fileItem.getFileSize(),
                    fileItem.getSortOrder(),
                    fileItem.getDescription(),
                    fileItem.getUseYn(),
                    fileItem.getDeletedYn()
            );
        }
    }
}
